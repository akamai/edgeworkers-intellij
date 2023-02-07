import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.*;
import config.EdgeWorkersConfig;
import config.SettingsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import utils.EdgeworkerWrapper;
import java.util.Arrays;

import java.util.ArrayList;

public class EdgeWorkerWrapperTest extends BasePlatformTestCase {
    protected CodeInsightTestFixture myFixture;
    EdgeworkerWrapper edgeworkerWrapperSpy;
    AnActionEvent anActionEventMock;
    PsiFile psiFileMock;
    GeneralCommandLine generalCommandLineMock;

    @Before
    public void setUp() throws Exception {
        anActionEventMock = Mockito.mock(AnActionEvent.class);
        psiFileMock = Mockito.mock(PsiFile.class);
        generalCommandLineMock = Mockito.mock(GeneralCommandLine.class);
        Mockito.when(anActionEventMock.getProject()).thenReturn(Mockito.mock(Project.class));
        edgeworkerWrapperSpy = Mockito.spy(new EdgeworkerWrapper());
        Mockito.doNothing().when(edgeworkerWrapperSpy).showErrorDialog(Mockito.anyString(), Mockito.anyString());

        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder("");
        IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture);
        myFixture.setTestDataPath(getTestDataPath());
        myFixture.setUp();
    }

    @After
    public void tearDown() throws Exception {
        myFixture.tearDown();
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/";
    }


    @Test
    public void test_checkIfAkamaiCliInstalled_whenEdgeWorkerCLINotInstalled() throws Exception{
        Mockito.doReturn("akamai").when(edgeworkerWrapperSpy).executeCommandAndGetOutput(Mockito.any());
        Mockito.doReturn(generalCommandLineMock).when(edgeworkerWrapperSpy).getCLICommandLineByParams("akamai", "help");
        GeneralCommandLine edgeWorkerInstallGCL = Mockito.mock(GeneralCommandLine.class);
        Mockito.doReturn(edgeWorkerInstallGCL).when(edgeworkerWrapperSpy).getCLICommandLineByParams("akamai", "install", "edgeworkers");
        Mockito.doReturn(1).when(edgeworkerWrapperSpy).executeCommand(edgeWorkerInstallGCL);
        GeneralCommandLine sandboxInstalledGCL = Mockito.mock(GeneralCommandLine.class);
        Mockito.doReturn(sandboxInstalledGCL).when(edgeworkerWrapperSpy).getCLICommandLineByParams("akamai", "install", "sandbox");
        Mockito.doReturn(0).when(edgeworkerWrapperSpy).executeCommand(sandboxInstalledGCL);
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).checkIfAkamaiCliInstalled();
        Boolean result = edgeworkerWrapperSpy.checkIfAkamaiCliInstalled();
        assertFalse(result);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).executeCommandAndGetOutput(generalCommandLineMock);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).executeCommand(edgeWorkerInstallGCL);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).executeCommand(sandboxInstalledGCL);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).showErrorDialog("Please install akamai edgeworkers cli", "Error");
    }

    @Test
    public void test_checkIfAkamaiCliInstalled_whenSandboxCLINotInstalled() throws Exception{
        Mockito.doReturn("akamai").when(edgeworkerWrapperSpy).executeCommandAndGetOutput(Mockito.any());
        Mockito.doReturn(generalCommandLineMock).when(edgeworkerWrapperSpy).getCLICommandLineByParams("akamai", "help");
        GeneralCommandLine edgeWorkerInstallGCL = Mockito.mock(GeneralCommandLine.class);
        Mockito.doReturn(edgeWorkerInstallGCL).when(edgeworkerWrapperSpy).getCLICommandLineByParams("akamai", "install", "edgeworkers");
        Mockito.doReturn(0).when(edgeworkerWrapperSpy).executeCommand(edgeWorkerInstallGCL);
        GeneralCommandLine sandboxInstalledGCL = Mockito.mock(GeneralCommandLine.class);
        Mockito.doReturn(sandboxInstalledGCL).when(edgeworkerWrapperSpy).getCLICommandLineByParams("akamai", "install", "sandbox");
        Mockito.doReturn(1).when(edgeworkerWrapperSpy).executeCommand(sandboxInstalledGCL);
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).checkIfAkamaiCliInstalled();
        Boolean result = edgeworkerWrapperSpy.checkIfAkamaiCliInstalled();
        assertFalse(result);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).executeCommandAndGetOutput(generalCommandLineMock);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).executeCommand(edgeWorkerInstallGCL);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).executeCommand(sandboxInstalledGCL);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).showErrorDialog("Please install akamai sandbox cli", "Error");
    }
    @Test
    public void test_checkIfIdeExtensionTypeOptionIsAddedWhenAkamaiCLiIsGood() throws Exception{
        ArrayList<String> cmd = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();
        cmd.addAll(Arrays.asList("--edgerc"));
        Mockito.doReturn(true).when(edgeworkerWrapperSpy).addIdeExtensionOptionWithCorrectAkamaiVersion();
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).addOptionsParams(Mockito.any());
        result  = edgeworkerWrapperSpy.addOptionsParams(cmd);
        assertEquals(result.contains("INTELLIJ"),true);

    }
    @Test
    public void test_checkIfIdeExtensionTypeOptionIsAddedWhenAkamaiCLiIsNotGood() throws Exception{
        ArrayList<String> cmd = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();
        cmd.addAll(Arrays.asList("--edgerc"));
        Mockito.doReturn(false).when(edgeworkerWrapperSpy).addIdeExtensionOptionWithCorrectAkamaiVersion();
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).addOptionsParams(Mockito.any());
        result  = edgeworkerWrapperSpy.addOptionsParams(cmd);
        assertEquals(result.contains("INTELLIJ"),false);
    }

    @Test
    public void test_checkIfAkamaiCliInstalled_whenEdgercFileDoesNotExist() throws Exception{
        EdgeWorkersConfig edgeWorkersConfig = SettingsService.getInstance().getState();
        EdgeWorkersConfig config = Mockito.mock(EdgeWorkersConfig.class);
        Mockito.doReturn("edgercPathMock").when(config).getEdgercFilePath();
        SettingsService.getInstance().updateConfig(config);
        GeneralCommandLine edgeWorkerInstallGCL = Mockito.mock(GeneralCommandLine.class);
        GeneralCommandLine sandboxInstalledGCL = Mockito.mock(GeneralCommandLine.class);
        Mockito.doReturn("When akamai, edgeworkers, and sandbox cli are installed").when(edgeworkerWrapperSpy).executeCommandAndGetOutput(Mockito.any());
        Mockito.doReturn(generalCommandLineMock).when(edgeworkerWrapperSpy).getCLICommandLineByParams("akamai", "help");
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).checkIfAkamaiCliInstalled();
        Boolean result = edgeworkerWrapperSpy.checkIfAkamaiCliInstalled();
        assertFalse(result);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).executeCommandAndGetOutput(generalCommandLineMock);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(0)).executeCommand(edgeWorkerInstallGCL);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(0)).executeCommand(sandboxInstalledGCL);
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).showErrorDialog("Please create and setup .edgerc file and configure EdgeWorkers settings at IntelliJ IDEA > Preferences/Settings > EdgeWorkers Configuration", "Error");
        SettingsService.getInstance().updateConfig(edgeWorkersConfig);
    }

    @Test
    public void test_getCLICommandLineByParams(){
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).getCLICommandLineByParams();
        assertEquals(edgeworkerWrapperSpy.getCLICommandLineByParams("akamai", "help").getCommandLineString(), "akamai help");
        assertEquals(edgeworkerWrapperSpy.getCLICommandLineByParams("akamai", "install", "edgeworkers").getCommandLineString(), "akamai install edgeworkers");
    }

    @Test
    public void test_updateEdgeWorkerToSandbox_whenSandboxNotCreated() throws Exception {
        ConsoleView consoleViewMock = Mockito.mock(ConsoleView.class);
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).updateEdgeWorkerToSandbox("5814", "bundlePathMock");
        Mockito.doReturn(consoleViewMock).when(edgeworkerWrapperSpy).createConsoleViewOnNewTabOfToolWindow(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("ERROR: Unable to determine sandbox-id").when(edgeworkerWrapperSpy).runCommandsInConsoleView(Mockito.any(), Mockito.any());
        Mockito.doReturn(Mockito.mock(GeneralCommandLine.class)).when(edgeworkerWrapperSpy).getUpdateEdgeWorkerToSandboxCommand("5814", "bundlePathMock");
        edgeworkerWrapperSpy.updateEdgeWorkerToSandbox("5814", "bundlePathMock");
        Mockito.verify(consoleViewMock, Mockito.times(1)).print("Create a sandbox with the CLI: https://learn.akamai.com/en-us/webhelp/sandbox/sandbox-user-guide/GUID-0D12845D-255E-4054-8A1D-59D11B931B81.html", ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    @Test
    public void test_updateEdgeWorkerToSandbox_whenNoError() throws Exception {
        ConsoleView consoleViewMock = Mockito.mock(ConsoleView.class);
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).updateEdgeWorkerToSandbox("5814", "bundlePathMock");
        Mockito.doReturn(consoleViewMock).when(edgeworkerWrapperSpy).createConsoleViewOnNewTabOfToolWindow(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("").when(edgeworkerWrapperSpy).runCommandsInConsoleView(Mockito.any(), Mockito.any());
        Mockito.doReturn(Mockito.mock(GeneralCommandLine.class)).when(edgeworkerWrapperSpy).getUpdateEdgeWorkerToSandboxCommand("5814", "bundlePathMock");
        edgeworkerWrapperSpy.updateEdgeWorkerToSandbox("5814", "bundlePathMock");
        Mockito.verify(consoleViewMock, Mockito.times(1)).print(
                "Use curl or a browser to test the functionality. \n" +
                "Curl: Run this command curl --header 'Host: www.example.com' http://127.0.0.1:9550/ \n" +
                "Browser: Open your /etc/hosts file and point the hostname associated with the property configuration to 127.0.0.1, then enter http://<your-hostname>:9550 in your browser.",
                ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    @Test
    public void test_updateEdgeWorkerToSandbox_whenConsoleViewThrowsException() throws Exception {
        ConsoleView consoleViewMock = Mockito.mock(ConsoleView.class);
        Mockito.doCallRealMethod().when(edgeworkerWrapperSpy).updateEdgeWorkerToSandbox("5814", "bundlePathMock");
        Mockito.doReturn(consoleViewMock).when(edgeworkerWrapperSpy).createConsoleViewOnNewTabOfToolWindow(Mockito.anyString(), Mockito.anyString());
        Mockito.doThrow(ExecutionException.class).when(edgeworkerWrapperSpy).runCommandsInConsoleView(Mockito.any(), Mockito.any());
        Mockito.doReturn(Mockito.mock(GeneralCommandLine.class)).when(edgeworkerWrapperSpy).getUpdateEdgeWorkerToSandboxCommand("5814", "bundlePathMock");
        edgeworkerWrapperSpy.updateEdgeWorkerToSandbox("5814", "bundlePathMock");
        Mockito.verify(edgeworkerWrapperSpy, Mockito.times(1)).showErrorDialog("EdgeWorker was not updated to the Sandbox!", "Error");
    }
}
