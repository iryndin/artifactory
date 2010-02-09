package org.artifactory.test.internal;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.DownloadService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockServer;
import org.artifactory.test.mock.MockTest;
import org.artifactory.test.mock.TestStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Creates a scenario where artifactory should send a conditional get method to the mock server
 *
 * @author Noam Tenne
 */
public class TestConditionalGet extends ArtifactoryTestBase {

    private final static Logger log = LoggerFactory.getLogger(TestConditionalGet.class);
    private final static String ARTIFACT = "ant/ant/1.6.5-SNAPSHOT/ant-1.6.5-SNAPSHOT.jar";
    private final static String TEST_NAME = "momo";
    private final static String MIME_TYPE = "application/octet-stream";
    private final static String ARTIFACT_PATH =
            "/mock/repo1/ant/ant/1.6.5-SNAPSHOT/ant-1.6.5-SNAPSHOT.jar";
    private final static String REPO_KEY = "libs-releases";
    private final static String REMOTE_REPO_CACHE_KEY = "localhost-repo-cache";
    private final static int ITERATIONS = 2;


    @BeforeClass
    public void setUp() throws Exception {
        log.info("Starting mock server");
        mockServer = MockServer.start();

        log.info("Adding mock test for path: " + ARTIFACT);
        MockTest test = new MockTest(TEST_NAME);
        test.addPath(new MockPathTest(ARTIFACT, MIME_TYPE, ARTIFACT_PATH));
        mockServer.addTest(test);
    }

    @Test
    public void testConditionalGet() throws IOException {

        log.info("Executing download: '" + ARTIFACT + "' from artifactory with " + ITERATIONS +
                " iterations");
        executeDownload(ARTIFACT, ITERATIONS);
        zap(ARTIFACT);
        executeDownload(ARTIFACT);

        TestStats testStats = mockServer.getTestStats(TEST_NAME);
        Assert.assertEquals(testStats.requestsPerMethod("HEAD"), 2);
        Assert.assertEquals(testStats.requestsPerMethod("GET"), 1);
    }

    private void executeDownload(String artifact) throws IOException {
        executeDownload(artifact, 1);
    }

    private void executeDownload(String artifact, int iterations) throws IOException {

        DownloadService downloadService = context.beanForType(DownloadService.class);

        for (int i = 0; i < iterations; i++) {
            log.info("Requesting: '" + ARTIFACT + "' from artifactory");

            ArtifactoryRequestStub request = new ArtifactoryRequestStub(REPO_KEY, artifact);
            ArtifactoryResponseStub response = new ArtifactoryResponseStub();
            downloadService.process(request, response);

            Assert.assertTrue(response.isSuccessful(),
                    "Failure status: " + response.getStatus() + ":" + response.getReason());
            Assert.assertTrue(response.getContentLength() > 0);
        }
    }

    private void zap(String artifact) {
        log.info("Zapping artifact: " + ARTIFACT);
        InternalRepositoryService repositoryService =
                context.beanForType(InternalRepositoryService.class);
        repositoryService.zap(new RepoPath(REMOTE_REPO_CACHE_KEY, artifact));
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }
}
