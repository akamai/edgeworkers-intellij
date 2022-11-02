package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class CodeProfilerToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CodeProfilerToolWindow codeProfilerToolWindow = new CodeProfilerToolWindow();
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        try {
            Content content = contentFactory.createContent(codeProfilerToolWindow.getContent(), "", false);
            toolWindow.getContentManager().addContent(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
