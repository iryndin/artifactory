package org.artifactory.test.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA. User: Noam
 */
public class HttpServerTest {

    @BeforeTest
    public void setUp() throws Exception {
        ArtifactoryServer startServer = new ArtifactoryServer("HttpServer");
        startServer.start();
    }

    @Test(invocationCount = 1)
    public void simpleArtifactRequest() throws IOException {
        HttpClient client = new HttpClient();

        GetMethod getMethod = new GetMethod(
                "http://localhost:8081/artifactory/repo/cz/softeu/softeu-rewriter/1.1/softeu-rewriter-1.1.jar");
        client.executeMethod(getMethod);
        Assert.assertEquals(getMethod.getStatusCode(), HttpStatus.SC_OK);
    }
}
