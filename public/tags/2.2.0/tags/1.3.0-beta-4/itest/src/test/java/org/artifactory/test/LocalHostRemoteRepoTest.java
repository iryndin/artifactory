package org.artifactory.test;

import org.apache.log4j.Logger;
import org.artifactory.engine.DownloadEngine;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

import java.util.Random;

public class LocalHostRemoteRepoTest extends ArtifactoryTestBase {
    private final static Logger log = Logger.getLogger(LocalHostRemoteRepoTest.class);

    public static final String[] artifacts = {
            "/log4j/log4j/1.2.14/log4j-1.2.14.jar",
            "/log4j/log4j/1.2.14/log4j-1.2.14.pom",
            "/log4j/log4j/1.2.13/log4j-1.2.13.jar",
            "/log4j/log4j/1.2.13/log4j-1.2.13.pom",
            "/ant/ant/1.6.5/ant-1.6.5.pom",
            "/ant/ant/1.6.5/ant-1.6.5.jar"
    };

    @Test(invocationCount = 20, threadPoolSize = 10)
    public void concurrentJarDownload() throws Exception {
        log.debug("Thread started concurrentJarDownload");
        DownloadEngine downloadEngine = context.beanForType(DownloadEngine.class);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(randomExistingArtifact());
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        downloadEngine.process(request, response);

        assertTrue(response.isSuccessful(),
                "Failure status: " + response.getStatus() + ":" + response.getReason());
        assertTrue(response.getContentLength() > 0);
        assertTrue(response.getLastModified() > 0);
        assertEquals(response.getContentType(), "application/octet-stream");
    }

    private static String randomExistingArtifact() {
        int randomIndex = new Random().nextInt(artifacts.length);
        return artifacts[randomIndex];
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }

}