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

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.RestCoreAddon;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.addon.plugin.download.AltRemoteContentAction;
import org.artifactory.addon.plugin.download.AltRemotePathAction;
import org.artifactory.addon.plugin.download.PathCtx;
import org.artifactory.addon.plugin.download.ResourceStreamCtx;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.search.ArchiveIndexer;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.StatusHolder;
import org.artifactory.concurrent.ExpiringDelayed;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.engine.InternalDownloadService;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyBase;
import org.artifactory.md.Properties;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.db.DbCacheRepo;
import org.artifactory.repo.remote.browse.RemoteItem;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RemoteRequestException;
import org.artifactory.request.RepoRequests;
import org.artifactory.request.Request;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.RemoteRepoResource;
import org.artifactory.resource.RepoResourceInfo;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.binstore.service.BinaryNotFoundException;
import org.artifactory.storage.binstore.service.BinaryStore;
import org.artifactory.traffic.TrafficService;
import org.artifactory.traffic.entry.UploadEntry;
import org.artifactory.util.CollectionUtils;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    /**
     * Flags this repository as assumed offline. The repository enters this state when a download request fails with
     * exception.
     */
    protected volatile boolean assumedOffline;

    /**
     * The next time, in milliseconds, to check online status of this repository
     */
    protected long nextOnlineCheckMillis;

    /**
     * Cache of resources not found on the remote machine. Keyed by resource path.
     */
    private Map<String, RepoResource> missedRetrievalsCache;
    /**
     * Cache of remote directories listing.
     */
    private Map<String, List<RemoteItem>> remoteResourceCache;

    private final ChecksumPolicy checksumPolicy;

    private boolean globalOfflineMode;
    private final HandleRefsTracker handleRefsTracker;
    private final ConcurrentMap<String, DownloadEntry> inTransit;

    protected RemoteRepoBase(T descriptor, InternalRepositoryService repositoryService,
            boolean globalOfflineMode,
            RemoteRepo oldRemoteRepo) {
        super(descriptor, repositoryService);

        ChecksumPolicyType checksumPolicyType = descriptor.getChecksumPolicyType();
        checksumPolicy = ChecksumPolicyBase.getByType(checksumPolicyType);

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

    @Override
    public void init() {
        if (isStoreArtifactsLocally()) {
            DbCacheRepo oldCacheRepo = null;
            if (oldRemoteRepo != null) {
                oldCacheRepo = (DbCacheRepo) oldRemoteRepo.localCacheRepo;
            }
            //Initialize the local cache
            localCacheRepo = new DbCacheRepo(this, oldCacheRepo);
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
        long missedRetrievalCachePeriodSecs = getDescriptor().getMissedRetrievalCachePeriodSecs();
        if (missedRetrievalCachePeriodSecs > 0) {
            log.debug("{}: Enabling misses retrieval cache with period of {} seconds",
                    this, missedRetrievalCachePeriodSecs);
        } else {
            log.debug("{}: Disabling misses retrieval cache", this);
        }
    }

    @Override
    public boolean isStoreArtifactsLocally() {
        return getDescriptor().isStoreArtifactsLocally();
    }

    @Override
    public String getUrl() {
        return getDescriptor().getUrl();
    }

    @Override
    public boolean isHardFail() {
        return getDescriptor().isHardFail();
    }

    @Override
    public boolean isOffline() {
        return getDescriptor().isOffline() || globalOfflineMode || isAssumedOffline();
    }

    @Override
    public boolean isAssumedOffline() {
        return assumedOffline;
    }

    @Override
    public long getNextOnlineCheckMillis() {
        return isAssumedOffline() ? nextOnlineCheckMillis : 0;
    }

    @Override
    public boolean isListRemoteFolderItems() {
        return getDescriptor().isListRemoteFolderItems() && !getDescriptor().isBlackedOut() && !isOffline();
    }

    @Override
    public long getRetrievalCachePeriodSecs() {
        return getDescriptor().getRetrievalCachePeriodSecs();
    }

    @Override
    public long getAssumedOfflinePeriodSecs() {
        return getDescriptor().getAssumedOfflinePeriodSecs();
    }

    @Override
    public long getMissedRetrievalCachePeriodSecs() {
        return getDescriptor().getMissedRetrievalCachePeriodSecs();
    }

    @Override
    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    /**
     * Retrieve the (metadata) information about the artifact, unless still cached as failure or miss. Reach this point
     * only if local and cached repo did not find resource or expired.
     *
     * @param context The request context holding additional parameters
     * @return A repository resource updated with the uptodate metadata
     */
    @Override
    public final RepoResource getInfo(InternalRequestContext context) throws FileExpectedException {
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        RestCoreAddon restCoreAddon = addonsManager.addonByType(RestCoreAddon.class);
        context = restCoreAddon.getDynamicVersionContext(this, context, true);

        String path = context.getResourcePath();
        // make sure the repo key is of this repository
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), path);

        //Skip if in blackout or not accepting/handling or cannot download
        StatusHolder statusHolder = checkDownloadIsAllowed(repoPath);
        if (statusHolder.isError()) {
            RepoRequests.logToContext("Download denied (%s) - returning unfound resource", statusHolder.getStatusMsg());
            return new UnfoundRepoResource(repoPath, statusHolder.getStatusMsg(), statusHolder.getStatusCode());
        }
        //Never query remote checksums
        if (NamingUtils.isChecksum(path)) {
            RepoRequests.logToContext("Download denied - checksums are not downloadable");
            return new UnfoundRepoResource(repoPath, "Checksums are not downloadable.");
        }

        //Try to get it from the caches
        RepoResource res = getMissedResource(path);
        if (res == null) {
            res = internalGetInfo(repoPath, context);
        }

        //If we cannot get the resource remotely and an expired (otherwise we would not be
        //attempting the remote repo at all) cache entry exists use it by unexpiring it
        if (res.isExpired() && isStoreArtifactsLocally()) {
            RepoRequests.logToContext("Hosting repository stores locally and the resource is expired - " +
                    "un-expiring if still exists");
            res = getRepositoryService().unexpireIfExists(localCacheRepo, path);
        }
        return res;
    }

    private RepoResource internalGetInfo(RepoPath repoPath, InternalRequestContext context) {
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
            RepoRequests.logToContext("Found resource in local cache - returning cached resource");
            // found in local cache
            return returnCachedResource(repoPath, cachedResource);
        }

        boolean foundExpiredInCache = ((cachedResource != null) && cachedResource.isExpired());

        //not found in local cache - try to get it from the remote repository
        if (!isOffline()) {
            RepoResource remoteResource = getRemoteResource(context, repoPath, foundExpiredInCache);
            if (!remoteResource.isFound() && foundExpiredInCache) {
                RepoRequests.logToContext("Resource doesn't exist remotely but is expired in the caches - " +
                        "returning expired cached resource");
                remoteResource = returnCachedResource(repoPath, cachedResource);
            }
            // there's a newer remote resource that should be downloaded this is not supported for zip resources
            if (remoteResource.isFound() && context.getRequest().isZipResourceRequest()) {
                RepoRequests.logToContext("Resource exists remotely but is contained within a ZIP variant - " +
                        "returning returning as unfound (remote download of archived content isn't supported)");
                return new UnfoundRepoResource(repoPath,
                        "Zip resources download is only supported on cached artifacts");
            }
            return remoteResource;
        } else if (foundExpiredInCache) {
            RepoRequests.logToContext("Repository is offline but the resource in exists in the local cache - " +
                    "returning cached resource");
            //Return the cached resource if remote fetch failed
            return returnCachedResource(repoPath, cachedResource);
        } else {
            String offlineMessage = isAssumedOffline() ? "assumed offline" : "offline";
            RepoRequests.logToContext("Repository is " + offlineMessage + " and the resource doesn't exist in the " +
                    "local cache - returning unfound resource");
            return new UnfoundRepoResource(repoPath,
                    String.format("%s: is %s, '%s' is not found at '%s'.", this, offlineMessage, repoPath, path));
        }
    }

    /**
     * Returns a resource from a remote repository
     *
     * @param context             Download request context
     * @param repoPath            Item repo path
     * @param foundExpiredInCache True if the an expired item was found in the cache    @return Repo resource object
     */
    private RepoResource getRemoteResource(RequestContext context, RepoPath repoPath, boolean foundExpiredInCache) {
        String path = repoPath.getPath();

        if (!getDescriptor().isSynchronizeProperties() && context.getProperties().hasMandatoryProperty()) {
            RepoRequests.logToContext("Repository doesn't sync properties and the request contains " +
                    "mandatory properties - returning unfound resource");
            return new UnfoundRepoResource(repoPath, this + ": does not synchronize remote properties and request " +
                    "contains mandatory property, '" + repoPath + "' will not be downloaded from '" + path + "'.");
        }

        RepoResource remoteResource;
        path = getAltRemotePath(repoPath);
        if (!repoPath.getPath().equals(path)) {
            RepoRequests.logToContext("Remote resource path was altered by the user plugins to - %s", path);
        }
        try {
            remoteResource = retrieveInfo(path, context);
            if (!remoteResource.isFound() && !foundExpiredInCache) {
                //Update the non-found cache for a miss
                RepoRequests.logToContext("Unable to find resource remotely - adding to the missed retrieval cache.");
                missedRetrievalsCache.put(path, remoteResource);
            }
        } catch (Exception e) {
            RepoRequests.logToContext("Failed to retrieve information: %s", e.getMessage());
            String reason = this + ": Error in getting information for '" + path + "' (" + e.getMessage() + ").";
            if (log.isDebugEnabled()) {
                log.warn(reason, e);
            } else {
                log.warn(reason);
            }
            remoteResource = new UnfoundRepoResource(repoPath, reason);
            if (!foundExpiredInCache) {
                RepoRequests.logToContext("Found no expired resource in the cache - " +
                        "adding to the failed retrieval cache");
                putOffline();
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
        cachedResource.setResponseRepoPath(InternalRepoPathFactory.create(localCacheRepo.getKey(), repoPath.getPath()));
        return cachedResource;
    }

    /**
     * Temporarily puts the repository in an assumed offline mode.
     */
    protected abstract void putOffline();

    protected abstract RepoResource retrieveInfo(String path, @Nullable RequestContext context);

    @Override
    public StatusHolder checkDownloadIsAllowed(RepoPath repoPath) {
        String path = repoPath.getPath();
        StatusHolder status = assertValidPath(path, true);
        if (status.isError()) {
            return status;
        }
        if (localCacheRepo != null) {
            repoPath = InternalRepoPathFactory.create(localCacheRepo.getKey(), path);
            status = localCacheRepo.checkDownloadIsAllowed(repoPath);
        }
        return status;
    }

    @Override
    public ResourceStreamHandle getResourceStreamHandle(InternalRequestContext requestContext, RepoResource res)
            throws IOException, RepoRejectException {
        // We also change the context here, otherwise if there is something in the cache
        // we will receive it instead of trying to download the latest from the remote
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        RestCoreAddon restCoreAddon = addonsManager.addonByType(RestCoreAddon.class);
        requestContext = restCoreAddon.getDynamicVersionContext(this, requestContext, true);
        RepoRequests.logToContext("Creating a resource handle from '%s'", res.getResponseRepoPath().getRepoKey());
        String path = res.getRepoPath().getPath();
        if (isStoreArtifactsLocally()) {
            RepoRequests.logToContext("Target repository is configured to retain artifacts locally - " +
                    "resource will be stored and the streamed to the user");
            try {
                //Reflect the fact that we return a locally cached resource
                res.setResponseRepoPath(InternalRepoPathFactory.create(localCacheRepo.getKey(), path));
                ResourceStreamHandle handle = getRepositoryService().downloadAndSave(requestContext, this, res);
                return handle;
            } catch (IOException e) {
                RepoRequests.logToContext("Error occurred while downloading artifact: %s", e.getMessage());
                //If we fail on remote fetching and we can get the resource from an expired entry in
                //the local cache - fallback to using it, else rethrow the exception
                if (res.isExpired()) {
                    ResourceStreamHandle result =
                            getRepositoryService().unexpireAndRetrieveIfExists(requestContext, localCacheRepo, path);
                    if (result != null) {
                        RepoRequests.logToContext("Requested artifact is expired and exists in the cache - " +
                                "un-expiring cached and returning it instead");
                        return result;
                    }
                }
                throw e;
            }
        } else {
            RepoRequests.logToContext("Target repository is configured to not retain artifacts locally - " +
                    "resource will be stream directly to the user");
            ResourceStreamHandle handle = downloadResource(path, requestContext);
            return handle;
        }
    }

    @Override
    public ResourceStreamHandle downloadAndSave(InternalRequestContext requestContext, RepoResource remoteResource,
            RepoResource cachedResource) throws IOException, RepoRejectException {
        RepoPath remoteRepoPath = remoteResource.getRepoPath();
        String path = remoteRepoPath.getPath();

        boolean offline = isOffline();
        boolean foundExpiredResourceAndNewerRemote = foundExpiredAndRemoteIsNewer(remoteResource, cachedResource);
        boolean cachedNotFoundAndNotExpired = notFoundAndNotExpired(cachedResource);

        RepoRequests.logToContext("Remote repository is offline = %s", offline);
        RepoRequests.logToContext("Found expired cached resource but remote is newer = %s",
                foundExpiredResourceAndNewerRemote);
        RepoRequests.logToContext("Resource isn't cached and isn't expired = %s", cachedNotFoundAndNotExpired);

        //Retrieve remotely only if locally cached artifact not found or is found but expired and is older than remote one
        if (!offline && (foundExpiredResourceAndNewerRemote || cachedNotFoundAndNotExpired)) {
            RepoRequests.logToContext("Downloading and saving");

            // Check for security deploy rights
            RepoRequests.logToContext("Asserting valid deployment path");
            getRepositoryService().assertValidDeployPath(localCacheRepo, path, remoteResource.getInfo().getSize());

            //Create the parent folder
            String parentPath = PathUtils.getParent(path);
            RepoPath parentRepoPath = InternalRepoPathFactory.create(localCacheRepo.getKey(), parentPath);

            //Check that the resource is not being downloaded in parallel
            DownloadEntry completedConcurrentDownload = getCompletedConcurrentDownload(path);
            if (completedConcurrentDownload == null) {
                //We need to download since no concurrent download of the same resource took place
                RepoRequests.logToContext("Found no completed concurrent download - starting download");
                ResourceStreamHandle handle = null;
                try {
                    beforeResourceDownload(remoteResource, requestContext.getProperties(), requestContext.getRequest());

                    RepoResourceInfo remoteInfo = remoteResource.getInfo();
                    Set<ChecksumInfo> remoteChecksums = remoteInfo.getChecksumsInfo().getChecksums();
                    boolean receivedRemoteChecksums = CollectionUtils.notNullOrEmpty(remoteChecksums);
                    if (receivedRemoteChecksums) {
                        RepoRequests.logToContext("Received remote checksums headers - %s", remoteChecksums);
                    } else {
                        RepoRequests.logToContext("Received no remote checksums headers");
                    }

                    //Allow plugins to provide an alternate content
                    handle = getAltContent(remoteRepoPath);

                    if (handle == null && receivedRemoteChecksums &&
                            shouldSearchForExistingResource(requestContext.getRequest())) {
                        RepoRequests.logToContext("Received no alternative content, received remote checksums headers" +
                                " and searching for existing resources on download is enabled");
                        handle = getExistingResourceByChecksum(remoteChecksums, remoteResource.getSize());
                    }

                    long remoteRequestStartTime = 0;
                    if (handle == null) {
                        RepoRequests.logToContext("Received no alternative content or existing resource - " +
                                "downloading resource");
                        //If we didn't get an alternate handle do the actual download
                        remoteRequestStartTime = System.currentTimeMillis();
                        handle = downloadResource(path, requestContext);
                    }

                    if (!receivedRemoteChecksums) {
                        RepoRequests.logToContext("Trying to find remote checksums");
                        remoteChecksums = getRemoteChecksums(path);
                        if (remoteResource instanceof RemoteRepoResource) {
                            ((RemoteRepoResource) remoteResource).getInfo().setChecksums(remoteChecksums);
                        } else {
                            // Cannot set the checksums on non remote repo resource
                            RepoRequests.logToContext("No checksums found on %s and it's not a remote resource!",
                                    remoteResource);
                        }
                    }

                    Properties properties = null;
                    boolean synchronizeProperties = getDescriptor().isSynchronizeProperties();

                    RepoRequests.logToContext("Remote property synchronization enabled = %s", synchronizeProperties);

                    if (synchronizeProperties) {
                        // No check for annotate permissions, since sync props is a configuration flag
                        // and file will be deployed here
                        RepoRequests.logToContext("Trying to find remote properties");
                        properties = getRemoteProperties(path);
                    }

                    //Create/override the resource in the storage cache
                    RepoRequests.logToContext("Saving resource to " + localCacheRepo);
                    SaveResourceContext saveResourceContext = new SaveResourceContext.Builder(remoteResource, handle)
                            .properties(properties).build();
                    cachedResource = getRepositoryService().saveResource(localCacheRepo, saveResourceContext);
                    if (remoteRequestStartTime > 0) {
                        // fire upload event only if the resource was downloaded from the remote repository
                        UploadEntry uploadEntry = new UploadEntry(remoteResource.getRepoPath().getId(),
                                cachedResource.getSize(), System.currentTimeMillis() - remoteRequestStartTime);
                        TrafficService trafficService = ContextHelper.get().beanForType(TrafficService.class);
                        trafficService.handleTrafficEntry(uploadEntry);
                    }

                    unexpire(cachedResource);
                    afterResourceDownload(remoteResource);
                } finally {
                    if (handle != null) {
                        handle.close();
                    }
                    //Notify concurrent download waiters
                    notifyConcurrentWaiters(requestContext, cachedResource, path);
                }
            } else {
                RepoRequests.logToContext("Found completed concurrent download - using existing handle");
                //We will not see the stored result here yet since it is saved in its own tx - return a direct handle
                ConcurrentLinkedQueue<ResourceStreamHandle> preparedHandles = completedConcurrentDownload.handles;
                ResourceStreamHandle handle = preparedHandles.poll();
                if (handle == null) {
                    log.error("No concurrent download handle is available.");
                    RepoRequests.logToContext("Unable find available concurrent download handle");
                }
                return handle;
            }
        }

        boolean foundExpiredAndNewerThanRemote = foundExpiredAndRemoteIsNotNewer(remoteResource, cachedResource);

        RepoRequests.logToContext("Found expired cached resource and is newer than remote = %s",
                foundExpiredAndNewerThanRemote);

        if (foundExpiredAndNewerThanRemote) {
            synchronizeExpiredResourceProperties(remoteRepoPath);
            unexpire(cachedResource);
        }

        RepoRequests.logToContext("Returning the cached resource");
        //Return the cached result (the newly downloaded or already cached resource)
        return localCacheRepo.getResourceStreamHandle(requestContext, cachedResource);
    }

    private boolean shouldSearchForExistingResource(Request request) {
        String searchForExistingResource = request.getParameter(
                ArtifactoryRequest.PARAM_SEARCH_FOR_EXISTING_RESOURCE_ON_REMOTE_REQUEST);
        if (StringUtils.isNotBlank(searchForExistingResource)) {
            return Boolean.valueOf(searchForExistingResource);
        }

        return ConstantValues.searchForExistingResourceOnRemoteRequest.getBoolean();
    }

    private ResourceStreamHandle getExistingResourceByChecksum(Set<ChecksumInfo> remoteChecksums, long size) {
        String remoteSha1 = getRemoteSha1(remoteChecksums);
        if (!ChecksumType.sha1.isValid(remoteSha1)) {
            RepoRequests.logToContext("Remote sha1 doesn't exist or is invalid: " + remoteSha1);
            return null;
        }
        try {
            RepoRequests.logToContext("Searching for existing resource with SHA-1 '%s'", remoteSha1);
            BinaryStore binaryStore = ContextHelper.get().beanForType(BinaryStore.class);
            InputStream data = binaryStore.getBinary(remoteSha1);
            RepoRequests.logToContext("Found existing resource with the same checksum - " +
                    "returning as normal content handle");
            return new SimpleResourceStreamHandle(data, size);
        } catch (BinaryNotFoundException e) {
            // not found - resume
            return null;
        }
    }

    private String getRemoteSha1(Set<ChecksumInfo> remoteChecksums) {
        for (ChecksumInfo remoteChecksum : remoteChecksums) {
            if (ChecksumType.sha1.equals(remoteChecksum.getType())) {
                return remoteChecksum.getOriginal();
            }
        }
        return null;
    }

    private void unexpire(RepoResource cachedResource) {
        RepoRequests.logToContext("Un-expiring cached resource if needed");
        String relativePath = cachedResource.getRepoPath().getPath();
        boolean isMetadata = cachedResource.isMetadata();

        RepoRequests.logToContext("Is resource metadata = %s", isMetadata);

        if (!isMetadata) {
            // unexpire the file
            RepoRequests.logToContext("Un-expiring the resource");
            localCacheRepo.unexpire(relativePath);
            // remove it from bad retrieval caches
            RepoRequests.logToContext("Removing the resource from all failed caches");
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
        RepoRequests.logToContext("Trying to find completed concurrent download");
        final DownloadEntry downloadEntry = new DownloadEntry(relPath);
        DownloadEntry currentDownload = inTransit.putIfAbsent(relPath, downloadEntry);
        if (currentDownload != null) {
            //No put since a concurrent download in progress - wait for it
            try {
                RepoRequests.logToContext("Found download in progress '%s'", currentDownload);
                //Increment the resource handles count
                int prevCount = currentDownload.handlesToPrepare.getAndIncrement();
                if (prevCount < 0) {
                    //Calculation already started
                    RepoRequests.logToContext("Not waiting on concurrent download %s since calculation already started",
                            currentDownload);
                    return null;
                }
                log.info("Waiting on concurrent download of '{}' in '{}'.", relPath, this);
                RepoRequests.logToContext("Waiting on concurrent download.");
                boolean latchTriggered =
                        currentDownload.latch.await(currentDownload.getDelay(TimeUnit.SECONDS), TimeUnit.SECONDS);
                if (!latchTriggered) {
                    //We exited because of a timeout
                    log.info("Timed-out waiting on concurrent download of '{}' in '{}'. Allowing concurrent " +
                            "downloads to proceed.", relPath, this);
                    RepoRequests.logToContext("Timed-out waiting on concurrent download. Allowing concurrent " +
                            "downloads to proceed.");
                    currentDownload.handlesToPrepare.decrementAndGet();
                    return null;
                } else {
                    return currentDownload;
                }
            } catch (InterruptedException e) {
                RepoRequests.logToContext("Interrupted while waiting on a concurrent download: %s", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    private void notifyConcurrentWaiters(InternalRequestContext requestContext, RepoResource resource, String relPath)
            throws IOException, RepoRejectException {
        DownloadEntry currentDownload = inTransit.remove(relPath);
        if (currentDownload != null) {
            //Put it low enough in case it is incremented by multiple late waiters
            int handlesCount = currentDownload.handlesToPrepare.getAndSet(-9999);
            RepoRequests.logToContext("Finished concurrent download. Preparing %s download handles for waiters",
                    handlesCount);
            //Add a new handle entries since the new resource is visible to this tx only
            for (int i = 0; i < handlesCount; i++) {
                ResourceStreamHandle extHandle = localCacheRepo.getResourceStreamHandle(requestContext, resource);
                currentDownload.handles.add(extHandle);
                //if waiters do not pick up the handles prepared (timed out waiting, exception,
                //interrupted...) so we keep track on their references
                handleRefsTracker.add(extHandle);
            }

            //Notify the download waiters
            RepoRequests.logToContext("Notifying waiters on: %s", currentDownload);
            InternalDownloadService downloadService = InternalContextHelper.get().beanForType(
                    InternalDownloadService.class);
            downloadService.releaseDownloadWaiters(currentDownload.latch);
        }
    }

    private Set<ChecksumInfo> getRemoteChecksums(String path) {
        Set<ChecksumInfo> checksums = new HashSet<ChecksumInfo>();
        for (ChecksumType checksumType : ChecksumType.values()) {
            String checksum = null;
            try {
                RepoRequests.logToContext("Trying to find remote checksum - %s", checksumType.ext());
                checksum = getRemoteChecksum(path + checksumType.ext());
            } catch (FileNotFoundException e) {
                RepoRequests.logToContext("Remote checksum file doesn't exist");
            } catch (Exception e) {
                RepoRequests.logToContext("Error occurred while retrieving remote checksum: %s", e.getMessage());
            }
            ChecksumInfo info = new ChecksumInfo(checksumType, null, null);
            if (StringUtils.isNotBlank(checksum)) {
                RepoRequests.logToContext("Found remote checksum with the value - %s", checksum);
                // set the remote checksum only if it is a valid string for that checksum
                if (checksumType.isValid(checksum)) {
                    info = new ChecksumInfo(checksumType, checksum, null);
                } else {
                    RepoRequests.logToContext("Remote checksum is invalid");
                }
            }
            checksums.add(info);
        }
        return checksums;
    }

    @Override
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

    @Override
    @Nullable
    public LocalCacheRepo getLocalCacheRepo() {
        return localCacheRepo;
    }

    @Override
    @Nonnull
    public List<RemoteItem> listRemoteResources(String directoryPath) throws IOException {
        assert !isOffline() : "Should never be called in offline mode";
        List<RemoteItem> cachedUrls = remoteResourceCache.get(directoryPath);
        if (CollectionUtils.notNullOrEmpty(cachedUrls)) {
            return cachedUrls;
        }

        checkForRemoteListingInMissedCaches(directoryPath);

        List<RemoteItem> urls;
        String fullDirectoryUrl = appendAndGetUrl(directoryPath);
        try {
            urls = getChildUrls(fullDirectoryUrl);
        } catch (IOException e) {
            addRemoteListingEntryToMissedCache(directoryPath, e);
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

    private void checkForRemoteListingInMissedCaches(String directoryPath) throws IOException {
        UnfoundRepoResource unfoundRepoResource = ((UnfoundRepoResource) missedRetrievalsCache.get(directoryPath));
        if (unfoundRepoResource != null) {
            throw new IOException(unfoundRepoResource.getReason());
        }
    }

    private void addRemoteListingEntryToMissedCache(String directoryPath, IOException e) {
        if (!missedRetrievalsCache.containsKey(directoryPath)) {
            String message = e.getMessage();
            missedRetrievalsCache.put(directoryPath, new UnfoundRepoResource(getRepoPath(directoryPath), message));
        }
    }

    protected abstract List<RemoteItem> getChildUrls(String dirUrl) throws IOException;

    @Override
    public void clearCaches() {
        clearCaches(missedRetrievalsCache, remoteResourceCache);
    }

    @Override
    public void removeFromCaches(String path, boolean removeSubPaths) {
        removeFromCaches(path, removeSubPaths, missedRetrievalsCache, remoteResourceCache);
    }

    /**
     * Executed before actual download. May return an alternate handle with its own input stream to circumvent download
     */
    private void beforeResourceDownload(RepoResource resource, Properties properties, Request request) {
        boolean fetchSourcesEagerly = getDescriptor().isFetchSourcesEagerly();
        boolean fetchJarsEagerly = getDescriptor().isFetchJarsEagerly();

        RepoRequests.logToContext("Eager source JAR fetching enabled = %s", fetchSourcesEagerly);
        RepoRequests.logToContext("Eager JAR fetching enabled = %s", fetchJarsEagerly);

        if (!fetchSourcesEagerly && !fetchJarsEagerly) {
            // eager fetching is disabled
            RepoRequests.logToContext("Eager JAR and source JAR fetching is disabled");
            return;
        }
        String replicationDownload = request.getParameter(ArtifactoryRequest.PARAM_REPLICATION_DOWNLOAD_REQUESET);
        if (StringUtils.isNotBlank(replicationDownload) && Boolean.valueOf(replicationDownload)) {
            // Do not perform eager fetching in case of replication download
            RepoRequests.logToContext("Eager JAR and source JAR fetching is disabled for replication download request");
            return;
        }
        RepoPath repoPath = resource.getRepoPath();
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(repoPath);
        boolean validMavenArtifactInfo = artifactInfo.isValid();
        boolean artifactHasClassifier = artifactInfo.hasClassifier();

        RepoRequests.logToContext("Valid Maven artifact info = %s", validMavenArtifactInfo);
        RepoRequests.logToContext("Artifact has classifier = %s", artifactHasClassifier);

        if (!validMavenArtifactInfo || artifactHasClassifier) {
            RepoRequests.logToContext("Eager JAR and source JAR fetching is not attempted");
            return;
        }

        String path = repoPath.getPath();
        int lastDotIndex = path.lastIndexOf('.');
        String eagerPath;

        boolean artifactIsPom = "pom".equals(artifactInfo.getType());
        boolean artifactIsJar = "jar".equals(artifactInfo.getType());

        if (fetchJarsEagerly && artifactIsPom) {
            eagerPath = path.substring(0, lastDotIndex) + ".jar";
            RepoRequests.logToContext("Eagerly fetching JAR '%s'", eagerPath);
        } else if (fetchSourcesEagerly && artifactIsJar) {
            // create a path to the sources
            eagerPath = PathUtils.injectString(path, "-sources", lastDotIndex);
            RepoRequests.logToContext("Eagerly fetching source JAR '%s'", eagerPath);
        } else {
            RepoRequests.logToContext("Eager JAR and source JAR fetching is not attempted");
            return;
        }

        // Attach matrix params is exist
        eagerPath += buildRequestMatrixParams(properties);

        // pass the repo path to download eagerly
        EagerResourcesDownloader resourcesDownloader =
                InternalContextHelper.get().beanForType(EagerResourcesDownloader.class);
        RepoPath eagerRepoPath = InternalRepoPathFactory.create(getDescriptor().getKey(), eagerPath);
        resourcesDownloader.downloadAsync(eagerRepoPath);
    }

    private void afterResourceDownload(RepoResource resource) {
        RepoRequests.logToContext("Executing async archive content indexing if needed");
        String path = resource.getRepoPath().getPath();
        ArchiveIndexer archiveIndexer =
                InternalContextHelper.get().beanForType(ArchiveIndexer.class);
        archiveIndexer.asyncIndex(InternalRepoPathFactory.create(localCacheRepo.getKey(), path));
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
        Properties properties = (Properties) InfoFactoryHolder.get().createProperties();
        ResourceStreamHandle handle = null;
        InputStream is = null;
        try {
            RepoRequests.logToContext("Trying to download remote properties");
            handle = downloadResource(relPath + ":" + Properties.ROOT);
            is = handle.getInputStream();
            if (is != null) {
                RepoRequests.logToContext("Received remote property content");
                Properties remoteProperties = (Properties) InfoFactoryHolder.get().getFileSystemXStream().fromXML(is);
                for (String remotePropertyKey : remoteProperties.keySet()) {
                    Set<String> values = remoteProperties.get(remotePropertyKey);
                    RepoRequests.logToContext("Found remote property key '{}' with values '%s'", remotePropertyKey,
                            values);
                    if (!remotePropertyKey.startsWith(ReplicationAddon.PROP_REPLICATION_PREFIX)) {
                        properties.putAll(remotePropertyKey, values);
                    }
                }
            }
        } catch (Exception e) {
            properties = null;
            RepoRequests.logToContext("Error occurred while retrieving remote properties: %s", e.getMessage());
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
            RepoRequests.logToContext("Remote property synchronization is disabled - " +
                    "expired resource property synchronization not attempted");
            return;
        }

        try {
            String artifactRelativePath = repoPath.getPath();
            String propertiesRelativePath = artifactRelativePath + ":" + Properties.ROOT;
            RepoPath propertiesRepoPath = InternalRepoPathFactory.create(repoPath.getRepoKey(), propertiesRelativePath);
            String remotePropertiesRelativePath = getAltRemotePath(propertiesRepoPath);

            if (!propertiesRepoPath.getPath().equals(remotePropertiesRelativePath)) {
                RepoRequests.logToContext("Remote resource path was altered by the user plugins to - %s",
                        remotePropertiesRelativePath);
            }

            LocalCacheRepo cache = getLocalCacheRepo();
            RepoResource cachedPropertiesResource = cache.getInfo(new NullRequestContext(propertiesRepoPath));

            Properties properties = (Properties) InfoFactoryHolder.get().createProperties();

            //Send HEAD
            RepoResource remoteResource = retrieveInfo(remotePropertiesRelativePath, null);
            if (remoteResource.isFound()) {
                RepoRequests.logToContext("Found remote properties");
                if (cachedPropertiesResource.isFound() &&
                        (cachedPropertiesResource.getLastModified() > remoteResource.getLastModified())) {
                    RepoRequests.logToContext("Remote properties were not modified - no changes will be applied");
                    // remote properties are not newer
                    return;
                }

                ResourceStreamHandle resourceStreamHandle = downloadResource(remotePropertiesRelativePath);
                InputStream inputStream = null;
                try {
                    inputStream = resourceStreamHandle.getInputStream();
                    Properties remoteProperties = (Properties) InfoFactoryHolder.get().getFileSystemXStream().fromXML(
                            inputStream);
                    for (String remotePropertyKey : remoteProperties.keySet()) {
                        Set<String> values = remoteProperties.get(remotePropertyKey);
                        RepoRequests.logToContext("Found remote property key '{}' with values '%s'", remotePropertyKey,
                                values);
                        if (!remotePropertyKey.startsWith(ReplicationAddon.PROP_REPLICATION_PREFIX)) {
                            properties.putAll(remotePropertyKey, values);
                        }
                    }
                } finally {
                    IOUtils.closeQuietly(inputStream);
                }
            } else {
                RepoRequests.logToContext("Found no remote properties");
            }

            RepoPath localCacheRepoPath = InternalRepoPathFactory.create(cache.getKey(), artifactRelativePath);
            getRepositoryService().setProperties(localCacheRepoPath, properties);
        } catch (Exception e) {
            String repoPathId = repoPath.getId();
            log.error("Unable to synchronize the properties of the item '{}' with the remote resource: {}",
                    repoPathId, e.getMessage());
            RepoRequests.logToContext("Error occurred while synchronizing the properties: %s", e.getMessage());
        }
    }

    private RepoResource getMissedResource(String path) {
        return missedRetrievalsCache.get(path);
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
     * @param repoPath The original repo path
     * @return Alternative path from the plugin or the same if no plugin changes it
     */
    private String getAltRemotePath(RepoPath repoPath) {
        RepoRequests.logToContext("Executing any AltRemotePath user plugins that may exist");
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
        RepoRequests.logToContext("Executing any AltRemoteContent user plugins that may exist");
        AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
        PluginsAddon pluginAddon = addonsManager.addonByType(PluginsAddon.class);
        ResourceStreamCtx rsCtx = new ResourceStreamCtx();
        pluginAddon.execPluginActions(AltRemoteContentAction.class, rsCtx, repoPath);
        InputStream is = rsCtx.getInputStream();
        if (is != null) {
            RepoRequests.logToContext("Received alternative content from a user plugin - " +
                    "using as a normal content handle");
            return new SimpleResourceStreamHandle(is, rsCtx.getSize());
        }

        RepoRequests.logToContext("Received no alternative content handle from a user plugin");
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

    /**
     * Constructs a matrix params string from the given properties ready to attach to an HTTP request
     *
     * @param requestProperties Properties to construct. Can be null
     * @return HTTP request ready property chain
     */
    protected String buildRequestMatrixParams(Properties requestProperties) {
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
                requestPropertyBuilder.append(key);
                if (isMandatory) {
                    requestPropertyBuilder.append("+");
                }
                String value = requestPropertyEntry.getValue();
                if (StringUtils.isNotBlank(value)) {
                    requestPropertyBuilder.append("=").append(value);
                }
            }
        }
        return requestPropertyBuilder.toString();
    }
}
