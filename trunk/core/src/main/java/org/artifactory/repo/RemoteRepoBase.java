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

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoType;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class RemoteRepoBase<T extends RemoteRepoDescriptor> extends RealRepoBase<T>
        implements RemoteRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(RemoteRepoBase.class);

    private LocalCacheRepo localCacheRepo;
    private Map<String, RepoResource> failedRetrievalsCache;
    private Map<String, RepoResource> missedRetrievalsCache;
    private boolean globalOfflineMode;

    protected RemoteRepoBase(InternalRepositoryService repositoryService, T descriptor,
            boolean globalOfflineMode) {
        super(repositoryService, descriptor);
        this.globalOfflineMode = globalOfflineMode;
    }

    public void init() {
        if (isStoreArtifactsLocally()) {
            //Initialize the local cache
            localCacheRepo = new LocalCacheRepo(this);
        }
        initCaches();
        logCacheInfo();
    }

    private void logCacheInfo() {
        long retrievalCachePeriodSecs = getDescriptor().getRetrievalCachePeriodSecs();
        if (retrievalCachePeriodSecs > 0) {
            log.info(this + ": Retrieval cache will be enabled with period of "
                    + retrievalCachePeriodSecs + " seconds");
        } else {
            log.info(this + ": Retrieval cache will be disbaled.");
        }
        long failedRetrievalCachePeriodSecs =
                getDescriptor().getFailedRetrievalCachePeriodSecs();
        if (failedRetrievalCachePeriodSecs > 0) {
            log.info(this + ": Enabling failed retrieval cache with period of "
                    + failedRetrievalCachePeriodSecs + " seconds");
        } else {
            log.info(this + ": Disabling failed retrieval cache");
        }
        long missedRetrievalCachePeriodSecs =
                getDescriptor().getMissedRetrievalCachePeriodSecs();
        if (missedRetrievalCachePeriodSecs > 0) {
            log.info(this + ": Enabling misses retrieval cache with period of "
                    + missedRetrievalCachePeriodSecs + " seconds");
        } else {
            log.info(this + ": Disabling misses retrieval cache");
        }
    }

    @SuppressWarnings({"unchecked"})
    private void initCaches() {
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        failedRetrievalsCache = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.failed);
        missedRetrievalsCache = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.missed);
    }

    public RemoteRepoType getType() {
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
     * Retrieve the (metadata) information about the artifact, unless still cached as failure or
     * miss. Reach this point only if local and cached repo did not find resource or expired.
     *
     * @param path the artifact's path
     * @return A repository resource updated with the uptodate metadata
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField", "OverlyComplexMethod"})
    public final RepoResource getInfo(String path) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), path);

        //Skip if in blackout or not accepting/handling or cannot download
        StatusHolder statusHolder = allowsDownload(repoPath);
        if (statusHolder.isError()) {
            return new UnfoundRepoResource(repoPath, statusHolder.getStatusMsg());
        }
        // TODO: Do a checksum security check on download
        //Never query remote checksums
        if (MavenUtils.isChecksum(path)) {
            return new UnfoundRepoResource(repoPath, "Checksums are not downloaded");
        }
        RepoResource res;
        //Try to get it from the caches
        res = getFailedOrMissedResource(path);
        if (res == null) {
            res = internalGetInfo(repoPath);
        } else if (!res.isFound()) {
            if (log.isDebugEnabled()) {
                log.debug(this + ": " + res + " cached as not found at '" + path + "'.");
            }
        } else {
            throw new IllegalStateException(this + ": " + res + " retrieved at '" + path +
                    "' should not be in failed caches.");
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
        RepoResource res;
        //Try to get it from the remote repository
        if (!isOffline()) {
            try {
                res = retrieveInfo(path);
                if (!res.isFound()) {
                    //Update the non-found cache for a miss
                    if (log.isDebugEnabled()) {
                        log.debug(this + ": " + res + " not found at '" + path + "'.");
                    }
                    missedRetrievalsCache.put(path, res);
                }
            } catch (Exception e) {
                String reason = this + ": Error in getting information for '" + path +
                        "' (" + e.getMessage() + ").";
                log.warn(reason);
                //Update the non-found cache for a failure
                res = new UnfoundRepoResource(repoPath, reason);
                failedRetrievalsCache.put(path, res);
                if (isHardFail()) {
                    throw new RuntimeException(
                            this + ": Error in getting information for '" + path + "'.", e);
                }
            }
        } else {
            String msg =
                    this + ": is offline, " + repoPath + " is not found at '" + path + "'.";
            res = new UnfoundRepoResource(repoPath, msg);
            log.debug(msg);
        }
        return res;
    }

    protected abstract RepoResource retrieveInfo(String path);

    public StatusHolder allowsDownload(RepoPath repoPath) {
        StatusHolder status = assertValidPath(repoPath);
        if (status.isError()) {
            return status;
        }
        if (localCacheRepo != null) {
            status = localCacheRepo.allowsDownload(repoPath);
        }
        return status;
    }

    public ResourceStreamHandle getResourceStreamHandle(RepoResource res)
            throws IOException, FileExpectedException {
        String path = res.getRepoPath().getPath();
        if (isStoreArtifactsLocally()) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Retrieving info from cache for '" + path + "' from '" +
                            localCacheRepo + "'.");
                }
                ResourceStreamHandle handle = getRepositoryService().downloadAndSave(this, res);
                return handle;
            } catch (IOException e) {
                //If we fail on remote fetching and we can get the resource from an expired entry in
                //the local cache - fallback to using it, else rethrow the exception
                if (res.isExpired()) {
                    ResourceStreamHandle result = getRepositoryService()
                            .unexpireAndRetrieveIfExists(localCacheRepo, path);
                    if (result != null) {
                        return result;
                    }
                }
                throw e;
            }
        } else {
            ResourceStreamHandle handle = retrieveResource(path);
            return handle;
        }
    }

    public ResourceStreamHandle downloadAndSave(RepoResource res, RepoResource targetResource)
            throws IOException {
        String path = res.getRepoPath().getPath();
        FileInfo info = res.getInfo();
        FileInfo targetInfo = targetResource.getInfo();
        //Retrieve remotely only if locally cached artifact is older than remote one
        if (!isOffline() && (!targetResource.isFound() ||
                info.getLastModified() > targetInfo.getLastModified())) {
            // Check for security deploy rigths
            StatusHolder status =
                    getRepositoryService().assertValidDeployPath(localCacheRepo, path);
            if (status.isError()) {
                throw new IOException(status.getStatusMsg());
            }

            //Do the actual download
            ResourceStreamHandle handle = retrieveResource(path);
            try {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Copying " + path + " from " + this + " to " + localCacheRepo);
                }
                //Create/override the resource in the storage cache
                InputStream is = handle.getInputStream();
                if (log.isDebugEnabled()) {
                    log.debug("Saving resource '" + res + "' into cache '" +
                            localCacheRepo + "'.");
                }
                targetResource = localCacheRepo.saveResource(res, is);
                //Unexpire the resource and remove it from bad retrieval caches
                localCacheRepo.unexpire(path);
                removeFromCaches(path);
            } finally {
                handle.close();
            }
        }
        return localCacheRepo.getResourceStreamHandle(targetResource);
    }

    public String getMetadataProperty(String path) throws IOException {
        String value;
        if (isStoreArtifactsLocally()) {
            //We assume the resource is already contained in the repo-cache
            value = localCacheRepo.getMetadataProperty(path);
        } else {
            //Ugly hack: try to download the value directly as the first line of a remote file
            //(assuming it's a cheksum file)
            ResourceStreamHandle handle = retrieveResource(path);
            InputStream is = handle.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            value = reader.readLine();
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

    public void removeFromCaches(String path) {
        if (failedRetrievalsCache != null) {
            failedRetrievalsCache.remove(path);
        }
        if (missedRetrievalsCache != null) {
            missedRetrievalsCache.remove(path);
        }
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    private RepoResource getFailedOrMissedResource(String path) {
        RepoResource res = failedRetrievalsCache.get(path);
        if (res == null) {
            res = missedRetrievalsCache.get(path);
        }
        return res;
    }
}
