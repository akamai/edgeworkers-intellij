package actions;

import com.intellij.json.JsonFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.CreateBundleUI;
import utils.EdgeworkerWrapper;
import javax.swing.*;

public class CreateBundleAction extends AnAction {

    public CreateBundleAction() {
        super();
    }

    public CreateBundleAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // make the action item visible if the filetype is JSON
        PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
        event.getPresentation().setEnabledAndVisible(null != event.getProject() && null != psiFile && psiFile.getName().equals("bundle.json"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        CreateBundleUI createBundleUI = new CreateBundleUI();
        VirtualFile[] filesToBeCompressed = createBundleUI.filesChooser(event);
        if (null == filesToBeCompressed || filesToBeCompressed.length==0){
            Messages.showInfoMessage("No file selected for code bundle", "Info");
            return;
        }

        VirtualFile tarballFileLocation = createBundleUI.destinationFolderChooser(event);
        if (null == tarballFileLocation){
            Messages.showInfoMessage("No folder selected for code bundle location", "Info");
            return;
        }

        PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
        VirtualFile parentPath = psiFile.getVirtualFile().getParent();
        String currentActionDirectory = parentPath.getCanonicalPath();

        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(project);

        try {
            edgeworkerWrapper.createAndValidateBundle(currentActionDirectory, filesToBeCompressed, tarballFileLocation);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
