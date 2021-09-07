import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.testFramework.fixtures.*;
import config.EdgeWorkersConfig;
import config.SettingsService;
import org.junit.After;
import org.junit.Before;
import utils.EdgeworkerWrapper;

public class UploadEdgeWorkerTest extends BasePlatformTestCase {

    protected CodeInsightTestFixture myFixture;
    EdgeWorkersConfig config;
    EdgeworkerWrapper edgeworkerWrapper;

    @Before
    public void setUp() throws Exception {
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder();
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

    public void test_getUploadEdgeWorkerCommand() throws Exception{
        config.setAccountKey("testKey");
        GeneralCommandLine commandLine = edgeworkerWrapper.getUploadEdgeWorkerCommand("123" , "tmpFile");
        assertEquals("akamai [edgeworkers, upload, 123, --bundle, tmpFile, --accountkey, testKey]", commandLine.toString());
    }

}
