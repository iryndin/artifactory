package org.artifactory.test.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SimpleArtifactoryTest extends ArtifactoryTestBase {
    private final static Logger log = LoggerFactory.getLogger(SimpleArtifactoryTest.class);
    private static final String LOG4J_1_2_14_JAR = "log4j/log4j/1.2.14/log4j-1.2.14.jar";

    @Override
    String getConfigName() {
        return "localhost-repo";
    }

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
        ArtifactoryResponseStub response =
                download(new RepoPath(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY, LOG4J_1_2_14_JAR));
        Assert.assertTrue(response.isSuccessful(),
                "Failure status: " + response.getStatus() + " reason:" + response.getReason());
        Assert.assertTrue(response.getContentLength() > 0);
        Assert.assertTrue(response.getLastModified() > 0);
        Assert.assertEquals(response.getContentType(), "application/zip");
    }

    @Test(invocationCount = 4, threadPoolSize = 1, description = "Upload of same release file")
    public void simpleReleaseUpload() throws Exception {
        RepoPath repoPath = new RepoPath("ext-releases-local", "test/test/1.0/test-1.0.jar");
        upload(repoPath, "resources/test.jar");
    }

    @Test(invocationCount = 4, threadPoolSize = 1, description = "Upload of same snapshot file")
    public void simpleSnapshotUpload() throws Exception {
        RepoPath repoPath = new RepoPath("ext-snapshots-local", "test/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.jar");
        upload(repoPath, "resources/test.jar");
    }
}