/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.test.mock;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.IOException;

/**
 * @author freds
 * @date Sep 14, 2008
 */
public class MockServer {
    private static final String PROTOCOL = "http://";
    private static final String PORT = ":8090";
    private static final String EXISTENCE_PREFIX = "/existence";
    private static final String LOCAL_HOST = "localhost";
    private static final String CONFIG_PREFIX = "/config/";
    private static final String CONTENT_TYPE = "application/xml";
    private static final String ENCODING = "utf-8";
    private static final String STATS_PREFIX = "/stats/";
    private String selectedURL = "";
    private HttpClient httpClient = new HttpClient();
    private XStream xStream;

    /**
     * Main constructor
     *
     * @param serverName
     */
    public MockServer(String serverName) {
        this.selectedURL = PROTOCOL + serverName + PORT;
    }

    /**
     * For each server name try to find if a MockServer was started on port 8090 The first one found
     * create an instance of MockServer with HttpClient initialized correctly If you don't find
     * anything and the last name is "localhost" Call directly the main method on StartDummyRepo
     *
     * @param serverNames
     * @return
     * @throws IOException
     */
    public static MockServer start(String... serverNames) {

        try {
            //Search over given server names
            for (String serverName : serverNames) {
                MockServer result = new MockServer(serverName);
                //If the current server is active, return it to the user
                if (result.isServerActive()) {
                    return result;
                }
            }

            //If no server from the given list is found
            //Check if the server on the local host is running
            MockServer result = new MockServer(LOCAL_HOST);
            if (!result.isServerActive()) {
                //If the server on the local host is not running, initiate it,
                // and return it to the user
                StartDummyRepo.main(null);
            }

            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the current server is active or not
     *
     * @return
     * @throws IOException
     */
    private boolean isServerActive() throws IOException {

        GetMethod getMethod = new GetMethod(selectedURL + EXISTENCE_PREFIX);
        int response;
        try {
            response = httpClient.executeMethod(getMethod);
            getMethod.releaseConnection();
        } catch (IOException e) {
            return false;
        }

        //If the responce is ok, return true
        return response == HttpStatus.SC_OK;
    }

    /**
     * Adds the given MockTest to the server
     *
     * @param test
     * @throws IOException
     */
    public void addTest(MockTest test) {
        //Get xml stream
        XStream xStream = getXStream();
        xStream.processAnnotations(MockTest.class);
        String xmlConfig = xStream.toXML(test);

        //Add test
        PutMethod putMethod = new PutMethod(getConfigURL(test.getName()));
        try {
            putMethod.setRequestEntity(new StringRequestEntity(xmlConfig, CONTENT_TYPE, ENCODING));
            httpClient.executeMethod(putMethod);
            putMethod.releaseConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a MockTest object via name
     *
     * @param testName
     * @return
     * @throws IOException
     */
    public MockTest getTest(String testName) {
        try {
            //Search for given test name
            GetMethod getMethod = new GetMethod(getConfigURL(testName));
            httpClient.executeMethod(getMethod);

            //If the responce is ok
            if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
                //Return test object
                XStream xStream = getXStream();
                MockTest result = (MockTest) xStream.fromXML(getMethod.getResponseBodyAsStream());
                getMethod.releaseConnection();
                return result;
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Replaces the given MockPathTest with the existing one
     *
     * @param testName
     * @param pathTest
     * @return
     * @throws IOException
     */
    public MockPathTest changeTest(String testName, MockPathTest pathTest) {
        try {
            //Get xml stream
            XStream xStream = getXStream();
            xStream.processAnnotations(MockTest.class);
            String xmlChange = xStream.toXML(pathTest);

            //Search for test
            PostMethod postMethod = new PostMethod(getConfigURL(testName));
            postMethod.setRequestEntity(new StringRequestEntity(xmlChange, CONTENT_TYPE, ENCODING));
            httpClient.executeMethod(postMethod);
            postMethod.releaseConnection();
            return pathTest;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns stats for the specified test
     *
     * @param testName
     * @return
     */
    public TestStats getTestStats(String testName) {
        try {
            //Search for given test name
            GetMethod getMethod = new GetMethod(getStatsURL(testName));
            httpClient.executeMethod(getMethod);

            //If the responce is ok
            if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
                //Return stats object
                XStream xStream = getXStream();
                TestStats result = (TestStats) xStream.fromXML(getMethod.getResponseBodyAsStream());
                getMethod.releaseConnection();
                return result;
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private XStream getXStream() {
        if (xStream == null) {
            xStream = new XStream();
            xStream.processAnnotations(MockTest.class);
        }
        return xStream;
    }

    private String getConfigURL(String url) {
        return selectedURL + CONFIG_PREFIX + url;
    }

    private String getStatsURL(String url) {
        return selectedURL + STATS_PREFIX + url;
    }

    public String getSelectedURL() {
        return selectedURL;
    }
}
