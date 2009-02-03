package org.artifactory.test;

import org.apache.log4j.Logger;
import org.artifactory.engine.DownloadEngine;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockServer;
import org.artifactory.test.mock.MockTest;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeClass;
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

    @BeforeClass
    public void setup() {
        MockServer mockServer = MockServer.start("localhost");

        MockTest test = new MockTest("momo");
        test.addPath(new MockPathTest("log4j/log4j/1.2.13/log4j-1.2.13.jar",
                "application/octet-stream", "/mock/repo1/log4j/log4j/1.2.13/log4j-1.2.13.jar"));
        test.addPath(new MockPathTest("log4j/log4j/1.2.13/log4j-1.2.13.pom",
                "text/xml", "/mock/repo1/log4j/log4j/1.2.13/log4j-1.2.13.pom"));
        test.addPath(new MockPathTest("log4j/log4j/1.2.14/log4j-1.2.14.jar",
                "application/octet-stream", "/mock/repo1/log4j/log4j/1.2.14/log4j-1.2.14.jar"));
        test.addPath(new MockPathTest("log4j/log4j/1.2.14/log4j-1.2.14.pom",
                "text/xml", "/mock/repo1/log4j/log4j/1.2.14/log4j-1.2.14.pom"));
        test.addPath(new MockPathTest("ant/ant/1.6.5/ant-1.6.5.pom",
                "text/xml", "/mock/repo1/ant/ant/1.6.5/ant-1.6.5.pom"));
        test.addPath(new MockPathTest("ant/ant/1.6.5/ant-1.6.5.jar",
                "application/octet-stream", "/mock/repo1/ant/ant/1.6.5/ant-1.6.5.jar"));
        mockServer.addTest(test);
    }

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
        assertEquals(response.getContentType(), "application/octet-stream");
        assertTrue(response.getLastModified() > 0, "Last modified sub zero: " +
                response.getLastModified());
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