/*
 * This file is part of Artifactory.
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

package org.artifactory.rest.test;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.artifactory.api.rest.SystemInfo;
import org.artifactory.api.xstream.XStreamFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * User: freds Date: Aug 12, 2008 Time: 5:36:40 PM
 */
public class TestSystemRest {

    /**
     * /api /system (GET, POST - import/export) /config (GET, PUT, POST, DELETE) /security (GET, PUT, POST, DELETE)
     * /security /users /[username] /groups /[groupname] /acls /[aclname] /repo /[repoName] (GET, POST - import/export)
     * /allpaths /config /backup /jobs /repo /local /remote /virtual /proxy
     */

    private static final String API_ROOT = "http://localhost:8080/artifactory/api/";

    @Test
    public void testExportTo() throws Exception {
        String systemUri = API_ROOT + "system";
        SystemInfo systemInfo = get(systemUri, SystemInfo.class);
        assertNotNull(systemInfo);
        //SystemActionInfo action = systemInfo.actionExample;
        //Assert.assertNotNull(action);
        //log.debug("Received SystemInfo " + systemInfo);
        //action.importFrom = "";
        //action.exportTo = "/tmp/testRestExport";
        //action.repositories = "";// All repos if empty
        //action.security = true;
        //action.config = true;
        //post(systemUri, action, null);
    }

    @Test
    public void testImportFrom() throws Exception {
        String systemUri = API_ROOT + "system";
        SystemInfo systemInfo = get(systemUri, SystemInfo.class);
        assertNotNull(systemInfo);
        //SystemActionInfo action = systemInfo.actionExample;
        //Assert.assertNotNull(action);
        //log.debug("Received SystemInfo " + systemInfo);
        //action.importFrom = "/tmp/testRestartifacts/20080901.121146";
        //action.exportTo = "";
        //action.repositories = "";// All repos if empty
        //action.security = true;
        //action.config = true;
        //post(systemUri, action, null);
    }

    @Test
    public void unauthorizedAccess() throws Exception {
        GetMethod method = new GetMethod(API_ROOT + "system");
        int status = getHttpClient(false).executeMethod(method);
        assertEquals(status, HttpStatus.SC_UNAUTHORIZED);
    }

    @SuppressWarnings({"unchecked"})
    protected static <I, O> O post(String uri, I inObj, Class<O> outObjClass)
            throws Exception {
        XStream xStream = XStreamFactory.create(inObj.getClass());
        if (outObjClass != null) {
            xStream.processAnnotations(outObjClass);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        xStream.toXML(inObj, bos);
        if (outObjClass != null) {
            byte[] bytes = post(uri, bos.toByteArray(), "application/xml", 200, "application/xml",
                    false);
            return (O) xStream.fromXML(new ByteArrayInputStream(bytes));
        } else {
            post(uri, bos.toByteArray(), "application/xml", 200, "text/plain", true);
            return null;
        }
    }

    @SuppressWarnings({"unchecked"})
    protected static <T> T get(String uri, Class<T> xstreamObjClass) throws Exception {
        byte[] bytes = get(uri, 200, "application/xml");
        XStream xStream = XStreamFactory.create(xstreamObjClass);
        return (T) xStream.fromXML(new ByteArrayInputStream(bytes));
    }

    protected static byte[] get(String uri, int expectedStatus,
            String expectedMediaType) throws Exception {
        GetMethod method = new GetMethod(uri);
        int status = getHttpClient(true).executeMethod(method);
        assertEquals(status, expectedStatus);
        Header mediaType = method.getResponseHeader("content-type");
        assertEquals(mediaType.getValue(), expectedMediaType);

        InputStream is = method.getResponseBodyAsStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int r;
        while ((r = is.read(buffer)) != -1) {
            baos.write(buffer, 0, r);
        }

        return baos.toByteArray();
    }

    protected static byte[] post(String uri, final byte[] data,
            final String inputDataType,
            int expectedStatus,
            String expectedMediaType, boolean printStream) throws Exception {
        PostMethod method = new PostMethod(uri);
        method.setRequestEntity(new RequestEntity() {
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
                return inputDataType;
            }
        });
        int status = getHttpClient(true).executeMethod(method);
        assertEquals(status, expectedStatus);
        Header mediaType = method.getResponseHeader("content-type");
        Assert.assertTrue(mediaType.getValue().contains(expectedMediaType));

        InputStream is = method.getResponseBodyAsStream();
        byte[] buffer = new byte[1024];
        int r;
        if (printStream) {
            while ((r = is.read(buffer)) != -1) {
                System.out.println(new String(buffer, 0, r, "utf-8"));
            }
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((r = is.read(buffer)) != -1) {
                baos.write(buffer, 0, r);
            }

            return baos.toByteArray();
        }
    }

    protected static HttpClient getHttpClient(boolean useAdmin) throws Exception {
        HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        //Set the socket connection timeout
        connectionManagerParams.setConnectionTimeout(10);
        HttpClient client = new HttpClient(connectionManager);
        HttpClientParams clientParams = client.getParams();
        //Set the socket data timeout
        clientParams.setSoTimeout(3000);
        if (useAdmin) {
            String host = new URL(API_ROOT).getHost();
            clientParams.setAuthenticationPreemptive(true);
            Credentials creds = new UsernamePasswordCredentials("admin", "password");
            AuthScope scope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
            client.getState().setCredentials(scope, creds);
        }
        return client;
    }
}
