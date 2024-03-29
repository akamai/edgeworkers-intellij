package utils;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
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
import ui.CheckAkamaiCLIDialog;
import ui.EdgeWorkerNotification;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
public class EdgeworkerWrapper implements Disposable {

    private String MIN_SUPPORTED_CLI_VER = "1.7.0";

    private ToolWindowManager toolWindowManager;
    private ToolWindow toolWindow;
    private Project project;
    private ResourceBundle resourceBundle;
    private ConsoleView console;

    public EdgeworkerWrapper(@NotNull Project project) {
        setUpToolWindowForConsoleViews(project);
    }

    public EdgeworkerWrapper() {
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
    }

    private void setUpToolWindowForConsoleViews(@NotNull Project project) {
        //create tool window
        this.project = project;
        toolWindowManager = ToolWindowManager.getInstance(project);
        resourceBundle = ResourceBundle.getBundle("ActionBundle");
        toolWindow = toolWindowManager.getToolWindow(resourceBundle.getString("toolwindow.id"));
        if (null == toolWindow) {
            RegisterToolWindowTask registerToolWindowTask = new RegisterToolWindowTask(resourceBundle.getString("toolwindow.id"), ToolWindowAnchor.BOTTOM, null, false,
                    true, true, true, null, null, null);
            toolWindow = toolWindowManager.registerToolWindow(registerToolWindowTask);
            toolWindow.setToHideOnEmptyContent(true);
        }
    }

    public ConsoleView createConsoleViewOnNewTabOfToolWindow(String title, String description) {
        //create console tab inside tool window
        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = contentManager.getFactory();
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        console = consoleBuilder.getConsole();
        console.clear();
        console.print("----------------" + description + "----------------" + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        JComponent consolePanel = createConsolePanel(console);
        Content content = contentFactory.createContent(consolePanel, title, false);
        content.setDisposer(this::dispose);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
        toolWindow.show();
        return console;
    }

    public String runCommandsInConsoleView(ConsoleView console, ArrayList<GeneralCommandLine> commandLines) throws ExecutionException {
        StringBuilder errorMsg = new StringBuilder();
        for (GeneralCommandLine cmdLine : commandLines) {
            ProcessHandler processHandler = new OSProcessHandler(cmdLine);
            processHandler.addProcessListener(new ProcessListener() {

                @Override
                public void startNotified(@NotNull ProcessEvent event) {

                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {

                }

                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                    if (!outputType.toString().equals("stderr")) {
                        console.print(event.getText(), ConsoleViewContentType.NORMAL_OUTPUT);
                    } else if (!event.getText().contains("\u001b[1G") && !event.getText().contains("\u001b[2K")) {
                        console.print(event.getText(), ConsoleViewContentType.ERROR_OUTPUT);
                        errorMsg.append(event.getText());
                    }
                }
            });
            processHandler.startNotify();
            processHandler.waitFor();
        }
        return errorMsg.toString();
    }

    public GeneralCommandLine getEdgeWorkerVersionListCommand(String eid, String tempFile) throws Exception {
        ArrayList<String> listEdgeWorkerVersionsCmd = new ArrayList<>();
        listEdgeWorkerVersionsCmd.addAll(Arrays.asList("akamai", "edgeworkers", "list-versions", eid, "--json", tempFile));
        listEdgeWorkerVersionsCmd = addOptionsParams(listEdgeWorkerVersionsCmd);
        GeneralCommandLine listEdgeWorkersCommandLine = new GeneralCommandLine(listEdgeWorkerVersionsCmd);
        listEdgeWorkersCommandLine.setCharset(StandardCharsets.UTF_8);
        return listEdgeWorkersCommandLine;
    }

    public ArrayList<Map<String, String>> getEdgeWorkerVersionsList(String eid) throws Exception {
        File tempFile = FileUtil.createTempFile("tempEdgeWorkerVersions", ".json");
        GeneralCommandLine listEdgeWorkerVersionsCmd = getEdgeWorkerVersionListCommand(eid, tempFile.getPath());
        Integer exitCode = executeCommand(listEdgeWorkerVersionsCmd);
        return parseEdgeWorkersTempFile("list-versions", tempFile);
    }

    public String executeCommandAndGetOutput(GeneralCommandLine commandLine) throws Exception {
        String output = ExecUtil.execAndGetOutput(commandLine, "");
        System.out.println(commandLine);
        return output;
    }

    public Integer executeCommand(GeneralCommandLine commandLine) throws Exception {
        ProcessHandler processHandler = new OSProcessHandler.Silent(commandLine);
        processHandler.startNotify();
        processHandler.waitFor();
        System.out.println(commandLine);
        return processHandler.getExitCode();
    }

    public ArrayList<Map<String, String>> parseEdgeWorkersTempFile(String commandType, File tempFile) {
        ArrayList<Map<String, String>> result = new ArrayList<>();
        try {
            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(Paths.get(tempFile.getPath()));
            Map<?, ?> map = gson.fromJson(reader, Map.class);
            if (null == map || null == map.get("cliStatus") || null == map.get("msg") || map.get("msg").toString().isEmpty()) {
                EdgeWorkerNotification.notifyError(null, "Error: Not Authorized.");
            }
            if (map.get("cliStatus").equals(0.0)) {
                if (map.get("data").toString().equals("{}")) {
                    // data is an empty object, cannot cast to ArrayList<LinkedTreeMap>
                    if (commandType.equals("auth")) {
                        Map<String, String> dataMap = new HashMap<>();
                        dataMap.put("msg", (String) map.get("msg"));
                        result.add(dataMap);
                    }
                } else {
                    ArrayList<LinkedTreeMap> treeMapList = (ArrayList<LinkedTreeMap>) map.get("data");
                    switch (commandType) {
                        case "list-ids":
                            for (LinkedTreeMap treeMap : treeMapList) {
                                if (!treeMap.isEmpty()) {
                                    Map<String, String> dataMap = new HashMap<>();
                                    Double edgeWorkerId = (Double) treeMap.get("edgeWorkerId");
                                    dataMap.put("edgeWorkerId", String.valueOf(edgeWorkerId.intValue()));
                                    dataMap.put("name", (String) treeMap.get("name"));
                                    result.add(dataMap);
                                }
                            }
                            break;
                        case "list-versions":
                            for (LinkedTreeMap treeMap : (ArrayList<LinkedTreeMap>) map.get("data")) {
                                Map<String, String> dataMap = new HashMap<>();
                                if (!treeMap.isEmpty()) {
                                    dataMap.put("version", (String) treeMap.get("version"));
                                    result.add(dataMap);
                                }
                            }
                            System.out.println(result);
                            break;
                        case "status":
                            boolean staging = false;
                            boolean prod = false;
                            for (LinkedTreeMap treeMap : (ArrayList<LinkedTreeMap>) map.get("data")) {
                                Map<String, String> dataMap = new HashMap<>();
                                if (!treeMap.isEmpty() && treeMap.get("status").equals("COMPLETE")) {
                                    if (!staging && treeMap.get("network").equals("STAGING")) {
                                        //first record in json response for staging is the active version
                                        staging = true;
                                    } else if (!prod && treeMap.get("network").equals("PRODUCTION")) {
                                        //first record in json response for prod is the active version
                                        prod = true;
                                    } else {
                                        continue;
                                    }
                                    dataMap.put("network", (String) treeMap.get("network"));
                                    dataMap.put("version", (String) treeMap.get("version"));
                                    dataMap.put("activationId", String.valueOf(treeMap.get("activationId")));
                                    result.add(dataMap);
                                }
                            }
                            break;
                        case "list-groups":
                            for (LinkedTreeMap treeMap : (ArrayList<LinkedTreeMap>) map.get("data")) {
                                Map<String, String> dataMap = new HashMap<>();
                                if (!treeMap.isEmpty()) {
                                    Double groupId = (Double) treeMap.get("groupId");
                                    dataMap.put("groupId", String.valueOf(groupId.intValue()));
                                    dataMap.put("groupName", (String) treeMap.get("groupName"));
                                    result.add(dataMap);
                                }
                            }
                            break;
                        case "list-contracts":
                            for (LinkedTreeMap treeMap : (ArrayList<LinkedTreeMap>) map.get("data")) {
                                Map<String, String> dataMap = new HashMap<>();
                                if (!treeMap.isEmpty()) {
                                    dataMap.put("ContractIds", (String) treeMap.get("ContractIds"));
                                    result.add(dataMap);
                                }
                            }
                            break;
                    }
                }
            } else if (!map.get("msg").toString().isEmpty()) {
                EdgeWorkerNotification.notifyError(null, (String) map.get("msg"));
            }
            reader.close();
            tempFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            EdgeWorkerNotification.notifyError(null, "Error in executing Akamai CLI command.");
        }
        return result;
    }

    public GeneralCommandLine getEdgeWorkersIdsListCommand(String tempFilePath) throws Exception {
        ArrayList<String> listEdgeWorkersCmd = new ArrayList<>();
        listEdgeWorkersCmd.addAll(Arrays.asList("akamai", "edgeworkers", "list-ids", "--json", tempFilePath));
        listEdgeWorkersCmd = addOptionsParams(listEdgeWorkersCmd);
        GeneralCommandLine listEdgeWorkersCommandLine = new GeneralCommandLine(listEdgeWorkersCmd);
        listEdgeWorkersCommandLine.setCharset(StandardCharsets.UTF_8);
        return listEdgeWorkersCommandLine;
    }

    public ArrayList<Map<String, String>> getEdgeWorkersIdsList() throws Exception {
        File tempFile = FileUtil.createTempFile("tempEdgeWorkersIds", ".json");
        GeneralCommandLine commandLine = getEdgeWorkersIdsListCommand(tempFile.getPath());
        Integer exitCode = executeCommand(commandLine);
        return parseEdgeWorkersTempFile("list-ids", tempFile);
    }

    public GeneralCommandLine getActiveEdgeWorkerVersionsOnStagingAndProd(String eid, String tempFile) {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("akamai", "edgeworkers", "status", eid, "--json", tempFile));
        cmd = addOptionsParams(cmd);
        GeneralCommandLine commandLine = new GeneralCommandLine(cmd);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }

    public ArrayList<Map<String, String>> getActiveEdgeWorkerVersionsOnStagingAndProd(String eid) throws Exception {
        File tempFile = FileUtil.createTempFile("tempActiveEdgeWorkerVersions", ".json");
        GeneralCommandLine commandLine = getActiveEdgeWorkerVersionsOnStagingAndProd(eid, tempFile.getPath());
        Integer exitCode = executeCommand(commandLine);
        return parseEdgeWorkersTempFile("status", tempFile);
    }

    public GeneralCommandLine getCreateBundleCommand(@NotNull String workDirectory, @NotNull VirtualFile[] ew_files, @NotNull VirtualFile destinationFolder) throws Exception {
        // command for creating Edgeworker bundle
        ArrayList<String> createBundleCmd = new ArrayList<>();
        createBundleCmd.addAll(Arrays.asList("tar", "-czvf", destinationFolder.getCanonicalPath() + "/" + resourceBundle.getString("action.createandvalidatebundle.filename")));
        for (VirtualFile file : ew_files) {
            createBundleCmd.add(file.getName());
        }
        GeneralCommandLine createBundleCommandLine = new GeneralCommandLine(createBundleCmd);
        createBundleCommandLine.setWorkDirectory(workDirectory);
        createBundleCommandLine.setCharset(StandardCharsets.UTF_8);
        return createBundleCommandLine;
    }

    public ArrayList<String> addOptionsParams(ArrayList<String> cmd) {
        EdgeWorkersConfig edgeWorkersConfig = SettingsService.getInstance().getState();
        if (null != edgeWorkersConfig.getEdgercFilePath() && !edgeWorkersConfig.getEdgercFilePath().isEmpty()) {
            cmd.addAll(Arrays.asList("--edgerc", edgeWorkersConfig.getEdgercFilePath()));
        }
        if (null != edgeWorkersConfig.getEdgercSectionName() && !edgeWorkersConfig.getEdgercSectionName().isEmpty()) {
            cmd.addAll(Arrays.asList("--section", edgeWorkersConfig.getEdgercSectionName()));
        }
        if (null != edgeWorkersConfig.getAccountKey() && !edgeWorkersConfig.getAccountKey().isEmpty()) {
            cmd.addAll(Arrays.asList("--accountkey", edgeWorkersConfig.getAccountKey()));
        }
        if(this.addIdeExtensionOptionWithCorrectAkamaiVersion() == true) {
            cmd.addAll(Arrays.asList("--ideExtension", "INTELLIJ"));
        }
        return cmd;
    }

    public GeneralCommandLine getValidateBundleCommand(@NotNull String workDirectory, @NotNull VirtualFile destinationFolder) throws Exception {
        // command for validating Edgeworker bundle
        ArrayList<String> validateBundleCmd = new ArrayList<>();
        validateBundleCmd.addAll(Arrays.asList("akamai", "edgeworkers", "validate", destinationFolder.getCanonicalPath() + "/" + resourceBundle.getString("action.createandvalidatebundle.filename")));
        validateBundleCmd = addOptionsParams(validateBundleCmd);
        GeneralCommandLine validateBundleCommandLine = new GeneralCommandLine(validateBundleCmd);
        validateBundleCommandLine.setWorkDirectory(workDirectory);
        validateBundleCommandLine.setCharset(StandardCharsets.UTF_8);
        return validateBundleCommandLine;
    }

    public void createAndValidateBundle(@NotNull String workDirectory, @NotNull VirtualFile[] ew_files, @NotNull VirtualFile destinationFolder) throws Exception {
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();
        commandLines.add(getCreateBundleCommand(workDirectory, ew_files, destinationFolder));
        commandLines.add(getValidateBundleCommand(workDirectory, destinationFolder));
        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow(resourceBundle.getString("action.createandvalidatebundle.title"), resourceBundle.getString("action.createandvalidatebundle.desc"));
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressManager.getInstance().getProgressIndicator().setText("Creating and Validating Bundle...");
                            runCommandsInConsoleView(consoleView, commandLines);
                        } catch (ExecutionException e) {
                            System.out.println("Command Execution failed!" + e);
                            Messages.showErrorDialog("EdgeWorker Bundle not created!", "Error");
                        }
                    }
                }, "", false, project);
        VfsUtil.markDirtyAndRefresh(false, false, true, destinationFolder);
    }

    public GeneralCommandLine getActivateEdgeWorkerCommand(String eid, String version, String network) throws Exception {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("akamai", "edgeworkers", "activate", eid, network, version));
        cmd = addOptionsParams(cmd);
        GeneralCommandLine commandLine = new GeneralCommandLine(cmd);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }

    public void activateEdgeWorker(String eid, String version, String network) throws Exception {
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();
        commandLines.add(getActivateEdgeWorkerCommand(eid, version, network));
        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow("Activate EdgeWorker", "Activate EdgeWorker");
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressManager.getInstance().getProgressIndicator().setText("Activating...");
                            String errorMsg = runCommandsInConsoleView(consoleView, commandLines);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            Messages.showErrorDialog("EdgeWorker activation failed.", "Error");
                        }

                    }
                }, "Activate EdgeWorker", false, null);
    }

    public GeneralCommandLine getRegisterEdgeWorkerCommand(String groupId, String edgeWorkerName, Integer resourceTierId) throws Exception {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("akamai", "edgeworkers", "register", groupId, edgeWorkerName));
        if (null != resourceTierId) {
            cmd.addAll(Arrays.asList("--resourceTierId", resourceTierId.toString()));
        }
        cmd = addOptionsParams(cmd);
        GeneralCommandLine commandLine = new GeneralCommandLine(cmd);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }

    public void registerEdgeWorker(String groupId, String edgeWorkerName, Integer resourceTierId) throws Exception {
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();
        commandLines.add(getRegisterEdgeWorkerCommand(groupId, edgeWorkerName, resourceTierId));
        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow("Register EdgeWorker", "Register EdgeWorker");
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressManager.getInstance().getProgressIndicator().setText("Registering...");
                            String errorMsg = runCommandsInConsoleView(consoleView, commandLines);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                            Messages.showErrorDialog("EdgeWorker registration failed.", "Error");
                        }

                    }
                }, "Register EdgeWorker", false, null);
    }

    public GeneralCommandLine getGroupsListCommand(String tmpFile) throws Exception {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("akamai", "edgeworkers", "list-groups", "--json", tmpFile));
        cmd = addOptionsParams(cmd);
        GeneralCommandLine commandLine = new GeneralCommandLine(cmd);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }

    public ArrayList<Map<String, String>> getGroupsList() throws Exception {
        File tempFile = FileUtil.createTempFile("tempGroupsList", ".json");
        GeneralCommandLine commandLine = getGroupsListCommand(tempFile.getPath());
        Integer exitCode = executeCommand(commandLine);
        return parseEdgeWorkersTempFile("list-groups", tempFile);
    }

    public GeneralCommandLine getContractIdsListCommand(String tmpFile) throws Exception {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("akamai", "edgeworkers", "list-contracts", "--json", tmpFile));
        cmd = addOptionsParams(cmd);
        GeneralCommandLine commandLine = new GeneralCommandLine(cmd);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }

    public ArrayList<Map<String, String>> getContractIdsList() throws Exception {
        File tempFile = FileUtil.createTempFile("tempContractIdsList", ".json");
        GeneralCommandLine commandLine = getContractIdsListCommand(tempFile.getPath());
        Integer exitCode = executeCommand(commandLine);
        return parseEdgeWorkersTempFile("list-contracts", tempFile);
    }

    private JComponent createConsolePanel(ConsoleView view) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(view.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    public GeneralCommandLine getUploadEdgeWorkerCommand(String eid, String bundlePath) throws Exception {
        ArrayList<String> uploadEdgeWorkerCmd = new ArrayList<>();
        uploadEdgeWorkerCmd.addAll(Arrays.asList("akamai", "edgeworkers", "upload", eid, "--bundle", bundlePath));
        uploadEdgeWorkerCmd = addOptionsParams(uploadEdgeWorkerCmd);
        GeneralCommandLine uploadEdgeWorkerCmdLine = new GeneralCommandLine(uploadEdgeWorkerCmd);
        uploadEdgeWorkerCmdLine.setCharset(StandardCharsets.UTF_8);
        return uploadEdgeWorkerCmdLine;
    }

    public void uploadEdgeWorker(String eid, String bundlePath) throws Exception {
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();
        commandLines.add(getUploadEdgeWorkerCommand(eid, bundlePath));
        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow("Upload EdgeWorker", "Upload EdgeWorker");
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressManager.getInstance().getProgressIndicator().setText("Uploading...");
                            runCommandsInConsoleView(consoleView, commandLines);
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                            Messages.showErrorDialog("EdgeWorker was not uploaded!", "Error");
                        }
                    }
                }, "Upload EdgeWorker", false, project);
    }

    public GeneralCommandLine getEdgeWorkerDownloadCommand(String eid, String versionId, String downloadPath) throws Exception {
        ArrayList<String> downloadEdgeWorkerCmd = new ArrayList<>();
        downloadEdgeWorkerCmd.addAll(Arrays.asList("akamai", "edgeworkers", "download-version", eid, versionId, "--downloadPath", downloadPath));
        downloadEdgeWorkerCmd = addOptionsParams(downloadEdgeWorkerCmd);
        GeneralCommandLine downloadEdgeWorkerCmdLine = new GeneralCommandLine(downloadEdgeWorkerCmd);
        downloadEdgeWorkerCmdLine.setCharset(StandardCharsets.UTF_8);
        return downloadEdgeWorkerCmdLine;
    }

    public Integer downloadEdgeWorker(String eid, String versionId, String downloadPath) throws Exception {
        GeneralCommandLine commandLine = getEdgeWorkerDownloadCommand(eid, versionId, downloadPath);
        Integer exitCode = executeCommand(commandLine);
        return exitCode;
    }

    public GeneralCommandLine getExtractTgzFileCommand(String tgzFilePath, String extractDirectory) throws Exception {
        ArrayList<String> listEdgeWorkerVersionsCmd = new ArrayList<>();
        listEdgeWorkerVersionsCmd.addAll(Arrays.asList("tar", "-xvzf", tgzFilePath, "-C", extractDirectory));
        GeneralCommandLine commandLine = new GeneralCommandLine(listEdgeWorkerVersionsCmd);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }

    public Integer extractTgzFile(String tgzFilePath, String extractDirectory) throws Exception {
        GeneralCommandLine commandLine = getExtractTgzFileCommand(tgzFilePath, extractDirectory);
        Integer exitCode = executeCommand(commandLine);
        if (null == exitCode || !exitCode.equals(0)) {
            System.out.println(" extractTgzFile exitCode");
        }
        return exitCode;
    }

    public GeneralCommandLine getUpdateEdgeWorkerToSandboxCommand(String eid, String bundlePath) {
        ArrayList<String> command = new ArrayList<>();
        command.addAll(Arrays.asList("akamai", "sandbox", "update-edgeworker", eid, bundlePath));
        command = addOptionsParams(command);
        GeneralCommandLine commandLine = new GeneralCommandLine(command);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }

    public void updateEdgeWorkerToSandbox(String eid, String bundlePath) throws Exception {
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();
        commandLines.add(getUpdateEdgeWorkerToSandboxCommand(eid, bundlePath));
        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow("Update EdgeWorker to Sandbox", "Update EdgeWorker to the default sandbox.");
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProgressManager.getInstance().getProgressIndicator().setText("Updating...");
                            String errorMsg = runCommandsInConsoleView(consoleView, commandLines);
                            if (errorMsg.isEmpty()) {
                                consoleView.print(resourceBundle.getString("sandbox.testing.info"), ConsoleViewContentType.LOG_INFO_OUTPUT);
                            } else if (errorMsg.contains(resourceBundle.getString("sandbox.error.doesNotExist"))) {
                                consoleView.print(resourceBundle.getString("sandbox.setup.info.link"), ConsoleViewContentType.LOG_INFO_OUTPUT);
                            }
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                            showErrorDialog("EdgeWorker was not updated to the Sandbox!", "Error");
                        }
                    }
                }, "Update EdgeWorker to Sandbox", false, project);
    }

    public GeneralCommandLine getEdgeWorkerAuthCommand(String hostname, String tempFile) {
        ArrayList<String> edgeWorkersAuthCommand = new ArrayList<>(Arrays.asList("akamai", "edgeworkers", "auth", hostname, "--expiry", "120", "--json", tempFile));
        edgeWorkersAuthCommand = addOptionsParams(edgeWorkersAuthCommand);
        GeneralCommandLine edgeWorkersAuthCommandLine = new GeneralCommandLine(edgeWorkersAuthCommand);
        edgeWorkersAuthCommandLine.setCharset(StandardCharsets.UTF_8);
        return edgeWorkersAuthCommandLine;
    }

    public String getEdgeWorkersAuthString(String hostname) throws Exception {
        File tempFile = FileUtil.createTempFile("tempEdgeWorkerAuth", ".json");
        GeneralCommandLine edgeWorkerAuthCmd = getEdgeWorkerAuthCommand(hostname, tempFile.getPath());
        int exitCode = executeCommand(edgeWorkerAuthCmd);
        ArrayList<Map<String, String>> result = parseEdgeWorkersTempFile("auth", tempFile);
        if (result.size() == 0) {
            // parseEdgeWorkersTempFile has already displayed a notification with the error message, just return null
            return null;
        } else {
            return result.get(0).get("msg");
        }
    }

    public GeneralCommandLine getCLICommandLineByParams(String... params) {
        ArrayList<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(params));
        GeneralCommandLine commandLine = new GeneralCommandLine(command);
        commandLine.setCharset(StandardCharsets.UTF_8);
        return commandLine;
    }



    public boolean checkIfAkamaiCliInstalled() {
        final boolean[] akamaiCliInstalled = {true};
        final boolean[] edgeWorkersCliInstalled = {true};
        final boolean[] sandboxCliInstalled = {true};
        final boolean[] edgercFileExist = {true};
        suppressCliPrompts();
        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                @Override
                public void run() {
                    try {
                        ProgressManager.getInstance().getProgressIndicator().setText("Loading...");
                        //check if akamai cli is installed
                        String output = executeCommandAndGetOutput(getCLICommandLineByParams("akamai", "help"));
                        if (!output.contains("akamai")) {
                            akamaiCliInstalled[0] = false;
                        } else {
                            if (output.contains("edgeworkers")) {
                                //attempt to upgrade edgeworkers CLI
                                updateEdgeWorkersCLI();
                            } else {
                                //install Akamai EdgeWorker CLI if not already installed
                                ProgressManager.getInstance().getProgressIndicator().setText("Installing Akamai EdgeWorkers CLI...");
                                if (executeCommand(getCLICommandLineByParams("akamai", "install", "edgeworkers")) == 1) {
                                    edgeWorkersCliInstalled[0] = false;
                                    System.out.println("Error came while installing akamai edgeworkers cli programmatically.");
                                }
                            }
                            //install Akamai sandbox CLI if not already installed
                            if (!output.contains("sandbox")) {
                                ProgressManager.getInstance().getProgressIndicator().setText("Installing Akamai Sandbox CLI...");
                                if (executeCommand(getCLICommandLineByParams("akamai", "install", "sandbox")) == 1) {
                                    sandboxCliInstalled[0] = false;
                                    System.out.println("Error came while installing akamai sandbox cli programmatically.");
                                }
                            }
                            //check if .edgerc file exist
                            EdgeWorkersConfig config = SettingsService.getInstance().getState();
                            if (null != config && null != config.getEdgercFilePath()) {
                                File file = new File(config.getEdgercFilePath());
                                if (!file.exists()) {
                                    edgercFileExist[0] = false;
                                }
                            }
                        }
                    } catch (Exception exception) {
                        akamaiCliInstalled[0] = false;
                        exception.printStackTrace();
                    }
                }
            }, "", false, project);
            if (!akamaiCliInstalled[0]) {
                //when akamai cli is not installed
                CheckAkamaiCLIDialog checkAkamaiCLIDialog = new CheckAkamaiCLIDialog();
                checkAkamaiCLIDialog.show();
            } else if (!edgeWorkersCliInstalled[0]) {
                showErrorDialog("Please install akamai edgeworkers cli", "Error");
            } else if (!sandboxCliInstalled[0]) {
                showErrorDialog("Please install akamai sandbox cli", "Error");
            } else if (!edgercFileExist[0]) {
                showErrorDialog("Please create and setup .edgerc file and configure EdgeWorkers settings at IntelliJ IDEA > Preferences/Settings > EdgeWorkers Configuration", "Error");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return akamaiCliInstalled[0] && edgeWorkersCliInstalled[0] && sandboxCliInstalled[0] && edgercFileExist[0];
    }


    static boolean updateAttempted = false;
    public void updateEdgeWorkersCLI() {
        // update EdgeWorkers CLI -- it's not easy to find if we have the latest other than to attempt upgrade
        // so we can do this upgrade attempt quietly to not annoy users
        // do the attempt once per process init using the static variable above
        if (!updateAttempted) {
            try {
                String version = executeCommandAndGetOutput(getCLICommandLineByParams("akamai", "edgeworkers", "--version")).trim();

                // if less than where we want to be
                if (version.compareTo(MIN_SUPPORTED_CLI_VER) < 0) {
                    executeCommandAndGetOutput(getCLICommandLineByParams("akamai", "update", "edgeworkers"));
                }
            } catch (Exception exception) {
                // the above command failing is non critical so we won't do anything on failure
                // just catch the error to prevent it from causing problems for extension use
            }
            updateAttempted = true;
        }
    }

    //check akamai version
    public boolean addIdeExtensionOptionWithCorrectAkamaiVersion() {
        final boolean[] akamaiVersionIsGood = {false}; // boolean to set to true if the akamai --version is greter than 1.6.1
            ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                @Override
                public void run() {
                    try {
                        ProgressManager.getInstance().getProgressIndicator().setText("Loading...");
                                                    //check version of akamai cli edgeworkers
                        String output = executeCommandAndGetOutput(getCLICommandLineByParams( "akamai","edgeworkers","--version"));
                        String[] substrings = output.replaceAll("\n","").split(" ");
                        if(substrings[substrings.length - 1].compareTo("1.6.1") > 0) {
                            System.out.println("The output for akamai edgeworkers version:"+substrings[substrings.length -1]);
                            //if the current akamai version(1.5.1 current) is greater than 1.6.1
                            // which means we have api and cli updated for adding ideExtensionType option
                            akamaiVersionIsGood[0] = true;
                        }
                    } catch (Exception exception) {
                        akamaiVersionIsGood[0] = false;
                        exception.printStackTrace();
                    }
                }
            }, "", false, project);
        return akamaiVersionIsGood[0];
    }

    private void suppressCliPrompts() {
        try {
            //check if akamai-cli/config file exist
            File home = new File(System.getProperty("user.home"));
            File cliConfigFile = new File(Paths.get(home.getAbsolutePath(), "/.akamai-cli/config").toString());
            if (cliConfigFile.exists()) {
                String content = Files.readString(cliConfigFile.toPath(), StandardCharsets.US_ASCII);
                if (content.contains("enable-cli-statistics = false") && content.contains("last-upgrade-check = ignore")) {
                    return;
                }
                StringBuilder newContent = new StringBuilder();
                for (String line : content.split("\n")) {
                    if (line.contains("enable-cli-statistics") || line.contains("last-upgrade-check")) {
                        continue;
                    }
                    newContent.append(line + "\n");
                }
                newContent.append("enable-cli-statistics = false\n");
                newContent.append("last-upgrade-check = ignore\n");
                //overwrite .akamai-cli/config file
                Files.write(cliConfigFile.toPath(), newContent.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void showErrorDialog(String message, String title) {
        Messages.showErrorDialog(message, title);
    }

    @Override
    public void dispose() {
        //gets executed when EdgeWorker's console view tabs are closed or intellij main window is closed
        console.dispose();
    }
}
