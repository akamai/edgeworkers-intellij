import actions.UploadEdgeWorkerAction;
import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ui.EdgeWorkerIdListDropdownInputDialog;
import utils.EdgeworkerWrapper;

public class UploadEdgeWorkerTest extends BasePlatformTestCase {

    protected CodeInsightTestFixture myFixture;
    EdgeworkerWrapper edgeworkerWrapperMock;
    AnActionEvent anActionEventMock;
    FileChooserDescriptor fileChooserDescriptorMock;
    UploadEdgeWorkerAction actionSpy;
    EdgeWorkerIdListDropdownInputDialog dialog;
    PsiFile psiFileMock;

    @Before
    public void setUp() throws Exception {
        edgeworkerWrapperMock = Mockito.mock(EdgeworkerWrapper.class);
        anActionEventMock = Mockito.mock(AnActionEvent.class);
        fileChooserDescriptorMock = Mockito.mock(FileChooserDescriptor.class);
        dialog = Mockito.mock(EdgeWorkerIdListDropdownInputDialog.class);
        psiFileMock = Mockito.mock(PsiFile.class);
        Mockito.when(anActionEventMock.getProject()).thenReturn(Mockito.mock(Project.class));
        actionSpy = Mockito.spy(new UploadEdgeWorkerAction("TestUploadEdgeWorkerAction", "Test UploadEdgeWorkerAction", null));
        Mockito.doReturn(fileChooserDescriptorMock).when(actionSpy).getFileChooserDescriptor();
        Mockito.doNothing().when(edgeworkerWrapperMock).uploadEdgeWorker(Mockito.any(), Mockito.any());
        Mockito.doNothing().when(actionSpy).showErrorDialog(Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(actionSpy).refreshEdgeWorkerList();
        Mockito.doReturn(dialog).when(actionSpy).getEdgeWorkerIdListDropdownInputDialog();
        Mockito.when(edgeworkerWrapperMock.checkIfAkamaiCliInstalled()).thenReturn(true);
        Mockito.doReturn(edgeworkerWrapperMock).when(actionSpy).getEdgeWorkerWrapper(Mockito.any());

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

    @Test
    public void test_actionPerformed_whenInvokedFromActionToolbarAndBundlePathIsPresent() throws Exception{
        Mockito.doReturn(new MockVirtualFile("bundlePath")).when(actionSpy).chooseEdgeWorkerBundlePath(Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(anActionEventMock).isFromActionToolbar();
        Mockito.doReturn(true).when(dialog).showAndGet();
        Mockito.doReturn("5418").when(dialog).getSelectedItem();
        actionSpy.actionPerformed(anActionEventMock);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
        Mockito.verify(actionSpy, Mockito.times(1)).getFileChooserDescriptor();
        Mockito.verify(actionSpy, Mockito.times(1)).chooseEdgeWorkerBundlePath(Mockito.any(), Mockito.any());
        Mockito.verify(actionSpy, Mockito.times(1)).getEdgeWorkerIdListDropdownInputDialog();
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).uploadEdgeWorker("5418", "MOCK_ROOT:/bundlePath");
        Mockito.verify(actionSpy, Mockito.times(1)).refreshEdgeWorkerList();
    }

    @Test
    public void test_actionPerformed_whenInvokedFromActionToolbarAndBundlePathIsNotPresent() throws Exception{
        Mockito.doReturn(null).when(actionSpy).chooseEdgeWorkerBundlePath(Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(anActionEventMock).isFromActionToolbar();
        actionSpy.actionPerformed(anActionEventMock);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
        Mockito.verify(actionSpy, Mockito.times(1)).getFileChooserDescriptor();
        Mockito.verify(actionSpy, Mockito.times(1)).chooseEdgeWorkerBundlePath(Mockito.any(), Mockito.any());
        Mockito.verify(actionSpy, Mockito.times(0)).getEdgeWorkerIdListDropdownInputDialog();
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(0)).uploadEdgeWorker("5418", "MOCK_ROOT:/bundlePath");
    }

    @Test
    public void test_actionPerformed_whenInvokedFromProjectMenuItemAndBundlePathIsPresent() throws Exception{
        VirtualFile vf = new MockVirtualFile("bundlePath");
        Mockito.doReturn(vf).when(psiFileMock).getVirtualFile();
        Mockito.doReturn(psiFileMock).when(anActionEventMock).getData(Mockito.any());
        Mockito.doReturn(false).when(anActionEventMock).isFromActionToolbar();
        Mockito.doReturn(true).when(dialog).showAndGet();
        Mockito.doReturn("5418").when(dialog).getSelectedItem();
        actionSpy.actionPerformed(anActionEventMock);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
        Mockito.verify(actionSpy, Mockito.times(0)).getFileChooserDescriptor();
        Mockito.verify(actionSpy, Mockito.times(0)).chooseEdgeWorkerBundlePath(Mockito.any(), Mockito.any());
        Mockito.verify(actionSpy, Mockito.times(1)).getEdgeWorkerIdListDropdownInputDialog();
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).uploadEdgeWorker("5418", "MOCK_ROOT:/bundlePath");
        Mockito.verify(actionSpy, Mockito.times(1)).refreshEdgeWorkerList();
    }

    @Test
    public void test_actionPerformed_whenInvokedFromProjectMenuItemAndBundlePathIsNotPresent() throws Exception{
        Mockito.doReturn(psiFileMock).when(anActionEventMock).getData(Mockito.any());
        Mockito.doReturn(null).when(psiFileMock).getVirtualFile();
        Mockito.doReturn(false).when(anActionEventMock).isFromActionToolbar();
        actionSpy.actionPerformed(anActionEventMock);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
        Mockito.verify(actionSpy, Mockito.times(0)).getFileChooserDescriptor();
        Mockito.verify(actionSpy, Mockito.times(0)).chooseEdgeWorkerBundlePath(Mockito.any(), Mockito.any());
        Mockito.verify(actionSpy, Mockito.times(0)).getEdgeWorkerIdListDropdownInputDialog();
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(0)).uploadEdgeWorker("5418", "MOCK_ROOT:/bundlePath");
        Mockito.verify(actionSpy, Mockito.times(0)).refreshEdgeWorkerList();
    }

    @Test
    public void test_actionPerformed_whenInvokedFromProjectMenuItemAndEdgeWorkerIdIsNull() throws Exception{
        VirtualFile vf = new MockVirtualFile("bundlePath");
        Mockito.doReturn(vf).when(psiFileMock).getVirtualFile();
        Mockito.doReturn(psiFileMock).when(anActionEventMock).getData(Mockito.any());
        Mockito.doReturn(false).when(anActionEventMock).isFromActionToolbar();
        Mockito.doReturn(true).when(dialog).showAndGet();
        Mockito.doReturn(null).when(dialog).getSelectedItem();
        actionSpy.actionPerformed(anActionEventMock);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
        Mockito.verify(actionSpy, Mockito.times(0)).getFileChooserDescriptor();
        Mockito.verify(actionSpy, Mockito.times(0)).chooseEdgeWorkerBundlePath(Mockito.any(), Mockito.any());
        Mockito.verify(actionSpy, Mockito.times(1)).getEdgeWorkerIdListDropdownInputDialog();
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(0)).uploadEdgeWorker("5418", "MOCK_ROOT:/bundlePath");
        Mockito.verify(actionSpy, Mockito.times(0)).refreshEdgeWorkerList();
    }

    @Test
    public void test_getFileChooserDescriptor() throws Exception{
        Mockito.doCallRealMethod().when(actionSpy).getFileChooserDescriptor();
        FileChooserDescriptor fileChooserDescriptor = actionSpy.getFileChooserDescriptor();
        assertEquals(fileChooserDescriptor.getTitle(), "Select EdgeWorker file");
        assertEquals(fileChooserDescriptor.getDescription(), "Select EdgeWorker tgz file");
        assertTrue(fileChooserDescriptor.isShowFileSystemRoots());
        assertTrue(fileChooserDescriptor.isForcedToUseIdeaFileChooser());
    }
//    @Test
//    public void test_actionPerformed_whenDownloadPathDoesNotExist_shouldNotInvokeDownloadAction() throws Exception{
//        Mockito.doReturn(null).when(actionSpy).chooseDownloadPath(Mockito.any(), Mockito.any());
//        actionSpy.actionPerformed(anActionEventMock);
//        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
//        Mockito.verify(actionSpy, Mockito.times(0)).invokeDownloadEdgeWorkerAction(Mockito.any(), Mockito.any(), Mockito.any());
//    }

//    public void test_getUploadEdgeWorkerCommand() throws Exception{
//        config.setAccountKey("testKey");
//        GeneralCommandLine commandLine = edgeworkerWrapper.getUploadEdgeWorkerCommand("123" , "tmpFile");
//        assertEquals("akamai [edgeworkers, upload, 123, --bundle, tmpFile, --accountkey, testKey]", commandLine.toString());
//    }

}
