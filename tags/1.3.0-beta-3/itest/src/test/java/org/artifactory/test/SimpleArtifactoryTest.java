package org.artifactory.test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.artifactory.engine.DownloadEngine;
import org.artifactory.engine.UploadEngine;
import org.artifactory.repo.HttpRepo;
import org.artifactory.repo.RemoteRepoBase;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SimpleArtifactoryTest extends ArtifactoryTestBase {
    private final static Logger log = Logger.getLogger(SimpleArtifactoryTest.class);

    @BeforeClass
    public void setLogLevel() {
        //Logger.getLogger(SessionLockManager.class).setLevel(Level.DEBUG);
        Logger.getLogger(RemoteRepoBase.class).setLevel(Level.DEBUG);
        Logger.getLogger(HttpRepo.class).setLevel(Level.DEBUG);
    }

    @Test(invocationCount = 20, threadPoolSize = 10)
    public void simpleJarDownload() throws Exception {
        log.debug("Thread started concurrent simpleJarDownload");
        DownloadEngine downloadEngine = context.beanForType(DownloadEngine.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(
                "/log4j/log4j/1.2.14/log4j-1.2.14.jar");
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        downloadEngine.process(request, response);

        Assert.assertTrue(response.isSuccessful(), "Failure status: " + response.getStatus());
        Assert.assertTrue(response.getContentLength() > 0);
        Assert.assertTrue(response.getLastModified() > 0);
        Assert.assertEquals(response.getContentType(), "application/octet-stream");
    }

    @Test(invocationCount = 1, threadPoolSize = 2,
            description = "Concurrent upload of same file")
    public void simpleJarUpload() throws Exception {
        UploadEngine uploadEngine = context.beanForType(UploadEngine.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(
                "ext-releases-local", "/test/test/1.0/test-1.0.jar");
        Resource resource = context.getResource("classpath:jars/test.jar");
        request.setInputStream(resource.getInputStream());
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();

        uploadEngine.process(request, response);

        Assert.assertTrue(response.isSuccessful(), "Failure status: " + response.getStatus());
    }

}