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

package org.artifactory.repo;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.RepoResourceInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoRejectionException;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.concurrent.ExpiringDelayed;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
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

    private void initCaches() {
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        failedRetrievalsCache = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.failed);
        missedRetrievalsCache = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.missed);
    }

    private void logCacheInfo() {
        long retrievalCachePeriodSecs = getDescriptor().getRetrievalCachePeriodSecs();
        if (retrievalCachePeriodSecs > 0) {
            log.debug("{}: Retrieval cache will be enabled with period of {} seconds",
                    this, retrievalCachePeriodSecs);
        } else {
            log.debug("{}: Retrieval cache will be disbaled.", this);
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

    public RepoType getType() {
        return getDescriptor().getType();
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
        RepoPath repoPath = new RepoPath(getKey(), path);

        //Skip if in blackout or not accepting/handling or cannot download
        StatusHolder statusHolder = checkDownloadIsAllowed(repoPath);
        if (statusHolder.isError()) {
            return new UnfoundRepoResource(repoPath, statusHolder.getStatusMsg());
        }
        // TODO: Do a checksum security check on download
        //Never query remote checksums
        if (NamingUtils.isChecksum(path)) {
            return new UnfoundRepoResource(repoPath, "Checksums are not downloaded");
        }
        //Try to get it from the caches
        RepoResource res = getFailedOrMissedResource(path);
        if (res == null) {
            res = internalGetInfo(repoPath);
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

    private RepoResource internalGetInfo(RepoPath repoPath) {
        String path = repoPath.getPath();
        RepoResource cachedResource = null;
        // first try to get it from the local cache repository
        if (isStoreArtifactsLocally() && localCacheRepo != null) {
            try {
                cachedResource = localCacheRepo.getInfo(new NullRequestContext(path));
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
        // not found in local cache - try to get it from the remote repository
        if (!isOffline()) {
            remoteResource = getRemoteResource(path, repoPath, foundExpiredInCache);
            if ((!remoteResource.isFound()) && foundExpiredInCache) {
                remoteResource = returnCachedResource(repoPath, cachedResource);
            }
            return remoteResource;
        } else if (foundExpiredInCache) {
            //Return the cached resource if remote fetch failed
            return returnCachedResource(repoPath, cachedResource);
        } else {
            String msg = this + ": is offline, '" + repoPath + "' is not found at '" + path + "'.";
            log.debug(msg);
            return new UnfoundRepoResource(repoPath, msg);
        }
    }

    /**
     * Returns a resource from a remote repository
     *
     * @param path                Item path
     * @param repoPath            Item repo path
     * @param foundExpiredInCache True if the an expired item was found in the cache
     * @return Repo resource object
     */
    private RepoResource getRemoteResource(String path, RepoPath repoPath, boolean foundExpiredInCache) {
        RepoResource remoteResource;
        try {
            remoteResource = retrieveInfo(path);
            if (remoteResource.isFound()) {
                if (isStoreArtifactsLocally()) {
                    //Create the parent folder eagerly so that we don't have to do it as part of the retrieval
                    String parentPath = PathUtils.getParent(path);
                    RepoPath parentRepoPath = new RepoPath(localCacheRepo.getKey(), parentPath);
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
        cachedResource.setResponseRepoPath(new RepoPath(localCacheRepo.getKey(), repoPath.getPath()));
        return cachedResource;
    }

    protected abstract RepoResource retrieveInfo(String path);

    public StatusHolder checkDownloadIsAllowed(RepoPath repoPath) {
        StatusHolder status = assertValidPath(repoPath);
        if (status.isError()) {
            return status;
        }
        if (localCacheRepo != null) {
            repoPath = new RepoPath(localCacheRepo.getKey(), repoPath.getPath());
            status = localCacheRepo.checkDownloadIsAllowed(repoPath);
        }
        return status;
    }

    public ResourceStreamHandle getResourceStreamHandle(RepoResource res) throws IOException, RepositoryException,
            RepoRejectionException {
        String path = res.getRepoPath().getPath();
        if (isStoreArtifactsLocally()) {
            try {
                //Reflect the fact that we return a locally cached resource
                res.setResponseRepoPath(new RepoPath(localCacheRepo.getKey(), path));
                ResourceStreamHandle handle = getRepositoryService().downloadAndSave(this, res);
                log.debug("Retrieving info from cache for '{}' from '{}'.", path, localCacheRepo);
                return handle;
            } catch (IOException e) {
                //If we fail on remote fetching and we can get the resource from an expired entry in
                //the local cache - fallback to using it, else rethrow the exception
                if (res.isExpired()) {
                    ResourceStreamHandle result =
                            getRepositoryService().unexpireAndRetrieveIfExists(localCacheRepo, path);
                    if (result != null) {
                        return result;
                    }
                }
                throw e;
            }
        } else {
            ResourceStreamHandle handle = downloadResource(path);
            return handle;
        }
    }

    public ResourceStreamHandle downloadAndSave(RepoResource remoteResource, RepoResource cachedResource)
            throws IOException, RepositoryException, RepoRejectionException {
        RepoPath remoteRepoPath = remoteResource.getRepoPath();
        String relativePath = remoteRepoPath.getPath();
        //Retrieve remotely only if locally cached artifact not found or is found but expired and is older than remote one
        if (!isOffline() && (foundExpiredAndRemoteIsNewer(remoteResource, cachedResource)
                || notFoundAndNotExpired(cachedResource))) {
            // Check for security deploy rights
            getRepositoryService().assertValidDeployPath(localCacheRepo, relativePath);
            //Check that the resource is not being downloaded in parallel
            DownloadEntry completedConcurrentDownload = getCompletedConcurrentDownload(relativePath);
            log.trace("Got completed concurrent download: {}.", completedConcurrentDownload);
            if (completedConcurrentDownload == null) {
                //We need to download since no concurrent download of the same resource took place
                log.debug("Starting download of '{}' in '{}'.", relativePath, this);
                ResourceStreamHandle handle = null;
                try {
                    beforeResourceDownload(remoteResource);
                    //Do the actual download
                    handle = downloadResource(relativePath);
                    Set<ChecksumInfo> remoteChecksums = getRemoteChecksums(relativePath);
                    RepoResourceInfo remoteInfo = remoteResource.getInfo();
                    remoteInfo.setChecksums(remoteChecksums);

                    Properties properties = null;
                    if (getDescriptor().isSynchronizeProperties() &&
                            getAuthorizationService().canAnnotate(remoteRepoPath)) {
                        properties = getRemoteProperties(relativePath);
                    }

                    //Create/override the resource in the storage cache
                    log.debug("Copying " + relativePath + " from " + this + " to " + localCacheRepo);
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
                    notifyConcurrentWaiters(cachedResource, relativePath);
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
        return localCacheRepo.getResourceStreamHandle(cachedResource);
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

    private void notifyConcurrentWaiters(RepoResource resource, String relPath) throws IOException, RepositoryException,
            RepoRejectionException {
        DownloadEntry currentDownload = inTransit.remove(relPath);
        if (currentDownload != null) {
            //Put it low enough in case it is incremented by multiple late waiters
            int handlesCount = currentDownload.handlesToPrepare.getAndSet(-9999);
            log.debug("Finished concurrent download of '{}' in '{}'. Preparing {} download handles for " +
                    "waiters.", new Object[]{relPath, this, handlesCount});
            //Add a new handle entries since the new resource is visible to this tx only
            for (int i = 0; i < handlesCount; i++) {
                ResourceStreamHandle extHandle = localCacheRepo.getResourceStreamHandle(resource);
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

    public void clearCaches() {
        if (failedRetrievalsCache != null) {
            failedRetrievalsCache.clear();
        }
        if (missedRetrievalsCache != null) {
            missedRetrievalsCache.clear();
        }
    }

    public void removeFromCaches(String path, boolean removeSubPaths) {
        if (failedRetrievalsCache != null && !failedRetrievalsCache.isEmpty()) {
            failedRetrievalsCache.remove(path);
            if (removeSubPaths) {
                removeSubPathsFromCache(path, failedRetrievalsCache);
            }
        }
        if (missedRetrievalsCache != null && !missedRetrievalsCache.isEmpty()) {
            missedRetrievalsCache.remove(path);
            if (removeSubPaths) {
                removeSubPathsFromCache(path, missedRetrievalsCache);
            }
        }
    }

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

        RepoPath eagerRepoPath = new RepoPath(getDescriptor().getKey(), eagerPath);
        // create a task to download the resource
        QuartzTask eagerFetchingTask = new QuartzTask(EagerResourcesFetchingJob.class, 0);
        // pass the repo path to download eagerly
        eagerFetchingTask.addAttribute(EagerResourcesFetchingJob.PARAM_REPO_PATH, eagerRepoPath);
        TaskService taskService = InternalContextHelper.get().beanForType(TaskService.class);
        taskService.startTask(eagerFetchingTask);
    }

    protected void afterResourceDownload(RepoResource resource) {
        String path = resource.getRepoPath().getPath();
        InternalSearchService internalSearchService =
                InternalContextHelper.get().beanForType(InternalSearchService.class);
        internalSearchService.asyncIndex(new RepoPath(localCacheRepo.getKey(), path));
    }

    private void removeSubPathsFromCache(String basePath, Map<String, RepoResource> cache) {
        Iterator<String> cachedPaths = cache.keySet().iterator();
        while (cachedPaths.hasNext()) {
            String key = cachedPaths.next();
            if (key.startsWith(basePath)) {
                cachedPaths.remove();
            }
        }
    }

    /**
     * Returns the checksum value from the given path of a remote checksum file
     *
     * @param path Path to remote checksum
     * @return Checksum value from the remote source
     * @throws IOException If remote checksum is not found or there was a problem retrieving it
     */
    private String getRemoteChecksum(String path) throws IOException {
        ResourceStreamHandle handle = downloadResource(path);
        try {
            InputStream is = handle.getInputStream();
            return readAndFormatChecksum(is);
        } finally {
            handle.close();
        }
    }

    /**
     * Reads and formats the checksum value from the given stream of a checksum file
     *
     * @param inputStream Input stream of checksum file
     * @return Extracted checksum value
     * @throws IOException If failed to read from the input stream
     */
    @SuppressWarnings({"unchecked"})
    private String readAndFormatChecksum(InputStream inputStream) throws IOException {
        List<String> lineList = IOUtils.readLines(inputStream, "utf-8");
        for (String line : lineList) {
            //Make sure the line isn't blank or commented out
            if (StringUtils.isNotBlank(line) && !line.startsWith("//")) {
                //Remove whitespaces at the end
                line = line.trim();
                //Check for 'MD5 (name) = CHECKSUM'
                int prefixPos = line.indexOf(")= ");
                if (prefixPos != -1) {
                    line = line.substring(prefixPos + 3);
                }
                //We don't simply return the file content since some checksum files have more
                //characters at the end of the checksum file.
                String checksum = StringUtils.split(line)[0];
                return checksum;
            }
        }
        return "";
    }

    /**
     * Returns the remote properties of the given path
     *
     * @param relPath Relative path of artifactp propeties to synchronize
     * @return Properties if found in remote. Empty if not
     */
    private Properties getRemoteProperties(String relPath) {
        Properties properties = new Properties();
        ResourceStreamHandle handle = null;
        InputStream is = null;
        try {
            handle = downloadResource(relPath + ":" + Properties.ROOT);
            is = handle.getInputStream();
            if (is != null) {
                properties = (Properties) XStreamFactory.create(Properties.class).fromXML(is);
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
            LocalCacheRepo cache = getLocalCacheRepo();
            RepoResource cachedPropertiesResource = cache.getInfo(new NullRequestContext(propertiesRelativePath));

            Properties properties = new Properties();

            //Send HEAD
            RepoResource remoteResource = retrieveInfo(propertiesRelativePath);
            if (remoteResource.isFound()) {
                if (cachedPropertiesResource.isFound() &&
                        (cachedPropertiesResource.getLastModified() > remoteResource.getLastModified())) {
                    // remote properties are not newer
                    return;
                }

                ResourceStreamHandle resourceStreamHandle = downloadResource(propertiesRelativePath);
                InputStream inputStream = null;
                try {
                    inputStream = resourceStreamHandle.getInputStream();
                    properties = (Properties) XStreamFactory.create(Properties.class).fromXML(inputStream);
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            }

            RepoPath localCacheRepoPath = new RepoPath(cache.getKey(), artifactRelativePath);
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
}
