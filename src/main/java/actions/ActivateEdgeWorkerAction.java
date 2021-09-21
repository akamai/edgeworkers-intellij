package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.ActivateEdgeWorkerDialog;
import utils.EdgeworkerWrapper;
import javax.swing.*;
import java.util.ResourceBundle;

public class ActivateEdgeWorkerAction extends AnAction {

    private ResourceBundle resourceBundle;

    public ActivateEdgeWorkerAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String eid = null;
        String version = null;
        String network = null;
        try {
            ActivateEdgeWorkerDialog dialog = new ActivateEdgeWorkerDialog();
            if(dialog.showAndGet()){
                //ok button pressed
                eid = dialog.getSelectedEdgeWorkerID();
                version = dialog.getSelectedEdgeWorkerVersion();
                network = dialog.getSelectedNetwork();
                System.out.println(eid+" "+version+" "+network);
                EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());
                if(null!=eid && null!=version && null!=network){
                    edgeworkerWrapper.activateEdgeWorker(eid, version, network);
                }else{
                    Messages.showErrorDialog("The EdgeWorker ID, Version and Network can't be null", "Error");
                    return;
                }
            }else {
                //cancel button pressed
                return;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}