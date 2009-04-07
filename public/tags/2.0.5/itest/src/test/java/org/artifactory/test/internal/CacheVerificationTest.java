package org.artifactory.test.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.api.request.DownloadService;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockTest;
import org.artifactory.test.mock.TestStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Tests the cache behavior with the MockServer as a remote repo
 *
 * @author Noam Tenne
 */
public class CacheVerificationTest extends ArtifactoryTestBase {

    private final static Logger log = LoggerFactory.getLogger(TestConditionalGet.class);
    private final static String TEST_NAME = "momo";
    private final static String MIME_TYPE = "application/octet-stream";
    private final static String ARTIFACT_PATH = "/mock/repo1/ant/ant/1.6.5/ant-1.6.5.jar";
    private final static String REPO_KEY = "libs-releases";
    private final static String REMOTE_REPO_KEY = "localhost-repo";
    private final static String REMOTE_REPO_SLOW_KEY = "localhost-repo-slow";
    private final static boolean BREAK_PIPE = false;

    @BeforeClass
    public void setUp() throws Exception {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(TestConditionalGet.class).setLevel(Level.DEBUG);
        log.info("Starting mock server");
    }

    @Test
    public void testSnapshotCacheVerification() throws IOException {
        final String artifactPath = "ant/ant/1.6.5-SNAPSHOT/ant-1.6.5-SNAPSHOT.jar";
        testCacheVerification(artifactPath, new AssertTestStats() {
            public void assertTestStats(TestStats stats) {
                int nbGet = stats.requestsPerMethod(artifactPath, "GET");
                int nbHead = stats.requestsPerMethod(artifactPath, "HEAD");
                Assert.assertEquals(nbGet, 0);
                Assert.assertEquals(nbHead, 0);
            }
        });
    }

    @Test
    public void testStandardCacheVerification() throws IOException {
        final String artifactPath = "ant/ant/1.6.5/ant-1.6.5.jar";
        testCacheVerification(artifactPath, new AssertTestStats() {
            public void assertTestStats(TestStats stats) {
                int nbGet = stats.requestsPerMethod(artifactPath, "GET");
                int nbHead = stats.requestsPerMethod(artifactPath, "HEAD");
                Assert.assertEquals(nbGet, 1);
                Assert.assertEquals(nbHead, 6);
            }
        });
    }

    private void testCacheVerification(String artifactPath, AssertTestStats func) throws IOException {

        log.info("Adding mock test for path: " + artifactPath);
        MockTest test = new MockTest(TEST_NAME);

        clearCache(artifactPath);

        testFailed(test, artifactPath);
        testAfterFailed(test, artifactPath, func);

        clearCache(artifactPath);

        testNormal(test, artifactPath);
        testDelayed(test, artifactPath);

        clearCache(artifactPath);

        testNormal(test, artifactPath);
    }

    /**
     * Ask for a file with deliberate failure. Expect failure
     *
     * @param test
     * @param artifactPath
     * @throws IOException
     */
    private void testFailed(MockTest test, String artifactPath) throws IOException {
        ArtifactoryResponseStub failedResponse = makeDelayedRequest(test, true, artifactPath);
        Assert.assertTrue(!failedResponse.isSuccessful());
        TestStats failedStats = mockServer.getTestStats(TEST_NAME);
        Assert.assertTrue(failedStats.requestsPerReturnCode(404) > 0);
    }

    /**
     * Ask for a file normally while still in missed cache list. Expect failure
     *
     * @param test
     * @param artifactPath
     * @param func
     * @throws IOException
     */
    private void testAfterFailed(MockTest test, String artifactPath, AssertTestStats func) throws IOException {
        ArtifactoryResponseStub afterFailedResponse = makeNormalRequest(test, artifactPath);
        Assert.assertFalse(afterFailedResponse.isSuccessful());
        log.debug("Received failed request reason {}", afterFailedResponse.getReason());
        TestStats afterFailedStats = mockServer.getTestStats(TEST_NAME);
        func.assertTestStats(afterFailedStats);
    }

    interface AssertTestStats {
        public void assertTestStats(TestStats stats);
    }

    /**
     * Ask for a file normally after zapping missed cache list. Expect success
     *
     * @param test
     * @param artifactPath
     * @throws IOException
     */
    private void testNormal(MockTest test, String artifactPath) throws IOException {
        ArtifactoryResponseStub normalResponse = makeNormalRequest(test, artifactPath);
        Assert.assertTrue(normalResponse.isSuccessful(), normalResponse.getReason());
        TestStats normalStats = mockServer.getTestStats(TEST_NAME);
        Assert.assertTrue(normalStats.requestsPerMethod(artifactPath, "GET") > 0);
        Assert.assertTrue(normalStats.requestsPerMethod(artifactPath, "HEAD") > 0);
    }

    /**
     * Ask for a file with delay after successful retrieval. Expect return from cache
     *
     * @param test
     * @param artifactPath
     * @throws IOException
     */
    private void testDelayed(MockTest test, String artifactPath) throws IOException {
        ArtifactoryResponseStub delayedResponse = makeDelayedRequest(test, false, artifactPath);
        Assert.assertTrue(delayedResponse.isSuccessful(), delayedResponse.getReason());
        TestStats delayedStats = mockServer.getTestStats(TEST_NAME);
        Assert.assertTrue(delayedStats.requestsPerMethod(artifactPath, "GET") < 3);
        Assert.assertTrue(delayedStats.requestsPerMethod(artifactPath, "HEAD") < 3);
    }

    /**
     * Make a delayed request with options of failing and breaking the pipe
     *
     * @param test
     * @param fail
     * @param artifactPath
     * @return ArtifactoryResponseStub
     * @throws IOException
     */
    private ArtifactoryResponseStub makeDelayedRequest(MockTest test, boolean fail,
            String artifactPath)
            throws IOException {
        MockPathTest delayedPath;

        if (BREAK_PIPE) {
            delayedPath = new MockPathTest(artifactPath, MIME_TYPE, ARTIFACT_PATH, 2, 120, 100);
        } else {
            delayedPath = new MockPathTest(artifactPath, MIME_TYPE, ARTIFACT_PATH, 2, 120);
        }

        if (fail) {
            delayedPath.returnCode = 404;
        }

        MockTest exisiting = mockServer.getTest(TEST_NAME);

        if (exisiting == null) {
            test.addPath(delayedPath);
            mockServer.addTest(test);
        } else {
            mockServer.changeTest(test.getName(), delayedPath);
        }
        ArtifactoryResponseStub delayedResponse = executeDownload(artifactPath, artifactPath);

        return delayedResponse;
    }

    /**
     * Make a normal request
     *
     * @param test
     * @param artifactPath
     * @return ArtifactoryResponseStub
     * @throws IOException
     */
    private ArtifactoryResponseStub makeNormalRequest(MockTest test, String artifactPath)
            throws IOException {
        MockPathTest afterFailedPath = new MockPathTest(artifactPath, MIME_TYPE, ARTIFACT_PATH);
        mockServer.changeTest(test.getName(), afterFailedPath);
        ArtifactoryResponseStub normalResponse = executeDownload(artifactPath, artifactPath);

        return normalResponse;
    }

    /**
     * Execute the download of a given artifact
     *
     * @param artifact
     * @param artifactPath
     * @return ArtifactoryResponseStub
     * @throws IOException
     */
    private ArtifactoryResponseStub executeDownload(String artifact, String artifactPath)
            throws IOException {
        log.info("Requesting: '" + artifactPath + "' from artifactory");
        DownloadService downloadService = context.beanForType(DownloadService.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(REPO_KEY, artifact);
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        downloadService.process(request, response);
        return response;
    }

    /**
     * Clear the missed and failed cache lists
     *
     * @param artifact
     */
    private void clearCache(String artifact) {
        log.info("Zapping artifact: " + artifact);
        InternalRepositoryService repositoryService =
                context.beanForType(InternalRepositoryService.class);
        RemoteRepo repo = repositoryService.remoteRepositoryByKey(REMOTE_REPO_KEY);
        repo.clearCaches();
        repo = repositoryService.remoteRepositoryByKey(REMOTE_REPO_SLOW_KEY);
        repo.clearCaches();
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }
}
