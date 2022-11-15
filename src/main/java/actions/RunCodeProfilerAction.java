package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import org.jetbrains.annotations.NotNull;
import ui.CodeProfilerToolWindow;
import ui.EdgeWorkerNotification;
import utils.DnsService;
import utils.EdgeworkerWrapper;

import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
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
     * @return String[]: header name value pair
     * @throws Exception thrown if we cannot get the secure trace headers
     */
    private String[] getSecureTraceHeader(EdgeworkerWrapper edgeworkerWrapper, String hostname) throws Exception {
        String[] secureTraceTokenHeader = getAuthCache(hostname);
        if (secureTraceTokenHeader != null) {
            return secureTraceTokenHeader;
        }
        String edgeWorkersAuthResponseString = edgeworkerWrapper.getEdgeWorkersAuthString(hostname);

        // Separate header name and value since they are returned as a string by the API
        String secureTraceHeader;
        String secureTraceValue;
        try {
            secureTraceHeader = edgeWorkersAuthResponseString.substring(0, edgeWorkersAuthResponseString.indexOf(": "));
            secureTraceValue = edgeWorkersAuthResponseString.substring(edgeWorkersAuthResponseString.indexOf(": ") + 2);
        } catch (IndexOutOfBoundsException exception) {
            // this shouldn't ever really happen but is useful in the rare chance that it ever does
            throw new Exception("Error: Unable to get enhanced debug headers: Invalid response");
        }

        // cache auth token
        putAuthCache(hostname, secureTraceHeader, secureTraceValue);
        return new String[]{secureTraceHeader, secureTraceValue};
    }

    /**
     * Creates a new thread and runs the various steps we need to get profiling data from the hostname.
     *
     * @param edgeworkerWrapper wrapper instance to call CLI with
     * @param event             event which triggered the action
     * @param hostname          hostname to profile
     * @param eventHandler      event handler to profile
     * @param filePath          profiling data directory
     * @param fileName          profiling data file name
     * @param headers           any additional headers to include in the http request
     */
    private void profileEdgeWorker(EdgeworkerWrapper edgeworkerWrapper, AnActionEvent event, String hostname, String eventHandler, String filePath, String fileName, ArrayList<String[]> headers) {
        codeProfiler.setIsLoading(true);
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                // Get staging IP
                ProgressManager.getInstance().getProgressIndicator().setText("Determining staging IP address...");
                InetAddress stagingIp = getStagingIp(hostname);

                // Get secure trace headers
                ProgressManager.getInstance().getProgressIndicator().setText("Generating enhanced debug header...");
                String[] secureTraceTokenHeader = getSecureTraceHeader(edgeworkerWrapper, hostname);
                headers.add(secureTraceTokenHeader);

                // Make http call
                //ProgressManager.getInstance().getProgressIndicator().setText("Getting profiling data...");


                System.out.println(stagingIp.toString());
                System.out.println(Arrays.toString(secureTraceTokenHeader));

                // convert profile to html & js
                // String pathToHtml = edgeworkerWrapper.getEdgeWorkerProfilingHtml(edgeWorkerURL, eventHandler);
                // bundling speedscope???
                // render html

            } catch (Exception exception) {
                EdgeWorkerNotification.notifyError(null,
                        Objects.requireNonNullElse(exception.getMessage(), "Error: Unable to profile hostname: " + hostname));
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
        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());
        String edgeWorkerURL = codeProfiler.getEdgeWorkerURL();
        String eventHandler = codeProfiler.getSelectedEventHandler();
        String filePath = codeProfiler.getFilePath();
        String fileName = codeProfiler.getFileName();
        ArrayList<String[]> headers = codeProfiler.getHeaders();
        String hostname;

        if (edgeWorkerURL.contains("://")) {
            // remove protocol from hostname
            hostname = edgeWorkerURL.split("(://)")[1];
        } else {
            hostname = edgeWorkerURL;
        }
        profileEdgeWorker(edgeworkerWrapper, e, hostname, eventHandler, filePath, fileName, headers);
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
