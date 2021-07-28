package ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ResourceBundle;

public class CreateAndValidateBundleUI {

    private ResourceBundle resourceBundle;

    public CreateAndValidateBundleUI(){
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }
    public VirtualFile[] filesChooser(@NotNull AnActionEvent event){
        Project project = event.getProject();
        PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
        VirtualFile parentPath = psiFile.getVirtualFile().getParent();

        // prompt user to select files for creating Edgeworker code bundle
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor();
        fileChooserDescriptor.setTitle(resourceBundle.getString("action.createandvalidatebundle.dialog.filechooser.title"));
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        fileChooserDescriptor.setRoots(parentPath.getChildren());

        VirtualFile[] vfs = FileChooser.chooseFiles(fileChooserDescriptor, project, null);
        for(VirtualFile v: vfs){
            System.out.println("vff"+v.getName()+" "+v.getPresentableName());
        }
        return vfs;
    }

    public VirtualFile destinationFolderChooser(@NotNull AnActionEvent event){
        FileChooserDescriptor folderChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        folderChooserDescriptor.setTitle(resourceBundle.getString("action.createandvalidatebundle.dialog.destinationfolderchooser.title"));
        folderChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        VirtualFile vf = FileChooser.chooseFile(folderChooserDescriptor, event.getProject(), null);
        return vf;
    }
}
