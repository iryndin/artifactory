package org.artifactory.test.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ConstantsValue;
import org.artifactory.jcr.lock.InternalLockManager;
import org.artifactory.jcr.lock.SessionLockEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ConcurrentSnapshotsTest extends ArtifactoryTestBase {
    private final static Logger log = LoggerFactory.getLogger(ConcurrentSnapshotsTest.class);

    @BeforeClass
    public void setLogLevel() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.getLogger(ConcurrentSnapshotsTest.class).setLevel(Level.DEBUG);
        lc.getLogger(SessionLockEntry.class).setLevel(Level.TRACE);
        lc.getLogger(InternalLockManager.class).setLevel(Level.TRACE);
    }

    @Override
    protected boolean shouldRunMockServer() {
        return false;
    }

    @Override
    protected void onBeforeHomeCreate() throws Exception {
        System.setProperty(ConstantsValue.lockTimeoutSecs.getPropertyName(), "5");
        super.onBeforeHomeCreate();
    }

    @Test(invocationCount = 10, threadPoolSize = 10, dependsOnMethods = "deploySnapshotPom",
            description = "Mimic maven parallel upload/download of pom + snapshot version metadata, in order to " +
                    "check for dealock handling.")
    public void concurrentSnapshotPartsUpload() throws Exception {
        Callable<?> metadataDownload = new Callable<String>() {
            public String call() throws Exception {
                log.info("........... Downloading MAVEN-METADATA");
                bindThreadContext();
                RepoPath repoPath =
                        new RepoPath("ext-snapshots-local", "test-group/test/1.0-SNAPSHOT/maven-metadata.xml");
                ArtifactoryResponseStub responseStub = download(repoPath);
                Assert.assertTrue(responseStub.isSuccessful(), "Metadata download should succeed.");
                return null;
            }
        };
        Callable<?> metadataUpload = new Callable<String>() {
            public String call() throws Exception {
                log.info("........... Deploying MAVEN-METADATA");
                bindThreadContext();
                RepoPath repoPath =
                        new RepoPath("ext-snapshots-local", "test-group/test/1.0-SNAPSHOT/maven-metadata.xml");
                upload(repoPath, "resources/test-1.0-SNAPSHOT-maven-metadata.xml");
                return null;
            }
        };
        Callable<?> pomDownload = new Callable() {
            public String call() throws Exception {
                log.info("........... Downloading POM");
                bindThreadContext();
                ArtifactoryContextThreadBinder.bind(context);
                RepoPath repoPath = new RepoPath(
                        "ext-snapshots-local", "test-group/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.pom");
                ArtifactoryResponseStub responseStub = download(repoPath);
                Assert.assertTrue(responseStub.isSuccessful(), "POM download should succeed");
                return null;
            }
        };
        Callable<?> pomUpload = new Callable() {
            public String call() throws Exception {
                log.info("........... Deploying POM");
                bindThreadContext();
                ArtifactoryContextThreadBinder.bind(context);
                RepoPath repoPath =
                        new RepoPath("ext-snapshots-local", "test-group/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.pom");
                //new RepoPath("ext-snapshots-local", "test-group/test/1.0-SNAPSHOT/test-20090511.121120-1.pom");
                upload(repoPath, "resources/test-1.0-SNAPSHOT.pom");
                return null;
            }
        };
        ExecutorService executorService = Executors.newCachedThreadPool();
        Future<?> metadataDownloadResult = executorService.submit(metadataDownload);
        Future<?> metadataUploadResult = executorService.submit(metadataUpload);
        Future<?> pomDownloadResult = executorService.submit(pomDownload);
        Future<?> pomUploadResult = executorService.submit(pomUpload);
        try {
            metadataDownloadResult.get();
            pomDownloadResult.get();
            metadataUploadResult.get();
            pomUploadResult.get();
        } catch (Exception e) {
            Assert.fail("Snapshot upload/download failed!", e.getCause());
        }
    }

    @Test
    void deploySnapshotPom() throws IOException {
        RepoPath repoPath =
                new RepoPath("ext-snapshots-local", "test-group/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.pom");
        upload(repoPath, "resources/test-1.0-SNAPSHOT.pom");
        repoPath = new RepoPath("ext-snapshots-local", "test-group/test/1.0-SNAPSHOT/maven-metadata.xml");
        upload(repoPath, "resources/test-1.0-SNAPSHOT-maven-metadata.xml");
    }
}
