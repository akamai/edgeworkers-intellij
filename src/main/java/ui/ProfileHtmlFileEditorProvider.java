package ui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import utils.Constants;

/**
 * FileEditorProvider implementation that will use our HtmlFileEditor to open converted cpuprofile files.
 */
public class ProfileHtmlFileEditorProvider implements FileEditorProvider, DumbAware {

    private static final String EDITOR_TYPE_ID = "ProfileView";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // Only accept our converted html files
        return virtualFile.getName().matches("(" + Constants.CONVERTED_FILE_NAME + ")-\\d+(.html)");
    }

    @Override
    public @NotNull
    FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return new HtmlFileEditor(virtualFile);
    }

    @Override
    public @NotNull
    @NonNls
    String getEditorTypeId() {
        return EDITOR_TYPE_ID;
    }

    @Override
    public @NotNull
    FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
