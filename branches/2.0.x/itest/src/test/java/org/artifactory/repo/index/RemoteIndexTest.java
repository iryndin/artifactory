package org.artifactory.repo.index;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.common.ConstantsValue;
import org.artifactory.test.http.ArtifactoryServer;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockServer;
import org.artifactory.test.mock.MockTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RemoteIndexTest {

    private static final String INDEX_ARTIFACT = "/export/index/" + MavenNaming.NEXUS_INDEX_ZIP;
    private static final String INDEX_ARTIFACT_PATH = MavenNaming.NEXUS_INDEX_DIR + "/" + MavenNaming.NEXUS_INDEX_ZIP;

    private ArtifactoryServer artifactory;
    private MockServer mockServer;

    @BeforeClass
    public void setup() throws Exception {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(RemoteIndexTest.class).setLevel(Level.DEBUG);
        lc.getLogger(IndexerService.class.getPackage().getName()).setLevel(Level.DEBUG);

        //Make sure we didn't refetch the index
        System.setProperty(ConstantsValue.mvnCentralHostPattern.getPropertyName(), "localhost");
        System.setProperty(ConstantsValue.mvnCentralIndexerMaxQueryIntervalSecs.getPropertyName(), "20");

        //Configure the mock server
        MockTest mockTest = new MockTest("momo");
        MockPathTest mockPathTest =
                new MockPathTest(INDEX_ARTIFACT_PATH, "application/zip", INDEX_ARTIFACT, System.currentTimeMillis());
        mockPathTest.lastModified = System.currentTimeMillis();
        mockTest.addPath(mockPathTest);
        mockServer = MockServer.start();
        mockServer.addTest(mockTest);

        artifactory = new ArtifactoryServer("localhost-repo", mockServer);
        artifactory.start();
    }

    @AfterClass
    public void stop() throws Exception {
        artifactory.stop();
    }

    @Test
    public void getRemoteIndex() throws Exception {
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod("http://localhost:8081/artifactory/repo/" + INDEX_ARTIFACT_PATH);
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_OK);
        String lastModified = getMethod.getResponseHeader("last-modified").getValue();

        //Replace the index artifact with a "newer" version
        MockPathTest mockPathTest =
                new MockPathTest(INDEX_ARTIFACT_PATH, "application/zip", INDEX_ARTIFACT, System.currentTimeMillis());
        mockPathTest.lastModified = System.currentTimeMillis();
        mockServer.changeTest("momo", mockPathTest);

        //Expect to see the cached, non-updated artifact
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_OK);
        Assert.assertEquals(getMethod.getResponseHeader("last-modified").getValue(), lastModified);
    }
}