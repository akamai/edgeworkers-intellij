package ui;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

/**
 * Simple FileEditor Implementation that will take an HTML file and display it using JCEF.
 * Will throw a runtime error if JCEF is not supported and the class attempts to display an HTML file.
 */
public class HtmlFileEditor extends UserDataHolderBase implements FileEditor {
    private final VirtualFile file;

    /**
     * Create a new ProfileHtmlFileEditor
     *
     * @param file HTML file to be loaded using JCEF
     */
    public HtmlFileEditor(VirtualFile file) {
        this.file = file;
    }

    /**
     * Returns a component which represents the editor in UI.
     */
    @Override
    public @NotNull JComponent getComponent() {
        if (!JBCefApp.isSupported()) {
            throw new RuntimeException("JCEF is not supported in this environment");
        }
        JPanel tabContent = new JPanel();
        tabContent.setLayout(new BorderLayout());

        JBCefBrowser browser = new JBCefBrowser();
        browser.loadURL("file:///" + file.getPath());

        tabContent.add(browser.getComponent(), BorderLayout.CENTER);
        return tabContent;
    }

    /**
     * Returns a component to be focused when the editor is opened.
     */
    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    /**
     * Returns editor's name - a string that identifies the editor among others
     * (e.g.: "GUI Designer" for graphical editing and "Text" for textual representation of a GUI form editors).
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) @NotNull String getName() {
        return "HTML Viewer";
    }

    /**
     * Applies given state to the editor.
     *
     * @param state
     */
    @Override
    public void setState(@NotNull FileEditorState state) {
    }

    /**
     * Returns {@code true} when editor's content differs from its source (e.g. a file).
     */
    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * An editor is valid if its contents still exist.
     * For example, an editor displaying the contents of some file stops being valid if the file is deleted.
     * An editor can also become invalid after being disposed of.
     */
    @Override
    public boolean isValid() {
        return true;
    }

    /**
     * Adds specified listener.
     *
     * @param listener
     */
    @Override
    public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    /**
     * Removes specified listener.
     *
     * @param listener
     */
    @Override
    public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    }

    /**
     * The method is optional. Currently, it is used only by the Find Usages subsystem.
     * Expected to return a location of user's focus - a caret or any other form of selection start.
     */
    @Override
    public @Nullable FileEditorLocation getCurrentLocation() {
        return null;
    }

    /**
     * Usually not invoked directly, see class javadoc.
     */
    @Override
    public void dispose() {
        Disposer.dispose(this);
    }

    @Override
    public @Nullable VirtualFile getFile() {
        return this.file;
    }
}
