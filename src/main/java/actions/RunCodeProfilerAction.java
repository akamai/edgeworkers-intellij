package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.CodeProfilerToolWindow;
import ui.EdgeWorkerNotification;
import utils.DnsService;
import utils.EdgeworkerWrapper;

import javax.net.ssl.HostnameVerifier;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

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
     * @param hostname String: hostname used to get staging IP address from
     * @return InetAddress: IP address of the staging hostname
     * @throws Exception thrown if we cannot get the staging IP address from the hostname
     */
    private InetAddress getStagingIp(String hostname) throws Exception {
        // Check if local url
        String[] localAddresses = new String[]{
                "127.0.0.1",
                "0.0.0.0",
                "localhost"
        };
        for (String address : localAddresses) {
            if (hostname.equals(address)) {
                return InetAddress.getLoopbackAddress();
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
                throw new Exception("Error: Unable to resolve CNAME for hostname: " + hostname);
            }
        }

        // Get staging IP
        try {
            String beginning = cname.substring(0, cname.indexOf(".net"));
            if (!beginning.endsWith("-staging")) {
                // get staging cname if needed
                cname = beginning + "-staging.net";
            }
            return InetAddress.getByName(cname);
        } catch (Exception exception) {
            throw new Exception("Error: Unable to get staging IP address for hostname : " + hostname);
        }
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

    private String callCodeProfiler(URI uri, InetAddress stagingIp, ArrayList<String[]> headers) throws Exception {
        System.setProperty("javax.net.debug", "ssl:handshake");
        HttpClient client;
        HttpGet request;
        HttpResponse response;
        HttpEntity entity;
        String jsonString = "";
        String noEventHandler = "cannot generate code profile for requested event handler. Check EdgeWorker code bundle for implemented event handlers.";

        try {
            // Disable hostname verification since we won't be modifying the hosts file
            // https://techdocs.akamai.com/api-acceleration/docs/test-stage
            HostnameVerifier hv = NoopHostnameVerifier.INSTANCE;

            // todo investigate SSL issues in EW-14599
            // certificate validation doesn't seem to change anything
            // Allow self-signed certs
//            TrustStrategy ts = new TrustSelfSignedStrategy();
//
//            //Create sslSocketFactory
//            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
//            sslContextBuilder.loadTrustMaterial(null, ts);
//            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
//                    sslContextBuilder.build(),
//                    hv
//            );

            client = HttpClients.custom()
                    .setSSLHostnameVerifier(hv)
                    .build();

            String stagingUri = uri.toString().replace(uri.getHost(), stagingIp.getHostAddress());
            request = new HttpGet(new URI(stagingUri));
            headers.forEach((x) -> request.addHeader(x[0], x[1]));

            response = client.execute(request);

            entity = response.getEntity();
            String contentType;

            if (entity == null || entity.getContentType() == null) {
                // the response is empty
                // this seems to happen when the edgeWorker runs into a limit while profiling
                throw new Exception("EdgeWorker took too long to respond.");
            } else {
                contentType = entity.getContentType().getValue();
            }

            if (contentType.contains("application/json")) {
                jsonString = EntityUtils.toString(entity);
            } else if (contentType.contains("multipart/form-data")) {
                // response provider event handler will return a multipart
                String boundary = contentType.split("(;\\s+boundary=)")[1];
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                StringBuilder sb = new StringBuilder();
                String line;

                while (reader.ready()) {
                    line = reader.readLine();
                    if (line.contains("content-disposition: form-data; name=\"cpu-profile\"")) {
                        // the next line is the break between the header and body of the section, let's skip it
                        reader.readLine();
                        line = reader.readLine();

                        while (!line.startsWith("--" + boundary) && reader.ready()) {
                            // loop in the event that profile data is changed to a multi line response in the future
                            sb.append(line).append(System.getProperty("line.separator"));
                            line = reader.readLine();
                        }
                        break;
                    }
                }
                jsonString = sb.toString();
                reader.close();
            } else if (contentType.contains("text/html")) {
                // we are getting back the actual website, not the profiling results
                throw new Exception(noEventHandler);
            }

            EntityUtils.consume(entity);

            if (jsonString.startsWith("{\"nodes\"")) {
                return jsonString;
            } else {
                throw new Exception(noEventHandler);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            throw new Exception("Error: Unable to run profiler: " + exception.getMessage());
        }
    }

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
     * Creates a new thread and runs the various steps we need to get profiling data from the hostname.
     *
     * @param edgeworkerWrapper wrapper instance to call CLI with
     * @param event             event which triggered the action
     * @param uri               uri to EdgeWorker
     * @param eventHandler      event handler to profile
     * @param filePath          profiling data directory
     * @param fileName          profiling data file name
     * @param headers           any additional headers to include in the http request
     */
    private void profileEdgeWorker(EdgeworkerWrapper edgeworkerWrapper, AnActionEvent event, URI uri, String eventHandler, String filePath, String fileName, ArrayList<String[]> headers) {
        codeProfiler.setIsLoading(true);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                // Get staging IP
                ProgressManager.getInstance().getProgressIndicator().setText("Determining staging IP address...");
                InetAddress stagingIp = getStagingIp(uri.getHost());

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
                headers.add(new String[]{"x-ew-code-profile-" + eventHandler.toLowerCase(), "on"});
                headers.add(new String[]{"user-agent", "EdgeWorkers IntelliJ Plugin"});

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
                String jsonString = callCodeProfiler(uri, stagingIp, headers);

                // Save json string to file
                String dest = saveStringToFile(filePath + fileName + ".cpuprofile", jsonString);
                EdgeWorkerNotification.notifyInfo(event.getProject(), "Successfully downloaded code profile to path: " + dest);

                // Convert profile to html & js
                // String pathToHtml = edgeworkerWrapper.getEdgeWorkerProfilingHtml(edgeWorkerURL, eventHandler);
                // bundling speedscope???
                // render html

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
        String edgeWorkerURL = codeProfiler.getEdgeWorkerURL();
        String eventHandler = codeProfiler.getSelectedEventHandler();
        URI uri;
        String filePath = codeProfiler.getFilePath();
        String fileName = codeProfiler.getFileName();
        ArrayList<String[]> headers = codeProfiler.getHeaders();

        try {
            uri = new URI(edgeWorkerURL);

            // append trailing separator to path if it's missing
            if (!filePath.endsWith(File.separator)) {
                filePath = filePath + File.separator;
            }

            profileEdgeWorker(edgeworkerWrapper, e, uri, eventHandler, filePath, fileName, headers);
        } catch (URISyntaxException ex) {
            // this should never really happen because the UI will validate the input for us
            EdgeWorkerNotification.notifyError(e.getProject(), "Error: EdgeWorker URL is an invalid URL");
        }
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
