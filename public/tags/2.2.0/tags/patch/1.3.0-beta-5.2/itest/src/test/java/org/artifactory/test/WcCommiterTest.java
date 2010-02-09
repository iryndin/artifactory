package org.artifactory.test;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.engine.DownloadEngine;
import org.artifactory.jcr.lock.SessionLockManager;
import org.artifactory.repo.service.InternalRepositoryService;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Tests the behavior of Artifactory when a repository import is done and in the same time clients
 * asks for one or more files which are currently imported.
 * <p/>
 * This test requires a valid export directory to use to initiate an import before the test begins.
 */
public class WcCommiterTest extends ArtifactoryTestBase {
    private final static Logger log = Logger.getLogger(WcCommiterTest.class);

    @BeforeClass
    public void setupWcDir() throws IOException, InterruptedException, URISyntaxException {
        InternalRepositoryService repoService =
                context.beanForType(InternalRepositoryService.class);
        StatusHolder statusHolder = new StatusHolder();
        URL exportUrl = getClass().getResource("/export");
        URI exportUri = exportUrl.toURI();
        if (exportUri.toString().contains(".jar")) {
            // TODO: Need to extract info from jar
            throw new IOException("Cannot run WcCommiter test if export in jar!");
        }
        loginAsAdmin();
        repoService.importRepo(
                "libs-releases-local",
                new ImportSettings(new File(exportUri)),
                statusHolder);
        if (statusHolder.isError()) {
            if (statusHolder.getException() != null) {
                throw new RuntimeException(
                        "Import of test failed with msg: " + statusHolder.getStatusMsg(),
                        statusHolder.getException());
            } else {
                throw new RuntimeException(
                        "Import of test failed with msg: " + statusHolder.getStatusMsg());
            }
        }
        repoService.stopWorkingCopyCommitter();
        Logger.getLogger(SessionLockManager.class).setLevel(Level.DEBUG);
    }

    @Test(invocationCount = 20, threadPoolSize = 10)
    public void concurrentJarDownload() throws Exception {
        log.info("### Thread started for concurrentJarDownload");
        DownloadEngine downloadEngine = context.beanForType(DownloadEngine.class);
        ArtifactoryRequestStub request =
                new ArtifactoryRequestStub("biz/aQute/bndlib/0.0.227/bndlib-0.0.227.pom");
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        //Download it
        log.info("Requesting " + request.getPath() + " ... ");
        downloadEngine.process(request, response);
        //Assert
        assertTrue(response.isSuccessful(),
                "Request failed with failure status: " + response.getStatus());
        assertEquals(response.getContentLength(), 950);//size of bndlib-0.0.227.pom
        assertTrue(response.getLastModified() > 0);
        assertEquals(response.getContentType(), "application/octet-stream");
    }

    @Override
    String getConfigName() {
        return "no-remote-repo";
    }
}