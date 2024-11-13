package actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.RegisterEdgeWorkerDialog;
import utils.EdgeworkerWrapper;

import javax.swing.*;
import java.util.ResourceBundle;

public class RegisterEdgeWorkerAction extends AnAction {

    private ResourceBundle resourceBundle;

    public RegisterEdgeWorkerAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if(new EdgeworkerWrapper().checkIfAkamaiCliInstalled()==false){
            return;
        }
        String groupId = null;
        String edgeWorkerName = null;
        Integer resourceTierId = null;
        try {
            RegisterEdgeWorkerDialog dialog = new RegisterEdgeWorkerDialog();
            if(null==dialog.getSelectedGroupId()){
                return;
            }
            if(dialog.showAndGet()){
                //ok button pressed
                groupId = dialog.getSelectedGroupId();
                edgeWorkerName = dialog.getEdgeWorkerName();
                resourceTierId = dialog.getSelectedResourceTierId();
                EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());
                if(null==edgeWorkerName){
                    Messages.showErrorDialog("The EdgeWorker Name can't be empty.", "Error");
                    return;
                }
                if(null==groupId){
                    Messages.showErrorDialog("The EdgeWorker Group can't be empty.", "Error");
                    return;
                }
                if(null==resourceTierId){
                    Messages.showErrorDialog("The Resource Tier can't be empty.", "Error");
                    return;
                }
                System.out.println(groupId+" "+edgeWorkerName+" "+resourceTierId);
                edgeworkerWrapper.registerEdgeWorker(groupId, edgeWorkerName, resourceTierId);
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
