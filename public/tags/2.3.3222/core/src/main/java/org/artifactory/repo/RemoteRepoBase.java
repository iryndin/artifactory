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

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.download.AltRemoteContentAction;
import org.artifactory.addon.plugin.download.AltRemotePathAction;
import org.artifactory.addon.plugin.download.PathCtx;
import org.artifactory.addon.plugin.download.ResourceStreamCtx;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.PropertiesImpl;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.StatusHolder;
import org.artifactory.concurrent.ExpiringDelayed;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.remote.browse.RemoteItem;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.RepoResourceInfo;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.CollectionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yoavl
 */
public abstract class RemoteRepoBase<T extends RemoteRepoDescriptor> extends RealRepoBase<T> implements RemoteRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(RemoteRepoBase.class);

    private LocalCacheRepo localCacheRepo;
    private RemoteRepoBase oldRemoteRepo;
    private Map<String, RepoResource> failedRetrievalsCache;
    private Map<String, RepoResource> missedRetrievalsCache;
    private Map<String, List<RemoteItem>> remoteResourceCache;

    private boolean globalOfflineMode;
    private final HandleRefsTracker handleRefsTracker;
    private final ConcurrentMap<String, DownloadEntry> inTransit;

    protected RemoteRepoBase(InternalRepositoryService repositoryService, T descriptor, boolean globalOfflineMode,
            RemoteRepo oldRemoteRepo) {
        super(repositoryService, descriptor);
        this.globalOfflineMode = globalOfflineMode;
        if (oldRemoteRepo instanceof RemoteRepoBase) {
            this.oldRemoteRepo = (RemoteRepoBase) oldRemoteRepo;
            // Always keep the in transit download map
            this.inTransit = this.oldRemoteRepo.inTransit;
            this.handleRefsTracker = this.oldRemoteRepo.handleRefsTracker;
        } else {
            this.oldRemoteRepo = null;
            this.inTransit = new ConcurrentHashMap<String, DownloadEntry>();
            this.handleRefsTracker = new HandleRefsTracker();
        }
    }

    public void init() {
        if (isStoreArtifactsLocally()) {
            LocalCacheRepo oldCacheRepo = null;
            if (oldRemoteRepo != null) {
                oldCacheRepo = oldRemoteRepo.localCacheRepo;
            }
            //Initialize the local cache
            localCacheRepo = new JcrCacheRepo(this, oldCacheRepo);
            localCacheRepo.init();
        }
        initCaches();
        logCacheInfo();
        // Clean the old repo not needed anymore
        oldRemoteRepo = null;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (isStoreArtifactsLocally()) {
            localCacheRepo.destroy();
        }
    }

    protected void initCaches() {
        failedRetrievalsCache = initCache(100, getDescriptor().getFailedRetrievalCachePeriodSecs(), false);
        missedRetrievalsCache = initCache(500, getDescriptor().getMissedRetrievalCachePeriodSecs(), false);
        remoteResourceCache = initCache(1000, getDescriptor().getRetrievalCachePeriodSecs(), true);
    }

    private <V> Map<String, V> initCache(int initialCapacity, long expirationSeconds, boolean softValues) {
        MapMaker mapMaker = new MapMaker().initialCapacity(initialCapacity);
        if (expirationSeconds >= 0) {
            mapMaker.expireAfterWrite(expirationSeconds, TimeUnit.SECONDS);
        }
        if (softValues) {
            mapMaker.softValues();
        }

        return mapMaker.makeMap();
    }

    private void logCacheInfo() {
        long retrievalCachePeriodSecs = getDescriptor().getRetrievalCachePeriodSecs();
        if (retrievalCachePeriodSecs > 0) {
            log.debug("{}: Retrieval cache will be enabled with period of {} seconds",
                    this, retrievalCachePeriodSecs);
        } else {
            log.debug("{}: Retrieval cache will be disabled.", this);
        }
        long failedRetrievalCachePeriodSecs = getDescriptor().getFailedRetrievalCachePeriodSecs();
        if (failedRetrievalCachePeriodSecs > 0) {
            log.debug("{}: Enabling failed retrieval cache with period of {} seconds",
                    this, failedRetrievalCachePeriodSecs);
        } else {
            log.debug("{}: Disabling failed retrieval cache", this);
        }
        long missedRetrievalCachePeriodSecs = getDescriptor().getMissedRetrievalCachePeriodSecs();
        if (missedRetrievalCachePeriodSecs > 0) {
            log.debug("{}: Enabling misses retrieval cache with period of {} seconds",
                    this, missedRetrievalCachePeriodSecs);
        } else {
            log.debug("{}: Disabling misses retrieval cache", this);
        }
    }

    public boolean isStoreArtifactsLocally() {
        return getDescriptor().isStoreArtifactsLocally();
    }

    public String getUrl() {
        return getDescriptor().getUrl();
    }

    public boolean isCache() {
        return false;
    }

    public boolean isHardFail() {
        return getDescriptor().isHardFail();
    }

    public boolean isOffline() {
        return getDescriptor().isOffline() || globalOfflineMode;
    }

    public boolean isListRemoteFolderItems() {
        return getDescriptor().isListRemoteFolderItems() && !getDescriptor().isBlackedOut() && !isOffline();
    }

    public long getRetrievalCachePeriodSecs() {
        return getDescriptor().getRetrievalCachePeriodSecs();
    }

    public long getFailedRetrievalCachePeriodSecs() {
        return getDescriptor().getFailedRetrievalCachePeriodSecs();
    }

    public long getMissedRetrievalCachePeriodSecs() {
        return getDescriptor().getMissedRetrievalCachePeriodSecs();
    }

    /**
     * Retrieve the (metadata) information about the artifact, unless still cached as failure or miss. Reach this point
     * only if local and cached repo did not find resource or expired.
     *
     * @param context The request context holding additional parameters
     * @return A repository resource updated with the uptodate metadata
     */
    public final RepoResource getInfo(RequestContext context) throws FileExpectedException {
        // make sure the repo key is of this repository
        String path = context.getResourcePath();

        RepoPath repoPath = new RepoPathImpl(getKey(), path);

        //Skip if in blackout or not accepting/handling or cannot download
        StatusHolder statusHolder = checkDownloadIsAllowed(repoPath);
        if (statusHolder.isError()) {
            return new UnfoundRepoResource(repoPath, statusHolder.getStatusMsg(), statusHolder.getStatusCode());
        }
        //Never query remote checksums
        if (NamingUtils.isChecksum(path)) {
            return new UnfoundRepoResource(repoPath, "Checksums are not downloaded");
        }
        //Try to get it from the caches
        RepoResource res = getFailedOrMissedResource(path);
        if (res == null) {
            res = internalGetInfo(repoPath, context);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(this + ": " + res + " cached as not found at '" + path + "'.");
            }
        }

        //If we cannot get the resource remotely and an expired (otherwise we would not be
        //attempting the remote repo at all) cache entry exists use it by unexpiring it
        if (res.isExpired() && isStoreArtifactsLocally()) {
            res = getRepositoryService().unexpireIfExists(localCacheRepo, path);
        }
        return res;
    }

    private RepoResource internalGetInfo(RepoPath repoPath, RequestContext context) {
        String path = repoPath.getPath();
        RepoResource cachedResource = null;
        // first try to get it from the local cache repository
        if (isStoreArtifactsLocally() && localCacheRepo != null) {
            try {
                cachedResource = localCacheRepo.getInfo(context);
            } catch (FileExpectedException e) {
                // rethrow using the remote repo path
                throw new FileExpectedException(repoPath);
            }
        }

        if (cachedResource != null && cachedResource.isFound()) {
            // found in local cache
            return returnCachedResource(repoPath, cachedResource);
        }

        RepoResource remoteResource;
        boolean foundExpiredInCache = ((cachedResource != null) && cachedResource.isExpired());

        //not found in local cache - try to get it from the remote repository
        if (!isOffline()) {
            remoteResource = getRemoteResource(context, repoPath, foundExpiredInCache);
            if (!remoteResource.isFound() && foundExpiredInCache) {
                remoteResource = returnCachedResource(repoPath, cachedResource);
            }
            return remoteResource;
        } else if (foundExpiredInCache) {
            //Return the cached resource if remote fetch failed
            return returnCachedResource(repoPath, cachedResource);
        } else {
            String msg = String.format("%s: is offline, '%s' is not found at '%s'.", this, repoPath, path);
            log.debug(msg);
            return new UnfoundRepoResource(repoPath, msg);
        }
    }

    /**
     * Returns a resource from a remote repository
     *
     * @param context             Download request context
     * @param repoPath            Item repo path
     * @param foundExpiredInCache True if the an expired item was found in the cache    @return Repo resource object
     */
    private RepoResource getRemoteResource(RequestContext context, RepoPath repoPath,
            boolean foundExpiredInCache) {
        String path = repoPath.getPath();

        if (!getDescriptor().isSynchronizeProperties() && context.getProperties().hasMandatoryProperty()) {
            String msg = this + ": does not synchronize remote properties and request contains mandatory property, '"
                    + repoPath + "' will not be downloaded from '" + path + "'.";
            log.debug(msg);
            return new UnfoundRepoResource(repoPath, msg);
        }

        RepoResource remoteResource;
        path = getAltRemotePath(repoPath);
        try {
            remoteResource = retrieveInfo(path, context.getProperties());
            if (remoteResource.isFound()) {
                if (isStoreArtifactsLocally()) {
                    //Create the parent folder eagerly so that we don't have to do it as part of the retrieval
                    String parentPath = PathUtils.getParent(path);
                    RepoPath parentRepoPath = new RepoPathImpl(localCacheRepo.getKey(), parentPath);
                    // Write lock auto upgrade supported LockingHelper.releaseReadLock(parentRepoPath);
                    JcrFolder parentFolder = localCacheRepo.getLockedJcrFolder(parentRepoPath, true);
                    parentFolder.mkdirs();
                }
            } else {
                if (!foundExpiredInCache) {
                    //Update the non-found cache for a miss
                    if (log.isDebugEnabled()) {
                        log.debug(this + ": " + remoteResource + " not found at '" + path + "'.");
                    }
                    missedRetrievalsCache.put(path, remoteResource);
                }
            }
        } catch (Exception e) {
            String reason = this + ": Error in getting information for '" + path + "' (" + e.getMessage() + ").";
            if (log.isDebugEnabled()) {
                log.warn(reason, e);
            } else {
                log.warn(reason);
            }
            remoteResource = new UnfoundRepoResource(repoPath, reason);
            if (!foundExpiredInCache) {
                //Update the non-found cache for a failure
                failedRetrievalsCache.put(path, remoteResource);
                if (isHardFail()) {
                    throw new RuntimeException(this + ": Error in getting information for '" + path + "'.", e);
                }
            }
        }

        return remoteResource;
    }

    /**
     * Sets the response repo path on a cached resource
     *
     * @param repoPath       Path item to resource
     * @param cachedResource Cached resource
     * @return Repo resource object
     */
    private RepoResource returnCachedResource(RepoPath repoPath, RepoResource cachedResource) {
        cachedResource.setResponseRepoPath(new RepoPathImpl(localCacheRepo.getKey(), repoPath.getPath()));
        return cachedResource;
    }

    protected abstract RepoResource retrieveInfo(String path, Properties requestProperties);

    public StatusHolder checkDownloadIsAllowed(RepoPath repoPath) {
        String path = repoPath.getPath();
        StatusHolder status = assertValidPath(path);
        if (status.isError()) {
            return status;
        }
        if (localCacheRepo != null) {
            repoPath = new RepoPathImpl(localCacheRepo.getKey(), path);
            status = localCacheRepo.checkDownloadIsAllowed(repoPath);
        }
        return status;
    }

    public ResourceStreamHandle getResourceStreamHandle(RequestContext requestContext, RepoResource res)
            throws IOException, RepositoryException,
            RepoRejectException {
        String path = res.getRepoPath().getPath();
        if (isStoreArtifactsLocally()) {
            try {
                //Reflect the fact that we return a locally cached resource
                res.setResponseRepoPath(new RepoPathImpl(localCacheRepo.getKey(), path));
                ResourceStreamHandle handle = getRepositoryService().downloadAndSave(requestContext, this, res);
                log.debug("Retrieving info from cache for '{}' from '{}'.", path, localCacheRepo);
                return handle;
            } catch (IOException e) {
                //If we fail on remote fetching and we can get the resource from an expired entry in
                //the local cache - fallback to using it, else rethrow the exception
                if (res.isExpired()) {
                    ResourceStreamHandle result =
                            getRepositoryService().unexpireAndRetrieveIfExists(requestContext, localCacheRepo, path);
                    if (result != null) {
                        return result;
                    }
                }
                throw e;
            }
        } else {
            ResourceStreamHandle handle = downloadResource(path, requestContext.getProperties());
            return handle;
        }
    }

    public ResourceStreamHandle downloadAndSave(RequestContext context, RepoResource remoteResource,
            RepoResource cachedResource) throws IOException, RepositoryException, RepoRejectException {
        RepoPath remoteRepoPath = remoteResource.getRepoPath();
        String path = remoteRepoPath.getPath();

        //Retrieve remotely only if locally cached artifact not found or is found but expired and is older than remote one
        if (!isOffline() && (foundExpiredAndRemoteIsNewer(remoteResource, cachedResource)
                || notFoundAndNotExpired(cachedResource))) {
            // Check for security deploy rights
            getRepositoryService().assertValidDeployPath(localCacheRepo, path);
            //Check that the resource is not being downloaded in parallel
            DownloadEntry completedConcurrentDownload = getCompletedConcurrentDownload(path);
            log.trace("Got completed concurrent download: {}.", completedConcurrentDownload);
            if (completedConcurrentDownload == null) {
                //We need to download since no concurrent download of the same resource took place
                log.debug("Starting download of '{}' in '{}'.", path, this);
                ResourceStreamHandle handle = null;
                try {
                    beforeResourceDownload(remoteResource);

                    RepoResourceInfo remoteInfo = remoteResource.getInfo();
                    Set<ChecksumInfo> remoteChecksums = remoteInfo.getChecksums();
                    boolean receivedRemoteChecksums = (remoteChecksums != null) && !remoteChecksums.isEmpty();

                    //Allow plugins to provide an alternate content
                    handle = getAltContent(remoteRepoPath);

                    if (shouldSearchForExistingResource(context.getRequest()) && (handle == null) &&
                            receivedRemoteChecksums) {
                        handle = getExistingResourceByChecksum(remoteChecksums, remoteResource.getSize());
                    }

                    if (handle == null) {
                        //If we didn't get an alternate handle do the actual download
                        handle = downloadResource(path, context.getProperties());
                    }

                    if (!receivedRemoteChecksums) {
                        remoteChecksums = getRemoteChecksums(path);
                        remoteInfo.setChecksums(remoteChecksums);
                    }

                    Properties properties = null;
                    if (getDescriptor().isSynchronizeProperties() &&
                            getAuthorizationService().canAnnotate(remoteRepoPath)) {
                        properties = getRemoteProperties(path);
                    }

                    //Create/override the resource in the storage cache
                    log.debug("Copying " + path + " from " + this + " to " + localCacheRepo);
                    InputStream is = handle.getInputStream();

                    log.debug("Saving resource '{}' into cache '{}'.", remoteResource, localCacheRepo);
                    cachedResource = localCacheRepo.saveResource(remoteResource, is, properties);
                    unexpire(cachedResource);
                    afterResourceDownload(remoteResource);
                } finally {
                    if (handle != null) {
                        handle.close();
                    }
                    //Notify concurrent download waiters
                    notifyConcurrentWaiters(context, cachedResource, path);
                }
            } else {
                //We will not see the stored result here yet since it is saved in its own tx - return a direct handle
                ConcurrentLinkedQueue<ResourceStreamHandle> preparedHandles = completedConcurrentDownload.handles;
                ResourceStreamHandle handle = preparedHandles.poll();
                if (handle == null) {
                    log.error("No concurrent download handle is available.");
                }
                return handle;
            }
        }

        if (foundExpiredAndRemoteIsNotNewer(remoteResource, cachedResource)) {
            synchronizeExpiredResourceProperties(remoteRepoPath);
            unexpire(cachedResource);
        }

        //Return the cached result (the newly downloaded or already cached resource)
        return localCacheRepo.getResourceStreamHandle(context, cachedResource);
    }

    private boolean shouldSearchForExistingResource(Request request) {
        String searchForExistingResource = request.getParameter(
                ArtifactoryRequest.SEARCH_FOR_EXISTING_RESOURCE_ON_REMOTE_REQUEST);
        if (StringUtils.isNotBlank(searchForExistingResource)) {
            return Boolean.valueOf(searchForExistingResource);
        }

        return ConstantValues.searchForExistingResourceOnRemoteRequest.getBoolean();
    }

    private ResourceStreamHandle getExistingResourceByChecksum(Set<ChecksumInfo> remoteChecksums, long size) {
        for (ChecksumInfo remoteChecksum : remoteChecksums) {
            if (ChecksumType.sha1.equals(remoteChecksum.getType())) {
                String sha1 = remoteChecksum.getOriginal();
                JcrService jcrService = ContextHelper.get().beanForType(JcrService.class);
                InputStream data = null;
                try {
                    data = jcrService.getDataStreamBySha1Checksum(sha1);
                } catch (DataStoreException e) {
                    log.debug("The data record for checksum '{}' could not be found.", sha1);
                }
                if (data != null) {
                    return new SimpleResourceStreamHandle(data, size);
                }
            }
        }

        return null;
    }

    private void unexpire(RepoResource cachedResource) {
        String relativePath = cachedResource.getRepoPath().getPath();
        if (MavenNaming.isSnapshotMavenMetadata(relativePath) || !cachedResource.isMetadata()) {
            if (MavenNaming.isSnapshotMavenMetadata(relativePath)) {
                // unexpire the parent folder
                localCacheRepo.unexpire(PathUtils.getParent(relativePath));
            } else {
                // unexpire the file
                localCacheRepo.unexpire(relativePath);
            }
            // remove it from bad retrieval caches
            removeFromCaches(relativePath, false);
        }
    }

    private boolean foundExpiredAndRemoteIsNewer(RepoResource remoteResource, RepoResource cachedResource) {
        return cachedResource.isExpired() && remoteResource.getLastModified() > cachedResource.getLastModified();
    }

    private boolean foundExpiredAndRemoteIsNotNewer(RepoResource remoteResource, RepoResource cachedResource) {
        return cachedResource.isExpired() && remoteResource.getLastModified() <= cachedResource.getLastModified();
    }

    private boolean notFoundAndNotExpired(RepoResource cachedResource) {
        return !cachedResource.isFound() && !cachedResource.isExpired();
    }

    private DownloadEntry getCompletedConcurrentDownload(String relPath) {
        final DownloadEntry downloadEntry = new DownloadEntry(relPath);
        DownloadEntry currentDownload = inTransit.putIfAbsent(relPath, downloadEntry);
        if (currentDownload != null) {
            //No put since a concurrent download in progress - wait for it
            try {
                log.trace("Current download is '{}'.", currentDownload);
                //Increment the resource handles count
                int prevCount = currentDownload.handlesToPrepare.getAndIncrement();
                if (prevCount < 0) {
                    //Calculation already started
                    log.trace("Not waiting on concurrent download {} since calculation already started.",
                            currentDownload);
                    return null;
                }
                log.info("Waiting on concurrent download of '{}' in '{}'.", relPath, this);
                if (LockingHelper.getSessionLockManager().hasPendingResources()) {
                    log.debug("Session locks exit while waiting on concurrent download.");
                }
                boolean latchTriggered =
                        currentDownload.latch.await(currentDownload.getDelay(TimeUnit.SECONDS), TimeUnit.SECONDS);
                if (!latchTriggered) {
                    //We exited because of a timeout
                    log.info("Timed-out waiting on concurrent download of '{}' in '{}'. Allowing concurrent " +
                            "downloads to proceed.", relPath, this);
                    currentDownload.handlesToPrepare.decrementAndGet();
                    return null;
                } else {
                    return currentDownload;
                }
            } catch (InterruptedException e) {
                log.trace("Interrupted while waiting on a concurrent download.");
                return null;
            }
        } else {
            return null;
        }
    }

    private void notifyConcurrentWaiters(RequestContext requestContext, RepoResource resource, String relPath)
            throws IOException, RepositoryException, RepoRejectException {
        DownloadEntry currentDownload = inTransit.remove(relPath);
        if (currentDownload != null) {
            //Put it low enough in case it is incremented by multiple late waiters
            int handlesCount = currentDownload.handlesToPrepare.getAndSet(-9999);
            log.debug("Finished concurrent download of '{}' in '{}'. Preparing {} download handles for " +
                    "waiters.", new Object[]{relPath, this, handlesCount});
            //Add a new handle entries since the new resource is visible to this tx only
            for (int i = 0; i < handlesCount; i++) {
                ResourceStreamHandle extHandle = localCacheRepo.getResourceStreamHandle(requestContext, resource);
                currentDownload.handles.add(extHandle);
                //if waiters do not pick up the handles prepared (timed out waiting, exception,
                //interrupted...) so we keep track on their references
                handleRefsTracker.add(extHandle);
            }

            //Notify the download waiters
            log.trace("Notifying waiters on: {}.", currentDownload);
            currentDownload.latch.countDown();
        }
    }

    private Set<ChecksumInfo> getRemoteChecksums(String path) {
        Set<ChecksumInfo> checksums = new HashSet<ChecksumInfo>();
        for (ChecksumType checksumType : ChecksumType.values()) {
            String checksum = null;
            try {
                checksum = getRemoteChecksum(path + checksumType.ext());
            } catch (FileNotFoundException e) {
                log.debug("Remote checksum file {} doesn't exist", path);
            } catch (Exception e) {
                log.debug("Could not retrieve remote checksum file {}: {}", path, e.getMessage());
            }
            ChecksumInfo info = new ChecksumInfo(checksumType, null, null);
            if (StringUtils.isNotBlank(checksum)) {
                // set the remote checksum only if it is a valid string for that checksum
                if (checksumType.isValid(checksum)) {
                    info = new ChecksumInfo(checksumType, checksum, null);
                } else {
                    log.debug("Invalid remote checksum for type {}: '{}'", checksumType, checksum);
                }
            }
            checksums.add(info);
        }
        return checksums;
    }

    public String getChecksum(String path, RepoResource res) throws IOException {
        String value = null;
        if (isStoreArtifactsLocally()) {
            //We assume the resource is already contained in the repo-cache
            value = localCacheRepo.getChecksum(path, res);
        } else {
            try {
                value = getRemoteChecksum(path);
            } catch (RemoteRequestException e) {
                // ok to fail with 404, just return null (which translates to not exist in higher levels)
                if (e.getRemoteReturnCode() != HttpStatus.SC_NOT_FOUND) {
                    throw e;
                }
            }
        }
        return value;
    }

    public LocalCacheRepo getLocalCacheRepo() {
        return localCacheRepo;
    }

    public List<RemoteItem> listRemoteResources(String directoryPath) throws IOException {
        assert !isOffline() : "Should never be called in offline mode";
        List<RemoteItem> cachedUrls = remoteResourceCache.get(directoryPath);
        if (CollectionUtils.notNullOrEmpty(cachedUrls)) {
            return cachedUrls;
        }

        checkForRemoteListingInFailedCaches(directoryPath);

        List<RemoteItem> urls;
        String fullDirectoryUrl = appendAndGetUrl(directoryPath);
        try {
            urls = getChildUrls(fullDirectoryUrl);
        } catch (IOException e) {
            addRemoteListingEntryToFailedOrMissedCache(directoryPath, e);
            throw e;
        }

        if (CollectionUtils.isNullOrEmpty(urls)) {
            log.debug("No remote URLS where found for: ", fullDirectoryUrl);
            return Lists.newArrayList();
        }
        remoteResourceCache.put(directoryPath, urls);
        return urls;
    }

    protected String appendAndGetUrl(String pathToAppend) {
        String remoteUrl = getUrl();
        StringBuilder baseUrlBuilder = new StringBuilder(remoteUrl);
        if (!remoteUrl.endsWith("/")) {
            baseUrlBuilder.append("/");
        }
        baseUrlBuilder.append(pathToAppend);
        return baseUrlBuilder.toString();
    }

    private void checkForRemoteListingInFailedCaches(String directoryPath) throws IOException {
        UnfoundRepoResource unfoundRepoResource = null;
        if (failedRetrievalsCache.containsKey(directoryPath)) {
            unfoundRepoResource = ((UnfoundRepoResource) failedRetrievalsCache.get(directoryPath));
        } else if (missedRetrievalsCache.containsKey(directoryPath)) {
            unfoundRepoResource = ((UnfoundRepoResource) missedRetrievalsCache.get(directoryPath));
        }

        if (unfoundRepoResource != null) {
            throw new IOException(unfoundRepoResource.getReason());
        }
    }

    private void addRemoteListingEntryToFailedOrMissedCache(String directoryPath, IOException e) {
        String message = e.getMessage();
        Map<String, RepoResource> relevantCache;
        if (StringUtils.isNotBlank(message) && message.contains("Failed")) {
            relevantCache = failedRetrievalsCache;
        } else {
            relevantCache = missedRetrievalsCache;
        }
        if (!relevantCache.containsKey(directoryPath)) {
            relevantCache.put(directoryPath, new UnfoundRepoResource(getRepoPath(directoryPath), message));
        }
    }

    protected abstract List<RemoteItem> getChildUrls(String dirUrl) throws IOException;

    public void clearCaches() {
        clearCaches(failedRetrievalsCache, missedRetrievalsCache, remoteResourceCache);
    }

    public void removeFromCaches(String path, boolean removeSubPaths) {
        removeFromCaches(path, removeSubPaths, failedRetrievalsCache, missedRetrievalsCache, remoteResourceCache);
    }

    /**
     * Executed before actual download. May return an alternate handle with its own input stream to circumvent download
     *
     * @param resource
     * @return
     */
    protected void beforeResourceDownload(RepoResource resource) {
        if (!getDescriptor().isFetchSourcesEagerly() && !getDescriptor().isFetchJarsEagerly()) {
            // eager fetching is disabled
            return;
        }
        RepoPath repoPath = resource.getRepoPath();
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(repoPath);
        if (!artifactInfo.isValid() || artifactInfo.hasClassifier()) {
            return;
        }

        String path = repoPath.getPath();
        int lastDotIndex = path.lastIndexOf('.');
        String eagerPath;
        if (getDescriptor().isFetchJarsEagerly() && "pom".equals(artifactInfo.getType())) {
            eagerPath = path.substring(0, lastDotIndex) + ".jar";
        } else if (getDescriptor().isFetchSourcesEagerly() && "jar".equals(artifactInfo.getType())) {
            // create a path to the sources
            eagerPath = PathUtils.injectString(path, "-sources", lastDotIndex);
        } else {
            return;
        }
        // pass the repo path to download eagerly
        EagerResourcesDownloader resourcesDownloader =
                InternalContextHelper.get().beanForType(EagerResourcesDownloader.class);
        RepoPath eagerRepoPath = new RepoPathImpl(getDescriptor().getKey(), eagerPath);
        resourcesDownloader.downloadAsync(eagerRepoPath);
    }

    protected void afterResourceDownload(RepoResource resource) {
        String path = resource.getRepoPath().getPath();
        InternalSearchService internalSearchService =
                InternalContextHelper.get().beanForType(InternalSearchService.class);
        internalSearchService.asyncIndex(new RepoPathImpl(localCacheRepo.getKey(), path));
    }

    /**
     * Returns the checksum value from the given path of a remote checksum file
     *
     * @param path Path to remote checksum
     * @return Checksum value from the remote source
     * @throws IOException If remote checksum is not found or there was a problem retrieving it
     */
    private String getRemoteChecksum(String path) throws IOException {
        ResourceStreamHandle handle = downloadResource(path, null);
        try {
            InputStream is = handle.getInputStream();
            return Checksum.checksumStringFromStream(is);
        } finally {
            handle.close();
        }
    }

    /**
     * Returns the remote properties of the given path
     *
     * @param relPath Relative path of artifact propeties to synchronize
     * @return Properties if found in remote. Empty if not
     */
    private Properties getRemoteProperties(String relPath) {
        Properties properties = new PropertiesImpl();
        ResourceStreamHandle handle = null;
        InputStream is = null;
        try {
            handle = downloadResource(relPath + ":" + Properties.ROOT, null);
            is = handle.getInputStream();
            if (is != null) {
                properties = (Properties) XStreamFactory.create(PropertiesImpl.class).fromXML(is);
            }
        } catch (Exception e) {
            properties = null;
            log.debug("Could not retrieve remote properties for file {}: {}", relPath, e.getMessage());
        } finally {
            if (handle != null) {
                handle.close();
            }
            IOUtils.closeQuietly(is);
        }
        return properties;
    }

    /**
     * To be called when retrieving an artifact which was found expired and it's remote was not newer. Synchronizes the
     * properties of the remote artifact with the local cached one
     *
     * @param repoPath Repo path to synchronize
     */
    private void synchronizeExpiredResourceProperties(RepoPath repoPath) {
        if (!getDescriptor().isSynchronizeProperties()) {
            return;
        }
        if (!getAuthorizationService().canAnnotate(repoPath)) {
            return;
        }

        try {
            String artifactRelativePath = repoPath.getPath();
            String propertiesRelativePath = artifactRelativePath + ":" + Properties.ROOT;
            RepoPathImpl propertiesRepoPath = new RepoPathImpl(repoPath.getRepoKey(), propertiesRelativePath);
            String remotePropertiesRelativePath = getAltRemotePath(propertiesRepoPath);

            LocalCacheRepo cache = getLocalCacheRepo();
            RepoResource cachedPropertiesResource = cache.getInfo(new NullRequestContext(propertiesRelativePath));

            Properties properties = new PropertiesImpl();

            //Send HEAD
            RepoResource remoteResource = retrieveInfo(remotePropertiesRelativePath, null);
            if (remoteResource.isFound()) {
                if (cachedPropertiesResource.isFound() &&
                        (cachedPropertiesResource.getLastModified() > remoteResource.getLastModified())) {
                    // remote properties are not newer
                    return;
                }

                ResourceStreamHandle resourceStreamHandle = downloadResource(remotePropertiesRelativePath, null);
                InputStream inputStream = null;
                try {
                    inputStream = resourceStreamHandle.getInputStream();
                    properties = (Properties) XStreamFactory.create(PropertiesImpl.class).fromXML(inputStream);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }

            RepoPath localCacheRepoPath = new RepoPathImpl(cache.getKey(), artifactRelativePath);
            getRepositoryService().setMetadata(localCacheRepoPath, Properties.class, properties);
        } catch (Exception e) {
            String repoPathId = repoPath.getId();
            log.error("Unable to synchronize the properties of the item '{}' with the remote resource: {}",
                    repoPathId, e.getMessage());
            log.debug("Unable to synchronize the properties of the item " + repoPathId +
                    " with the remote resource.", e);
        }
    }

    private RepoResource getFailedOrMissedResource(String path) {
        RepoResource res = failedRetrievalsCache.get(path);
        if (res == null) {
            res = missedRetrievalsCache.get(path);
        }
        return res;
    }

    private static class DownloadEntry extends ExpiringDelayed {

        private final String path;
        private final CountDownLatch latch;
        private final AtomicInteger handlesToPrepare = new AtomicInteger();
        private final ConcurrentLinkedQueue<ResourceStreamHandle> handles =
                new ConcurrentLinkedQueue<ResourceStreamHandle>();

        DownloadEntry(String path) {
            super(System.currentTimeMillis() + (ConstantValues.repoConcurrentDownloadSyncTimeoutSecs.getLong() * 1000));
            this.path = path;
            this.latch = new CountDownLatch(1);
        }

        @Override
        public String getSubject() {
            return path;
        }
    }

    private static class HandleRefsTracker {

        @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
        private ConcurrentLinkedQueue<WeakReference<ResourceStreamHandle>> handles =
                new ConcurrentLinkedQueue<WeakReference<ResourceStreamHandle>>();
        private final ReferenceQueue<ResourceStreamHandle> handlesRefQueue = new ReferenceQueue<ResourceStreamHandle>();

        public void add(ResourceStreamHandle handle) {
            WeakReference<ResourceStreamHandle> ref = new WeakReference<ResourceStreamHandle>(handle, handlesRefQueue);
            handles.add(ref);
            //Clean up weakly referenced handles that were not picked-up by waiters before
            WeakReference<ResourceStreamHandle> defunctRef;
            while ((defunctRef = (WeakReference<ResourceStreamHandle>) handlesRefQueue.poll()) != null) {
                try {
                    log.trace("Cleaning up defunct download handle in '{}'.", this);
                    ResourceStreamHandle defunctHandle = defunctRef.get();
                    if (defunctHandle != null) {
                        defunctHandle.close();
                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Could not cleanup handle reference.", e);
                    } else {
                        log.warn("Could not cleanup handle reference: {}", e.getMessage());
                    }
                }
                handles.remove(defunctRef);
            }
        }
    }

    /**
     * Allow plugins to override the path
     *
     * @param path
     * @return
     */
    private String getAltRemotePath(RepoPath repoPath) {
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
        PathCtx pathCtx = new PathCtx(repoPath.getPath());
        pluginAddon.execPluginActions(AltRemotePathAction.class, pathCtx, repoPath);
        String path = pathCtx.getPath();
        return path;
    }

    /**
     * Allow plugins to override the path
     *
     * @param repoPath
     * @return
     */
    private ResourceStreamHandle getAltContent(RepoPath repoPath) {
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
        ResourceStreamCtx rsCtx = new ResourceStreamCtx();
        pluginAddon.execPluginActions(AltRemoteContentAction.class, rsCtx, repoPath);
        InputStream is = rsCtx.getInputStream();
        if (is != null) {
            return new SimpleResourceStreamHandle(is, rsCtx.getSize());
        }
        return null;
    }

    private void clearCaches(Map<String, ?>... caches) {
        for (Map<String, ?> cache : caches) {
            if (cache != null) {
                cache.clear();
            }
        }
    }

    private void removeFromCaches(String path, boolean removeSubPaths, Map<String, ?>... caches) {
        for (Map<String, ?> cache : caches) {
            if (cache != null && !cache.isEmpty()) {
                cache.remove(path);
                if (removeSubPaths) {
                    removeSubPathsFromCache(path, cache);
                }
            }
        }
    }

    private void removeSubPathsFromCache(String basePath, Map<String, ?> cache) {
        Iterator<String> cachedPaths = cache.keySet().iterator();
        while (cachedPaths.hasNext()) {
            String key = cachedPaths.next();
            if (key.startsWith(basePath)) {
                cachedPaths.remove();
            }
        }
    }
}
