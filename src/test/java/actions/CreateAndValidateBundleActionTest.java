package actions;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.*;
import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import utils.EdgeworkerWrapper;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CreateAndValidateBundleActionTest extends BasePlatformTestCase{

    protected CodeInsightTestFixture myFixture;

    @Before
    public void setUp() throws Exception {
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder();
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

    public void test_createAndValidateBundle_whenMandatoryFilesArePresent() throws Exception{
        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(myFixture.getProject());
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
        for(PsiElement e : es){
            System.out.println(e);
        }
        //check if bundle file exist after running the create bundle command
        assertTrue(Files.exists(Paths.get(myFixture.getTempDirPath()+"/edgeworker_bundle.tgz")));

        int validateBundleExitStatus = executeValidateBundleCommand(edgeworkerWrapper, virtualFiles);
        //0 exit code means no error came while running validate bundle command
        assertEquals(0, validateBundleExitStatus);
    }

    public void test_createAndValidateBundle_whenAllMandatoryFilesAreNotPresent() throws Exception{
        EdgeworkerWrapper edgeworkerWrapper = new EdgeworkerWrapper(myFixture.getProject());
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

        int validateBundleExitStatus = executeValidateBundleCommand(edgeworkerWrapper, virtualFiles);
        //exit code 1 means validate bundle command failed
        assertEquals(1, validateBundleExitStatus);
    }

    private int executeCreateBundleCommand(EdgeworkerWrapper edgeworkerWrapper, VirtualFile[] virtualFiles) throws Exception{
        GeneralCommandLine createBundleCmd = edgeworkerWrapper.getCreateBundleCommand(myFixture.getTestDataPath(), virtualFiles, virtualFiles[0].getParent());
        System.out.println(createBundleCmd);
        ProcessHandler processHandler = new OSProcessHandler(createBundleCmd);
        processHandler.startNotify();
        processHandler.waitFor();
        System.out.println(processHandler.getExitCode());
        return processHandler.getExitCode();
    }

    private int executeValidateBundleCommand(EdgeworkerWrapper edgeworkerWrapper, VirtualFile[] virtualFiles) throws Exception{
        GeneralCommandLine validateBundleCmd = edgeworkerWrapper.getValidateBundleCommand(myFixture.getTestDataPath(), virtualFiles, virtualFiles[0].getParent());
        System.out.println(validateBundleCmd);
        ProcessHandler processHandler = new OSProcessHandler(validateBundleCmd);
        processHandler.startNotify();
        processHandler.waitFor();
        System.out.println(processHandler.getExitCode());
        return processHandler.getExitCode();
    }

}
