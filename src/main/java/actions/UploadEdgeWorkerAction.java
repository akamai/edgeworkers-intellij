package actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.EdgeWorkerIdInputDialog;
import utils.EdgeworkerWrapper;

import javax.swing.*;
import java.util.ResourceBundle;

public class UploadEdgeWorkerAction extends AnAction {

    private ResourceBundle resourceBundle;

    public UploadEdgeWorkerAction() {
        super();
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }

    public UploadEdgeWorkerAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        // make the action menu item visible if the file is tgz
        PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
        if(event.isFromActionToolbar()){
            event.getPresentation().setEnabledAndVisible(true);
        }else{
            event.getPresentation().setEnabledAndVisible(null != event.getProject() && null != psiFile && psiFile.getName().endsWith(".tgz"));
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(event.getProject());
        if(edgeworkerWrapper.checkIfAkamaiCliInstalled()==false){
            return;
        }
        String bundlePath;
        if(event.isFromActionToolbar()){
            // prompt user to select path to EdgeWorker tgz file
            FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
            fileChooserDescriptor.setTitle(resourceBundle.getString("action.uploadEdgeWorker.fileChooser.title"));
            fileChooserDescriptor.setDescription(resourceBundle.getString("action.uploadEdgeWorker.fileChooser.desc"));
            fileChooserDescriptor.setForcedToUseIdeaFileChooser(true);
            fileChooserDescriptor.withFileFilter(new Condition<VirtualFile>() {
                @Override
                public boolean value(VirtualFile virtualFile) {
                    return null != virtualFile.getExtension() && virtualFile.getExtension().equals("tgz");
                }
            });
            VirtualFile vfs = FileChooser.chooseFile(fileChooserDescriptor, event.getProject(), null);
            if(null!=vfs){
                bundlePath = vfs.getCanonicalPath();
            }else{
                System.out.println("No EdgeWorker file selected");
                return;
            }
        }else{
            PsiFile psiFile =  event.getData(CommonDataKeys.PSI_FILE);
            if(null != psiFile && null != psiFile.getVirtualFile()){
                bundlePath = psiFile.getVirtualFile().getCanonicalPath();
            }else{
                System.out.println("Bundle path can't be null or empty");
                return;
            }
        }

        // prompt user to enter EdgeWorker ID
        EdgeWorkerIdInputDialog inputDialog = new EdgeWorkerIdInputDialog();
        Long eid = null;
        if(inputDialog.showAndGet()){
            //ok button pressed
            try {
                eid = inputDialog.getEdgeWorkerId();
            }catch (Exception ex){
                ex.printStackTrace();
                Messages.showErrorDialog("Invalid EdgeWorker ID entered.", "Error");
                return;
            }
        }else {
            //cancel button pressed
            return;
        }
        if(null==eid){
            Messages.showErrorDialog("EdgeWorker Id can't be null or empty.", "Error");
            return;
        }

        try {
            edgeworkerWrapper.uploadEdgeWorker(eid.toString(), bundlePath);
            //refresh EdgeWorkers list
            ActionManager actionManager = ActionManager.getInstance();
            if(null!=actionManager.getAction(resourceBundle.getString("action.listEdgeWorkers.id"))){
                ListEdgeWorkersAction listEdgeWorkersAction = (ListEdgeWorkersAction) actionManager.getAction(resourceBundle.getString("action.listEdgeWorkers.id"));
                //get data context for listEdgeWorkersAction's refresh button on toolbar
                DataContext dataContext = DataManager.getInstance().getDataContext(listEdgeWorkersAction.getPanel().getToolbar().getComponent(0));
                listEdgeWorkersAction.actionPerformed(new AnActionEvent(null,
                        dataContext,
                        ActionPlaces.UNKNOWN,
                        new Presentation(),
                        ActionManager.getInstance(),0 ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
