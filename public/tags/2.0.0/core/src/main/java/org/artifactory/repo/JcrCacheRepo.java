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

import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyBase;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.jcr.JcrRepoBase;
import org.artifactory.resource.ExpiredRepoResource;
import org.artifactory.resource.RepoResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrCacheRepo extends JcrRepoBase implements LocalCacheRepo {
    private static final Logger log = LoggerFactory.getLogger(LocalCacheRepo.class);

    private RemoteRepo remoteRepo;

    private ChecksumPolicy checksumPolicy;

    public JcrCacheRepo(RemoteRepo<? extends RemoteRepoDescriptor> remoteRepo) {
        super(remoteRepo.getRepositoryService());
        this.remoteRepo = remoteRepo;
        // create descriptor on-the-fly since this repo is created by a remote repo
        LocalCacheRepoDescriptor descriptor = new LocalCacheRepoDescriptor();
        descriptor.setDescription(remoteRepo.getDescription() + " (local file cache)");
        descriptor.setKey(remoteRepo.getKey() + PATH_SUFFIX);
        descriptor.setRemoteRepo(remoteRepo.getDescriptor());
        setDescriptor(descriptor);
        ChecksumPolicyType checksumPolicyType = remoteRepo.getDescriptor().getChecksumPolicyType();
        checksumPolicy = ChecksumPolicyBase.getByType(checksumPolicyType);
    }

    @Override
    public RepoResource getInfo(final String path) throws FileExpectedException {
        RepoResource repoResource = super.getInfo(path);
        if (repoResource.isFound()) {
            //Check for expiry
            boolean expired = isExpired(repoResource);
            if (expired) {
                log.debug("Returning expired resource {} ", path);
                //Return not found
                repoResource = new ExpiredRepoResource(repoResource);
            }
        }
        return repoResource;
    }

    @Override
    public boolean isCache() {
        return true;
    }

    @Override
    public int getMaxUniqueSnapshots() {
        return remoteRepo.getMaxUniqueSnapshots();
    }

    public RemoteRepo getRemoteRepo() {
        return remoteRepo;
    }

    @Override
    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    public void unexpire(String path) {
        // TODO: Change this mechanism since the last updated is used for artifact popularity measurement
        //Reset the resource age so it is kept being cached
        JcrFsItem item = getLockedJcrFsItem(path);
        item.setLastUpdated(System.currentTimeMillis());
        log.debug("Unexpired '{}' from local cache '{}'.", path, getKey());
    }

    public void zap(RepoPath repoPath) {
        //Zap all nodes recursively from all retrieval caches
        JcrFsItem fsItem = getLockedJcrFsItem(repoPath);
        if (fsItem != null && !fsItem.isDeleted()) {
            // Exists and not deleted... Let's roll
            long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000L;
            long expiredLastUpdated = System.currentTimeMillis() - retrievalCahePeriodMillis;
            int itemsZapped = fsItem.zap(expiredLastUpdated);
            // now remove all the caches related to this path and any sub paths
            remoteRepo.removeFromCaches(fsItem.getRelativePath(), true);
            log.info("Zapped '{}' from local cache: {} items zapped.", repoPath, itemsZapped);
        }
    }

    @Override
    public void updateCache(JcrFsItem fsItem) {
        super.updateCache(fsItem);
        remoteRepo.removeFromCaches(fsItem.getRelativePath(), false);
    }

    /**
     * Check that the item has not expired yet, unless it's a release which never expires or a unique snapshot.
     *
     * @param repoResource The resource to check for expiery
     * @return boolean - True is resource is expired. False if not
     */
    private boolean isExpired(RepoResource repoResource) {
        String path = repoResource.getRepoPath().getPath();
        boolean release = MavenNaming.isRelease(path);
        if (release || MavenNaming.isNonUniqueSnapshot(path)) {
            return false;
        }
        // it is a snapshot
        long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000L;
        long cacheAge = repoResource.getCacheAge();
        return cacheAge > retrievalCahePeriodMillis || cacheAge == -1;
    }
}