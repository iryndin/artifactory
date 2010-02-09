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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.RepoResourceInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.search.InternalSearchService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class RemoteRepoBase<T extends RemoteRepoDescriptor> extends RealRepoBase<T> implements RemoteRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(RemoteRepoBase.class);

    private LocalCacheRepo localCacheRepo;
    private Map<String, RepoResource> failedRetrievalsCache;
    private Map<String, RepoResource> missedRetrievalsCache;
    private boolean globalOfflineMode;

    protected RemoteRepoBase(InternalRepositoryService repositoryService, T descriptor, boolean globalOfflineMode) {
        super(repositoryService, descriptor);
        this.globalOfflineMode = globalOfflineMode;
    }

    public void init() {
        if (isStoreArtifactsLocally()) {
            //Initialize the local cache
            localCacheRepo = new JcrCacheRepo(this);
            localCacheRepo.init();
        }
        initCaches();
        logCacheInfo();
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
        final String path = context.getResourcePath();
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
            //Return the cahced resource if remote fetch failed
            return returnCachedResource(repoPath, cachedResource);
        } else {
            String msg = this + ": is offline, " + repoPath + " is not found at '" + path + "'.";
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
                //Create the parent folder eagerly so that we don't have to do it as part of the retrieval
                String parentPath = PathUtils.getParent(path);
                RepoPath parentRepoPath = new RepoPath(localCacheRepo.getKey(), parentPath);
                LockingHelper.releaseReadLock(parentRepoPath);
                JcrFolder parentFolder = localCacheRepo.getLockedJcrFolder(parentRepoPath, true);
                parentFolder.mkdirs();
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

    public ResourceStreamHandle getResourceStreamHandle(RepoResource res) throws IOException {
        String path = res.getRepoPath().getPath();
        if (isStoreArtifactsLocally()) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Retrieving info from cache for '" + path + "' from '" + localCacheRepo + "'.");
                }
                //Reflect the fact that we return a locally cached resource
                res.setResponseRepoPath(new RepoPath(localCacheRepo.getKey(), path));
                ResourceStreamHandle handle = getRepositoryService().downloadAndSave(this, res);
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

    public ResourceStreamHandle downloadAndSave(RepoResource res, RepoResource targetResource) throws IOException {
        String path = res.getRepoPath().getPath();
        RepoResourceInfo info = res.getInfo();
        RepoResourceInfo targetInfo = targetResource.getInfo();
        //Retrieve remotely only if locally cached artifact is older than remote one
        if (!isOffline() && (!targetResource.isFound() || info.getLastModified() > targetInfo.getLastModified())) {
            // Check for security deploy rights
            StatusHolder status = getRepositoryService().assertValidDeployPath(localCacheRepo, path);
            if (status.isError()) {
                throw new IOException(status.getStatusMsg());
            }
            ResourceStreamHandle handle = null;
            try {
                beforeResourceDownload(res);
                //Do the actual download
                handle = downloadResource(path);
                Set<ChecksumInfo> remoteChecksums = getRemoteChecksums(path);
                info.setChecksums(remoteChecksums);

                //Create/override the resource in the storage cache
                log.debug("Copying " + path + " from " + this + " to " + localCacheRepo);
                InputStream is = handle.getInputStream();

                log.debug("Saving resource '{}' into cache '{}'.", res, localCacheRepo);
                targetResource = localCacheRepo.saveResource(res, is, null);

                if (MavenNaming.isSnapshotMavenMetadata(path) || !res.isMetadata()) {
                    if (MavenNaming.isSnapshotMavenMetadata(path)) {
                        // unexpire the parent folder
                        localCacheRepo.unexpire(PathUtils.getParent(path));
                    } else {
                        // unexpire the file
                        localCacheRepo.unexpire(path);
                    }
                    // remove it from bad retrieval caches
                    removeFromCaches(path, false);
                }
                afterResourceDownload(res);

                InternalSearchService internalSearchService =
                        InternalContextHelper.get().beanForType(InternalSearchService.class);
                internalSearchService.asyncIndex(new RepoPath(localCacheRepo.getKey(), path));
            } finally {
                if (handle != null) {
                    handle.close();
                }
            }
        }
        return localCacheRepo.getResourceStreamHandle(targetResource);
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
                log.debug("Failed to retrieve remote checksum file {}: {}", path, e.getMessage());
            }
            ChecksumInfo info = new ChecksumInfo(checksumType);
            info.setOriginal(checksum);
            checksums.add(info);
        }
        return checksums;
    }

    public String getChecksum(String path, RepoResource res) throws IOException {
        String value;
        if (isStoreArtifactsLocally()) {
            //We assume the resource is already contained in the repo-cache
            value = localCacheRepo.getChecksum(path, res);
        } else {
            value = getRemoteChecksum(path);

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
        int lastDotIndex = path.lastIndexOf(".");
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
     * Returns the checksum value from the given path of a remote checkum file
     *
     * @param path Path to remote checksum
     * @return Checksum value
     * @throws IOException
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
     * @throws IOException
     */
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
                //We don't simply returns the file content since some checksum files have more
                //characters at the end of the checksum file.
                String checksum = StringUtils.split(line)[0];
                return checksum;
            }
        }

        return "";
    }

    private RepoResource getFailedOrMissedResource(String path) {
        RepoResource res = failedRetrievalsCache.get(path);
        if (res == null) {
            res = missedRetrievalsCache.get(path);
        }
        return res;
    }
}