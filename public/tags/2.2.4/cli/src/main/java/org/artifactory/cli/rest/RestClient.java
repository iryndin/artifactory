/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.cli.rest;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.io.output.TeeOutputStream;
import org.artifactory.api.rest.constant.SystemRestConstants;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.PathUtils;
import org.artifactory.util.RemoteCommandException;
import org.slf4j.Logger;

import javax.net.ssl.SSLException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Provides a comfortable API to the different rest commands
 *
 * @author Noam Tenne
 */
public class RestClient {
    private static final Logger log = LoggerFactory.getLogger(RestClient.class);

    //TODO: [by yl] Use com.sun.jersey.api.client.WebResource instead of commons-httpclient

    /**
     * URL for Rest API
     */
    public static final String SYSTEM_URL = SystemRestConstants.PATH_ROOT;
    public static final String CONFIG_URL = SYSTEM_URL + "/" + SystemRestConstants.PATH_CONFIGURATION;
    public static final String EXPORT_URL = SYSTEM_URL + "/" + SystemRestConstants.PATH_EXPORT;
    public static final String IMPORT_URL = SYSTEM_URL + "/" + SystemRestConstants.PATH_IMPORT;
    public static final String SECURITY_URL = SYSTEM_URL + "/" + SystemRestConstants.PATH_SECURITY;
    public static final String COMPRESS_URL = SYSTEM_URL + "/" + SystemRestConstants.PATH_STORAGE + "/" +
            SystemRestConstants.PATH_STORAGE_COMPRESS;
    public static final String REPOSITORIES_URL = SystemRestConstants.PATH_REPOSITORIES;

    /**
     * Get method with default settings
     *
     * @param uri             Target URL
     * @param xstreamObjClass Expected class of returned object
     * @param timeout         Request timeout
     * @param credentials     For authentication
     * @param <T>             The type of class to be returned
     * @return Request object
     * @throws Exception
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T get(String uri, Class<T> xstreamObjClass, int timeout, Credentials credentials)
            throws Exception {
        if (xstreamObjClass != null) {
            byte[] bytes = get(uri, 200, "application/xml", false, timeout, credentials);
            XStream xStream = XStreamFactory.create(xstreamObjClass);
            return (T) xStream.fromXML(new ByteArrayInputStream(bytes));
        } else {
            get(uri, 200, null, true, timeout, credentials);
            return null;
        }
    }

    /**
     * Get method with full settings
     *
     * @param uri                Target URL
     * @param expectedStatus     The expected return status
     * @param expectedResultType The expected media type of the returned data
     * @param printStream        True if should print response stream to system.out
     * @param timeout            Request timeout
     * @param credentials        For authentication
     * @return byte[] - Response stream
     * @throws Exception
     */
    public static byte[] get(String uri, int expectedStatus, String expectedResultType, boolean printStream,
            int timeout, Credentials credentials) throws Exception {
        GetMethod method;
        try {
            method = new GetMethod(uri);
        } catch (IllegalStateException ise) {
            throw new RemoteCommandException("\nAn error has occurred while trying to resolve the given url: " +
                    uri + "\n" + ise.getMessage());
        }
        return executeMethod(uri, method, expectedStatus, expectedResultType, timeout, credentials, printStream);
    }

    /**
     * Get method with receiving customize Get method
     *
     * @param uri
     * @param expectedStatus
     * @param expectedResultType
     * @param printStream
     * @param timeout
     * @param credentials
     * @param method
     * @return
     * @throws Exception
     */
    public static byte[] get(String uri, int expectedStatus, String expectedResultType, boolean printStream,
            int timeout, Credentials credentials, GetMethod method) throws Exception {
        return executeMethod(uri, method, expectedStatus, expectedResultType, timeout, credentials, printStream);
    }

    public static String getString(String uri, String username, String password) throws Exception {
        byte[] bytes = get(uri, HttpStatus.SC_OK, null, false, -1, getCredentials(username, password));
        if (bytes == null) {
            return null;
        }
        return new String(bytes, "utf-8");
    }

    public static byte[] get(String uri, String username, String password) throws Exception {
        return get(uri, HttpStatus.SC_OK, null, false, -1, getCredentials(username, password));
    }

    public static byte[] post(String uri, byte[] input, String username, String password) throws Exception {
        return post(uri, input, null, HttpStatus.SC_OK, null, false, -1, getCredentials(username, password));
    }

    public static byte[] post(String uri, RequestEntity requestEntity, String username, String password)
            throws Exception {
        return post(uri, requestEntity, HttpStatus.SC_OK, null, false, -1, getCredentials(username, password));
    }

    public static void delete(String uri, String username, String password) throws Exception {
        delete(uri, HttpStatus.SC_OK, null, false, -1, getCredentials(username, password));
    }

    public static byte[] delete(String uri, int expectedStatus, String expectedResultType, boolean printStream,
            int timeout, Credentials credentials) throws Exception {
        DeleteMethod method;
        try {
            method = new DeleteMethod(uri);
        } catch (IllegalStateException ise) {
            throw new RemoteCommandException("\nAn error has occurred while trying to resolve the given url: " +
                    uri + "\n" + ise.getMessage());
        }
        return executeMethod(uri, method, expectedStatus, expectedResultType, timeout, credentials, printStream);
    }

    public static byte[] put(String uri, File input, String username, String password) throws Exception {
        return put(uri, new BufferedInputStream(new FileInputStream(input)), username, password);
    }

    public static byte[] put(String uri, byte[] input, String username, String password) throws Exception {
        return put(uri, new ByteArrayInputStream(input), username, password);
    }

    public static byte[] put(String uri, InputStream input, String username, String password) throws Exception {
        return put(uri, input, null, HttpStatus.SC_OK, null, false, -1, getCredentials(username, password));
    }

    public static byte[] put(String uri, RequestEntity requestEntity, String username, String password)
            throws Exception {
        return put(uri, requestEntity, HttpStatus.SC_OK, null, false, -1, getCredentials(username, password));
    }

    public static byte[] put(String uri, InputStream input, final String inputType, int expectedStatus,
            String expectedResultType, boolean printStream, int timeout, Credentials credentials) throws Exception {
        return put(uri, new InputStreamRequestEntity(input, inputType), expectedStatus, expectedResultType,
                printStream, timeout, credentials);
    }

    public static byte[] put(String uri, RequestEntity requestEntity, int expectedStatus,
            String expectedResultType, boolean printStream, int timeout, Credentials credentials) throws Exception {
        PutMethod method = new PutMethod(uri);
        method.setRequestEntity(requestEntity);
        return executeMethod(uri, method, expectedStatus, expectedResultType, timeout, credentials, printStream);
    }

    /**
     * Post method with default settings
     *
     * @param uri         Target URL
     * @param inObj       Object to send
     * @param outObjClass Class of expected object from response
     * @param timeout     Request timeout
     * @param credentials For authentication
     * @param <I>         Type of class to send
     * @param <O>         Type of class to be returned
     * @return Response object
     * @throws Exception
     */
    @SuppressWarnings({"unchecked"})
    public static <I, O> O post(String uri, I inObj, Class<O> outObjClass, int timeout, Credentials credentials)
            throws Exception {
        XStream xStream = XStreamFactory.create();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (inObj != null) {
            xStream.processAnnotations(inObj.getClass());
            xStream.toXML(inObj, bos);
        }
        if (outObjClass != null) {
            xStream.processAnnotations(outObjClass);
            byte[] bytes = post(uri, bos.toByteArray(), "application/xml", 200, "application/xml", false, timeout,
                    credentials);
            return (O) xStream.fromXML(new ByteArrayInputStream(bytes));
        } else {
            post(uri, bos.toByteArray(), "application/xml", 200, null, true, timeout, credentials);
            return null;
        }
    }

    /**
     * post method with full settings
     *
     * @param uri                Target URL
     * @param data               Data to send
     * @param inputType          Type of data which is sent
     * @param expectedStatus     Expected return status
     * @param expectedResultType Expected response media type
     * @param printStream        True if should print response stream to system.out
     * @param timeout            Request timeout
     * @param credentials        For authentication
     * @return byte[] - Response stream
     * @throws Exception
     */
    public static byte[] post(String uri, final byte[] data, final String inputType, int expectedStatus,
            String expectedResultType, boolean printStream, int timeout, Credentials credentials) throws Exception {
        RequestEntity requestEntity = new RequestEntity() {
            public boolean isRepeatable() {
                return true;
            }

            public void writeRequest(OutputStream out) throws IOException {
                out.write(data);
            }

            public long getContentLength() {
                return data.length;
            }

            public String getContentType() {
                return inputType;
            }
        };
        return post(uri, requestEntity, expectedStatus, expectedResultType, printStream, timeout, credentials);
    }

    /**
     * post method with full settings
     *
     * @param uri                Target URL
     * @param requestEntity      Request entity to provide the method with
     * @param expectedStatus     Expected return status
     * @param expectedResultType Expected response media type
     * @param printStream        True if should print response stream to system.out
     * @param timeout            Request timeout
     * @param credentials        For authentication
     * @return byte[] - Response stream
     * @throws Exception
     */
    public static byte[] post(String uri, RequestEntity requestEntity, int expectedStatus,
            String expectedResultType, boolean printStream, int timeout, Credentials credentials) throws Exception {
        PostMethod method = new PostMethod(uri);
        method.setRequestEntity(requestEntity);
        return executeMethod(uri, method, expectedStatus, expectedResultType, timeout, credentials, printStream);
    }

    /**
     * Executes a configured HTTP
     *
     * @param uri                Target URL
     * @param method             Method to execute
     * @param expectedStatus     Expected return status
     * @param expectedResultType Expected response media type
     * @param timeout            Request timeout
     * @param credentials        For authentication
     * @throws Exception
     */
    private static byte[] executeMethod(String uri, HttpMethod method, int expectedStatus, String expectedResultType,
            int timeout, Credentials credentials, boolean printStream) throws Exception {
        try {
            getHttpClient(uri, timeout, credentials).executeMethod(method);
            checkStatus(uri, expectedStatus, method);
            Header contentTypeHeader = method.getResponseHeader("content-type");
            if (contentTypeHeader != null) {
                //Check result content type
                String contentType = contentTypeHeader.getValue();
                checkContentType(uri, expectedResultType, contentType);
            }
            return analyzeResponse(method, printStream);
        } catch (SSLException ssle) {
            throw new RemoteCommandException("\nThe host you are trying to reach does not support SSL.");
        } catch (ConnectTimeoutException cte) {
            throw new RemoteCommandException("\n" + cte.getMessage());
        } catch (UnknownHostException uhe) {
            throw new RemoteCommandException("\nThe host of the specified URL: " + uri + " could not be found.\n" +
                    "Please make sure you have specified the correct path. The default should be:\n" +
                    "http://myhost:8081/artifactory/api/system");
        } catch (ConnectException ce) {
            throw new RemoteCommandException("\nCannot not connect to: " + uri + ". " +
                    "Please make sure to specify a valid host (--host <host>:<port>) or URL (--url http://...).");
        } catch (NoRouteToHostException nrthe) {
            throw new RemoteCommandException("\nCannot reach: " + uri + ".\n" +
                    "Please make sure that the address is valid and that the port is open (firewall, router, etc').");
        } finally {
            method.releaseConnection();
        }
    }

    /**
     * Writes the response stream to the selected outputs
     *
     * @param method      The method that was executed
     * @param printStream True if should print response stream to system.out
     * @return byte[] Response
     * @throws IOException
     */
    private static byte[] analyzeResponse(HttpMethod method, boolean printStream) throws IOException {
        InputStream is = method.getResponseBodyAsStream();
        if (is == null) {
            return null;
        }
        byte[] buffer = new byte[1024];
        int r;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream os = baos;
            if (printStream) {
                os = new TeeOutputStream(baos, System.out);
            }
            while ((r = is.read(buffer)) != -1) {
                os.write(buffer, 0, r);
            }
            if (printStream) {
                System.out.println("");
            }
            return baos.toByteArray();
        } catch (SocketTimeoutException e) {
            log.error("Communication with the server has timed out: " + e.getMessage());
            log.error("ATTENTION: The command on the server may still be running!");
            String url = method.getURI().toString();
            int apiPos = url.indexOf("/api");
            String logsUrl;
            if (apiPos != -1) {
                logsUrl = url.substring(0, apiPos) + "/webapp/systemlogs.html";
            } else {
                logsUrl = "http://" + method.getURI().getHost() + "/artifactory/webapp/systemlogs.html";
            }
            log.error("Please check the server logs " + logsUrl + " before re-running the command.");
            return null;
        }
    }

    /**
     * Validates the expected content type
     *
     * @param uri          Target URL
     * @param expectedType Expected response media type
     * @param contentType  Actual response media type
     */
    private static void checkContentType(String uri, String expectedType, String contentType) {
        if (!PathUtils.hasText(expectedType)) {
            return;
        }
        if (!contentType.contains(expectedType)) {
            throw new RuntimeException("HTTP content type was " + contentType + " and should be " +
                    expectedType + " for request on " + uri);
        }
    }

    private static UsernamePasswordCredentials getCredentials(String username, String password) {
        if (username == null) {
            return null;
        }
        return new UsernamePasswordCredentials(username, password);
    }

    /**
     * Validates the expected returned status
     *
     * @param uri            Target URL
     * @param expectedStatus Expected returned status
     * @param method         The method after execution (holds the returned status)
     */
    private static void checkStatus(String uri, int expectedStatus, HttpMethod method) {
        int status = method.getStatusCode();
        if (status != expectedStatus) {
            throw new RemoteCommandException("\nUnexpected response status for request: " + uri + "\n" +
                    "Expected status: " + expectedStatus + " (" + HttpStatus.getStatusText(expectedStatus) + ")" +
                    "\n " +
                    "Received status: " + status + " (" + HttpStatus.getStatusText(status) + ") - " +
                    method.getStatusText() + "\n");
        }
    }

    /**
     * Returnes an HTTP client object with the given configurations
     *
     * @param url         Target URL
     * @param timeout     Request timeout
     * @param credentials For authentication
     * @return HttpClient - Configured client
     * @throws Exception
     */
    private static HttpClient getHttpClient(String url, int timeout, Credentials credentials)
            throws Exception {
        HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        //Set the socket connection timeout
        connectionManagerParams.setConnectionTimeout(3000);
        HttpClient client = new HttpClient(connectionManager);
        HttpClientParams clientParams = client.getParams();
        //Set the socket data timeout
        int to = 60000;
        if (timeout > 0) {
            to = timeout;
        }
        clientParams.setSoTimeout(to);

        if (credentials != null) {
            String host;
            try {
                host = new URL(url).getHost();
            } catch (MalformedURLException mue) {
                throw new RemoteCommandException("\nAn error has occurred while trying to resolve the given url: " +
                        url + "\n" + mue.getMessage());
            }
            clientParams.setAuthenticationPreemptive(true);
            AuthScope scope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
            client.getState().setCredentials(scope, credentials);
        }
        return client;
    }
}