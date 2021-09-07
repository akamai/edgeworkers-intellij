package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.EdgeworkerWrapper;

import javax.swing.*;
import java.util.ResourceBundle;

public class DownloadEdgeWorkerAction extends AnAction {

    private String edgeWorkerId;
    private String edgeWorkerVersion;
    private ResourceBundle resourceBundle;

    public DownloadEdgeWorkerAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(event.getProject());
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooserDescriptor.setTitle(resourceBundle.getString("action.downloadEdgeWorker.folderChooser.title"));
        fileChooserDescriptor.setDescription(resourceBundle.getString("action.downloadEdgeWorker.folderChooser.desc"));
        fileChooserDescriptor.setShowFileSystemRoots(true);
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        TextFieldWithBrowseButton textFieldWithBrowseButton = new TextFieldWithBrowseButton();
        textFieldWithBrowseButton.addBrowseFolderListener(new TextBrowseFolderListener(fileChooserDescriptor));
        VirtualFile[] vfs = FileChooser.chooseFiles(fileChooserDescriptor, event.getProject(), null);
        if(null==vfs || vfs.length==0){
            return;
        }
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Integer exitCode = edgeworkerWrapper.downloadEdgeWorker(edgeWorkerId, edgeWorkerVersion, vfs[0].getCanonicalPath());
                            VfsUtil.markDirtyAndRefresh(false, false, true, vfs[0]);
                            if(null == exitCode || !exitCode.equals(0)){
                                System.out.println("Downloading EdgeWorker failed!");
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }, "Downloading...", false, event.getProject());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

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
}
