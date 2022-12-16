package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ListEdgeWorkersToolWindowFactory implements ToolWindowFactory {

    @Override
    public boolean isApplicable(@NotNull Project project) {
        return ToolWindowFactory.super.isApplicable(project);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return ToolWindowFactory.super.shouldBeAvailable(project);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ListEdgeWorkersToolWindow listEdgeWorkersToolWindow = new ListEdgeWorkersToolWindow();
        ContentFactory contentFactory = ContentFactory.getInstance();
        try {
            Content content = contentFactory.createContent(listEdgeWorkersToolWindow.getContent(), "", false);
            toolWindow.getContentManager().addContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
