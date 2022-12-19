import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.*;
import config.EdgeWorkersConfig;
import config.SettingsService;
import org.junit.After;
import org.junit.Before;
import utils.EdgeworkerWrapper;

import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateAndValidateBundleTest extends BasePlatformTestCase{

    protected CodeInsightTestFixture myFixture;
    EdgeWorkersConfig config;
    EdgeworkerWrapper edgeworkerWrapper;

    @Before
    public void setUp() throws Exception {
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder("");
        IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture);
        myFixture.setTestDataPath(getTestDataPath());
        myFixture.setUp();
        edgeworkerWrapper = new EdgeworkerWrapper(myFixture.getProject());
        myFixture.configureByFiles("bundle.json");
        config = SettingsService.getInstance().getState();
        config.setAccountKey("");
        config.setEdgercSectionName("");
        config.setEdgercFilePath("");
    }

    @After
    public void tearDown() throws Exception {
        myFixture.tearDown();
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData/";
    }

    public void test_getValidateBundleCommand_whenAccountKeyIsPresent() throws Exception{
        config.setAccountKey("testKey");
        GeneralCommandLine validateBundleCommand = edgeworkerWrapper.getValidateBundleCommand(myFixture.getTestDataPath(), myFixture.getFile().getParent().getVirtualFile());
        assertEquals("akamai [edgeworkers, validate, "+myFixture.getTempDirPath()+"/edgeworker_bundle.tgz, --accountkey, testKey]", validateBundleCommand.toString());
    }

    public void test_getValidateBundleCommand_whenAccountKeyAndEdgercSectionIsPresent() throws Exception{
        config.setAccountKey("testKey");
        config.setEdgercSectionName("default");
        GeneralCommandLine validateBundleCommand = edgeworkerWrapper.getValidateBundleCommand(myFixture.getTestDataPath(), myFixture.getFile().getParent().getVirtualFile());
        assertEquals("akamai [edgeworkers, validate, "+myFixture.getTempDirPath()+"/edgeworker_bundle.tgz, --section, default, --accountkey, testKey]", validateBundleCommand.toString());
    }

    public void test_getValidateBundleCommand_whenAccountKeyEdgercSectionAndPathIsPresent() throws Exception{
        config.setAccountKey("testKey2");
        config.setEdgercSectionName("default2");
        config.setEdgercFilePath("~/.edgerc");
        GeneralCommandLine validateBundleCommand = edgeworkerWrapper.getValidateBundleCommand(myFixture.getTestDataPath(), myFixture.getFile().getParent().getVirtualFile());
        assertEquals("akamai [edgeworkers, validate, "+myFixture.getTempDirPath()+"/edgeworker_bundle.tgz, --edgerc, ~/.edgerc, --section, default2, --accountkey, testKey2]", validateBundleCommand.toString());
    }

    public void test_createAndValidateBundle_whenMandatoryFilesArePresent() throws Exception{
        PsiFile[] psiFiles = myFixture.configureByFiles("bundle.json", "main.js");
        VirtualFile[] virtualFiles = new VirtualFile[2];
        virtualFiles[0] = psiFiles[0].getVirtualFile();
        virtualFiles[1] = psiFiles[1].getVirtualFile();

        System.out.println(myFixture.getTempDirPath()+"/edgeworker_bundle.tgz");
        //bundle file doesn't exist before running the create bundle command
        assertFalse(Files.exists(Paths.get(myFixture.getTempDirPath()+"/edgeworker_bundle.tgz")));

        int createBundleExitStatus = executeCreateBundleCommand(edgeworkerWrapper, virtualFiles);
        //0 exit code means no error came while running the create bundle command
        assertEquals(0, createBundleExitStatus);
        VfsUtil.markDirtyAndRefresh(false, false, true, myFixture.getFile().getParent().getVirtualFile());
        PsiElement[] es = myFixture.getFile().getParent().getChildren();

        //check if bundle file exist after running the create bundle command
        assertTrue(Files.exists(Paths.get(myFixture.getTempDirPath()+"/edgeworker_bundle.tgz")));

        GeneralCommandLine validateBundleCommand = edgeworkerWrapper.getValidateBundleCommand(myFixture.getTestDataPath(), virtualFiles[0].getParent());
        assertEquals("akamai [edgeworkers, validate, "+myFixture.getTempDirPath()+"/edgeworker_bundle.tgz]", validateBundleCommand.toString());
    }

    public void test_createAndValidateBundle_whenAllMandatoryFilesAreNotPresent() throws Exception{
        PsiFile[] psiFiles = myFixture.configureByFiles("bundle.json");
        VirtualFile[] virtualFiles = new VirtualFile[1];
        virtualFiles[0] = psiFiles[0].getVirtualFile();

        assertFalse(Files.exists(Paths.get(myFixture.getTempDirPath()+"/edgeworker_bundle.tgz")));

        int createBundleExitStatus = executeCreateBundleCommand(edgeworkerWrapper, virtualFiles);
        //0 exit code means no error came while running the create bundle command
        assertEquals(0, createBundleExitStatus);
        VfsUtil.markDirtyAndRefresh(false, false, true, myFixture.getFile().getParent().getVirtualFile());

        //check if bundle file exist after running the create bundle command
        assertTrue(Files.exists(Paths.get(myFixture.getTempDirPath()+"/edgeworker_bundle.tgz")));

        GeneralCommandLine validateBundleCommand = edgeworkerWrapper.getValidateBundleCommand(myFixture.getTestDataPath(), virtualFiles[0].getParent());
        assertEquals("akamai [edgeworkers, validate, "+myFixture.getTempDirPath()+"/edgeworker_bundle.tgz]", validateBundleCommand.toString());
    }

    private int executeCreateBundleCommand(EdgeworkerWrapper edgeworkerWrapper, VirtualFile[] virtualFiles) throws Exception{
        GeneralCommandLine createBundleCmd = edgeworkerWrapper.getCreateBundleCommand(myFixture.getTestDataPath(), virtualFiles, virtualFiles[0].getParent());
        ProcessHandler processHandler = new OSProcessHandler(createBundleCmd);
        processHandler.startNotify();
        processHandler.waitFor();
        return processHandler.getExitCode();
    }

}
