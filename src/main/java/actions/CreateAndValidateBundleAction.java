package actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.CreateAndValidateBundleUI;
import utils.EdgeworkerWrapper;
import javax.swing.*;

public class CreateAndValidateBundleAction extends AnAction {

    public CreateAndValidateBundleAction() {
        super();
    }

    public CreateAndValidateBundleAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // make the action item visible if the file is bundle.json
        PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
        event.getPresentation().setEnabledAndVisible(null != event.getProject() && null != psiFile && psiFile.getName().equals("bundle.json"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        EdgeworkerWrapper edgeworkerWrapper = getEdgeWorkerWrapper(event);
        if(edgeworkerWrapper.checkIfAkamaiCliInstalled()==false){
            return;
        }
        CreateAndValidateBundleUI createAndValidateBundleUI = getCreateAndValidateBundleUI();
        VirtualFile[] filesToBeCompressed = createAndValidateBundleUI.filesChooser(event);
        if (null == filesToBeCompressed || filesToBeCompressed.length==0){
            Messages.showInfoMessage("No file selected for code bundle", "Info");
            return;
        }

        VirtualFile tarballFileLocation = createAndValidateBundleUI.destinationFolderChooser(event);
        if (null == tarballFileLocation){
            Messages.showInfoMessage("No destination folder selected for code bundle location", "Info");
            return;
        }

        PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
        VirtualFile parentPath = psiFile.getVirtualFile().getParent();
        String currentActionDirectory = parentPath.getCanonicalPath();

        try {
            edgeworkerWrapper.createAndValidateBundle(currentActionDirectory, filesToBeCompressed, tarballFileLocation);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public EdgeworkerWrapper getEdgeWorkerWrapper(AnActionEvent event){
        return new EdgeworkerWrapper(event.getProject());
    }

    public CreateAndValidateBundleUI getCreateAndValidateBundleUI(){
        return new CreateAndValidateBundleUI();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
