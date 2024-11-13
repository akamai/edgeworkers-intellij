package actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.ActivateEdgeWorkerDialog;
import utils.EdgeworkerWrapper;
import javax.swing.*;
import java.util.ResourceBundle;

public class ActivateEdgeWorkerAction extends AnAction {

    private ResourceBundle resourceBundle;
    private ActivateEdgeWorkerDialog dialog;
    private String edgeWorkerId;
    private String edgeWorkerVersion;

    public String getEdgeWorkerId() {
        return edgeWorkerId;
    }

    public void setEdgeWorkerId(String edgeWorkerId) {
        this.edgeWorkerId = edgeWorkerId;
    }

    public String getEdgeWorkerVersion() {
        return edgeWorkerVersion;
    }

    public void setEdgeWorkerVersion(String edgeWorkerVersion) {
        this.edgeWorkerVersion = edgeWorkerVersion;
    }

    public ActivateEdgeWorkerAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }

    public ActivateEdgeWorkerDialog getDialog() {
        return dialog;
    }

    public void setDialog(ActivateEdgeWorkerDialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if(new EdgeworkerWrapper().checkIfAkamaiCliInstalled()==false){
            return;
        }
        String eid = null;
        String version = null;
        String network = null;
        try {
            if(null==dialog || dialog.isDisposed()){
                dialog = new ActivateEdgeWorkerDialog(e.getProject(), edgeWorkerId, edgeWorkerVersion);
            }
            if(null==dialog.getSelectedEdgeWorkerID()){
                dialog.disposeIfNeeded();
                return;
            }
            if(dialog.showAndGet()){
                //ok button pressed
                eid = dialog.getSelectedEdgeWorkerID();
                version = dialog.getSelectedEdgeWorkerVersion();
                network = dialog.getSelectedNetwork();
                EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());
                if(null==eid){
                    Messages.showErrorDialog("The EdgeWorker ID can't be empty", "Error");
                    return;
                }
                if(null==version){
                    Messages.showErrorDialog("The EdgeWorker version can't be empty", "Error");
                    return;
                }
                if(null==network){
                    Messages.showErrorDialog("Network can't be empty", "Error");
                    return;
                }
                System.out.println(eid+" "+version+" "+network);
                edgeworkerWrapper.activateEdgeWorker(eid, version, network);
            }else {
                //cancel button pressed
                return;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
