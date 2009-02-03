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
package org.artifactory.repo;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.artifactory.engine.ResourceStreamHandle;
import org.artifactory.request.HttpArtifactoryRequest;
import org.artifactory.resource.NotFoundRepoResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.SimpleRepoResource;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

@XmlType(name = "RemoteRepoType",
        propOrder = {"username", "password", "socketTimeoutMillis", "proxy"})
public class HttpRepo extends RemoteRepoBase {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger
            .getLogger(HttpRepo.class);

    private String username;

    private String password;

    private int socketTimeoutMillis = 5000;//Default socket timeout

    private Proxy proxy;

    private transient HttpClient client;

    @Override
    public void init(CentralConfig cc) {
        super.init(cc);
        this.client = createHttpClient();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @XmlIDREF
    @XmlElement(name = "proxyRef")
    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    @XmlElement(defaultValue = "0", required = false)
    public int getSocketTimeoutMillis() {
        return socketTimeoutMillis;
    }

    public void setSocketTimeoutMillis(int socketTimeoutMillis) {
        this.socketTimeoutMillis = socketTimeoutMillis;
    }

    public ResourceStreamHandle retrieveResource(String relPath) throws IOException {
        String fullUrl = getUrl() + "/" + relPath;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("*** Retrieving " + fullUrl + "...");
        }
        final GetMethod method = new GetMethod(fullUrl);
        updateMethod(method);
        ResourceStreamHandle handle;
        client.executeMethod(method);
        //Not found
        if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            throw new FileNotFoundException("Unable to find " + fullUrl + " status " + method.getStatusCode());
        }
        //Found
        if (method.getStatusCode() != HttpStatus.SC_OK) {
            String msg = "Error fetching " + fullUrl + " status " + method.getStatusCode();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(this + ": " + msg);
            }
            throw new FileNotFoundException(msg);
        }
        final InputStream is = method.getResponseBodyAsStream();
        //Create a handle and return it
        handle = new ResourceStreamHandle() {
            public InputStream getInputStream() {
                return is;
            }

            public void close() {
                IOUtils.closeQuietly(is);
                method.releaseConnection();
            }
        };
        return handle;
    }

    protected RepoResource retrieveInfo(String path) {
        String fullUrl = getUrl() + "/" + path;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(this + ": Checking last modified time for " + fullUrl);
        }
        HeadMethod method = new HeadMethod(fullUrl);
        try {
            updateMethod(method);
            client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return new NotFoundRepoResource(path, this);
            }
            if (method.getStatusCode() != HttpStatus.SC_OK) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(this + ": Unable to find " + fullUrl + " because of [" +
                            method.getStatusCode() + "] = " + method.getStatusText());
                }
                return new NotFoundRepoResource(path, this);
            }
            long lastModified = getLastModified(method);
            long size = getContentLength(method);
            SimpleRepoResource res = new SimpleRepoResource(path, this);
            res.setLastModifiedTime(lastModified);
            res.setSize(size);
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //Released the connection back to the connection manager
            method.releaseConnection();
        }
    }

    private HttpClient createHttpClient() {
        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        connectionManagerParams.setDefaultMaxConnectionsPerHost(5);
        connectionManagerParams.setMaxTotalConnections(25);
        HttpClient client = new HttpClient(connectionManager);
        HttpClientParams clientParams = client.getParams();
        //Set the socket connection timeout
        clientParams.setConnectionManagerTimeout(socketTimeoutMillis);
        //Set the socket data timeout
        clientParams.setSoTimeout(socketTimeoutMillis);
        //Limit the retries to a signle retry
        clientParams.setParameter(
                HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(1, false));
        HostConfiguration hostConf = new HostConfiguration();
        Proxy proxy = getProxy();

        if (proxy != null) {
            hostConf.setProxy(proxy.getHost(), proxy.getPort());
            client.setHostConfiguration(hostConf);
            if (proxy.getUsername() != null) {
                if (proxy.getDomain() == null) {
                    Credentials creds = new UsernamePasswordCredentials(proxy.getUsername(),
                            proxy.getPassword());
                    client.getState().setProxyCredentials(AuthScope.ANY, creds);
                } else {
                    try {
                        Credentials creds = new NTCredentials(proxy.getUsername(),
                                proxy.getPassword(), InetAddress.getLocalHost().getHostName(),
                                proxy.getDomain());
                        client.getState().setProxyCredentials(AuthScope.ANY, creds);
                    } catch (UnknownHostException e) {
                        LOGGER.error(
                                "Failed to determine required local hostname for NTLM credentials.",
                                e);
                    }
                }
            }
        } else if (username != null) {
            try {
                String host = new URL(getUrl()).getHost();
                clientParams.setAuthenticationPreemptive(true);
                Credentials creds = new UsernamePasswordCredentials(username, password);
                AuthScope scope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
                client.getState().setCredentials(scope, creds);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Cannot parse the url " + getUrl(), e);
            }
        }
        return client;
    }

    private void updateMethod(HttpMethod method) {
        //Explicitly force keep alive
        method.setRequestHeader("Connection", "Keep-Alive");
        //Set the current requestor
        method.setRequestHeader(HttpArtifactoryRequest.ORIGIN_ARTIFACTORY,
                HttpArtifactoryRequest.HOST_ID);
        //Follow redirects
        method.setFollowRedirects(true);
    }

    private static long getLastModified(HttpMethod method) {
        Header lastModifiedHeader = method.getResponseHeader("Last-Modified");
        if (lastModifiedHeader == null) {
            return -1;
        }
        String lastModifiedString = lastModifiedHeader.getValue();
        try {
            return DateUtil.parseDate(lastModifiedString).getTime();
        }
        catch (DateParseException e) {
            LOGGER.warn("Unable to parse Last-Modified header : " + lastModifiedString);
            return System.currentTimeMillis();
        }
    }

    private static long getContentLength(HeadMethod method) {
        Header contentLengthHeader = method.getResponseHeader("Content-Length");
        if (contentLengthHeader == null) {
            return -1L;
        }
        String lastModifiedString = contentLengthHeader.getValue();
        return Long.parseLong(lastModifiedString);
    }
}