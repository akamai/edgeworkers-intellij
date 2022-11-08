import actions.DownloadEdgeWorkerAction;
import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import utils.EdgeworkerWrapper;

public class DownloadEdgeWorkerTest extends BasePlatformTestCase{

    protected CodeInsightTestFixture myFixture;
    EdgeworkerWrapper edgeworkerWrapperMock;
    AnActionEvent anActionEventMock;
    FileChooserDescriptor fileChooserDescriptorMock;
    DownloadEdgeWorkerAction actionSpy;

    @Before
    public void setUp() throws Exception {
        edgeworkerWrapperMock = Mockito.mock(EdgeworkerWrapper.class);
        anActionEventMock = Mockito.mock(AnActionEvent.class);
        fileChooserDescriptorMock = Mockito.mock(FileChooserDescriptor.class);
        Mockito.when(anActionEventMock.getProject()).thenReturn(Mockito.mock(Project.class));
        actionSpy = Mockito.spy(new DownloadEdgeWorkerAction("Test", "Test DownloadEdgeWorkerAction", null));
        Mockito.doReturn(fileChooserDescriptorMock).when(actionSpy).getFileChooserDescriptor();
        Mockito.doNothing().when(actionSpy).addTextFieldWithBrowseButton(fileChooserDescriptorMock);
        Mockito.doNothing().when(actionSpy).invokeDownloadEdgeWorkerAction(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(actionSpy).notifyError(anActionEventMock, "Error: Downloading EdgeWorker failed!");
        Mockito.when(edgeworkerWrapperMock.checkIfAkamaiCliInstalled()).thenReturn(true);
        Mockito.doReturn(edgeworkerWrapperMock).when(actionSpy).getEdgeWorkerWrapper(Mockito.any());

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
    public void test_actionPerformed_whenDownloadPathExist_shouldInvokeDownloadAction() throws Exception{
        VirtualFile[] vfs = new VirtualFile[1];
        vfs[0]=new MockVirtualFile("downloadPath");
        Mockito.doReturn(vfs).when(actionSpy).chooseDownloadPath(Mockito.any(), Mockito.any());
        actionSpy.actionPerformed(anActionEventMock);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
        Mockito.verify(actionSpy, Mockito.times(1)).invokeDownloadEdgeWorkerAction(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void test_actionPerformed_whenDownloadPathDoesNotExist_shouldNotInvokeDownloadAction() throws Exception{
        Mockito.doReturn(null).when(actionSpy).chooseDownloadPath(Mockito.any(), Mockito.any());
        actionSpy.actionPerformed(anActionEventMock);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).checkIfAkamaiCliInstalled();
        Mockito.verify(actionSpy, Mockito.times(0)).invokeDownloadEdgeWorkerAction(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void test_getFileChooserDescriptor() throws Exception{
        Mockito.doCallRealMethod().when(actionSpy).getFileChooserDescriptor();
        FileChooserDescriptor fileChooserDescriptor = actionSpy.getFileChooserDescriptor();
        assertEquals(fileChooserDescriptor.getTitle(), "Select Download Folder");
        assertEquals(fileChooserDescriptor.getDescription(), "EdgeWorker Download Folder");
        assertTrue(fileChooserDescriptor.isShowFileSystemRoots());
        assertTrue(fileChooserDescriptor.isForcedToUseIdeaFileChooser());
    }

    @Test
    public void test_invokeDownloadEdgeWorkerAction_whenNoError() throws Exception{
        Mockito.doCallRealMethod().when(actionSpy).invokeDownloadEdgeWorkerAction(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.doReturn(0).when(edgeworkerWrapperMock).downloadEdgeWorker(Mockito.any(),Mockito.any(),Mockito.any());
        VirtualFile[] vfs = new VirtualFile[1];
        vfs[0]=new MockVirtualFile("downloadPath");
        actionSpy.invokeDownloadEdgeWorkerAction(edgeworkerWrapperMock, anActionEventMock, vfs);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).downloadEdgeWorker(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(actionSpy, Mockito.times(0)).notifyError(anActionEventMock, "Error: Downloading EdgeWorker failed!");
    }

    @Test
    public void test_invokeDownloadEdgeWorkerAction_whenError() throws Exception{
        Mockito.doCallRealMethod().when(actionSpy).invokeDownloadEdgeWorkerAction(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.doReturn(1).when(edgeworkerWrapperMock).downloadEdgeWorker(Mockito.any(),Mockito.any(),Mockito.any());
        VirtualFile[] vfs = new VirtualFile[1];
        vfs[0]=new MockVirtualFile("downloadPath");
        actionSpy.invokeDownloadEdgeWorkerAction(edgeworkerWrapperMock, anActionEventMock, vfs);
        Mockito.verify(edgeworkerWrapperMock, Mockito.times(1)).downloadEdgeWorker(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(actionSpy, Mockito.times(1)).notifyError(anActionEventMock, "Error: Downloading EdgeWorker failed!");
    }

}
