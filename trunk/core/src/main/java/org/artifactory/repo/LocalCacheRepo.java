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
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.jcr.JcrRepoBase;
import org.artifactory.resource.ExpiredRepoResource;
import org.artifactory.resource.RepoResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalCacheRepo extends JcrRepoBase<LocalCacheRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(LocalCacheRepo.class);

    public static final String PATH_SUFFIX = "-cache";

    private RemoteRepo remoteRepo;

    public LocalCacheRepo(RemoteRepo<? extends RemoteRepoDescriptor> remoteRepo) {
        super(remoteRepo.getRepositoryService());
        this.remoteRepo = remoteRepo;
        LocalCacheRepoDescriptor descriptor = new LocalCacheRepoDescriptor();
        descriptor.setDescription(remoteRepo.getDescription() + " (local file cache)");
        descriptor.setKey(remoteRepo.getKey() + PATH_SUFFIX);
        descriptor.setRemoteRepo(remoteRepo.getDescriptor());
        setDescriptor(descriptor);
        init();
    }

    @Override
    public RepoResource getInfo(final String path) throws FileExpectedException {
        RepoResource repoResource = super.getInfo(path);
        if (repoResource.isFound()) {
            //Check for expiry
            boolean expired = isExpired(repoResource);
            if (expired) {
                //Return not found
                repoResource = new ExpiredRepoResource(repoResource.getInfo());
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

    public void unexpire(final String path) {
        // TODO: Change this mechanism since the last updated is used for artifact popularity measurement
        //Reset the resource age so it is kept being cached
        JcrFile file = getLockedJcrFile(path, true);
        file.setLastUpdated(System.currentTimeMillis());
        if (log.isDebugEnabled()) {
            log.debug("Unexpired '" + path + "' from local cache '" + getKey() + "'.");
        }
    }

    public void zap(RepoPath repoPath) {
        //Zap all nodes recursively from all retrieval caches
        JcrFsItem fsItem = getLockedJcrFsItem(repoPath);
        if (fsItem != null && !fsItem.isDeleted()) {
            // Exists and not deleted... Let's roll
            long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000L;
            long expiredLastUpdated = System.currentTimeMillis() - retrievalCahePeriodMillis;
            int itemsZapped = fsItem.zap(expiredLastUpdated);
            log.info("Zapped '{}' from local cache: {} items zapped.", repoPath, itemsZapped);
        }
    }

    @Override
    public void updateCache(JcrFsItem fsItem) {
        super.updateCache(fsItem);
        remoteRepo.removeFromCaches(fsItem.getPath());
    }

    /**
     * Check that the item has not expired yet, unless it's a release which never expires
     *
     * @param repoResource The resource to check for expiery
     * @return boolean - True is resource is expired. False if not
     */
    private boolean isExpired(RepoResource repoResource) {
        boolean release = MavenNaming.isRelease(repoResource.getRepoPath().getPath());
        if (release) {
            return false;
        }
        long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000L;
        long age = repoResource.getAge();
        return age > retrievalCahePeriodMillis || age == -1;
    }
}