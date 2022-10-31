package actions;

import javax.swing.*;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.GetEdgeWorkerProfilingDataDialog;
import utils.EdgeworkerWrapper;
import java.util.Arrays;

public class GetEdgeWorkerProfilingDataAction extends AnAction {

    public GetEdgeWorkerProfilingDataAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (!new EdgeworkerWrapper().checkIfAkamaiCliInstalled()) {
            return;
        }
        String edgeWorkerURL;
        String eventHandler;
        String filePath;
        String fileName;
        String [][] headers;
        try {
            GetEdgeWorkerProfilingDataDialog dialog = new GetEdgeWorkerProfilingDataDialog();
            if (null == dialog.getEdgeWorkerURL() || null == dialog.getSelectedEventHandler()) {
                return;
            }
            if (dialog.showAndGet()) {
                //ok button pressed
                eventHandler = dialog.getSelectedEventHandler();
                edgeWorkerURL = dialog.getEdgeWorkerURL();
                filePath = dialog.getFilePath();
                fileName = dialog.getFileName();
                headers = dialog.getHeaders();
                //EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());

                System.out.println(edgeWorkerURL + " " + eventHandler + " " + filePath + " " + fileName + " " + Arrays.deepToString(headers));
                // staging override
                // get secure trace token
                // make http call
                // convert profile to html & js
                    // String pathToHtml = edgeworkerWrapper.getEdgeWorkerProfilingHtml(edgeWorkerURL, eventHandler);
                    // bundling speedscope???
                // render html
            } else {
                //cancel button pressed
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
