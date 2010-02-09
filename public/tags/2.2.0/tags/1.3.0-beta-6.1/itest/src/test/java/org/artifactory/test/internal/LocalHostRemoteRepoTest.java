package org.artifactory.test.internal;

import org.artifactory.api.mime.ContentType;
import org.artifactory.api.request.DownloadService;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockTest;
import org.artifactory.test.mock.TestStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalHostRemoteRepoTest extends ArtifactoryTestBase {
    private final static Logger log = LoggerFactory.getLogger(LocalHostRemoteRepoTest.class);

    public static final String[] artifacts = {
            "log4j/log4j/1.2.14/log4j-1.2.14.jar",
            "log4j/log4j/1.2.14/log4j-1.2.14.pom",
            "log4j/log4j/1.2.13/log4j-1.2.13.jar",
            "log4j/log4j/1.2.13/log4j-1.2.13.pom",
            "ant/ant/1.6.5/ant-1.6.5.pom",
            "ant/ant/1.6.5/ant-1.6.5.jar"
    };

    public static final String[] moreArtifacts = {
            "log4j/apache-log4j-extras/1.0/apache-log4j-extras-1.0.pom",
            "log4j/log4j/1.2.4/log4j-1.2.4.pom",
            "log4j/log4j/1.2.8/log4j-1.2.8.pom",
            "log4j/log4j/1.2.14/log4j-1.2.14.pom",
            "log4j/log4j/1.2.12/log4j-1.2.12.pom",
            "log4j/log4j/1.2.9/log4j-1.2.9.pom",
            "log4j/log4j/1.2.13/log4j-1.2.13.pom",
            "log4j/log4j/1.2.15/log4j-1.2.15.pom",
            "log4j/log4j/1.2.11/log4j-1.2.11.pom",
            "log4j/apache-log4j-extras/1.0/apache-log4j-extras-1.0.jar",
            "log4j/log4j/1.2.4/log4j-1.2.4.jar",
            "log4j/log4j/1.2.8/log4j-1.2.8.jar",
            "log4j/log4j/1.2.14/log4j-1.2.14.jar",
            "log4j/log4j/1.2.14/log4j-1.2.14-sources.jar",
            "log4j/log4j/1.2.12/log4j-1.2.12.jar",
            "log4j/log4j/1.2.9/log4j-1.2.9-sources.jar",
            "log4j/log4j/1.2.9/log4j-1.2.9.jar",
            "log4j/log4j/1.2.13/log4j-1.2.13.jar",
            "log4j/log4j/1.2.13/log4j-1.2.13-sources.jar",
            "log4j/log4j/1.2.15/log4j-1.2.15-sources.jar",
            "log4j/log4j/1.2.15/log4j-1.2.15.jar",
            "log4j/log4j/1.2.11/log4j-1.2.11.jar",
            "ant/ant-trax/1.6.5/ant-trax-1.6.5.jar",
            "ant/ant-nodeps/1.6.5/ant-nodeps-1.6.5.jar",
            "ant/ant-optional/1.5.3-1/ant-optional-1.5.3-1.jar",
            "ant/ant-optional/1.5.1/ant-optional-1.5.1.jar",
            "ant/ant-junit/1.6.5/ant-junit-1.6.5.jar",
            "ant/ant/1.5.3-1/ant-1.5.3-1.jar",
            "ant/ant/1.6.2/ant-1.6.2.jar",
            "ant/ant/1.6/ant-1.6.jar",
            "ant/ant/1.6.5/ant-1.6.5.jar",
            "ant/ant/1.6.5/ant-1.6.5-sources.jar",
            "ant/ant/1.5.4/ant-1.5.4.jar",
            "ant/ant/1.5/ant-1.5.jar",
            "ant/ant/1.6.4/ant-1.6.4.jar",
            "ant/ant-launcher/1.6.5/ant-launcher-1.6.5.jar",
            "ant/ant-trax/1.6.5/ant-trax-1.6.5.pom",
            "ant/ant-nodeps/1.6.5/ant-nodeps-1.6.5.pom",
            "ant/ant-optional/1.5.3-1/ant-optional-1.5.3-1.pom",
            "ant/ant-optional/1.5.1/ant-optional-1.5.1.pom",
            "ant/ant-junit/1.6.5/ant-junit-1.6.5.pom",
            "ant/ant/1.5.2/ant-1.5.2.pom",
            "ant/ant/1.7.0/ant-1.7.0.pom",
            "ant/ant/1.6.2/ant-1.6.2.pom",
            "ant/ant/1.6/ant-1.6.pom",
            "ant/ant/1.6.5/ant-1.6.5.pom",
            "ant/ant/1.5.4/ant-1.5.4.pom",
            "ant/ant/1.5/ant-1.5.pom",
            "ant/ant-launcher/1.6.5/ant-launcher-1.6.5.pom",
            "org/testng/testng/5.7/testng-5.7.pom",
            "org/testng/testng/5.1/testng-5.1.pom",
            "org/testng/testng/5.5/testng-5.5.pom",
            "org/testng/testng/5.8/testng-5.8.pom",
            "org/testng/testng/5.7/testng-5.7-jdk15.jar",
            "org/testng/testng/5.1/testng-5.1-jdk15.jar",
            "org/testng/testng/5.5/testng-5.5-jdk15.jar",
            "org/testng/testng/5.5/testng-5.5.jar",
            "org/testng/testng/5.5/testng-5.5-sources.jar",
            "org/testng/testng/5.8/testng-5.8-sources.jar",
            "org/testng/testng/5.8/testng-5.8-jdk15.jar"
    };

    private static final String MOMO = "momo";
    private static final String SLOW_MOMO = "slowMomo";
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final String APPLICATION_XML = "application/xml";
    private static final String APPLICATION_ZIP = "application/zip";
    private MockTest momoTest;
    private MockTest slowMomoTest;
    private AtomicInteger nbMomoCalls = new AtomicInteger(0);
    private AtomicInteger nbSlowMomoCalls = new AtomicInteger(0);

    @BeforeClass
    public void setup() {

        momoTest = new MockTest(MOMO);
        for (String artifact : artifacts) {
            String ct = getContentType(artifact);
            momoTest.addPath(new MockPathTest(artifact, ct, "/mock/repo1/" + artifact));
        }
        mockServer.addTest(momoTest);
        slowMomoTest = new MockTest(SLOW_MOMO,
                "/home/bartender/work/artifactory/test/export/jfrog/repositories/repo1-cache/");
        for (String artifact : moreArtifacts) {
            String ct = getContentType(artifact);
            slowMomoTest
                    .addPath(new MockPathTest(artifact, ct, "/" + artifact, 10, 600));
        }
        mockServer.addTest(slowMomoTest);
    }

    private String getContentType(String artifact) {
        String ct = APPLICATION_ZIP;
        if (artifact.endsWith("pom")) {
            ct = ContentType.mavenPom.getMimeType();
        }
        return ct;
    }

    @Test(description = "Localhost mock server concurrent Jar download", invocationCount = 1,
            threadPoolSize = 1)
    public void concurrentJarDownload() throws Exception {
        //ArtifactoryRequestStub request = new ArtifactoryRequestStub("remote-repos-slow",randomExistingArtifact());
        nbMomoCalls.incrementAndGet();
        executeDownload("libs-releases", momoTest, true);
    }

    @Test(description = "Localhost mock server concurrent SLOW Jar download", invocationCount = 30,
            threadPoolSize = 15, enabled = false)
    public void concurrentSlowJarDownload() throws Exception {
        nbSlowMomoCalls.incrementAndGet();
        executeDownload("remote-repos-slow", slowMomoTest, false);
    }

    private void executeDownload(String repoKey, MockTest test, boolean checkSuccess)
            throws IOException {
        log.debug("Thread started concurrentJarDownload for " + repoKey);
        DownloadService downloadService = context.beanForType(DownloadService.class);
        MockPathTest path = randomExistingArtifact(test);
        ArtifactoryRequestStub request = new ArtifactoryRequestStub(repoKey, path.path);
        ArtifactoryResponseStub response = new ArtifactoryResponseStub();
        downloadService.process(request, response);

        if (checkSuccess) {
            assertTrue(response.isSuccessful(),
                    "Failure status: " + response.getStatus() + ":" + response.getReason());
            assertTrue(response.getContentLength() > 0);
            assertEquals(response.getContentType(), path.contentType);
            assertTrue(response.getLastModified() > 0, "Last modified sub zero: " +
                    response.getLastModified());
        } else {
            assertTrue(response.getStatus() == 404);
        }
    }

    @AfterClass
    public void verifyStats() {
        int nbCalls = nbMomoCalls.get();
        if (nbCalls > 0) {
            TestStats testStats = mockServer.getTestStats(MOMO);
            assertStats(nbCalls, testStats);
        }
        nbCalls = nbSlowMomoCalls.get();
        if (nbCalls > 0) {
            TestStats testStats = mockServer.getTestStats(SLOW_MOMO);
            assertStats(nbCalls, testStats);
        }
    }

    private void assertStats(int nbCalls, TestStats testStats) {
        int maxGets = Math.min(nbCalls, artifacts.length);
        Assert.assertEquals(testStats.requestsPerMethod("GET"),
                maxGets);
        int headRequests = testStats.requestsPerMethod("HEAD");
        System.out.println("Head requests " + headRequests);
        Assert.assertTrue(headRequests < 30);
        Assert.assertTrue(headRequests >= maxGets);
    }

    private MockPathTest randomExistingArtifact(MockTest test) {
        List<MockPathTest> pathTests = test.getPaths();
        int randomIndex = RANDOM.nextInt(pathTests.size());
        return pathTests.get(randomIndex);
    }

    @Override
    String getConfigName() {
        return "localhost-repo";
    }

}