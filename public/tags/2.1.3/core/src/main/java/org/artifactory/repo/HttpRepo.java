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
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.request.ArtifactoryRequest;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.util.HttpClientUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

public class HttpRepo extends RemoteRepoBase<HttpRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(HttpRepo.class);

    private HttpClient client;

    public HttpRepo(
            InternalRepositoryService repositoryService, HttpRepoDescriptor descriptor, boolean globalOfflineMode) {
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
        if (isStoreArtifactsLocally()) {
            LocalCacheRepo cache = getLocalCacheRepo();
            RepoResource cachedResource = cache.getInfo(new NullRequestContext(relPath));
            if (cachedResource.isFound()) {
                if (cachedResource.isExpired()) {
                    //Send HEAD
                    RepoResource resource = retrieveInfo(relPath);
                    if (resource.isFound()) {
                        if (cachedResource.getLastModified() > resource.getLastModified()) {
                            return new NullResourceStreamHandle();
                        }
                    }
                } else {
                    return new NullResourceStreamHandle();
                }
            }
        }
        //Do GET
        return downloadResource(relPath);
    }

    public ResourceStreamHandle downloadResource(String relPath) throws IOException {
        assert !isOffline() : "Should never be called in offline mode";
        RepoType repoType = getType();
        final String fullUrl = getUrl() + "/" + pathConvert(repoType, relPath);
        if (log.isDebugEnabled()) {
            log.debug("Retrieving " + relPath + " from remote repository '" + getKey() + "' URL '" + fullUrl + "'.");
        }
        final GetMethod method = new GetMethod(fullUrl);
        updateMethod(method);
        client.executeMethod(method);
        //Not found
        int statusCode = method.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            throw new RemoteRequestException("Unable to find " + fullUrl, statusCode);
        }
        if (statusCode != HttpStatus.SC_OK) {
            String msg = "Error fetching " + fullUrl;
            if (log.isDebugEnabled()) {
                log.debug(this + ": " + msg + " status " + statusCode);
            }
            throw new RemoteRequestException(msg, statusCode);
        }
        //Found
        if (log.isInfoEnabled()) {
            log.info(this + ": Retrieving '{}'...", fullUrl);
        }
        final InputStream is = method.getResponseBodyAsStream();
        //Create a handle and return it
        ResourceStreamHandle handle = new ResourceStreamHandle() {
            public InputStream getInputStream() {
                return is;
            }

            public long getSize() {
                return -1;
            }

            public void close() {
                IOUtils.closeQuietly(is);
                method.releaseConnection();
                StatusLine statusLine = method.getStatusLine();
                log.info(HttpRepo.this + ": Retrieved '{}' with return code: {}.", fullUrl,
                        statusLine != null ? statusLine.getStatusCode() : "unknown");
            }
        };
        return handle;
    }

    @Override
    protected RepoResource retrieveInfo(String relPath) {
        assert !isOffline() : "Should never be called in offline mode";
        RepoType repoType = getType();
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
            //If redirected to a directory - return 404
            if (method.getPath().endsWith("/")) {
                return new UnfoundRepoResource(repoPath, "Expected file response but received a directory response.");
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
            RepoResource res;
            if (NamingUtils.isMetadata(relPath)) {
                res = new MetadataResource(repoPath);
            } else {
                res = new FileResource(repoPath);
            }
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

    HttpClient createHttpClient() {
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
        //Set uagent
        HttpClientUtils.configureUserAgent(client);
        HttpClientParams clientParams = client.getParams();
        //Set the socket data timeout
        clientParams.setSoTimeout(socketTimeoutMillis);
        //Limit the retries to a signle retry
        clientParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(1, false));
        //Set the host
        String host;
        try {
            host = new URL(getUrl()).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot parse the url " + getUrl(), e);
        }
        client.getHostConfiguration().setHost(host);
        //Set the local address if exists
        String localAddress = getLocalAddress();
        if (StringUtils.isNotBlank(localAddress)) {
            InetAddress address;
            try {
                address = InetAddress.getByName(localAddress);
                client.getHostConfiguration().setLocalAddress(address);
            } catch (UnknownHostException e) {
                log.error("Invalid local address: " + localAddress, e);
            }
        }
        //Set the proxy
        ProxyDescriptor proxy = getProxy();
        HttpClientUtils.configureProxy(client, proxy);
        //Set authentication
        String username = getUsername();
        if (StringUtils.isNotBlank(username)) {
            clientParams.setAuthenticationPreemptive(true);
            Credentials creds = new UsernamePasswordCredentials(username, getPassword());
            AuthScope scope = new AuthScope(host, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
            client.getState().setCredentials(scope, creds);
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

    private static String pathConvert(RepoType repoType, String path) {
        switch (repoType) {
            case maven2:
                return path;
            case maven1:
                return MavenNaming.toMaven1Path(path);
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
}