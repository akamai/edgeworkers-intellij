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
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import config.EdgeWorkersConfig;
import config.SettingsService;
import org.jetbrains.annotations.NotNull;
import ui.CheckAkamaiCLIDialog;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class EdgeworkerWrapper implements Disposable {

    private ToolWindowManager toolWindowManager;
    private ToolWindow toolWindow;
    private Project project;
    private ResourceBundle resourceBundle;
    private ConsoleView console;

    public EdgeworkerWrapper(@NotNull Project project){
        setUpToolWindowForConsoleViews(project);
    }

    public EdgeworkerWrapper(){
    }

    private void setUpToolWindowForConsoleViews(@NotNull Project project){
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
        console = consoleBuilder.getConsole();
        console.clear();
        console.print("----------------"+description+"----------------" + "\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
        JComponent consolePanel = createConsolePanel(console);
        Content content = contentFactory.createContent(consolePanel, title, false);
        content.setDisposer(this::dispose);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
        toolWindow.show();
        return console;
    }

    private void runCommandsInConsoleView(ConsoleView console, ArrayList<GeneralCommandLine> commandLines) throws ExecutionException {

        for(GeneralCommandLine cmdLine: commandLines){
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
                    if(!outputType.toString().equals("stderr")){
                        console.print(event.getText(), ConsoleViewContentType.NORMAL_OUTPUT);
                    }
                }
            });
            processHandler.startNotify();
            processHandler.waitFor();
        }
    }

    public GeneralCommandLine getEdgeWorkerVersionListCommand(String eid, String tempFile) throws Exception{
        ArrayList<String> listEdgeWorkerVersionsCmd = new ArrayList<>();
        listEdgeWorkerVersionsCmd.addAll(Arrays.asList("akamai", "edgeworkers", "list-versions", eid, "--json", tempFile));
        listEdgeWorkerVersionsCmd = addOptionsParams(listEdgeWorkerVersionsCmd);
        GeneralCommandLine listEdgeWorkersCommandLine = new GeneralCommandLine(listEdgeWorkerVersionsCmd);
        listEdgeWorkersCommandLine.setCharset(Charset.forName("UTF-8"));
        return listEdgeWorkersCommandLine;
    }

    public ArrayList<Map<String, String>> getEdgeWorkerVersionsList(String eid) throws Exception{
        File tempFile = FileUtil.createTempFile("tempEdgeWorkerVersions",".json");
        GeneralCommandLine listEdgeWorkerVersionsCmd = getEdgeWorkerVersionListCommand(eid, tempFile.getPath());
        Integer exitCode = executeCommand(listEdgeWorkerVersionsCmd);
        if(null == exitCode || !exitCode.equals(0)){
            return new ArrayList<>();
        }
        return parseListEdgeWorkersTempFile("list-versions", tempFile);
    }

    public String executeCommandAndGetOutput(GeneralCommandLine commandLine) throws Exception{
        String output = ExecUtil.execAndGetOutput(commandLine, "");
        System.out.println(commandLine);
        return output;
    }

    public Integer executeCommand(GeneralCommandLine commandLine) throws Exception{
        ProcessHandler processHandler = new OSProcessHandler.Silent(commandLine);
        processHandler.startNotify();
        processHandler.waitFor();
        System.out.println(commandLine);
        return processHandler.getExitCode();
    }

    public ArrayList<Map<String, String>> parseListEdgeWorkersTempFile(String commandType, File tempFile){
        ArrayList<Map<String, String>> result = new ArrayList<>();
        try {
            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(Paths.get(tempFile.getPath()));
            Map<?, ?> map = gson.fromJson(reader, Map.class);
            if(map.get("cliStatus").equals(0.0)){
                ArrayList<LinkedTreeMap> treeMapList = (ArrayList<LinkedTreeMap>) map.get("data");
                if(commandType=="list-ids"){
                    for(LinkedTreeMap treeMap: treeMapList){
                        if(!treeMap.isEmpty()){
                            Map<String, String> dataMap = new HashMap<>();
                            Double edgeWorkerId = (Double)treeMap.get("edgeWorkerId");
                            dataMap.put("edgeWorkerId", String.valueOf(Double.valueOf(edgeWorkerId).intValue()));
                            dataMap.put("name", (String)treeMap.get("name"));
                            result.add(dataMap);
                        }
                    }
                }else if(commandType=="list-versions"){
                    for(LinkedTreeMap treeMap: (ArrayList<LinkedTreeMap>) map.get("data")){
                        Map<String, String> dataMap = new HashMap<>();
                        if(!treeMap.isEmpty()){
                            dataMap.put("version", (String)treeMap.get("version"));
                            result.add(dataMap);
                        }
                    }
                    System.out.println(result);
                }
            }
            reader.close();
            tempFile.delete();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public GeneralCommandLine getEdgeWorkersIdsListCommand(String tempFilePath) throws Exception{
        ArrayList<String> listEdgeWorkersCmd = new ArrayList<>();
        listEdgeWorkersCmd.addAll(Arrays.asList("akamai", "edgeworkers", "list-ids", "--json", tempFilePath));
        listEdgeWorkersCmd = addOptionsParams(listEdgeWorkersCmd);
        GeneralCommandLine listEdgeWorkersCommandLine = new GeneralCommandLine(listEdgeWorkersCmd);
        listEdgeWorkersCommandLine.setCharset(Charset.forName("UTF-8"));
        return listEdgeWorkersCommandLine;
    }

    public ArrayList<Map<String, String>> getEdgeWorkersIdsList() throws Exception{
        File tempFile = FileUtil.createTempFile("tempEdgeWorkersIds",".json");
        GeneralCommandLine commandLine = getEdgeWorkersIdsListCommand(tempFile.getPath());
        Integer exitCode = executeCommand(commandLine);
        if(null == exitCode || !exitCode.equals(0)){
            return new ArrayList<>();
        }
        return parseListEdgeWorkersTempFile("list-ids", tempFile);
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

    public ArrayList<String> addOptionsParams(ArrayList<String> cmd){
        EdgeWorkersConfig edgeWorkersConfig = SettingsService.getInstance().getState();
        if(null != edgeWorkersConfig.getEdgercFilePath() && !edgeWorkersConfig.getEdgercFilePath().isEmpty()){
            cmd.addAll(Arrays.asList("--edgerc", edgeWorkersConfig.getEdgercFilePath()));
        }
        if(null != edgeWorkersConfig.getEdgercSectionName() && !edgeWorkersConfig.getEdgercSectionName().isEmpty()){
            cmd.addAll(Arrays.asList("--section", edgeWorkersConfig.getEdgercSectionName()));
        }
        if(null != edgeWorkersConfig.getAccountKey() && !edgeWorkersConfig.getAccountKey().isEmpty()){
            cmd.addAll(Arrays.asList("--accountkey", edgeWorkersConfig.getAccountKey()));
        }
        return cmd;
    }
    public GeneralCommandLine getValidateBundleCommand(@NotNull String workDirectory, @NotNull VirtualFile destinationFolder) throws Exception{
        // command for validating Edgeworker bundle
        ArrayList<String> validateBundleCmd = new ArrayList<>();
        validateBundleCmd.addAll(Arrays.asList("akamai", "edgeworkers", "validate",destinationFolder.getCanonicalPath()+"/"+resourceBundle.getString("action.createandvalidatebundle.filename")));
        validateBundleCmd = addOptionsParams(validateBundleCmd);
        GeneralCommandLine validateBundleCommandLine = new GeneralCommandLine(validateBundleCmd);
        validateBundleCommandLine.setWorkDirectory(workDirectory);
        validateBundleCommandLine.setCharset(Charset.forName("UTF-8"));
        return validateBundleCommandLine;
    }

    public void createAndValidateBundle(@NotNull String workDirectory, @NotNull VirtualFile[] ew_files, @NotNull VirtualFile destinationFolder) throws Exception{
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();
        commandLines.add(getCreateBundleCommand(workDirectory, ew_files, destinationFolder));
        commandLines.add(getValidateBundleCommand(workDirectory, destinationFolder));
        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow(resourceBundle.getString("action.createandvalidatebundle.title"), resourceBundle.getString("action.createandvalidatebundle.desc"));
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            runCommandsInConsoleView(consoleView, commandLines);
                        } catch (ExecutionException e) {
                            System.out.println("Command Execution failed!"+ e);
                            Messages.showErrorDialog("Edgeworker bundle not created!", "Error");
                        }
                    }
                },"Creating and Validating...", false, project);
        VfsUtil.markDirtyAndRefresh(false, false, true, destinationFolder);
    }

    private JComponent createConsolePanel(ConsoleView view) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(view.getComponent(), BorderLayout.CENTER);
        return panel;
    }

    public GeneralCommandLine getUploadEdgeWorkerCommand(String eid, String bundlePath) throws Exception{
        ArrayList<String> uploadEdgeWorkerCmd = new ArrayList<>();
        uploadEdgeWorkerCmd.addAll(Arrays.asList("akamai", "edgeworkers", "upload", eid, "--bundle", bundlePath));
        uploadEdgeWorkerCmd = addOptionsParams(uploadEdgeWorkerCmd);
        GeneralCommandLine uploadEdgeWorkerCmdLine = new GeneralCommandLine(uploadEdgeWorkerCmd);
        uploadEdgeWorkerCmdLine.setCharset(Charset.forName("UTF-8"));
        return uploadEdgeWorkerCmdLine;
    }

    public void uploadEdgeWorker(String eid, String bundlePath) throws Exception{
        ArrayList<GeneralCommandLine> commandLines = new ArrayList<>();
        commandLines.add(getUploadEdgeWorkerCommand(eid, bundlePath));
        ConsoleView consoleView = createConsoleViewOnNewTabOfToolWindow("Upload EdgeWorker", "Upload EW");
        ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(new Runnable() {
                                                      @Override
                                                      public void run() {
                                                          try {
                                                              runCommandsInConsoleView(consoleView, commandLines);
                                                          } catch (ExecutionException e) {
                                                              e.printStackTrace();
                                                              Messages.showErrorDialog("EdgeWorker was not uploaded!", "Error");
                                                          }
                                                      }
                                                  },"Uploading...", false, project);
    }

    public GeneralCommandLine getEdgeWorkerDownloadCommand(String eid, String versionId, String downloadPath) throws Exception{
        ArrayList<String> downloadEdgeWorkerCmd = new ArrayList<>();
        downloadEdgeWorkerCmd.addAll(Arrays.asList("akamai", "edgeworkers", "download-version", eid, versionId, "--downloadPath", downloadPath));
        downloadEdgeWorkerCmd = addOptionsParams(downloadEdgeWorkerCmd);
        GeneralCommandLine downloadEdgeWorkerCmdLine = new GeneralCommandLine(downloadEdgeWorkerCmd);
        downloadEdgeWorkerCmdLine.setCharset(Charset.forName("UTF-8"));
        return downloadEdgeWorkerCmdLine;
    }

    public Integer downloadEdgeWorker(String eid, String versionId, String downloadPath) throws Exception{
        GeneralCommandLine commandLine = getEdgeWorkerDownloadCommand(eid, versionId, downloadPath);
        Integer exitCode = executeCommand(commandLine);
        return exitCode;
    }

    public GeneralCommandLine getExtractTgzFileCommand(String tgzFilePath, String extractDirectory) throws Exception{
        ArrayList<String> listEdgeWorkerVersionsCmd = new ArrayList<>();
        listEdgeWorkerVersionsCmd.addAll(Arrays.asList("tar", "-xvzf", tgzFilePath, "-C", extractDirectory));
        GeneralCommandLine commandLine = new GeneralCommandLine(listEdgeWorkerVersionsCmd);
        commandLine.setCharset(Charset.forName("UTF-8"));
        return commandLine;
    }

    public Integer extractTgzFile(String tgzFilePath, String extractDirectory) throws Exception{
        GeneralCommandLine commandLine = getExtractTgzFileCommand(tgzFilePath, extractDirectory);
        Integer exitCode = executeCommand(commandLine);
        if(null == exitCode || !exitCode.equals(0)){
            System.out.println(" extractTgzFile exitCode");
        }
        return exitCode;
    }

    public GeneralCommandLine getCLICommandLineByParams(String ...params){
        ArrayList<String> command = new ArrayList<>();
        command.addAll(Arrays.asList(params));
        GeneralCommandLine commandLine = new GeneralCommandLine(command);
        commandLine.setCharset(Charset.forName("UTF-8"));
        return commandLine;
    }

    public boolean checkIfAkamaiCliInstalled(){
        final boolean[] akamaiCliInstalled = {false};
        final Integer[] exitCode = {1};
        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                @Override
                public void run() {
                    try {
                        //check if akamai cli is installed
                        exitCode[0] = executeCommand(getCLICommandLineByParams("akamai", "--version"));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }, "Loading...", false, project);
            if (exitCode[0] == 1) {
                //when akamai cli is not installed
                CheckAkamaiCLIDialog checkAkamaiCLIDialog = new CheckAkamaiCLIDialog();
                checkAkamaiCLIDialog.show();
            } else {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            akamaiCliInstalled[0] = true;
                            String output = executeCommandAndGetOutput(getCLICommandLineByParams("akamai", "help"));
                            //install Akamai EdgeWorker CLI if not already installed
                            if (!output.contains("edgeworkers")) {
                                if (executeCommand(getCLICommandLineByParams("akamai", "install", "edgeworkers")) == 1) {
                                    akamaiCliInstalled[0] = false;
                                    System.out.println("Error came while installing akamai edgeworkers cli.");
                                }
                            }
                            //install Akamai sandbox CLI if not already installed
                            if (!output.contains("sandbox")) {
                                if (executeCommand(getCLICommandLineByParams("akamai", "install", "sandbox")) == 1) {
                                    akamaiCliInstalled[0] = false;
                                    System.out.println("Error came while installing akamai sandbox cli.");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, "Loading...", false, project);
            }
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return akamaiCliInstalled[0];
    }

    @Override
    public void dispose() {
        //gets executed when EdgeWorker's console view tabs are closed or intellij main window is closed
        console.dispose();
    }
}
