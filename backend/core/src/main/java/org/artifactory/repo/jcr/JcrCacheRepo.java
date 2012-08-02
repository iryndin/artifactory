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

package org.artifactory.repo.jcr;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyBase;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.jcr.cache.expirable.CacheExpiry;
import org.artifactory.repo.jcr.cache.expirable.ZapItemVisitor;
import org.artifactory.repo.snapshot.MavenSnapshotVersionAdapter;
import org.artifactory.request.ArtifactoryRequest;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.RepoRequests;
import org.artifactory.request.Request;
import org.artifactory.resource.ExpiredRepoResource;
import org.slf4j.Logger;

public class JcrCacheRepo extends JcrRepoBase<LocalCacheRepoDescriptor> implements LocalCacheRepo {
    private static final Logger log = LoggerFactory.getLogger(LocalCacheRepo.class);

    private RemoteRepo<? extends RemoteRepoDescriptor> remoteRepo;

    private ChecksumPolicy checksumPolicy;

    public JcrCacheRepo(RemoteRepo<? extends RemoteRepoDescriptor> remoteRepo, LocalCacheRepo oldCacheRepo) {
        super(remoteRepo.getRepositoryService(), oldCacheRepo != null ? oldCacheRepo.getStorageMixin() : null);
        this.remoteRepo = remoteRepo;
        // create descriptor on-the-fly since this repo is created by a remote repo
        LocalCacheRepoDescriptor descriptor = new LocalCacheRepoDescriptor();
        descriptor.setDescription(remoteRepo.getDescription() + " (local file cache)");
        descriptor.setKey(remoteRepo.getKey() + PATH_SUFFIX);

        RemoteRepoDescriptor remoteRepoDescriptor = remoteRepo.getDescriptor();

        descriptor.setRemoteRepo(remoteRepoDescriptor);
        descriptor.setRepoLayout(remoteRepoDescriptor.getRepoLayout());
        setDescriptor(descriptor);
        ChecksumPolicyType checksumPolicyType = remoteRepoDescriptor.getChecksumPolicyType();
        checksumPolicy = ChecksumPolicyBase.getByType(checksumPolicyType);
    }

    @Override
    public RepoResource getInfo(InternalRequestContext context) throws FileExpectedException {
        RepoResource repoResource = super.getInfo(context);
        if (repoResource.isFound()) {
            //Check for expiry
            RepoRequests.logToContext("Found the resource in the cache - checking for expiry");
            boolean forceDownloadIfNewer = false;
            Request request = context.getRequest();
            if (request != null) {
                String forcePropValue = request.getParameter(ArtifactoryRequest.PARAM_FORCE_DOWNLOAD_IF_NEWER);
                if (StringUtils.isNotBlank(forcePropValue)) {
                    forceDownloadIfNewer = Boolean.valueOf(forcePropValue);
                    RepoRequests.logToContext("Found request parameter {}=%s",
                            ArtifactoryRequest.PARAM_FORCE_DOWNLOAD_IF_NEWER, forceDownloadIfNewer);
                }
            }

            if (forceDownloadIfNewer || isExpired(repoResource)) {
                RepoRequests.logToContext("Returning resource as expired");
                repoResource = new ExpiredRepoResource(repoResource);
            } else {
                RepoRequests.logToContext("Returning cached resource");
            }
        }
        return repoResource;
    }

    @Override
    public boolean isCache() {
        return true;
    }

    @Override
    public boolean isSuppressPomConsistencyChecks() {
        return getDescriptor().getRemoteRepo().isSuppressPomConsistencyChecks();
    }

    @Override
    public int getMaxUniqueSnapshots() {
        return remoteRepo.getMaxUniqueSnapshots();
    }

    @Override
    public RemoteRepo<? extends RemoteRepoDescriptor> getRemoteRepo() {
        return remoteRepo;
    }

    @Override
    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    @Override
    public void onCreate(JcrFsItem fsItem) {
    }

    @Override
    public void unexpire(String path) {
        //Reset the resource age so it is kept being cached
        JcrFsItem item = getLockedJcrFsItem(path);
        item.unexpire();
    }

    @Override
    public int zap(RepoPath repoPath) {
        int itemsZapped = 0;
        //Zap all nodes recursively from all retrieval caches
        JcrFsItem fsItem = getLockedJcrFsItem(repoPath);
        if (fsItem != null && !fsItem.isDeleted()) {
            // Exists and not deleted... Let's roll
            ZapItemVisitor zapVisitor = new ZapItemVisitor(this);
            fsItem.accept(zapVisitor);
            itemsZapped = zapVisitor.getUpdatedItemsCount();
            // now remove all the caches related to this path and any sub paths
            remoteRepo.removeFromCaches(fsItem.getRelativePath(), true);
            log.info("Zapped '{}' from local cache: {} items zapped.", repoPath, itemsZapped);
        }
        return itemsZapped;
    }

    @Override
    public void updateCache(JcrFsItem fsItem) {
        super.updateCache(fsItem);
        remoteRepo.removeFromCaches(fsItem.getRelativePath(), false);
    }

    /**
     * Check that the item has not expired yet, unless it's a release which never expires or a unique snapshot.
     *
     * @param repoResource The resource to check for expiry
     * @return boolean - True if resource is expired. False if not
     */
    protected boolean isExpired(RepoResource repoResource) {
        String path = repoResource.getRepoPath().getPath();
        CacheExpiry cacheExpiry = ContextHelper.get().beanForType(CacheExpiry.class);

        if (cacheExpiry.isExpirable(this, path)) {
            long retrievalCachePeriodMillis = getRetrievalCachePeriodMillis(path);
            long cacheAge = repoResource.getCacheAge();
            return cacheAge > retrievalCachePeriodMillis || cacheAge == -1;
        }
        return false;
    }

    private long getRetrievalCachePeriodMillis(String path) {
        long retrievalCachePeriodMillis;
        if (MavenNaming.isIndex(path) &&
                remoteRepo.getUrl().contains(ConstantValues.mvnCentralHostPattern.getString())) {
            //If it is a central maven index use the hardcoded cache value
            long centralMaxQueryIntervalSecs = ConstantValues.mvnCentralIndexerMaxQueryIntervalSecs.getLong();
            retrievalCachePeriodMillis = centralMaxQueryIntervalSecs * 1000L;
        } else {
            //It is a non-unique snapshot or snapshot metadata
            retrievalCachePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000L;
        }
        return retrievalCachePeriodMillis;
    }

    @Override
    public MavenSnapshotVersionAdapter getMavenSnapshotVersionAdapter() {
        throw new UnsupportedOperationException("Local cache repositories doesn't have snapshot version adapter");
    }
}
