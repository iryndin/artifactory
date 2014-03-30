/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.StatusLine;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.LayoutsCoreAddon;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.download.AfterRemoteDownloadAction;
import org.artifactory.addon.plugin.download.BeforeRemoteDownloadAction;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.io.RemoteResourceStreamHandle;
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.model.common.RepoPathImpl;
import org.artifactory.repo.remote.browse.HtmlRepositoryBrowser;
import org.artifactory.repo.remote.browse.HttpExecutor;
import org.artifactory.repo.remote.browse.RemoteItem;
import org.artifactory.repo.remote.browse.RemoteRepositoryBrowser;
import org.artifactory.repo.remote.browse.S3RepositoryBrowser;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.RepoRequests;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.RemoteRepoResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HttpRepo extends RemoteRepoBase<HttpRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(HttpRepo.class);

    @Nullable
    private HttpClient client;
    private boolean handleGzipResponse;
    protected RemoteRepositoryBrowser remoteBrowser;
    private LayoutsCoreAddon layoutsCoreAddon;

    @GuardedBy("offlineCheckerSync")
    private Thread onlineMonitorThread;
    private Object onlineMonitorSync = new Object();

    public HttpRepo(HttpRepoDescriptor descriptor, InternalRepositoryService repositoryService,
            boolean globalOfflineMode, RemoteRepo oldRemoteRepo) {
        super(descriptor, repositoryService, globalOfflineMode, oldRemoteRepo);
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        layoutsCoreAddon = addonsManager.addonByType(LayoutsCoreAddon.class);
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
        HttpExecutor clientExec = new HttpExecutor() {
            @Override
            public int executeMethod(HttpMethod method) throws IOException {
                return HttpRepo.this.executeMethod(method);
            }
        };
        boolean s3Repository = S3RepositoryBrowser.isS3Repository(getUrl(), getHttpClient());
        if (s3Repository) {
            log.debug("Repository {} caches S3 repository", getKey());
            remoteBrowser = new S3RepositoryBrowser(clientExec, this);
        } else {
            remoteBrowser = new HtmlRepositoryBrowser(clientExec);
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
        cleanupResources();
    }

    @Override
    public void cleanupResources() {
        stopOfflineCheckThread();
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

    @Override
    public ResourceStreamHandle conditionalRetrieveResource(String relPath, boolean forceRemoteDownload)
            throws IOException {
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
        if (!forceRemoteDownload && isStoreArtifactsLocally()) {
            RepoResource cachedResource = getCachedResource(relPath);
            if (cachedResource.isFound()) {
                if (cachedResource.isExpired()) {
                    //Send HEAD
                    RepoResource resource = retrieveInfo(relPath, false/*The relPath refers to files (.gz)*/, null);
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

    @Override
    public ResourceStreamHandle downloadResource(String relPath) throws IOException {
        return downloadResource(relPath, new NullRequestContext(getRepoPath(relPath)));
    }

    @Override
    public ResourceStreamHandle downloadResource(final String relPath, final RequestContext requestContext)
            throws IOException {
        assert !isOffline() : "Should never be called in offline mode";
        String pathForUrl = convertRequestPathIfNeeded(relPath);
        if (!relPath.equals(pathForUrl)) {
            RepoRequests.logToContext("Remote resource path was translated (%s) due to repository " +
                    "layout differences", pathForUrl);
        }
        Request request = requestContext.getRequest();
        if (request != null) {
            String alternativeRemoteDownloadUrl =
                    request.getParameter(ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL);
            if (StringUtils.isNotBlank(alternativeRemoteDownloadUrl)) {
                RepoRequests.logToContext("Request contains alternative remote resource path ({}=%s)",
                        ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL, alternativeRemoteDownloadUrl);
                pathForUrl = alternativeRemoteDownloadUrl;
            }
        }

        RepoRequests.logToContext("Appending matrix params to remote request URL");
        pathForUrl += buildRequestMatrixParams(requestContext.getProperties());
        final String fullUrl = appendAndGetUrl(convertRequestPathIfNeeded(pathForUrl));
        RepoRequests.logToContext("Using remote request URL - %s", fullUrl);
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        final PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);

        final RepoPath repoPath = InternalRepoPathFactory.create(getKey(), pathForUrl);
        final Request requestForPlugins = requestContext.getRequest();
        RepoRequests.logToContext("Executing any BeforeRemoteDownload user plugins that may exist");
        pluginAddon.execPluginActions(BeforeRemoteDownloadAction.class, null, requestForPlugins, repoPath);

        final GetMethod method = new GetMethod(HttpUtils.encodeQuery(fullUrl));
        RepoRequests.logToContext("Executing GET request to %s", fullUrl);
        executeMethod(method);

        int statusCode = method.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            //Not found
            method.releaseConnection();
            RepoRequests.logToContext("Received response status %s - throwing exception", statusCode);
            throw new RemoteRequestException("Unable to find " + fullUrl, statusCode);
        }
        if (statusCode != HttpStatus.SC_OK) {
            String msg = "Error fetching " + fullUrl;
            method.releaseConnection();
            RepoRequests.logToContext("Received response status %s - throwing exception", statusCode);
            throw new RemoteRequestException(msg, statusCode);
        }
        //Found
        logDownload(fullUrl, "Downloading...");
        RepoRequests.logToContext("Downloading content");

        final InputStream is = HttpUtils.getGzipAwareResponseStream(method);

        return new RemoteResourceStreamHandle() {
            @Override
            public InputStream getInputStream() {
                return is;
            }

            @Override
            public long getSize() {
                return -1;
            }

            @Override
            public void close() {
                IOUtils.closeQuietly(is);
                method.releaseConnection();
                StatusLine statusLine = method.getStatusLine();

                Throwable throwable = getThrowable();
                if (throwable != null) {
                    log.error("{}: Failed to download '{}'. Received status code {} and caught exception: {}",
                            HttpRepo.this, fullUrl, statusLine != null ? statusLine.getStatusCode() : "unknown",
                            throwable.getMessage());
                    log.debug("Failed to download '"+ fullUrl + "'", throwable);
                    RepoRequests.logToContext("Failed to download: %s", throwable.getMessage());
                } else {
                    logDownload(fullUrl,
                            "Downloaded status='" + (statusLine != null ? statusLine.getStatusCode() :
                                    "unknown") + "'");
                    RepoRequests.logToContext("Downloaded content");
                }
                RepoRequests.logToContext("Executing any AfterRemoteDownload user plugins that may exist");
                pluginAddon.execPluginActions(AfterRemoteDownloadAction.class, null, requestForPlugins, repoPath);
                RepoRequests.logToContext("Executed all AfterRemoteDownload user plugins");
            }
        };
    }

    private void logDownload(String fullUrl, String status) {
        if (NamingUtils.isChecksum(fullUrl)) {
            log.debug("{}: url='{}' : {}", this, fullUrl, status);
        } else {
            log.info("{}: url='{}' : {}", this, fullUrl, status);
        }
    }

    /**
     * Executes an HTTP method using the repositories client while auto-following redirects
     *
     * @param method Method to execute
     * @return Response code
     * @throws IOException If the repository is offline or if any error occurs during the execution
     */
    public int executeMethod(HttpMethod method) throws IOException {
        return executeMethod(method, true);
    }

    /**
     * Executes an HTTP method using the repositories client
     *
     * @param method          Method to execute
     * @param followRedirects True if redirections should be auto-followed
     * @return Response code
     * @throws IOException If the repository is offline or if any error occurs during the execution
     */
    public int executeMethod(HttpMethod method, boolean followRedirects) throws IOException {
        updateMethod(method, followRedirects);
        return getHttpClient().executeMethod(method);
    }

    @Override
    protected RepoResource retrieveInfo(String path, boolean folder, @Nullable RequestContext context) {
        assert !isOffline() : "Should never be called in offline mode";
        RepoPath repoPath = InternalRepoPathFactory.create(this.getKey(), path, folder);

        String fullUrl = assembleRetrieveInfoUrl(path, context);
        HeadMethod method = new HeadMethod(HttpUtils.encodeQuery(fullUrl));
        RepoRequests.logToContext("Executing HEAD request to %s", fullUrl);
        try {

            try {
                executeMethod(method);
            } catch (IOException e) {
                RepoRequests.logToContext("Failed to execute HEAD request: %s", e.getMessage());
                StringBuilder messageBuilder = new StringBuilder("Failed retrieving resource from ").append(fullUrl).
                        append(": ");
                if (e instanceof UnknownHostException) {
                    messageBuilder.append("Unknown host - ");
                }
                messageBuilder.append(e.getMessage());
                throw new RuntimeException(messageBuilder.toString(), e);
            }

            return handleGetInfoResponse(repoPath, method);
        } finally {
            //Released the connection back to the connection manager
            method.releaseConnection();
        }
    }

    protected String assembleRetrieveInfoUrl(String path, RequestContext context) {
        String pathForUrl = convertRequestPathIfNeeded(path);
        if (!path.equals(pathForUrl)) {
            RepoRequests.logToContext("Remote resource path was translated (%s) due to repository " +
                    "layout differences", pathForUrl);
        }
        boolean validContext = context != null;
        if (validContext) {
            Request request = context.getRequest();
            if (request != null) {
                String alternativeRemoteDownloadUrl =
                        request.getParameter(ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL);
                if (StringUtils.isNotBlank(alternativeRemoteDownloadUrl)) {
                    RepoRequests.logToContext("Request contains alternative remote resource path ({}=%s)",
                            ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL, alternativeRemoteDownloadUrl);
                    pathForUrl = alternativeRemoteDownloadUrl;
                }
            }
        }

        String fullUrl = appendAndGetUrl(pathForUrl);
        if (validContext) {
            RepoRequests.logToContext("Appending matrix params to remote request URL");
            Properties properties = context.getProperties();
            fullUrl += buildRequestMatrixParams(properties);
        }
        RepoRequests.logToContext("Using remote request URL - %s", fullUrl);
        return fullUrl;
    }

    /**
     * Notice: for use with HEAD method, no content is expected in the response.
     * Process the remote repository's response and construct a repository resource.
     *
     * @param repoPath of requested resource
     * @param method   executed {@link HeadMethod} from which to process the response.
     * @return
     */
    protected RepoResource handleGetInfoResponse(RepoPath repoPath, HttpMethodBase method) {
        int statusCode = method.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            RepoRequests.logToContext("Received status 404 (message: %s) on remote info request - returning unfound " +
                    "resource", method.getStatusText());
            return new UnfoundRepoResource(repoPath, method.getStatusText());
        }
        //If redirected to a directory - redirect client
        if (method.getPath().endsWith("/")) {
            RepoRequests.logToContext("Remote info request was redirected to a directory - returning unfound resource");
            throw new FileExpectedException(new RepoPathImpl(repoPath.getRepoKey(), repoPath.getPath(), true));
        }
        // Some servers may return 204 instead of 200
        if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_NO_CONTENT) {
            RepoRequests.logToContext("Received status {} (message: %s) on remote info request - returning unfound " +
                    "resource", statusCode, method.getStatusText());
            // send back unfound resource with 404 status
            return new UnfoundRepoResource(repoPath, method.getStatusText());
        }

        long lastModified = getLastModified(method);
        RepoRequests.logToContext("Found remote resource with last modified time - %s",
                new Date(lastModified).toString());

        long contentLength = HttpUtils.getContentLength(method);
        if (contentLength != -1) {
            RepoRequests.logToContext("Found remote resource with content length - %s", contentLength);
        }

        // if status is 204 and length is not 0 then the remote server is doing something wrong
        if (statusCode == HttpStatus.SC_NO_CONTENT && contentLength > 0) {
            // send back unfound resource with 404 status
            RepoRequests.logToContext("Received status {} (message: %s) on remote info request - returning unfound " +
                    "resource", statusCode, method.getStatusText());
            return new UnfoundRepoResource(repoPath, method.getStatusText());
        }

        Set<ChecksumInfo> checksums = getChecksums(method);
        if (!checksums.isEmpty()) {
            RepoRequests.logToContext("Found remote resource with checksums - %s", checksums);
        }

        String originalPath = repoPath.getPath();
        String filename = getFilename(method, originalPath);
        if (StringUtils.isNotBlank(filename)) {
            RepoRequests.logToContext("Found remote resource with filename header - %s", filename);
            if (NamingUtils.isMetadata(originalPath)) {
                String originalPathStrippedOfMetadata = NamingUtils.getMetadataParentPath(originalPath);
                String originalPathWithMetadataNameFromHeader =
                        NamingUtils.getMetadataPath(originalPathStrippedOfMetadata, filename);
                repoPath = InternalRepoPathFactory.create(repoPath.getRepoKey(),
                        originalPathWithMetadataNameFromHeader);
            } else {
                repoPath = InternalRepoPathFactory.create(repoPath.getParent(), filename);
            }
        }

        RepoRequests.logToContext("Returning found remote resource info");
        RepoResource res = new RemoteRepoResource(repoPath, lastModified, contentLength, checksums);
        return res;
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
    private void updateMethod(HttpMethod method, boolean followRedirects) {
        //Explicitly force keep alive
        method.setRequestHeader("Connection", "Keep-Alive");

        //Add the current requester host id
        Set<String> originatedHeaders = RepoRequests.getOriginatedHeaders();
        for (String originatedHeader : originatedHeaders) {
            method.addRequestHeader(ArtifactoryRequest.ARTIFACTORY_ORIGINATED, originatedHeader);
        }
        String hostId = ContextHelper.get().beanForType(AddonsManager.class).addonByType(
                HaCommonAddon.class).getHostId();
        method.addRequestHeader(ArtifactoryRequest.ARTIFACTORY_ORIGINATED, hostId);

        //For backwards compatibility
        method.setRequestHeader(ArtifactoryRequest.ORIGIN_ARTIFACTORY, hostId);

        //Follow redirects
        if (!(method instanceof EntityEnclosingMethod)) {
            method.setFollowRedirects(followRedirects);
        }

        //Set gzip encoding
        if (handleGzipResponse) {
            method.addRequestHeader("Accept-Encoding", "gzip");
        }

        // Set custom query params
        String queryParams = getDescriptor().getQueryParams();
        if (StringUtils.isNotBlank(queryParams)) {
            String existingQueryParams = method.getQueryString();
            String encodedQueryParams = HttpUtils.encodeQuery(queryParams);
            if (StringUtils.isBlank(existingQueryParams)) {
                method.setQueryString(encodedQueryParams);
            } else {
                method.setQueryString(existingQueryParams + "&" + encodedQueryParams);
            }
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

    private String getFilename(HttpMethod method, String originalPath) {
        // Skip filename parsing if we are not dealing with latest maven non-unique snapshot request
        if (!isRequestForLatestMavenSnapshot(originalPath)) {
            return null;
        }

        // Try our custom X-Artifactory-Filename header
        Header filenameHeader = method.getResponseHeader(ArtifactoryRequest.FILE_NAME);
        if (filenameHeader != null) {
            String filenameString = filenameHeader.getValue();
            try {
                return URLDecoder.decode(filenameString, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.warn("Unable to decode '{}' header '{}', returning un-encoded value.",
                        ArtifactoryRequest.FILE_NAME, filenameString);
                return filenameString;
            }
        }

        // Didn't find any filename, return null
        return null;
    }

    private boolean isRequestForLatestMavenSnapshot(String originalPath) {
        if (ConstantValues.requestDisableVersionTokens.getBoolean()) {
            return false;
        }

        if (!getDescriptor().isMavenRepoLayout()) {
            return false;
        }

        if (!MavenNaming.isNonUniqueSnapshot(originalPath)) {
            return false;
        }

        return true;
    }

    private static Set<ChecksumInfo> getChecksums(HttpMethodBase method) {
        Set<ChecksumInfo> remoteChecksums = Sets.newHashSet();

        ChecksumInfo md5ChecksumInfo = getChecksumInfoObject(ChecksumType.md5,
                method.getResponseHeader(ArtifactoryRequest.CHECKSUM_MD5));
        if (md5ChecksumInfo != null) {
            remoteChecksums.add(md5ChecksumInfo);
        }

        ChecksumInfo sha1ChecksumInfo = getChecksumInfoObject(ChecksumType.sha1,
                method.getResponseHeader(ArtifactoryRequest.CHECKSUM_SHA1));
        if (sha1ChecksumInfo != null) {
            remoteChecksums.add(sha1ChecksumInfo);
        }

        return remoteChecksums;
    }

    private static ChecksumInfo getChecksumInfoObject(ChecksumType type, Header checksumHeader) {
        if (checksumHeader == null) {
            return null;
        }

        return new ChecksumInfo(type, checksumHeader.getValue(), null);
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
        return layoutsCoreAddon.translateArtifactPath(descriptor.getRepoLayout(), descriptor.getRemoteRepoLayout(),
                path);
    }

    @Override
    protected void putOffline() {
        long assumedOfflinePeriodSecs = getDescriptor().getAssumedOfflinePeriodSecs();
        if (assumedOfflinePeriodSecs <= 0) {
            return;
        }
        // schedule the offline thread to run immediately
        //scheduler.schedule(new OfflineCheckCallable(), 0, TimeUnit.MILLISECONDS);
        synchronized (onlineMonitorSync) {
            if (onlineMonitorThread != null) {
                return;
            }
            onlineMonitorThread = new Thread(new OnlineMonitorRunnable(), "online-monitor-" + getKey());
            onlineMonitorThread.setDaemon(true);
            log.trace("Online monitor starting {}", onlineMonitorThread.getName());
            onlineMonitorThread.start();
        }
    }

    @Override
    public void resetAssumedOffline() {
        synchronized (onlineMonitorSync) {
            stopOfflineCheckThread();
            assumedOffline = false;
        }
    }

    private void stopOfflineCheckThread() {
        log.trace("Online monitor stop on {}", getKey());
        synchronized (onlineMonitorSync) {
            if (onlineMonitorThread != null) {
                log.trace("Online monitor stopping {}", onlineMonitorThread.getName());
                onlineMonitorThread.interrupt();
                onlineMonitorThread = null;
            }
        }
    }

    private HttpClient getHttpClient() {
        if (client == null) {
            throw new IllegalStateException("Repo is offline. Cannot use the HTTP client.");
        }
        return client;
    }

    private class OnlineMonitorRunnable implements Runnable {
        /**
         * max attempts until reaching the maximum wait time
         */
        private static final int MAX_FAILED_ATTEMPTS = 10;

        /**
         * Failed requests counter
         */
        private int failedAttempts = 0;

        @Override
        public void run() {
            log.debug("Online monitor started for {}", getKey());
            while (true) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    if (checkOnline()) {
                        assumedOffline = false;
                        return;
                    }

                    long nextOnlineCheckDelay = calculateNextOnlineCheckDelay();
                    assumedOffline = true;
                    nextOnlineCheckMillis = System.currentTimeMillis() + nextOnlineCheckDelay;
                    log.debug("Online monitor sleeping for {} millis", nextOnlineCheckDelay);
                    Thread.sleep(nextOnlineCheckDelay);
                } catch (InterruptedException e) {
                    log.debug("Online monitor interrupted");
                    Thread.interrupted();
                    return;
                }
            }
        }

        private long calculateNextOnlineCheckDelay() {
            long maxFailureCacheSecs = getDescriptor().getAssumedOfflinePeriodSecs();
            long maxFailureCacheMillis = TimeUnit.SECONDS.toMillis(maxFailureCacheSecs);    // always >= 1000

            long nextOnlineCheckDelayMillis;
            failedAttempts++;
            if (failedAttempts < MAX_FAILED_ATTEMPTS) {
                if (maxFailureCacheSecs / MAX_FAILED_ATTEMPTS < 2) {
                    long failurePenaltyMillis = maxFailureCacheMillis / MAX_FAILED_ATTEMPTS;
                    nextOnlineCheckDelayMillis = failedAttempts * failurePenaltyMillis;
                } else {
                    // exponential delay
                    // calculate the base of the exponential equation based on the MAX_FAILED_ATTEMPTS and max offline period
                    // BASE pow MAX_FAILED_ATTEMPTS = MAX_DELAY ==> BASE = MAX_DELAY pow 1/MAX_FAILED_ATTEMPTS
                    double base = Math.pow(maxFailureCacheMillis, 1.0 / (double) MAX_FAILED_ATTEMPTS);
                    nextOnlineCheckDelayMillis = (long) Math.pow(base, failedAttempts);
                    // in any case don't attempt too rapidly
                    nextOnlineCheckDelayMillis = Math.max(100, nextOnlineCheckDelayMillis);
                }
            } else {
                nextOnlineCheckDelayMillis = maxFailureCacheMillis;
            }
            return nextOnlineCheckDelayMillis;
        }

        private boolean checkOnline() {
            // always test with url trailing slash
            String url = PathUtils.addTrailingSlash(getDescriptor().getUrl());
            GetMethod getMethod = new GetMethod(HttpUtils.encodeQuery(url));
            try {
                log.debug("Online monitor checking URL: {}", url);
                int status = getHttpClient().executeMethod(getMethod);
                String statusText = getMethod.getStatusText();
                log.debug("Online monitor http method completed with no exception: {}: {}", status, statusText);
                // consider putting offline if status > 500 && status < 600
                // no exception - consider back online
                return true;
            } catch (IOException e) {
                log.debug("Online monitor http method failed with exception: {}", e.getMessage());
            } finally {
                getMethod.releaseConnection();
            }
            return false;
        }

    }
}
