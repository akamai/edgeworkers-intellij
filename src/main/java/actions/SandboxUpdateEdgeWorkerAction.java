package actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.EdgeWorkerIdListDropdownInputDialog;
import utils.EdgeworkerWrapper;
import javax.swing.*;
import java.io.IOException;

public class SandboxUpdateEdgeWorkerAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
        event.getPresentation().setEnabledAndVisible(null != event.getProject() && null != psiFile && psiFile.getName().endsWith(".tgz"));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public SandboxUpdateEdgeWorkerAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(e.getProject());
        if(edgeworkerWrapper.checkIfAkamaiCliInstalled()==false){
            return;
        }
        PsiFile psiFile =  e.getData(CommonDataKeys.PSI_FILE);
        try {
            EdgeWorkerIdListDropdownInputDialog inputDialog = new EdgeWorkerIdListDropdownInputDialog();
            String eid = inputDialog.getSelectedItem();
            if(null==eid){
                return;
            }
            if(inputDialog.showAndGet()){
                //ok button pressed
                try {
                    eid = inputDialog.getSelectedItem();
                }catch (Exception ex){
                    ex.printStackTrace();
                    Messages.showErrorDialog("Invalid EdgeWorker ID selected.", "Error");
                    return;
                }
            }else {
                //cancel button pressed
                return;
            }
            if(null!=eid){
                edgeworkerWrapper.updateEdgeWorkerToSandbox(eid, psiFile.getVirtualFile().getCanonicalPath());
            }else {
                System.out.println("Error: EdgeWorker ID can't be null.");
            }

        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
