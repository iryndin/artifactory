package org.artifactory.test.internal;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.artifactory.test.http.ArtifactoryServer;
import org.artifactory.test.mock.MockPathTest;
import org.artifactory.test.mock.MockServer;
import org.artifactory.test.mock.MockTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * Tests the behaviour of the client\server sides when trying upload\download artifacts and suffering a broken pipe
 *
 * @author Noam Tenne
 */
public class BrokenStreamTest {
    private static final String ARTIFACTORY_PREFIX = "/artifactory/";
    private static final String PROTOCOL = "http://";
    private static final String HOST_NAME = "localhost";
    private static final String HOST = PROTOCOL + HOST_NAME;
    private static final String PORT = ":8081";
    private static final String URL = HOST + PORT;
    private static final String REPO = "libs-releases-local/";
    private static final String APPLICATION_ZIP = "application/zip";
    private static final String ARTIFACT = "mysql/connector-java/5.1.5/connector-java-5.1.5-bin.jar";
    private static final String REPO_PATH = REPO + ARTIFACT;
    private static final String ARTIFACT_PATH =
            "/export/VersionTest/repositories/" + REPO_PATH;
    MockServer mockServer;
    MockTest momoTest = new MockTest("momo");
    MockPathTest mockPathTest = new MockPathTest(ARTIFACT, APPLICATION_ZIP, ARTIFACT_PATH, 5, 10, 0, 40);

    @BeforeClass
    public void setup() throws IOException {
        mockServer = MockServer.start(HOST_NAME);
        momoTest.addPath(mockPathTest);
        mockServer.addTest(momoTest);
        ArtifactoryServer startServer = new ArtifactoryServer("HttpServer", mockServer);
        startServer.start();
    }

    /**
     * Attempt to download an artifact with as broken pipe
     *
     * @throws IOException IOException
     */
    @Test
    public void testBrokenDownloadStream() throws IOException {
        HttpMethod getMethod = download(URL, ARTIFACTORY_PREFIX + REPO_PATH, false);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_NOT_FOUND,
                "Reponse should have a 404 status for timing out.");
    }

    /**
     * Attempt to upload an artifact with a borken pipe, and make sure the artifact does not partially exist in the
     * Repository
     *
     * @throws URISyntaxException   URISyntaxException
     * @throws IOException          IOException
     * @throws InterruptedException InterruptedException
     */
    @Test(dependsOnMethods = "testBrokenDownloadStream")
    public void testBrokenUpload() throws URISyntaxException, IOException, InterruptedException {
        File file = new File(getClass().getResource(ARTIFACT_PATH).toURI());
        upload(URL, ARTIFACTORY_PREFIX + REPO_PATH, file, true);
        Thread.sleep(5000);
        mockPathTest = new MockPathTest(ARTIFACT, HttpStatus.SC_NOT_FOUND, "File Not Found.");
        mockServer.changeTest(momoTest.getName(), mockPathTest);
        HttpMethod getMethod = download(URL, ARTIFACTORY_PREFIX + REPO_PATH, false);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_NOT_FOUND,
                "Reponse should have a 404 status since " +
                        "the upload should have failed.");
    }

    /**
     * Attempt to upload an artifact with a borken pipe, and make sure the artifact does not partially exist in the
     * Repository
     *
     * @throws URISyntaxException   URISyntaxException
     * @throws IOException          IOException
     * @throws InterruptedException InterruptedException
     */
    @Test(dependsOnMethods = "testBrokenUpload")
    public void testNormalUpload() throws URISyntaxException, IOException, InterruptedException {
        File file = new File(getClass().getResource(ARTIFACT_PATH).toURI());
        upload(URL, ARTIFACTORY_PREFIX + REPO_PATH, file, false);
        Thread.sleep(5000);
        mockPathTest = new MockPathTest(ARTIFACT, HttpStatus.SC_NOT_FOUND, "File Not Found.");
        mockServer.changeTest(momoTest.getName(), mockPathTest);
        HttpMethod getMethod = download(URL, ARTIFACTORY_PREFIX + REPO_PATH, true);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_OK,
                "Reponse should have a 202 status since " +
                        "the upload should have succeeded.");
    }

    /**
     * Uploads an artifact to the repository
     *
     * @param url       Url of the repository
     * @param path      Path to the artifact
     * @param file      Local file representation of the artifact
     * @param breakPipe True if to break the pipe in the middle of the transfer
     * @return HttpMethod - Method object that was executed
     * @throws FileNotFoundException FileNotFoundException
     */
    private HttpMethod upload(String url, String path, File file, final boolean breakPipe)
            throws FileNotFoundException {
        final PutMethod putMethod = new PutMethod(url);
        //Disable retries
        putMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
        final InputStream fileInputStream = new FileInputStream(file) {
            int timesCalled = 0;

            @SuppressWarnings({"deprecation"})
            @Override
            public int read(byte[] buffer) {
                //Break pipe if requested
                if (breakPipe) {
                    timesCalled++;
                    //Wait till some data has been transferred before breaking
                    if (timesCalled > 2) {
                        Thread.currentThread().stop();
                    }
                }
                int returned = 0;
                try {
                    returned = super.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return returned;
            }
        };
        final HttpClient httpClient = new HttpClient();
        HttpClientParams clientParams = httpClient.getParams();
        clientParams.setAuthenticationPreemptive(true);
        Credentials creds = new UsernamePasswordCredentials("admin", "password");
        AuthScope scope = new AuthScope(HOST_NAME, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
        httpClient.getState().setCredentials(scope, creds);
        putMethod.setPath(path);

        InputStreamRequestEntity entity =
                new InputStreamRequestEntity(fileInputStream, file.length(), APPLICATION_ZIP);
        putMethod.setRequestEntity(entity);

        //Execute method in different thread so we don't kill the test
        Thread executionThread = new Thread() {
            @Override
            public void run() {
                try {
                    httpClient.executeMethod(putMethod);
                } catch (IOException e) {
                    System.err.println("expected exception was thrown: " + e.getMessage());
                } finally {
                    putMethod.releaseConnection();
                    IOUtils.closeQuietly(fileInputStream);
                }
            }
        };
        executionThread.start();

        return putMethod;
    }

    /**
     * Downloads an artifact from the repository
     *
     * @param url                    Url of the repository
     * @param path                   Path to the artifact
     * @param checkValidResponseBody True if to check that the response body is valid
     * @return HttpMethod - Method object that was executed
     * @throws IOException IOException
     */
    private HttpMethod download(String url, String path, boolean checkValidResponseBody) throws IOException {
        GetMethod getMethod = new GetMethod(url);
        try {
            HttpClient httpClient = new HttpClient();
            getMethod.setPath(path);
            httpClient.executeMethod(getMethod);
            if (checkValidResponseBody) {
                byte[] responseBody = getMethod.getResponseBody();
                Assert.assertNotNull(responseBody);
                Assert.assertTrue(responseBody.length > 0);
            }
        } finally {
            getMethod.releaseConnection();
        }

        return getMethod;
    }
}