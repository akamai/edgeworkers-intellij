import actions.*;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ResourceBundle;

public class EdgeworkersActionGroup extends ActionGroup {

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("ActionBundle");
        return new AnAction[]{
                new CreateAndValidateBundleAction(resourceBundle.getString("action.createandvalidatebundle.title"), resourceBundle.getString("action.createandvalidatebundle.desc"), AllIcons.Actions.AddMulticaret),
                new UploadEdgeWorkerAction(resourceBundle.getString("action.uploadEdgeWorker.title"), resourceBundle.getString("action.uploadEdgeWorker.desc"), AllIcons.Actions.Upload),
                new SandboxUpdateEdgeWorkerAction(resourceBundle.getString("action.testEdgeWorkerInSandbox.title"), resourceBundle.getString("action.testEdgeWorkerInSandbox.desc"), AllIcons.General.ExternalTools),
                new ActivateEdgeWorkerAction(resourceBundle.getString("action.activateEdgeWorker.title"), resourceBundle.getString("action.activateEdgeWorker.desc"), AllIcons.Actions.Install),
                new RegisterEdgeWorkerAction(resourceBundle.getString("action.registerEdgeWorker.title"), resourceBundle.getString("action.registerEdgeWorker.desc"), AllIcons.Actions.RefactoringBulb),
        };
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
