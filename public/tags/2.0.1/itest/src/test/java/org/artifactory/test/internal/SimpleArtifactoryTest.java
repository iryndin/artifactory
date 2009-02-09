package org.artifactory.test.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.DownloadService;
import org.artifactory.engine.InternalUploadService;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class SimpleArtifactoryTest extends ArtifactoryTestBase {
    private final static Logger log = LoggerFactory.getLogger(SimpleArtifactoryTest.class);
    private static final String LOG4J_1_2_14_JAR = "log4j/log4j/1.2.14/log4j-1.2.14.jar";

    @BeforeClass
    public void setLogLevel() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(SimpleArtifactoryTest.class).setLevel(Level.DEBUG);
        //lc.getLogger(HttpRepo.class).setLevel(Level.DEBUG);

        MockTest test = new MockTest("momo");
        test.addPath(new MockPathTest(LOG4J_1_2_14_JAR, "application/octet-stream", "/mock/repo1/" + LOG4J_1_2_14_JAR));
        mockServer.addTest(test);
    }

    @Test(invocationCount = 60, threadPoolSize = 20)
    public void simpleJarDownload() throws Exception {
        log.debug("Thread started concurrent simpleJarDownload");
        DownloadService downloadService = context.beanForType(DownloadService.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(LOG4J_1_2_14_JAR);
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        downloadService.process(request, response);
        Assert.assertTrue(response.isSuccessful(),
                "Failure status: " + response.getStatus() + " reason:" + response.getReason());
        Assert.assertTrue(response.getContentLength() > 0);
        Assert.assertTrue(response.getLastModified() > 0);
        Assert.assertEquals(response.getContentType(), "application/zip");
    }

    @Test(invocationCount = 4, threadPoolSize = 1, description = "Upload of same release file")
    public void simpleReleaseUpload() throws Exception {
        RepoPath repoPath = new RepoPath("ext-releases-local", "test/test/1.0/test-1.0.jar");
        upload(repoPath);
    }

    @Test(invocationCount = 4, threadPoolSize = 1, description = "Upload of same snapshot file")
    public void simpleSnapshotUpload() throws Exception {
        RepoPath repoPath = new RepoPath("ext-snapshots-local", "test/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.jar");
        upload(repoPath);
    }

    private void upload(RepoPath repoPath) throws IOException {
        InternalUploadService uploadService = context.beanForType(InternalUploadService.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(repoPath.getRepoKey(), "/" + repoPath.getPath());
        Resource resource = context.getResource("classpath:jars/test.jar");
        request.setInputStream(resource.getInputStream());
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();

        uploadService.process(request, response);

        Assert.assertTrue(response.isSuccessful(),
                "Failure status: " + response.getStatus() + " reason:" + response.getReason() + " for " + repoPath);
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }
}