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
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoType;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class HttpRepo extends RemoteRepoBase<HttpRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(HttpRepo.class);

    private HttpClient client;

    public HttpRepo(InternalRepositoryService repositoryService, HttpRepoDescriptor descriptor,
            boolean globalOfflineMode) {
        super(repositoryService, descriptor, globalOfflineMode);
    }

    @Override
    public void init() {
        super.init();
        if (!isOffline()) {
            this.client = createHttpClient();
        }
    }

    public String getUsername() {
        return getDescriptor().getUsername();
    }

    public String getPassword() {
        return getDescriptor().getPassword();
    }

    public int getSocketTimeoutMillis() {
        return getDescriptor().getSocketTimeoutMillis();
    }

    public String getLocalAddress() {
        return getDescriptor().getLocalAddress();
    }

    public ProxyDescriptor getProxy() {
        return getDescriptor().getProxy();
    }

    public ResourceStreamHandle conditionalRetrieveResource(String relPath) throws IOException {
        //repo1 does not respect conditional get so the following is irrelevant for now.
        /*
        Date modifiedSince;
        if (modifiedSince != null) {
            //Add the if modified since
            String formattedDate = DateUtil.formatDate(modifiedSince);
            method.setRequestHeader("If-Modified-Since", formattedDate);
        }
        if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
            return new NullResourceStreamHandle();
        }
        */
        //Need to do a conditional get by hand - testing a head result last modified date against the current file
        JcrFile jcrFile = getLocalCacheRepo().getJcrFile(relPath);
        if (jcrFile != null) {
            //Send HEAD
            RepoResource resource = retrieveInfo(relPath);
            if (resource.isFound()) {
                //Test its date
                Date existingDate = new Date(jcrFile.getLastModified());
                Date foundDate = new Date(resource.getLastModified());
                if (existingDate.after(foundDate)) {
                    return new NullResourceStreamHandle();
                }
            }
        }
        //Do GET
        return retrieveResource(relPath);
    }

    public ResourceStreamHandle retrieveResource(String relPath) throws IOException {
        assert !isOffline() : "Should never be called in offline mode";
        RemoteRepoType repoType = getType();
        String fullUrl = getUrl() + "/" + pathConvert(repoType, relPath);
        if (log.isDebugEnabled()) {
            log.debug("Retrieving " + relPath + " from remote repository '" + getKey() + "' URL '" + fullUrl + "'.");
        }
        final GetMethod method = new GetMethod(fullUrl);
        updateMethod(method);
        client.executeMethod(method);
        //Not found
        int statusCode = method.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            throw new FileNotFoundException("Unable to find " + fullUrl + " status " + statusCode);
        }
        if (statusCode != HttpStatus.SC_OK) {
            String msg = "Error fetching " + fullUrl + " status " + statusCode;
            if (log.isDebugEnabled()) {
                log.debug(this + ": " + msg);
            }
            throw new FileNotFoundException(msg);
        }
        //Found
        if (log.isInfoEnabled()) {
            log.info(this + ": Retrieving '" + fullUrl + "'...");
        }
        final InputStream is = method.getResponseBodyAsStream();
        //Create a handle and return it
        ResourceStreamHandle handle = new ResourceStreamHandle() {
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

    @Override
    protected RepoResource retrieveInfo(String relPath) {
        assert !isOffline() : "Should never be called in offline mode";
        RemoteRepoType repoType = getType();
        RepoPath repoPath = new RepoPath(this.getKey(), relPath);
        String fullUrl = getUrl() + "/" + pathConvert(repoType, relPath);
        log.debug("{}: Checking last modified time for {}", this, fullUrl);
        HeadMethod method = new HeadMethod(fullUrl);
        try {
            updateMethod(method);
            client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return new UnfoundRepoResource(repoPath, method.getStatusText());
            }
            if (method.getStatusCode() != HttpStatus.SC_OK) {
                if (log.isDebugEnabled()) {
                    log.debug(this + ": Unable to find " + fullUrl + " because of [" +
                            method.getStatusCode() + "] = " + method.getStatusText());
                }
                return new UnfoundRepoResource(repoPath, method.getStatusText());
            }
            long lastModified = getLastModified(method);
            long size = getContentLength(method);
            FileResource res = new FileResource(repoPath);
            res.getInfo().setLastModified(lastModified);
            res.getInfo().setSize(size);
            if (log.isDebugEnabled()) {
                log.debug(this + ": Retrieved " + res + " info at '" + fullUrl + "'.");
            }
            return res;
        } catch (Exception e) {
            throw new RuntimeException("Failed retrieving resource from " + fullUrl + ": " + e.getMessage(), e);
        } finally {
            //Released the connection back to the connection manager
            method.releaseConnection();
        }
    }

    private HttpClient createHttpClient() {
        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams connectionManagerParams = connectionManager.getParams();
        connectionManagerParams.setDefaultMaxConnectionsPerHost(50);
        connectionManagerParams.setMaxTotalConnections(50);
        //Set the socket connection timeout
        int socketTimeoutMillis = getSocketTimeoutMillis();
        connectionManagerParams.setConnectionTimeout(socketTimeoutMillis);
        connectionManagerParams.setSoTimeout(socketTimeoutMillis);
        connectionManagerParams.setStaleCheckingEnabled(true);
        HttpClient client = new HttpClient(connectionManager);
        HttpClientParams clientParams = client.getParams();
        //Set the socket data timeout
        clientParams.setSoTimeout(socketTimeoutMillis);
        //Limit the retries to a signle retry
        clientParams.setParameter(
                HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(1, false));
        HostConfiguration hostConf = new HostConfiguration();
        //Set the local address if exists
        String localAddress = getLocalAddress();
        if (!StringUtils.isEmpty(localAddress)) {
            InetAddress address;
            try {
                address = InetAddress.getByName(localAddress);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Invalid local address: " + localAddress, e);
            }
            hostConf.setLocalAddress(address);
        }
        //Set the proxy
        ProxyDescriptor proxy = getProxy();
        if (proxy != null) {
            hostConf.setProxy(proxy.getHost(), proxy.getPort());
            client.setHostConfiguration(hostConf);
            if (proxy.getUsername() != null) {
                if (proxy.getDomain() == null) {
                    Credentials creds = new UsernamePasswordCredentials(proxy.getUsername(),
                            proxy.getPassword());
                    //This will exclude the NTLM authentication scheme so that the proxy won't barf
                    //when we try to give it traditional credentials. If the proxy doesn't do NTLM
                    //then this won't hurt it (jcej at tragus dot org)
                    List<String> authPrefs = Arrays.asList(AuthPolicy.DIGEST, AuthPolicy.BASIC);
                    client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
                    client.getState().setProxyCredentials(AuthScope.ANY, creds);
                } else {
                    try {
                        Credentials creds = new NTCredentials(proxy.getUsername(),
                                proxy.getPassword(), InetAddress.getLocalHost().getHostName(),
                                proxy.getDomain());
                        client.getState().setProxyCredentials(AuthScope.ANY, creds);
                    } catch (UnknownHostException e) {
                        log.error(
                                "Failed to determine required local hostname for NTLM credentials.",
                                e);
                    }
                }
            }
        }
        String username = getUsername();
        if (username != null) {
            try {
                String host = new URL(getUrl()).getHost();
                clientParams.setAuthenticationPreemptive(true);
                Credentials creds = new UsernamePasswordCredentials(username, getPassword());
                AuthScope scope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
                client.getState().setCredentials(scope, creds);
            } catch (MalformedURLException e) {
                throw new RuntimeException("Cannot parse the url " + getUrl(), e);
            }
        }
        return client;
    }

    @SuppressWarnings({"deprecation"})
    private static void updateMethod(HttpMethod method) {
        //Explicitly force keep alive
        method.setRequestHeader("Connection", "Keep-Alive");
        //Set the current requestor
        method.setRequestHeader(ArtifactoryRequest.ARTIFACTORY_ORIGINATED, PathUtils.getHostId());
        //For backwards compatibility
        method.setRequestHeader(ArtifactoryRequest.ORIGIN_ARTIFACTORY, PathUtils.getHostId());
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
            log.warn("Unable to parse Last-Modified header : " + lastModifiedString);
            return System.currentTimeMillis();
        }
    }

    private static String pathConvert(RemoteRepoType repoType, String path) {
        switch (repoType) {
            case maven2:
                return path;
            case maven1:
                return MavenUtils.toMaven1Path(path);
            case obr:
                return path;
        }
        // Cannot reach here
        return path;
    }

    private static long getContentLength(HeadMethod method) {
        Header contentLengthHeader = method.getResponseHeader("Content-Length");
        if (contentLengthHeader == null) {
            return -1;
        }
        String contentLengthString = contentLengthHeader.getValue();
        return Long.parseLong(contentLengthString);
    }

    public String getMetadata(String path) throws IOException {
        return null;
    }
}