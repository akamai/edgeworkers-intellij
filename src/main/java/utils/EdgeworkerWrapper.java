package utils;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class EdgeworkerWrapper {

    private ToolWindowManager toolWindowManager;
    private ToolWindow toolWindow;
    private static final String TOOL_WINDOW_ID = "Edgeworkers";
    private static final String EW_BUNDLE_FILENAME = "edgeworker_bundle.tgz";
    private static final String ACCOUNT_KEY = "***REMOVED***";
    private Project project;

    public EdgeworkerWrapper(@NotNull Project project){
        //create tool window
        this.project = project;
        toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        System.out.println(toolWindow);
        if(null == toolWindow){
            RegisterToolWindowTask registerToolWindowTask = new RegisterToolWindowTask("Edgeworkers", ToolWindowAnchor.BOTTOM, null, false,
                    true, true, true, null, null, null);
            toolWindow = toolWindowManager.registerToolWindow(registerToolWindowTask);
            toolWindow.setToHideOnEmptyContent(true);
        }
    }

    private ConsoleView createConsoleViewOnNewTabOfToolWindow(String title, String description){
        //create console tab inside tool window
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        ConsoleView console = consoleBuilder.getConsole();
        console.clear();
        console.print("------------"+description+"------------" + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        JComponent consolePanel = createConsolePanel(console);
        Content content = contentFactory.createContent(consolePanel, title, false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.show();
        return console;
    }

    private void runCommandsInConsoleView(ConsoleView console, GeneralCommandLine commandLine) throws ExecutionException {
        ProcessHandler processHandler = new OSProcessHandler(commandLine);
        console.attachToProcess(processHandler);
        processHandler.startNotify();
        System.out.println(commandLine);
    }

    public void validateBundle(@NotNull String workDirectory, @NotNull VirtualFile bundlePath) throws ExecutionException {
        ArrayList<String> commands = new ArrayList<>();
        commands.add("akamai");
        commands.add("edgeworkers");
        commands.add("validate");
        commands.add(bundlePath.getCanonicalPath()+"/"+EW_BUNDLE_FILENAME);
        commands.add("--accountkey");
        commands.add(ACCOUNT_KEY);

        GeneralCommandLine commandLine = new GeneralCommandLine(commands);
        commandLine.setWorkDirectory(workDirectory);
        commandLine.setCharset(Charset.forName("UTF-8"));

        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow("Validate Bundle", "Validate Edgeworker Bundle");
        runCommandsInConsoleView(consoleView, commandLine);
    }

    public void createTarball(@NotNull String workDirectory, @NotNull VirtualFile[] ew_files, @NotNull VirtualFile destinationFolder) throws Exception{
        ArrayList<String> commands = new ArrayList<>();
        commands.add("tar");
        commands.add("-czvf");
        commands.add(destinationFolder.getCanonicalPath()+"/"+EW_BUNDLE_FILENAME);
        for(VirtualFile file: ew_files){
            commands.add(file.getName());
        }

        GeneralCommandLine commandLine = new GeneralCommandLine(commands);
        commandLine.setWorkDirectory(workDirectory);
        commandLine.setCharset(Charset.forName("UTF-8"));

        try {
            System.out.println(commandLine);
            ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow("Create Bundle", "Create Edgeworker Bundle");
            runCommandsInConsoleView(consoleView, commandLine);
            System.out.println("ToolWindow: "+toolWindow.isActive()+' '+toolWindow.isDisposed()+" "+toolWindow.isVisible()+" "+toolWindow.isAvailable());

        }catch (Exception e){
            System.out.println("Command Execution failed!"+ e);
            Messages.showErrorDialog("Edgeworker bundle not created!", "Error");
            throw new Exception(e);
        }
    }

    private JComponent createConsolePanel(ConsoleView view) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(view.getComponent(), BorderLayout.CENTER);
        return panel;
    }
}
