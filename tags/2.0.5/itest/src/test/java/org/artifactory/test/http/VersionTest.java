package org.artifactory.test.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.artifactory.cli.main.ArtifactoryCli;
import org.artifactory.test.mock.MockServer;
import org.artifactory.test.mock.MockTest;
import org.artifactory.test.mock.TestStats;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Performs different test on Artifactory with the MockServer working as a remote repository
 *
 * @author Noam Tenne
 */
public class VersionTest {

    /**
     * API root for use with REST commands
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private static final String API_ROOT = "http://localhost:8081/artifactory/api/";
    /**
     * Global artifactory variable
     */
    ArtifactoryServer artifactory = null;
    /**
     * Mock server as remote repo
     */
    MockServer mockServer = null;

    @BeforeTest
    public void setUp() throws Exception {
        /**
         * Start artifactory and mock server
         */
        artifactory = new ArtifactoryServer("VersionTest");
        artifactory.start();

        mockServer = MockServer.start("localhost");

        importData();
    }

    /**
     * Requests a non-existant artifact, waits 'till the missing-cache time runs out, Creates a
     * mock-test for the artifact and retries the request
     *
     * @throws Exception
     */
    @Test(invocationCount = 1, threadPoolSize = 20)
    public void testMissing() throws Exception {

        //MockTest mockTest = new MockTest("momo");
        //mockServer.addTest(mockTest);
        //
        //HttpClient client = new HttpClient();
        //GetMethod getMethod = new GetMethod(
        //        "http://localhost:8081/artifactory/repo/mysql/connector-java/5.1.7/connector-java-5.1.7-bin.jar");
        //client.executeMethod(getMethod);
        //
        //Thread.sleep(5000);

        MockTest mockTest = new MockTest("momo", "C:\\");
        mockServer.addTest(mockTest);

        HttpClient client1 = new HttpClient();
        GetMethod getMethod1 = new GetMethod(
                "http://localhost:8081/artifactory/repo/mysql/connector-java/5.1.7/connector-java-5.1.7-bin.jar");
        client1.executeMethod(getMethod1);

        //Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_NOT_FOUND);
        Assert.assertEquals(getMethod1.getStatusCode(), HttpStatus.SC_OK);

        TestStats testStats = mockServer.getTestStats("momo");
        testStats.getStatsMap();
    }

    /**
     * Tests the retrieval of meta data from various versions of an artifact From the imported
     * repositories
     *
     * @throws Exception
     */
    @Test(invocationCount = 1)
    public void testMetaData() throws Exception {

        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod(
                "http://localhost:8081/artifactory/repo/mysql/connector-java/maven-metadata.xml");
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_OK);
    }

    /**
     * Import existing data from - resources/repositories via CLI
     *
     * @throws Exception
     */
    private void importData() throws Exception {

        ArtifactoryCli.main(new String[]{
                "import", getClass().getResource("/export/VersionTest").getPath(),
                "--server", "localhost:8081",
                "--username", "admin",
                "--password", "password",
                "--timeout", "3600"
        });
    }
}