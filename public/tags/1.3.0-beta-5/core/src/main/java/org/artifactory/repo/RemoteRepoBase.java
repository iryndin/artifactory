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

import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.cache.DefaultRetrievalCache;
import org.artifactory.cache.RetrievalCache;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoType;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.jcr.md.MetadataValue;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.spring.InternalContextHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class RemoteRepoBase<T extends RemoteRepoDescriptor> extends RealRepoBase<T>
        implements RemoteRepo<T> {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(RemoteRepoBase.class);

    private LocalCacheRepo localCacheRepo;
    private RetrievalCache failedRetrievalsCache;
    private RetrievalCache missedRetrievalsCache;

    protected RemoteRepoBase(InternalRepositoryService repositoryService) {
        super(repositoryService);
    }

    protected RemoteRepoBase(InternalRepositoryService repositoryService, T descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
    }

    @Override
    public void setDescriptor(T descriptor) {
        super.setDescriptor(descriptor);
    }

    public void init() {
        if (isStoreArtifactsLocally()) {
            //Initialize the local cache
            localCacheRepo = new LocalCacheRepo(this);
        }
        long retrievalCachePeriodSecs = getDescriptor().getRetrievalCachePeriodSecs();
        if (retrievalCachePeriodSecs > 0) {
            LOGGER.info(this + ": Retrieval cache will be enabled with period of "
                    + retrievalCachePeriodSecs + " seconds");
        } else {
            LOGGER.info(this + ": Retrieval cache will be disbaled.");
        }
        if (isFailedRetrievalCacheEnabled()) {
            long failedRetrievalCachePeriodSecs =
                    getDescriptor().getFailedRetrievalCachePeriodSecs();
            LOGGER.info(this + ": Enabling failed retrieval cache with period of "
                    + failedRetrievalCachePeriodSecs + " seconds");
            failedRetrievalsCache =
                    new DefaultRetrievalCache(failedRetrievalCachePeriodSecs * 1000);
        } else {
            LOGGER.info(this + ": Disabling failed retrieval cache");
        }
        if (isMissedRetrievalCacheEnabled()) {
            long missedRetrievalCachePeriodSecs =
                    getDescriptor().getMissedRetrievalCachePeriodSecs();
            LOGGER.info(this + ": Enabling misses retrieval cache with period of "
                    + missedRetrievalCachePeriodSecs + " seconds");
            missedRetrievalsCache =
                    new DefaultRetrievalCache(missedRetrievalCachePeriodSecs * 1000);
        } else {
            LOGGER.info(this + ": Disabling misses retrieval cache");
        }
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
        return getDescriptor().isOffline();
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
     * Retrieve the (metadata) information about the artifact, unless still cahced as failure or
     * miss.
     *
     * @param path the artifact's path
     * @return A repository resource updated with the uptodate metadata
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField", "OverlyComplexMethod"})
    public final RepoResource getInfo(String path) throws FileExpectedException {
        //Skip if in blackout or not accepting/handling or cannot download
        StatusHolder statusHolder = getRepositoryService().assertValidPath(this, path);
        if (statusHolder.isError() || !allowsDownload(path)) {
            return new UnfoundRepoResource(this, path);
        }
        // TODO: Do a checksum security check on download
        //Never query remote checksums
        if (MavenUtils.isChecksum(path)) {
            return new UnfoundRepoResource(this, path);
        }
        RepoResource res;
        try {
            //Try to get it from the caches
            res = getFailedOrMissedResource(path);
            if (res == null) {
                //Try to get it from the remote repository
                if (!isOffline()) {
                    res = retrieveInfo(path);
                    if (!res.isFound()) {
                        //Update the non-found cache for a miss
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(this + ": " + res + " not found at '" + path + "'.");
                        }
                        if (isMissedRetrievalCacheEnabled()) {
                            synchronized (missedRetrievalsCache) {
                                missedRetrievalsCache.setResource(res);
                            }
                        }
                    }
                } else {
                    res = new UnfoundRepoResource(this, path);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                this + ": is offline, " + res + " is not found at '" + path + "'.");
                    }
                }
            } else if (!res.isFound()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(this + ": " + res + " cached as not found at '" + path + "'.");
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(this + ": " + res + " retrieved at '" + path + "'.");
                }
            }
        } catch (Exception e) {
            LOGGER.warn(this + ": Error in getting information for '" + path +
                    "' (" + e.getMessage() + ").");
            //Update the non-found cache for a failure
            res = new UnfoundRepoResource(this, path);
            if (isFailedRetrievalCacheEnabled()) {
                synchronized (failedRetrievalsCache) {
                    failedRetrievalsCache.setResource(res);
                }
            }
            if (isHardFail()) {
                throw new RuntimeException(
                        this + ": Error in getting information for '" + path + "'.", e);
            }
        }
        //If we cannot get the resource remotely and a (expired - otherwise we would not be
        //attempting the remote repo at all) cache entry exists use it by unexpiring it
        if (!res.isFound() && isStoreArtifactsLocally()) {
            res = getRepositoryService().unexpireIfExists(localCacheRepo, path);
        }
        return res;
    }

    protected abstract RepoResource retrieveInfo(String path);

    public boolean allowsDownload(String path) {
        return localCacheRepo == null || localCacheRepo.allowsDownload(path);
    }

    public ResourceStreamHandle getResourceStreamHandle(RepoResource res)
            throws IOException, RepoAccessException, FileExpectedException {
        String path = res.getPath();
        if (isStoreArtifactsLocally()) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Retrieving info from cache for '" + path + "' from '" +
                            localCacheRepo + "'.");
                }
                return getRepositoryService().downloadAndSave(this, res);
            } catch (IOException e) {
                //If we fail on remote fetching and we can get the resource from an expired entry in
                //the local cache - fallback to using it, else rethrow the exception
                ResourceStreamHandle result =
                        getRepositoryService().unexpireAndRetrieveIfExists(localCacheRepo, path);
                if (result == null) {
                    throw e;
                }
                return result;
            }
        } else {
            ResourceStreamHandle handle = retrieveResource(path);
            return handle;
        }
    }

    public ResourceStreamHandle downloadAndSave(RepoResource res, RepoResource targetResource)
            throws IOException {
        String path = res.getPath();
        //Retrieve remotely only if locally cached artifact is older than remote one
        if (!isOffline() && (!targetResource.isFound() ||
                res.getLastModified() > targetResource.getLastModified())) {
            // Check for security deploy rigths
            getRepositoryService().assertValidDeployPath(localCacheRepo, path);
            // First lock on local cache repo maybe someone already downloading
            MetadataService md = InternalContextHelper.get().beanForType(MetadataService.class);
            MetadataValue value = md.lockCreateIfEmpty(FileInfo.class,
                    localCacheRepo.getRepoRootPath() + "/" + path);
            // After acquiring the lock, test if the local cache is now populated and so return
            // without retrieving again from remote repo
            if (!value.isTransient()) {
                JcrFile lockedJcrFile = localCacheRepo.getLockedJcrFile(path);
                if (lockedJcrFile != null) {
                    // Another thread already downloaded the resource
                    return new SimpleResourceStreamHandle(lockedJcrFile.getStreamForDownload());
                }
                return null;
            }
            //Do the actual download
            ResourceStreamHandle handle = retrieveResource(path);
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Copying " + path + " from " + this + " to " + localCacheRepo);
                }
                //Create/override the resource in the storage cache
                InputStream is = handle.getInputStream();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Saving resource '" + res + "' into cache '" +
                            localCacheRepo + "'.");
                }
                localCacheRepo.saveResource(res, is);
                //Unexpire the resource and remove it from bad retrieval caches
                localCacheRepo.unexpire(path);
                removeFromCaches(path);
            } finally {
                handle.close();
            }
        }
        return localCacheRepo.getResourceStreamHandle(targetResource);
    }

    public String getProperty(String path) throws IOException {
        String value;
        if (isStoreArtifactsLocally()) {
            //We assume the resource is already contained in the repo-cache
            value = localCacheRepo.getProperty(path);
        } else {
            //Try to download the value directly as the first line of a remote file
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
            failedRetrievalsCache.removeResource(path);
        }
        if (missedRetrievalsCache != null) {
            missedRetrievalsCache.removeResource(path);
        }
    }

    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    private RepoResource getFailedOrMissedResource(String path) {
        RepoResource res = null;
        if (isFailedRetrievalCacheEnabled()) {
            synchronized (failedRetrievalsCache) {
                res = failedRetrievalsCache.getResource(path);
            }
        }
        if (res == null && isMissedRetrievalCacheEnabled()) {
            synchronized (missedRetrievalsCache) {
                res = missedRetrievalsCache.getResource(path);
            }
        }
        return res;
    }

    private boolean isMissedRetrievalCacheEnabled() {
        return getMissedRetrievalCachePeriodSecs() > 0;
    }

    private boolean isFailedRetrievalCacheEnabled() {
        return getFailedRetrievalCachePeriodSecs() > 0;
    }
}
