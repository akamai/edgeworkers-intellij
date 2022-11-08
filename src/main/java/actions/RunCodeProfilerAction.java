package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import ui.CodeProfilerToolWindow;
import ui.EdgeWorkerNotification;
import utils.DnsService;
import utils.EdgeworkerWrapper;

import javax.naming.NamingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class RunCodeProfilerAction extends AnAction {
    private final CodeProfilerToolWindow codeProfiler;

    public RunCodeProfilerAction(CodeProfilerToolWindow codeProfiler) {
        super();
        this.codeProfiler = codeProfiler;
    }

    /**
     * @param hostname String: hostname used to get staging IP address from
     * @return InetAddress: IP address of the staging hostname
     * @throws NamingException      thrown if the CNAME lookup for the hostname fails
     * @throws UnknownHostException thrown if the CNAME IP address cannot be resolved
     */
    private InetAddress getStagingIp(String hostname) throws NamingException, UnknownHostException {
        if (hostname.contains("://")) {
            // remove protocol from hostname
            hostname = hostname.split("(://)")[1];
        }

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
            cname = DnsService.getCNAME(hostname)[0];
        }

        // Get staging name if needed
        String beginning = cname.substring(0, cname.indexOf(".net"));
        if (!beginning.endsWith("-staging")) {
            cname = beginning + "-staging.net";
        }

        // Get IP for CNAME
        return InetAddress.getByName(cname);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!new EdgeworkerWrapper().checkIfAkamaiCliInstalled()) {
            return;
        }
        String edgeWorkerURL = codeProfiler.getEdgeWorkerURL();
        String eventHandler = codeProfiler.getSelectedEventHandler();
        String filePath = codeProfiler.getFilePath();
        String fileName = codeProfiler.getFileName();
        String[][] headers = codeProfiler.getHeaders();
        InetAddress stagingIp;
        //EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());

        System.out.println(edgeWorkerURL + " " + eventHandler + " " + filePath + " " + fileName + " " + Arrays.deepToString(headers));
        try {
            stagingIp = getStagingIp(edgeWorkerURL);
            System.out.println(stagingIp.toString());
        } catch (Exception exception) {
            EdgeWorkerNotification.notifyError(null, "Error: Unable to get staging IP for URL: " + edgeWorkerURL);
        }
        // get secure trace token
        // make http call
        // convert profile to html & js
        // String pathToHtml = edgeworkerWrapper.getEdgeWorkerProfilingHtml(edgeWorkerURL, eventHandler);
        // bundling speedscope???
        // render html
    }
}
