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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.LayoutsCoreAddon;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.download.AfterRemoteDownloadAction;
import org.artifactory.addon.plugin.download.BeforeRemoteDownloadAction;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.io.RemoteResourceStreamHandle;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.remote.browse.HtmlRepositoryBrowser;
import org.artifactory.repo.remote.browse.HttpExecutor;
import org.artifactory.repo.remote.browse.RemoteItem;
import org.artifactory.repo.remote.browse.RemoteRepositoryBrowser;
import org.artifactory.repo.remote.browse.S3RepositoryBrowser;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.request.RequestTraceLogger;
import org.artifactory.resource.RemoteRepoResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HttpRepo extends RemoteRepoBase<HttpRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(HttpRepo.class);

    private HttpClient client;
    private boolean handleGzipResponse;
    protected RemoteRepositoryBrowser remoteBrowser;
    private LayoutsCoreAddon layoutsCoreAddon;

    @GuardedBy("offlineCheckerSync")
    private Thread onlineMonitorThread;
    private Object onlineMonitorSync = new Object();

    public HttpRepo(InternalRepositoryService repositoryService, HttpRepoDescriptor descriptor,
            boolean globalOfflineMode, RemoteRepo oldRemoteRepo) {
        super(repositoryService, descriptor, globalOfflineMode, oldRemoteRepo);
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
        boolean s3Repository = S3RepositoryBrowser.isS3Repository(getUrl(), client);
        if (s3Repository) {
            log.debug("Repository {} caches S3 repository", getKey());
            remoteBrowser = new S3RepositoryBrowser(clientExec);
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
            RequestTraceLogger.log("Remote resource path was translated (%s) due to repository " +
                    "layout differences", pathForUrl);
        }
        Request request = requestContext.getRequest();
        if (request != null) {
            String alternativeRemoteDownloadUrl =
                    request.getParameter(ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL);
            if (StringUtils.isNotBlank(alternativeRemoteDownloadUrl)) {
                RequestTraceLogger.log("Request contains alternative remote resource path ({}=%s)",
                        ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL, alternativeRemoteDownloadUrl);
                pathForUrl = alternativeRemoteDownloadUrl;
            }
        }

        if (getDescriptor().isSynchronizeProperties()) {
            RequestTraceLogger.log("Appending properties to remote request URL if needed");
            pathForUrl += encodeForRequest(requestContext.getProperties());
        }
        final String fullUrl = appendAndGetUrl(convertRequestPathIfNeeded(pathForUrl));
        RequestTraceLogger.log("Using remote request URL - %s", fullUrl);
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        final PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);

        final RepoPath repoPath = InternalRepoPathFactory.create(getKey(), pathForUrl);
        final Request requestForPlugins = requestContext.getRequest();
        pluginAddon.execPluginActions(BeforeRemoteDownloadAction.class, null, requestForPlugins, repoPath);
        RequestTraceLogger.log("Executing any BeforeRemoteDownload user plugins the may exist");

        final GetMethod method = new GetMethod(fullUrl);
        RequestTraceLogger.log("Executing GET request to %s", fullUrl);
        executeMethod(method);

        int statusCode = method.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            //Not found
            method.releaseConnection();
            RequestTraceLogger.log("Received response status %s - throwing exception", statusCode);
            throw new RemoteRequestException("Unable to find " + fullUrl, statusCode);
        }
        if (statusCode != HttpStatus.SC_OK) {
            String msg = "Error fetching " + fullUrl;
            method.releaseConnection();
            RequestTraceLogger.log("Received response status %s - throwing exception", statusCode);
            throw new RemoteRequestException(msg, statusCode);
        }
        //Found
        log.info("{}: Downloading content from '{}'...", this, fullUrl);
        RequestTraceLogger.log("Downloading content");

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
                            new Object[]{HttpRepo.this, fullUrl,
                                    statusLine != null ? statusLine.getStatusCode() : "unknown",
                                    throwable.getMessage()});
                    RequestTraceLogger.log("Failed to download: %s", throwable.getMessage());
                } else {
                    log.info("{}: Downloaded '{}' with return code: {}.", new Object[]{HttpRepo.this, fullUrl,
                            statusLine != null ? statusLine.getStatusCode() : "unknown"});
                    RequestTraceLogger.log("Downloaded content");
                }
                pluginAddon.execPluginActions(AfterRemoteDownloadAction.class, null, requestForPlugins, repoPath);
                RequestTraceLogger.log("Executing any AfterRemoteDownload user plugins the may exist");
            }
        };
    }

    /**
     * Encodes the given properties ready to attach to an HTTP request
     *
     * @param requestProperties Properties to encode. Can be null
     * @return HTTP request ready property chain
     */
    public static String encodeForRequest(Properties requestProperties) throws UnsupportedEncodingException {
        StringBuilder requestPropertyBuilder = new StringBuilder();
        if (requestProperties != null) {
            for (Map.Entry<String, String> requestPropertyEntry : requestProperties.entries()) {
                requestPropertyBuilder.append(Properties.MATRIX_PARAMS_SEP);

                String key = requestPropertyEntry.getKey();
                boolean isMandatory = false;
                if (key.endsWith(Properties.MANDATORY_SUFFIX)) {
                    key = key.substring(0, key.length() - 1);
                    isMandatory = true;
                }
                requestPropertyBuilder.append(URLEncoder.encode(key, "utf-8"));
                if (isMandatory) {
                    requestPropertyBuilder.append("+");
                }
                String value = requestPropertyEntry.getValue();
                if (StringUtils.isNotBlank(value)) {
                    requestPropertyBuilder.append("=").append(URLEncoder.encode(value, "utf-8"));
                }
            }
        }
        return requestPropertyBuilder.toString();
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
        return client.executeMethod(method);
    }

    @Override
    protected RepoResource retrieveInfo(String path, @Nullable RequestContext context) {
        assert !isOffline() : "Should never be called in offline mode";
        RepoPath repoPath = InternalRepoPathFactory.create(this.getKey(), path);

        String fullUrl = assembleRetrieveInfoUrl(path, context);
        HeadMethod method = new HeadMethod(fullUrl);
        RequestTraceLogger.log("Executing HEAD request to %s", fullUrl);
        try {

            try {
                executeMethod(method);
            } catch (IOException e) {
                RequestTraceLogger.log("Failed to execute HEAD request: %s", e.getMessage());
                StringBuilder messageBuilder = new StringBuilder("Failed retrieving resource from ").append(fullUrl).
                        append(": ");
                if (e instanceof UnknownHostException) {
                    messageBuilder.append("Unknown host - ");
                }
                messageBuilder.append(e.getMessage());
                throw new RuntimeException(messageBuilder.toString(), e);
            }

            return handleGetInfoResponse(repoPath, fullUrl, method);
        } finally {
            //Released the connection back to the connection manager
            method.releaseConnection();
        }
    }

    protected String assembleRetrieveInfoUrl(String path, RequestContext context) {
        String pathForUrl = convertRequestPathIfNeeded(path);
        if (!path.equals(pathForUrl)) {
            RequestTraceLogger.log("Remote resource path was translated (%s) due to repository " +
                    "layout differences", pathForUrl);
        }
        boolean validContext = context != null;
        if (validContext) {
            Request request = context.getRequest();
            if (request != null) {
                String alternativeRemoteDownloadUrl =
                        request.getParameter(ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL);
                if (StringUtils.isNotBlank(alternativeRemoteDownloadUrl)) {
                    RequestTraceLogger.log("Request contains alternative remote resource path ({}=%s)",
                            ArtifactoryRequest.PARAM_ALTERNATIVE_REMOTE_DOWNLOAD_URL, alternativeRemoteDownloadUrl);
                    pathForUrl = alternativeRemoteDownloadUrl;
                }
            }
        }

        String fullUrl = appendAndGetUrl(pathForUrl);
        if (validContext && getDescriptor().isSynchronizeProperties()) {
            RequestTraceLogger.log("Appending properties to remote request URL if needed");
            Properties properties = context.getProperties();
            try {
                fullUrl += encodeForRequest(properties);
            } catch (UnsupportedEncodingException e) {
                RequestTraceLogger.log("Failed to encode remote request properties: %s", e.getMessage());
                throw new RuntimeException("Failed to encode request properties '" + properties + "' for request '" +
                        fullUrl + "'.", e);
            }
        }
        RequestTraceLogger.log("Using remote request URL - %s", fullUrl);
        return fullUrl;
    }

    protected RepoResource handleGetInfoResponse(RepoPath repoPath, String fullUrl, HttpMethodBase method) {
        if (method.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            RequestTraceLogger.log("Received status 404 (message: %s) on remote info request - returning unfound " +
                    "resource", method.getStatusText());
            return new UnfoundRepoResource(repoPath, method.getStatusText());
        }
        //If redirected to a directory - return 404
        if (method.getPath().endsWith("/")) {
            RequestTraceLogger.log("Remote info request was redirected to a directory - returning unfound resource");
            return new UnfoundRepoResource(repoPath, "Expected file response but received a directory response.");
        }
        if (method.getStatusCode() != HttpStatus.SC_OK) {
            RequestTraceLogger.log("Received status {} (message: %s) on remote info request - returning unfound " +
                    "resource", method.getStatusCode(), method.getStatusText());
            // send back unfound resource with 404 status
            return new UnfoundRepoResource(repoPath, method.getStatusText());
        }

        long lastModified = getLastModified(method);
        RequestTraceLogger.log("Found remote resource with last modified time - %s",
                new Date(lastModified).toString());

        long size = getContentLength(method);
        if (size != -1) {
            RequestTraceLogger.log("Found remote resource with content length - %s", size);
        }

        Set<ChecksumInfo> checksums = getChecksums(method);
        if (!checksums.isEmpty()) {
            RequestTraceLogger.log("Found remote resource with checksums - %s", checksums);
        }

        RequestTraceLogger.log("Returning found remote resource info");
        RepoResource res = new RemoteRepoResource(repoPath, lastModified, size, checksums);
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
        //Set the current requester
        method.setRequestHeader(ArtifactoryRequest.ARTIFACTORY_ORIGINATED, HttpUtils.getHostId());
        //For backwards compatibility
        method.setRequestHeader(ArtifactoryRequest.ORIGIN_ARTIFACTORY, HttpUtils.getHostId());
        //Follow redirects
        method.setFollowRedirects(followRedirects);
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

    private static Set<ChecksumInfo> getChecksums(HttpMethodBase method) {
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
            GetMethod getMethod = new GetMethod(url);
            try {
                log.debug("Online monitor checking URL: {}", url);
                int status = client.executeMethod(getMethod);
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
