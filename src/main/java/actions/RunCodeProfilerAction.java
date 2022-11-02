package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import ui.CodeProfilerToolWindow;
import utils.EdgeworkerWrapper;

import java.util.Arrays;

public class RunCodeProfilerAction extends AnAction {
    private final CodeProfilerToolWindow codeProfiler;

    public RunCodeProfilerAction(CodeProfilerToolWindow codeProfiler) {
        super();
        this.codeProfiler = codeProfiler;
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
        //EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());

        System.out.println(edgeWorkerURL + " " + eventHandler + " " + filePath + " " + fileName + " " + Arrays.deepToString(headers));
        // staging override
        // get secure trace token
        // make http call
        // convert profile to html & js
        // String pathToHtml = edgeworkerWrapper.getEdgeWorkerProfilingHtml(edgeWorkerURL, eventHandler);
        // bundling speedscope???
        // render html
//        try {
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }
}
