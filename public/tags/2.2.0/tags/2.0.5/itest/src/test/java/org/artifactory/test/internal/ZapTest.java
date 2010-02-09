package org.artifactory.test.internal;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.DownloadService;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockTest;
import org.artifactory.test.mock.TestStats;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Tests that failed snapshots are being zapped properly and removed from cache
 *
 * @author Noam Tenne
 */
public class ZapTest extends ArtifactoryTestBase {

    private static final String APPLICATION_ZIP = "application/zip";
    private final static String ARTIFACT = "mysql/connector-java/5.1.6-SNAPSHOT/connector-java-5.1.6-SNAPSHOT.jar";
    private static final String ARTIFACT_PATH =
            "/export/VersionTest/repositories/libs-snapshots-local/" + ARTIFACT;
    private final static String REPO_KEY = "localhost-repo";
    private final static String TEST_NAME = "momo";

    private InternalRepositoryService repositoryService;
    private RepoPath repoPath;
    private MockTest mockTest = new MockTest(TEST_NAME);
    private MockPathTest mockPathTest = new MockPathTest(ARTIFACT, APPLICATION_ZIP, ARTIFACT_PATH);

    /**
     * Set different elements which are needed in order to execute the test
     */
    @BeforeClass
    public void setup() {
        repositoryService = context.beanForType(InternalRepositoryService.class);
        RemoteRepo repo = repositoryService.remoteRepositoryByKey(REPO_KEY);
        String localCacheKey = repo.getLocalCacheRepo().getKey();
        repoPath = new RepoPath(localCacheKey, ARTIFACT);
        mockTest.addPath(mockPathTest);
        mockServer.addTest(mockTest);
    }

    /**
     * Tests that the zap works properly
     *
     * @throws IOException Might occur in the download engine
     */
    @Test
    public void testSnapshotFailed() throws IOException {

        retrieveSuccessfully();

        ItemInfo itemInfoBeforeZap = repositoryService.getItemInfo(repoPath);
        long ageBeforeZap = itemInfoBeforeZap.getInernalXmlInfo().getLastUpdated();

        mockPathTest.returnCode = HttpStatus.SC_NOT_FOUND;
        mockServer.changeTest(TEST_NAME, mockPathTest);

        retrieveFailed();

        repositoryService.zap(repoPath);

        ItemInfo itemInfoAfterZap = repositoryService.getItemInfo(repoPath);
        long ageAfterZap = itemInfoAfterZap.getInernalXmlInfo().getLastUpdated();

        Assert.assertTrue(ageBeforeZap > ageAfterZap);

        retrieveSuccessfully();
    }

    /**
     * Try to retrieve an artifact and fail on purpose
     *
     * @throws IOException Might occur in the download engine
     */
    private void retrieveFailed() throws IOException {
        ArtifactoryResponseStub firstFailedResponse = retrieveResource();
        Assert.assertTrue(!firstFailedResponse.isSuccessful());

        ArtifactoryResponseStub secondFailedResponse = retrieveResource();
        Assert.assertTrue(!secondFailedResponse.isSuccessful());

        TestStats failedStats = mockServer.getTestStats(TEST_NAME);
        int numberOfRequests = failedStats.requestsPerReturnCode(HttpStatus.SC_NOT_FOUND);
        Assert.assertTrue(numberOfRequests == 1);
    }

    /**
     * Try to retrieve and artifact successfully
     *
     * @throws IOException Might occur in the download engine
     */
    private void retrieveSuccessfully() throws IOException {
        mockPathTest.returnCode = HttpStatus.SC_OK;
        mockServer.changeTest(TEST_NAME, mockPathTest);
        ArtifactoryResponseStub successfullResponse = retrieveResource();
        Assert.assertTrue(successfullResponse.isSuccessful());
    }

    /**
     * Try to retrieve an artifact from the download service
     *
     * @return ArtifactoryResponseStub - The response stub
     * @throws IOException Might occur in the download engine
     */
    private ArtifactoryResponseStub retrieveResource() throws IOException {
        DownloadService downloadService = context.beanForType(DownloadService.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(REPO_KEY, ARTIFACT);
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        downloadService.process(request, response);
        return response;
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }
}