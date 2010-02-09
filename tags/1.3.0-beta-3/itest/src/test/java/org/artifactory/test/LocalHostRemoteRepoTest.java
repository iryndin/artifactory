package org.artifactory.test;

import org.apache.log4j.Logger;
import org.artifactory.engine.DownloadEngine;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.util.Random;

public class LocalHostRemoteRepoTest extends ArtifactoryTestBase {
    private final static Logger log = Logger.getLogger(LocalHostRemoteRepoTest.class);

    private final String[] artifacts = {
            "/log4j/log4j/1.2.14/log4j-1.2.14.jar",
            "/log4j/log4j/1.2.14/log4j-1.2.14.pom",
            "/log4j/log4j/1.2.14/log4j-1.2.13.jar",
            "/ant/ant/1.6.5/ant-1.6.5.jar",
            "/ant/ant/1.6.5/ant-1.6.5-sources.jar"
    };

    @Test(invocationCount = 10, threadPoolSize = 5)
    public void concurrentJarDownload() throws Exception {
        log.debug("Thread started concurrentJarDownload");
        DownloadEngine downloadEngine = context.beanForType(DownloadEngine.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(randomExistingArtifact());
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        downloadEngine.process(request, response);

        assertTrue(response.isSuccessful(), "Failure status: " + response.getStatus());
        assertTrue(response.getContentLength() > 0);
        assertTrue(response.getLastModified() > 0);
        assertEquals(response.getContentType(), "application/octet-stream");
    }

    private String randomExistingArtifact() {
        int randomIndex = new Random().nextInt(artifacts.length);
        return artifacts[randomIndex];
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }

}