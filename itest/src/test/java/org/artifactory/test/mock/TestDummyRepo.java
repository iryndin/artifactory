package org.artifactory.test.mock;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * User: Noam
 * Date: Sep 11, 2008
 * Time: 4:03:14 PM
 */
public class TestDummyRepo {
    private static final String TEST_DUMMY = "testDummy";
    private MockTest test;

    @BeforeClass
    public void setup() throws IOException {
        // Start the server in my test
        Logger.getLogger(StartDummyRepo.class).setLevel(Level.DEBUG);
        StartDummyRepo.main(null);

        // Send him data
        test = new MockTest(TEST_DUMMY);
        test.paths.add(new MockPathTest("ok1", "application/octet-stream",
                "/mock/repo1/log4j/log4j/1.2.14/log4j-1.2.14.jar", 3, 9, 1000));
        test.paths.add(new MockPathTest("ok2", "text/xml", "/mock/jetty.xml"));
        test.paths.add(new MockPathTest("nok", 404, "Not Found"));

//        String[] artifacts = LocalHostRemoteRepoTest.artifacts;
//        for (String artifact : artifacts) {
//
//        }
        XStream xStream = new XStream();
        xStream.processAnnotations(MockTest.class);
        String xmlConfig = xStream.toXML(test);

        String rootUrl = "http://localhost:8090/config/" + TEST_DUMMY;
        HttpClient httpClient = new HttpClient();
        PutMethod putMethod = new PutMethod(rootUrl);
        putMethod.setRequestEntity(new StringRequestEntity(xmlConfig, "application/xml", "utf-8"));
        httpClient.executeMethod(putMethod);

        Assert.assertEquals(putMethod.getStatusCode(), 200);
    }

    @Test
    public void testDummy() throws Exception {
        String rootUrl = "http://localhost:8090/" + TEST_DUMMY + "/";
        HttpClient httpClient = new HttpClient();
/*
            HostConfiguration configuration = new HostConfiguration();
            configuration.setHost(rootUrl);
            httpClient.setHostConfiguration(configuration);
*/
        for (MockPathTest pathTest : test.paths) {
            GetMethod getMethod = new GetMethod(rootUrl + pathTest.path);
            httpClient.executeMethod(getMethod);
            Assert.assertEquals(getMethod.getStatusCode(), pathTest.returnCode);
//            if (pathTest.contentType != null) {
//                Assert.assertEquals(getMethod.getResponseHeader("content/type").getValue(),pathTest.contentType);
//            }
        }
    }
}
