package actions;

import com.intellij.openapi.actionSystem.*;
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
import ui.EdgeWorkerNotification;
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
        EdgeworkerWrapper edgeworkerWrapper = getEdgeWorkerWrapper(event);
        if(edgeworkerWrapper.checkIfAkamaiCliInstalled()==false){
            return;
        }
        FileChooserDescriptor fileChooserDescriptor = getFileChooserDescriptor();
        addTextFieldWithBrowseButton(fileChooserDescriptor);
        VirtualFile[] vfs = chooseDownloadPath(fileChooserDescriptor, event);
        if(null==vfs || vfs.length==0){
            return;
        }
        invokeDownloadEdgeWorkerAction(edgeworkerWrapper,  event, vfs);
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

    public EdgeworkerWrapper getEdgeWorkerWrapper(AnActionEvent event){
        return new EdgeworkerWrapper(event.getProject());
    }

    public FileChooserDescriptor getFileChooserDescriptor(){
        FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fileChooserDescriptor.setTitle(resourceBundle.getString("action.downloadEdgeWorker.folderChooser.title"));
        fileChooserDescriptor.setDescription(resourceBundle.getString("action.downloadEdgeWorker.folderChooser.desc"));
        fileChooserDescriptor.setShowFileSystemRoots(true);
        fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
        return fileChooserDescriptor;
    }

    public void addTextFieldWithBrowseButton(FileChooserDescriptor fileChooserDescriptor){
        TextFieldWithBrowseButton textFieldWithBrowseButton = new TextFieldWithBrowseButton();
        textFieldWithBrowseButton.addBrowseFolderListener(new TextBrowseFolderListener(fileChooserDescriptor));
    }

    public VirtualFile[] chooseDownloadPath(FileChooserDescriptor fileChooserDescriptor, AnActionEvent event){
        return FileChooser.chooseFiles(fileChooserDescriptor, event.getProject(), null);
    }

    public void invokeDownloadEdgeWorkerAction(EdgeworkerWrapper edgeworkerWrapper, AnActionEvent event, VirtualFile[] vfs){
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    ProgressManager.getInstance().getProgressIndicator().setText("Downloading...");
                    Integer exitCode = edgeworkerWrapper.downloadEdgeWorker(edgeWorkerId, edgeWorkerVersion, vfs[0].getCanonicalPath());
                    VfsUtil.markDirtyAndRefresh(false, false, true, vfs[0]);
                    if(null == exitCode || !exitCode.equals(0)){
                        System.out.println("Downloading EdgeWorker failed!");
                        notifyError(event, "Error: Downloading EdgeWorker failed!");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, "Download EdgeWorker", false, event.getProject());
    }

    public void notifyError(AnActionEvent event, String content){
        EdgeWorkerNotification.notifyError(event.getProject(), content);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

}
