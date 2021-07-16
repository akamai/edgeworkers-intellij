import actions.CreateBundleAction;
import actions.UploadEdgeworkerAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EdgeworkersActionGroup extends ActionGroup {

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        return new AnAction[]{ new UploadEdgeworkerAction("Upload Mock", "Upload Edgeworker to Luna", null),
                new CreateBundleAction("Create Bundle and Validate", "Create Edgeworker bundle tgz file and validate it", null) };
    }
}
