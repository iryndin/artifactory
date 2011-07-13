/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import com.google.common.collect.Sets;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.download.AfterRemoteDownloadAction;
import org.artifactory.addon.plugin.download.BeforeRemoteDownloadAction;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.md.PropertiesImpl;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.remote.browse.HtmlRepositoryBrowser;
import org.artifactory.repo.remote.browse.RemoteItem;
import org.artifactory.repo.remote.browse.RemoteRepositoryBrowser;
import org.artifactory.repo.remote.browse.S3RepositoryBrowser;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.RemoteRepoResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class HttpRepo extends RemoteRepoBase<HttpRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(HttpRepo.class);

    private HttpClient client;
    private boolean handleGzipResponse;
    protected RemoteRepositoryBrowser remoteBrowser;

    public HttpRepo(InternalRepositoryService repositoryService, HttpRepoDescriptor descriptor,
            boolean globalOfflineMode, RemoteRepo oldRemoteRepo) {
        super(repositoryService, descriptor, globalOfflineMode, oldRemoteRepo);
    }

    @Override
    public void init() {
        super.init();
        handleGzipResponse = ConstantValues.httpAcceptEncodingGzip.getBoolean();
        if (!isOffline()) {
            this.client = createHttpClient();
        }
    }

    private synchronized void initRemoteRepositoryBrowser() {
        if (remoteBrowser != null) {
            return; // already initialized
        }
        boolean s3Repository = S3RepositoryBrowser.isS3Repository(getUrl(), client);
        if (s3Repository) {
            log.debug("Repository {} caches S3 repository", getKey());
            remoteBrowser = new S3RepositoryBrowser(client);
        } else {
            remoteBrowser = new HtmlRepositoryBrowser(client);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (client != null) {
            HttpConnectionManager cm = client.getHttpConnectionManager();
            if (cm instanceof MultiThreadedHttpConnectionManager) {
                MultiThreadedHttpConnectionManager mtcm = (MultiThreadedHttpConnectionManager) cm;
                int inPool = mtcm.getConnectionsInPool();
                if (inPool > 0) {
                    log.info("Shutting down {} connections to '{}'...", inPool, getUrl());
                }
                mtcm.shutdown();
            }
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
            RepoResource cachedResource = getCachedResource(relPath);
            if (cachedResource.isFound()) {
                if (cachedResource.isExpired()) {
                    //Send HEAD
                    RepoResource resource = retrieveInfo(relPath, null);
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
        return downloadResource(relPath, new NullRequestContext(getRepoPath(relPath)));
    }

    public ResourceStreamHandle downloadResource(final String relPath, final RequestContext requestContext)
            throws IOException {
        assert !isOffline() : "Should never be called in offline mode";
        String fullPath = convertRequestPathIfNeeded(relPath);
        if (getDescriptor().isSynchronizeProperties()) {
            fullPath += PropertiesImpl.encodeForRequest(requestContext.getProperties());
        }
        final String fullUrl = appendAndGetUrl(convertRequestPathIfNeeded(fullPath));
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        final PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);

        final RepoPathImpl repoPath = new RepoPathImpl(getKey(), fullPath);
        final Request requestForPlugins = requestContext.getRequest();
        pluginAddon.execPluginActions(BeforeRemoteDownloadAction.class, null, requestForPlugins, repoPath);
        if (log.isDebugEnabled()) {
            log.debug("Retrieving " + relPath + " from remote repository '" + getKey() + "' URL '" + fullUrl + "'.");
        }

        final GetMethod method = new GetMethod(fullUrl);
        updateMethod(method);
        client.executeMethod(method);

        int statusCode = method.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            //Not found
            method.releaseConnection();
            throw new RemoteRequestException("Unable to find " + fullUrl, statusCode);
        }
        if (statusCode != HttpStatus.SC_OK) {
            String msg = "Error fetching " + fullUrl;
            if (log.isDebugEnabled()) {
                log.debug(this + ": " + msg + " status " + statusCode);
            }
            method.releaseConnection();
            throw new RemoteRequestException(msg, statusCode);
        }
        //Found
        log.info("{}: Downloading content from '{}'...", this, fullUrl);

        final InputStream is = getResponseStream(method);

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
                log.info(HttpRepo.this + ": Downloaded '{}' with return code: {}.", fullUrl,
                        statusLine != null ? statusLine.getStatusCode() : "unknown");
                pluginAddon.execPluginActions(AfterRemoteDownloadAction.class, null, requestForPlugins, repoPath);
            }
        };
        return handle;
    }

    /**
     * Executes an HTTP method using the repositories client
     *
     * @param method Method to execute
     * @return Response code
     * @throws IOException If the repository is offline or if any error occurs during the execution
     */
    public int executeMethod(HttpMethod method) throws IOException {
        return client.executeMethod(method);
    }

    @Override
    protected RepoResource retrieveInfo(String path, Properties requestProperties) {
        assert !isOffline() : "Should never be called in offline mode";
        RepoPath repoPath = new RepoPathImpl(this.getKey(), path);

        String fullUrl = appendAndGetUrl(convertRequestPathIfNeeded(path));

        HeadMethod method = null;
        try {
            if (getDescriptor().isSynchronizeProperties()) {
                fullUrl += PropertiesImpl.encodeForRequest(requestProperties);
            }
            log.debug("{}: Checking last modified time for {}", this, fullUrl);
            method = new HeadMethod(fullUrl);
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
                // send back unfound resource with 404 status
                return new UnfoundRepoResource(repoPath, method.getStatusText());
            }

            long lastModified = getLastModified(method);
            log.debug("{}: Found last modified time '{}' for {}",
                    new Object[]{this, new Date(lastModified).toString(), fullUrl});

            long size = getContentLength(method);

            Set<ChecksumInfo> checksums = getChecksums(method);

            RepoResource res = new RemoteRepoResource(repoPath, lastModified, size, checksums);
            if (log.isDebugEnabled()) {
                log.debug("{}: Retrieved {} info at '{}'.", new Object[]{this, res, fullUrl});
            }
            return res;
        } catch (IOException e) {
            StringBuilder messageBuilder = new StringBuilder("Failed retrieving resource from ").append(fullUrl).
                    append(": ");
            if (e instanceof UnknownHostException) {
                messageBuilder.append("Unknown host - ");
            }
            messageBuilder.append(e.getMessage());
            throw new RuntimeException(messageBuilder.toString(), e);
        } finally {
            //Released the connection back to the connection manager
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    HttpClient createHttpClient() {
        return new HttpClientConfigurator(true)
                .hostFromUrl(getUrl())
                .defaultMaxConnectionsPerHost(50)
                .maxTotalConnections(50)
                .connectionTimeout(getSocketTimeoutMillis())
                .soTimeout(getSocketTimeoutMillis())
                .staleCheckingEnabled(true)
                .retry(1, false)
                .localAddress(getLocalAddress())
                .proxy(getProxy())
                .authentication(getUsername(), getPassword())
                .getClient();
    }

    private RepoResource getCachedResource(String relPath) {
        LocalCacheRepo cache = getLocalCacheRepo();
        final NullRequestContext context = new NullRequestContext(getRepoPath(relPath));
        RepoResource cachedResource = cache.getInfo(context);
        return cachedResource;
    }

    @SuppressWarnings({"deprecation"})
    private void updateMethod(HttpMethod method) {
        //Explicitly force keep alive
        method.setRequestHeader("Connection", "Keep-Alive");
        //Set the current requestor
        method.setRequestHeader(ArtifactoryRequest.ARTIFACTORY_ORIGINATED, HttpUtils.getHostId());
        //For backwards compatibility
        method.setRequestHeader(ArtifactoryRequest.ORIGIN_ARTIFACTORY, HttpUtils.getHostId());
        //Follow redirects
        method.setFollowRedirects(true);
        //Set gzip encoding
        if (handleGzipResponse) {
            method.addRequestHeader("Accept-Encoding", "gzip");
        }
    }

    private static long getLastModified(HttpMethod method) {
        Header lastModifiedHeader = method.getResponseHeader("Last-Modified");
        if (lastModifiedHeader == null) {
            return -1;
        }
        String lastModifiedString = lastModifiedHeader.getValue();
        try {
            return DateUtil.parseDate(lastModifiedString).getTime();
        } catch (DateParseException e) {
            log.warn("Unable to parse Last-Modified header : " + lastModifiedString);
            return System.currentTimeMillis();
        }
    }

    public static long getContentLength(HttpMethod method) {
        Header contentLengthHeader = method.getResponseHeader("Content-Length");
        if (contentLengthHeader == null) {
            return -1;
        }
        String contentLengthString = contentLengthHeader.getValue();
        return Long.parseLong(contentLengthString);
    }

    private static Set<ChecksumInfo> getChecksums(HeadMethod method) {
        Set<ChecksumInfo> actualChecksums = Sets.newHashSet();

        ChecksumInfo md5ChecksumInfo = getChecksumInfoObject(ChecksumType.md5,
                method.getResponseHeader(ArtifactoryRequest.CHECKSUM_MD5));
        if (md5ChecksumInfo != null) {
            actualChecksums.add(md5ChecksumInfo);
        }

        ChecksumInfo sha1ChecksumInfo = getChecksumInfoObject(ChecksumType.sha1,
                method.getResponseHeader(ArtifactoryRequest.CHECKSUM_SHA1));
        if (sha1ChecksumInfo != null) {
            actualChecksums.add(sha1ChecksumInfo);
        }

        return actualChecksums;
    }

    private static ChecksumInfo getChecksumInfoObject(ChecksumType type, Header checksumHeader) {
        if (checksumHeader == null) {
            return null;
        }

        return new ChecksumInfo(type, checksumHeader.getValue(), null);
    }

    private InputStream getResponseStream(GetMethod method) throws IOException {
        InputStream is = method.getResponseBodyAsStream();
        if (handleGzipResponse) {
            Header[] contentEncodings = method.getResponseHeaders("Content-Encoding");
            for (int i = 0, n = contentEncodings.length; i < n; i++) {
                if ("gzip".equalsIgnoreCase(contentEncodings[i].getValue())) {
                    return new GZIPInputStream(is);
                }
            }
        }
        return is;
    }

    @Override
    protected List<RemoteItem> getChildUrls(String dirUrl) throws IOException {
        if (remoteBrowser == null) {
            initRemoteRepositoryBrowser();
        }
        return remoteBrowser.listContent(dirUrl);
    }

    /**
     * Converts the given path to the remote repo's layout if defined
     *
     * @param path Path to convert
     * @return Converted path if required and conversion was successful, given path if not
     */
    public String convertRequestPathIfNeeded(String path) {
        HttpRepoDescriptor descriptor = getDescriptor();
        return ModuleInfoUtils.
                translateArtifactPath(descriptor.getRepoLayout(), descriptor.getRemoteRepoLayout(), path);
    }
}
