package actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.CodeProfilerToolWindow;
import ui.EdgeWorkerNotification;
import utils.Constants;
import utils.DnsService;
import utils.EdgeworkerWrapper;
import utils.ZipResourceExtractor;

import javax.naming.NamingException;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

public class RunCodeProfilerAction extends AnAction {
    private final CodeProfilerToolWindow codeProfiler;
    private final HashMap<String, HeaderCache> authCache;
    private final int cacheExpiryInSeconds = 115 * 60; // token expiry is 120 minutes

    public RunCodeProfilerAction(CodeProfilerToolWindow codeProfiler) {
        super();
        this.codeProfiler = codeProfiler;
        this.authCache = new HashMap<>();
    }

    /**
     * Get header name value pair from cache
     *
     * @param key cache key
     * @return String[] containing header name and value or null if the value does not exist or is expired
     */
    private String[] getAuthCache(String key) {
        HeaderCache val = authCache.get(key);
        if (val != null) {
            if (val.created > Instant.now().getEpochSecond() - cacheExpiryInSeconds) {
                return new String[]{val.name, val.value};
            } else {
                authCache.remove(key);
            }
        }
        return null;
    }

    /**
     * Add a new header name value pair to the cache
     *
     * @param key        cache key
     * @param headerName header name
     * @param value      header value
     */
    private void putAuthCache(String key, String headerName, String value) {
        authCache.put(key, new HeaderCache(headerName, value));
    }

    /**
     * @param hostname String: EdgeWorker hostname
     * @return String: Staging hostname for given EdgeWorker
     * @throws NamingException: Thrown if we cannot determine the EdgeWorker's CNAME using the system DNS
     */
    private String getStagingName(String hostname) throws NamingException {
        // Check if local url
        String[] localAddresses = new String[]{
                "127.0.0.1",
                "0.0.0.0",
                "localhost"
        };
        for (String address : localAddresses) {
            if (hostname.equals(address)) {
                return InetAddress.getLoopbackAddress().getHostName();
            }
        }

        // Get CNAME
        String[] edgeHostEndings = new String[]{
                ".edgekey.net",
                ".edgesuite.net",
                ".akamaiedge.net",
                ".akamaized.net"
        };
        String[] edgeHostStagingEndings = new String[]{
                ".edgekey-staging.net",
                ".edgesuite-staging.net",
                ".akamaiedge-staging.net",
                ".akamaized-staging.net"
        };
        String cname = null;

        for (int i = 0; i < 4; i++) {
            if (hostname.endsWith(edgeHostEndings[i]) || hostname.endsWith(edgeHostStagingEndings[i])) {
                // hostname is already the cname
                cname = hostname;
                break;
            }
        }
        if (cname == null) {
            // get cname if we don't already have it
            try {
                cname = DnsService.getCNAME(hostname)[0];
            } catch (Exception exception) {
                throw new NamingException("Error: Unable to resolve CNAME for hostname: " + hostname);
            }
        }

        // Get staging IP
        String beginning = cname.substring(0, cname.indexOf(".net"));
        if (!beginning.endsWith("-staging")) {
            // get staging cname if needed
            cname = beginning + "-staging.net";
        }
        return cname;
    }

    /**
     * Get secure trace header used to make a profiling request. Will use cached header if available.
     *
     * @param edgeworkerWrapper wrapper instance to call CLI with
     * @param hostname          hostname to get headers for
     * @return String[]: header name value pair, or null if the header could not be generated
     * @throws Exception thrown if we cannot get the secure trace headers
     */
    private @Nullable String[] getSecureTraceHeader(EdgeworkerWrapper edgeworkerWrapper, String hostname) throws Exception {
        String[] secureTraceTokenHeader = getAuthCache(hostname);
        if (secureTraceTokenHeader != null) {
            return secureTraceTokenHeader;
        }
        String edgeWorkersAuthResponseString = edgeworkerWrapper.getEdgeWorkersAuthString(hostname);
        if (edgeWorkersAuthResponseString == null) {
            return null;
        }

        // Separate header name and value since they are returned as a string by the API
        String secureTraceHeader;
        String secureTraceValue;
        try {
            secureTraceHeader = edgeWorkersAuthResponseString.substring(0, edgeWorkersAuthResponseString.indexOf(": "));
            secureTraceValue = edgeWorkersAuthResponseString.substring(edgeWorkersAuthResponseString.indexOf(": ") + 2);
        } catch (IndexOutOfBoundsException exception) {
            // this shouldn't ever really happen but is useful in the rare chance that it ever does
            throw new Exception("Error: Unable to get secure trace header: Invalid response");
        }

        // cache auth token
        putAuthCache(hostname, secureTraceHeader, secureTraceValue);
        return new String[]{secureTraceHeader, secureTraceValue};
    }

    /**
     * Make a Http request to get profiling information
     *
     * @param uri        URI to EdgeWorker
     * @param httpMethod HTTP method to profile. Must be one of {@link Constants#EW_HTTP_METHODS EW_HTTP_METHODS}.
     * @param stagingIp  IP address of EdgeWorker on staging network
     * @param headers    ArrayList containing any headers to send with the request
     * @return String: String representing the profiling data
     * @throws Exception Thrown if something goes wrong while making the http call
     */
    private String callCodeProfiler(URI uri, String httpMethod, InetAddress stagingIp, ArrayList<String[]> headers) throws Exception {
        System.setProperty("javax.net.debug", "ssl:handshake");
        HttpClient client;
        HttpUriRequest request;
        HttpResponse response;
        HttpEntity entity = null;
        String jsonString = "";
        String noEventHandler = "cannot generate code profile for requested event handler or method. Check EdgeWorker code bundle for implemented event handlers.";

        try {
            // Custom DNS resolver to use staging IP
            DnsResolver dnsResolver = new SystemDefaultDnsResolver() {
                @Override
                public InetAddress[] resolve(final String host) {
                    return new InetAddress[]{stagingIp};
                }
            };

            // Create HttpClientConnectionManager to use custom DnsResolver
            BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager(
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("http", PlainConnectionSocketFactory.getSocketFactory())
                            .register("https", SSLConnectionSocketFactory.getSocketFactory())
                            .build(),
                    null, //Default ConnectionFactory
                    null, //Default SchemePortResolver
                    dnsResolver  // Custom DnsResolver
            );

            client = HttpClientBuilder.create()
                    .setConnectionManager(connManager)
                    .build();

            switch (httpMethod) {
                case "GET":
                    request = new HttpGet(uri);
                    break;
                case "HEAD":
                    request = new HttpHead(uri);
                    break;
                case "POST":
                    request = new HttpPost(uri);
                    break;
                case "PUT":
                    request = new HttpPut(uri);
                    break;
                case "PATCH":
                    request = new HttpPatch(uri);
                    break;
                case "DELETE":
                    request = new HttpDelete(uri);
                    break;
                default:
                    throw new RuntimeException("Invalid http method, must be one of: " + Arrays.toString(Constants.EW_HTTP_METHODS));
            }

            headers.forEach((x) -> request.addHeader(x[0], x[1]));

            // max 5 attempts in case we get back blank data due to memory profiler bug
            // in most cases the first attempt has good data
            int maxAttempts = 5;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                System.out.println("Attempt " + attempt + " to get profile data");
                response = client.execute(request);

                entity = response.getEntity();
                String contentType;

                if (entity == null || entity.getContentType() == null) {
                    // the response body is empty, seems to happen when the EdgeWorker runs into a limit while profiling
                    if (httpMethod.equals("HEAD")) {
                        // not necessarily a timeout, could be just a regular HEAD request that did not return profiling info
                        throw new Exception(noEventHandler);
                    } else {
                        throw new Exception("Received null response body or EdgeWorker took too long to respond.");
                    }
                } else {
                    contentType = entity.getContentType().getValue();
                }

                if (contentType.contains("application/json")) {
                    jsonString = EntityUtils.toString(entity);
                } else if (contentType.contains("multipart/form-data")) {
                    try (InputStreamReader isr = new InputStreamReader(entity.getContent());
                         BufferedReader reader = new BufferedReader(isr)) {
                        // response provider event handler will return a multipart
                        String boundary = contentType.split("(;\\s+boundary=)")[1];
                        StringBuilder sb = new StringBuilder();
                        String line = reader.readLine();

                        while (line != null) {
                            if (line.contains("content-disposition: form-data; name=\"cpu-profile\"") ||
                                    line.contains("content-disposition: form-data; name=\"memory-profile\"")) {
                                // the next line is the break between the header and body of the section, let's skip it
                                reader.readLine();
                                line = reader.readLine();

                                while (line != null && !line.startsWith("--" + boundary)) {
                                    // loop in the event that profile data is changed to a multi line response in the future
                                    sb.append(line).append(System.getProperty("line.separator"));
                                    line = reader.readLine();
                                }
                                break;
                            } else {
                                line = reader.readLine();
                            }
                        }
                        jsonString = sb.toString();
                    }
                } else if (contentType.contains("text/html")) {
                    // we are getting back the actual website, not the profiling results
                    throw new Exception(noEventHandler);
                }

                // there is a bug causing empty memory profile to come back in some cases
                // this is usually resolved by re-attempting to get memory profile
                // if this happens we can retry up to 5 times total
                String emptyJson = "{\"head\":{\"callFrame\":{\"functionName\":\"(root)\",\"scriptId\":\"0\",\"url\":\"\",\"lineNumber\":-1,\"columnNumber\":-1},\"selfSize\":0,\"id\":1,\"children\":[]},\"samples\":[]}";

                if (jsonString.equals(emptyJson)) {
                    Thread.sleep(1000);
                    if (attempt < maxAttempts - 1) {
                        continue;   // attempt again
                    } else {
                        throw new Exception("Profiler came back with no data; please try again");
                    }
                } else if (jsonString.startsWith("{\"nodes\"") || jsonString.startsWith("{\"head\"")) {
                    return jsonString;
                } else {
                    throw new Exception(noEventHandler);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new Exception("Error: Unable to run profiler: " + exception.getMessage());
        } finally {
            EntityUtils.consume(entity);
        }

        // fallback return
        return jsonString;
    }

    /**
     * Save a string to the disk
     *
     * @param pathname Pathname String of file to be written
     * @param data     Data String to be written to disk
     * @return Absolute path of written file
     * @throws Exception Thrown if file cannot be written to pathname
     */
    private String saveStringToFile(String pathname, String data) throws Exception {
        try {
            File file = new File(pathname);
            if (file.createNewFile()) {
                Files.writeString(file.toPath(), data);
            } else {
                Files.writeString(file.toPath(), data, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return file.getAbsoluteFile().toString();
        } catch (Exception exception) {
            throw new Exception("Error: Cannot save file to " + pathname + " : " + exception.getMessage());
        }
    }

    /**
     * Convert a cpuprofile or heapprofile formatted string to a speedscope JS & HTML file
     *
     * @param fileName            file name to be used for JS file and HTML file
     * @param profileData         String representing data to convert
     * @param title               title for speedscope HTML application
     * @param speedScopeIndexPath path pointing to a speedscope standalone index.html file
     * @return File: HTML file that can be opened to view the profile data using speedscope
     * @throws IOException Thrown if files cannot be written to disk
     */
    private File convertCodeProfile(String fileName, String profileData, String title, String speedScopeIndexPath) throws IOException {
        String encodedProfile = Base64.getEncoder().encodeToString(profileData.getBytes(StandardCharsets.UTF_8));

        // create strings
        String jsString = "speedscope.loadFileFromBase64(" + "\"" + title + "\", " + "\"" + encodedProfile + "\"" + ")";
        String htmlString = "<script>window.location=\"" + "file:///" + speedScopeIndexPath + "#localProfilePath=" + Constants.JAVA_TMP_URL + fileName + ".js" + "\"</script>";

        // write strings to disk
        File htmlFile = FileUtil.createTempFile(fileName, ".html", true);
        File jsFile = FileUtil.createTempFile(fileName, ".js", true);
        Files.writeString(jsFile.toPath(), jsString);
        Files.writeString(htmlFile.toPath(), htmlString);

        // return the html file
        return htmlFile;
    }

    /**
     * Creates a new thread and runs the various steps we need to get profiling data from the hostname.
     * Will then display the results inside the IDE if supported or using the user's default browser.
     *
     * @param edgeworkerWrapper wrapper instance to call CLI with
     * @param event             event which triggered the action
     * @param profilingMode     String specifying whether to profile CPU or memory usage.
     *                          Must be one of {@link Constants#CPU_PROFILING} or {@link Constants#MEM_PROFILING}
     * @param forceColdStart    Turn on forcing cold start for profiling
     * @param uri               uri to EdgeWorker
     * @param httpMethod        what Http method to use
     * @param eventHandler      event handler to profile
     * @param filePath          profiling data directory
     * @param fileName          profiling data file name
     * @param headers           any additional headers to include in the http request
     * @param edgeIpOverride    IP address that can be used to override the IP lookup for the EdgeWorkers staging server.
     *                          Will automatically determine IP address if null.
     */
    private void profileEdgeWorker(EdgeworkerWrapper edgeworkerWrapper, AnActionEvent event, String profilingMode, boolean forceColdStart, URI uri, String httpMethod, String eventHandler, String filePath, String fileName, ArrayList<String[]> headers, @Nullable InetAddress edgeIpOverride) {
        codeProfiler.setIsLoading(true);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                ProgressManager.getInstance().getProgressIndicator().setText("Initializing...");

                // Extract bundled speedscope if needed
                String speedScopeDir = Constants.JAVA_TMP_URL + "speedscope";
                File speedScopeFolder = new File(speedScopeDir);
                String[] fileList = speedScopeFolder.list();
                if (fileList == null || fileList.length < Constants.SPEEDSCOPE_NUMBER_OF_FILES) {
                    try {
                        ZipResourceExtractor.extractZipResource(
                                RunCodeProfilerAction.class,
                                "/speedscope.zip",
                                Path.of(System.getProperty("java.io.tmpdir"))
                        );
                    } catch (IOException exception) {
                        Messages.showErrorDialog("Error: Unable to extract bundled resources to temp directory.", "Fatal Error");
                        throw new Exception();
                    }
                }

                // Get staging IP
                ProgressManager.getInstance().getProgressIndicator().setText("Determining staging IP address...");
                String stagingName = getStagingName(uri.getHost());
                InetAddress stagingIp;
                if (edgeIpOverride == null) {
                    stagingIp = InetAddress.getByName(stagingName);
                } else {
                    // use staging hostname but override IP
                    stagingIp = InetAddress.getByAddress(stagingName, edgeIpOverride.getAddress());
                }

                // Get secure trace headers
                ProgressManager.getInstance().getProgressIndicator().setText("Generating secure trace header...");
                String[] secureTraceTokenHeader = getSecureTraceHeader(edgeworkerWrapper, uri.getHost());
                if (secureTraceTokenHeader == null) {
                    // something went wrong in the wrapper when generating the header
                    // the wrapper has already displayed an error notification explaining why
                    return;
                }

                // Set headers
                headers.add(secureTraceTokenHeader);
                headers.add(new String[]{"user-agent", Constants.EW_USER_AGENT});
                headers.add(new String[]{"x-ew-code-profile-" + eventHandler.toLowerCase(), "on"});
                if (profilingMode.equals(Constants.MEM_PROFILING)) {
                    headers.add(new String[]{Constants.EW_MEM_PROFILING_HEADER, "on"});
                }

                if (forceColdStart) {
                    headers.add(new String[]{Constants.EW_COLD_START_HEADER, "on"});
                }

                // Add Host header if not already set by the user
                boolean hostExists = false;
                for (String[] header : headers) {
                    if (header[0].equals("Host")) {
                        hostExists = true;
                        break;
                    }
                }
                if (!hostExists) {
                    headers.add(new String[]{"Host", uri.getHost()});
                }

                // Make http call
                ProgressManager.getInstance().getProgressIndicator().setText("Getting profiling data...");
                String jsonString = callCodeProfiler(uri, httpMethod, stagingIp, headers);
                if (profilingMode.equals(Constants.MEM_PROFILING) && jsonString.startsWith("{\"nodes\"")) {
                    // we got back the cpuprofile and not the heapprofile, shouldn't really happen in prod
                    throw new Exception("Expected memory profile response, received CPU profile.");
                }

                // Save json string to file
                String fileExtension = profilingMode.equals(Constants.MEM_PROFILING) ? ".heapprofile" : ".cpuprofile";
                String dest = saveStringToFile(filePath + fileName + fileExtension, jsonString);
                EdgeWorkerNotification.notifyInfo(event.getProject(), "Successfully downloaded code profile to path: " + dest);

                // Convert to speedscope js & html files
                String speedScopeIndex = speedScopeDir + "/index.html";

                File htmlFile;
                try {
                    String convertName = Constants.CONVERTED_FILE_NAME + "-" + System.currentTimeMillis();
                    htmlFile = convertCodeProfile(convertName, jsonString, fileName, speedScopeIndex);
                } catch (IOException ioException) {
                    Messages.showErrorDialog("Error: Unable to convert code profile", "Fatal Error");
                    throw new Exception();
                }

                // render html
                if (JBCefApp.isSupported()) {
                    // load html file using custom file editor
                    VirtualFile virtualHtmlFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(htmlFile);
                    if (virtualHtmlFile == null) {
                        throw new Exception("Error: Unable to save profile data. Please try again.");
                    }
                    ApplicationManager.getApplication().invokeLater(() ->
                            FileEditorManager.getInstance(event.getProject()).openFile(virtualHtmlFile, true));
                } else {
                    // open file using default html application
                    Desktop.getDesktop().open(htmlFile);
                }
            } catch (Exception exception) {
                EdgeWorkerNotification.notifyError(null,
                        Objects.requireNonNullElse(exception.getMessage(), "Error: Unable to profile URL " + uri.toString()));
                exception.printStackTrace();
            } finally {
                codeProfiler.setIsLoading(false);
            }
        }, "Profile EdgeWorker", false, event.getProject());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!new EdgeworkerWrapper().checkIfAkamaiCliInstalled()) {
            return;
        }
        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper();
        String profilingMode = codeProfiler.getProfilingMode();
        boolean forceColdStart = codeProfiler.getForceColdStart();
        String edgeWorkerURL = codeProfiler.getEdgeWorkerURL();
        String httpMethod = codeProfiler.getHttpMethod();
        String eventHandler = codeProfiler.getSelectedEventHandler();
        URI uri;
        String filePath = codeProfiler.getFilePath();
        String fileName = codeProfiler.getFileName();
        ArrayList<String[]> headers = codeProfiler.getHeaders();
        InetAddress edgeIpOverride;

        // Set Headers
        headers.add(new String[]{Constants.EW_SAMPLING_HEADER, codeProfiler.getSamplingInterval()});

        try {
            uri = new URI(edgeWorkerURL);

            if (codeProfiler.getEdgeIpOverride() != null) {
                edgeIpOverride = InetAddress.getByName(codeProfiler.getEdgeIpOverride());
            } else {
                edgeIpOverride = null;
            }

            // append trailing separator to path if it's missing
            if (!filePath.endsWith(File.separator)) {
                filePath = filePath + File.separator;
            }

            profileEdgeWorker(edgeworkerWrapper, e, profilingMode, forceColdStart, uri, httpMethod, eventHandler, filePath, fileName, headers, edgeIpOverride);
        } catch (URISyntaxException ex) {
            // this should never really happen because the UI will validate the URL for us
            EdgeWorkerNotification.notifyError(e.getProject(), "Error: EdgeWorker URL is an invalid URL");
        } catch (UnknownHostException ex) {
            EdgeWorkerNotification.notifyError(e.getProject(), "Error: Edge IP override is not a valid IP address");
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    static class HeaderCache {
        protected final long created;
        protected final String name;
        protected final String value;

        public HeaderCache(String name, String value) {
            this.created = Instant.now().getEpochSecond();
            this.name = name;
            this.value = value;
        }
    }
}
