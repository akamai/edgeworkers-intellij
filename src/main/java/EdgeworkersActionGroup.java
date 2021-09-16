import actions.CreateAndValidateBundleAction;
import actions.UploadEdgeWorkerAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ResourceBundle;

public class EdgeworkersActionGroup extends ActionGroup {

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("ActionBundle");
        return new AnAction[]{
                new CreateAndValidateBundleAction(resourceBundle.getString("action.createandvalidatebundle.title"), resourceBundle.getString("action.createandvalidatebundle.desc"), null),
                new UploadEdgeWorkerAction(resourceBundle.getString("action.uploadEdgeWorker.title"), resourceBundle.getString("action.uploadEdgeWorker.desc"), null)
        };
    }
}
