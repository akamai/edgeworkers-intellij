import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.*;
import config.EdgeWorkersConfig;
import config.SettingsService;
import org.junit.After;
import org.junit.Before;
import utils.EdgeworkerWrapper;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public class ListEdgeWorkersTest extends BasePlatformTestCase{

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

    public void test_getEdgeWorkerVersionListCommand() throws Exception{
        config.setAccountKey("testKey");
        GeneralCommandLine commandLine = edgeworkerWrapper.getEdgeWorkerVersionListCommand("123" , "tmpFile");
        assertEquals("akamai [edgeworkers, list-versions, 123, --json, tmpFile, --accountkey, testKey]", commandLine.toString());
    }

    public void test_getEdgeWorkersIdsListCommand() throws Exception{
        GeneralCommandLine commandLine = edgeworkerWrapper.getEdgeWorkersIdsListCommand( "tmpFile");
        assertEquals("akamai [edgeworkers, list-ids, --json, tmpFile]", commandLine.toString());
    }

    public void test_parseEdgeWorkersTempFile_whenListIds() throws Exception{
        PsiFile psiFile = myFixture.configureByFile("tempEdgeWorkersIds.json");
        String path = psiFile.getVirtualFile().getCanonicalPath();
        ArrayList<Map<String, String>> result = edgeworkerWrapper.parseEdgeWorkersTempFile("list-ids", Paths.get(path).toFile());
        assertEquals(result.size(), 2);
        assertEquals(result.toString(), "[{edgeWorkerId=8, name=ew_test}, {edgeWorkerId=9, name=EdgeWorker for MoFroYo Cart Count}]");
    }

    public void test_parseEdgeWorkersTempFile_whenListVersions() throws Exception{
        PsiFile psiFile = myFixture.configureByFile("tempEdgeWorkerVersions.json");
        String path = psiFile.getVirtualFile().getCanonicalPath();
        ArrayList<Map<String, String>> result = edgeworkerWrapper.parseEdgeWorkersTempFile("list-versions", Paths.get(path).toFile());
        assertEquals(result.size(), 8);
        assertEquals(result.toString(), "[{version=1.0}, {version=1.1}, {version=1}, {version=1.3}, {version=5.0}, {version=6.0}, {version=7.0}, {version=9.0}]");
    }
}
