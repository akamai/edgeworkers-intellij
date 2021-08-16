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
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import config.EdgeWorkersConfig;
import config.SettingsService;
import org.jetbrains.annotations.NotNull;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

public class EdgeworkerWrapper {

    private ToolWindowManager toolWindowManager;
    private ToolWindow toolWindow;
    private Project project;
    private ResourceBundle resourceBundle;

    public EdgeworkerWrapper(@NotNull Project project){
        //create tool window
        this.project = project;
        toolWindowManager = ToolWindowManager.getInstance(project);
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
        toolWindow = toolWindowManager.getToolWindow(resourceBundle.getString("toolwindow.id"));
        if(null == toolWindow){
            RegisterToolWindowTask registerToolWindowTask = new RegisterToolWindowTask(resourceBundle.getString("toolwindow.id"), ToolWindowAnchor.BOTTOM, null, false,
                    true, true, true, null, null, null);
            toolWindow = toolWindowManager.registerToolWindow(registerToolWindowTask);
            toolWindow.setToHideOnEmptyContent(true);
        }
    }

    private ConsoleView createConsoleViewOnNewTabOfToolWindow(String title, String description){
        //create console tab inside tool window
        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = contentManager.getFactory();
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        ConsoleView console = consoleBuilder.getConsole();
        console.clear();
        console.print("------------"+description+"------------" + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        JComponent consolePanel = createConsolePanel(console);
        Content content = contentFactory.createContent(consolePanel, title, false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
        toolWindow.show();
        return console;
    }

    private void runCommandsInConsoleView(ConsoleView console, ArrayList<GeneralCommandLine> commandLines) throws ExecutionException {
        for(GeneralCommandLine cmdLine: commandLines){
            ProcessHandler processHandler = new OSProcessHandler(cmdLine);
            console.attachToProcess(processHandler);
            processHandler.startNotify();
            System.out.println(cmdLine);
        }
    }

    public GeneralCommandLine getCreateBundleCommand(@NotNull String workDirectory, @NotNull VirtualFile[] ew_files, @NotNull VirtualFile destinationFolder) throws Exception{
        // command for creating Edgeworker bundle
        ArrayList<String> createBundleCmd = new ArrayList<>();
        createBundleCmd.addAll(Arrays.asList("tar", "-czvf", destinationFolder.getCanonicalPath()+"/"+resourceBundle.getString("action.createandvalidatebundle.filename")));
        for(VirtualFile file: ew_files){
            createBundleCmd.add(file.getName());
        }
        GeneralCommandLine createBundleCommandLine = new GeneralCommandLine(createBundleCmd);
        createBundleCommandLine.setWorkDirectory(workDirectory);
        createBundleCommandLine.setCharset(Charset.forName("UTF-8"));
        return createBundleCommandLine;
    }

    public GeneralCommandLine getValidateBundleCommand(@NotNull String workDirectory, @NotNull VirtualFile destinationFolder) throws Exception{
        // command for validating Edgeworker bundle
        ArrayList<String> validateBundleCmd = new ArrayList<>();
        validateBundleCmd.addAll(Arrays.asList("akamai", "edgeworkers", "validate",destinationFolder.getCanonicalPath()+"/"+resourceBundle.getString("action.createandvalidatebundle.filename")));
        EdgeWorkersConfig edgeWorkersConfig = SettingsService.getInstance().getState();
        if(null != edgeWorkersConfig.getEdgercFilePath() && !edgeWorkersConfig.getEdgercFilePath().isEmpty()){
            validateBundleCmd.addAll(Arrays.asList("--edgerc", edgeWorkersConfig.getEdgercFilePath()));
        }
        if(null != edgeWorkersConfig.getEdgercSectionName() && !edgeWorkersConfig.getEdgercSectionName().isEmpty()){
            validateBundleCmd.addAll(Arrays.asList("--section", edgeWorkersConfig.getEdgercSectionName()));
        }
        if(null != edgeWorkersConfig.getAccountKey() && !edgeWorkersConfig.getAccountKey().isEmpty()){
            validateBundleCmd.addAll(Arrays.asList("--accountkey", edgeWorkersConfig.getAccountKey()));
        }
        GeneralCommandLine validateBundleCommandLine = new GeneralCommandLine(validateBundleCmd);
        validateBundleCommandLine.setWorkDirectory(workDirectory);
        validateBundleCommandLine.setCharset(Charset.forName("UTF-8"));
        return validateBundleCommandLine;
    }

    public void createAndValidateBundle(@NotNull String workDirectory, @NotNull VirtualFile[] ew_files, @NotNull VirtualFile destinationFolder) throws Exception{
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();

        commandLines.add(getCreateBundleCommand(workDirectory, ew_files, destinationFolder));
        commandLines.add(getValidateBundleCommand(workDirectory, destinationFolder));

        try {
            ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow(resourceBundle.getString("action.createandvalidatebundle.title"), resourceBundle.getString("action.createandvalidatebundle.desc"));
            runCommandsInConsoleView(consoleView, commandLines);
        }catch (Exception e){
            System.out.println("Command Execution failed!"+ e);
            Messages.showErrorDialog("Edgeworker bundle not created!", "Error");
            throw new Exception(e);
        }
        VfsUtil.markDirtyAndRefresh(false, false, true, destinationFolder);
    }

    private JComponent createConsolePanel(ConsoleView view) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(view.getComponent(), BorderLayout.CENTER);
        return panel;
    }
}
