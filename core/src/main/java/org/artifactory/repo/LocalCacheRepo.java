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
import org.artifactory.config.CentralConfig;
import org.artifactory.jcr.JcrFile;
import org.artifactory.jcr.JcrFolder;
import org.artifactory.jcr.JcrFsItem;
import org.artifactory.repo.exception.FileExpectedException;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;

import java.util.List;

public class LocalCacheRepo extends JcrRepo {
    private static final Logger LOGGER = Logger.getLogger(LocalCacheRepo.class);

    public static final String PATH_SUFFIX = "-cache";

    private RemoteRepo remoteRepo;

    public LocalCacheRepo(RemoteRepo remoteRepo, CentralConfig cc) {
        assert remoteRepo != null;
        this.remoteRepo = remoteRepo;
        setDescription(remoteRepo.getDescription() + " (local file cache)");
        setKey(remoteRepo.getKey() + PATH_SUFFIX);
        init(cc);
    }

    @Override
    public RepoResource getInfo(final String path) throws FileExpectedException {
        RepoResource repoResource = super.getInfo(path);
        if (repoResource.isFound()) {
            //Check that the item has not expired yet
            boolean expired = isExpired(repoResource);
            if (expired) {
                //Return not found
                repoResource = new UnfoundRepoResource(path, this);
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

    @Override
    public void setMaxUniqueSnapshots(int maxUniqueSnapshots) {
        throw new UnsupportedOperationException(
                "Cannot set maxUniqueSnapshots directly on repo cache.");
    }

    public void undeploy(String path) {
        //Undeploy all nodes recursively (need to clear the caches for each removed item)
        int itemsEffected = processNodesCleanup(path, true);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Removed '" + path + "' from local cache: " + itemsEffected +
                    " items effected.");
        }
    }

    public RemoteRepo getRemoteRepo() {
        return remoteRepo;
    }

    public void unexpire(final String path) {
        //Reset the resource age so it is kept being cached
        JcrFile file = (JcrFile) getFsItem(path);
        file.setLastUpdatedTime(System.currentTimeMillis());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unexpired '" + path + "' from local cache '" + getKey() + "'.");
        }
    }

    public void expire(String path) {
        //Zap all nodes recursively from all retrieval caches
        int itemsEffected = processNodesCleanup(path, false);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Zapped '" + path + "' from local cache: " + itemsEffected +
                    " items effected.");
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private int processNodesCleanup(final String path, final boolean remove) {
        JcrFsItem fsItem = getFsItem(path);
        int itemsEffected = expireNode(fsItem, 0, remove);
        //Only remove the top-most folder to avoid jcr pending changes errors on lock when
        //deleting the folder
        if (remove) {
            delete(path, true);
        }
        return itemsEffected;
    }

    private int expireNode(JcrFsItem fsItem, int itemsCount, boolean partOfRemove) {
        if (fsItem.isFolder()) {
            List<JcrFsItem> list = ((JcrFolder) fsItem).getItems();
            for (JcrFsItem item : list) {
                itemsCount = expireNode(item, itemsCount, partOfRemove);
            }
        } else {
            //Remove each from remote repo caches
            String path = fsItem.relPath();
            remoteRepo.removeFromCaches(path);
            if (!partOfRemove) {
                //Effectively force expiry on the file by changing it's lastUpdated time
                JcrFile file = (JcrFile) fsItem;
                long retrievalCahePeriodMillis =
                        remoteRepo.getRetrievalCachePeriodSecs() * 1000;
                file.setLastUpdatedTime(System.currentTimeMillis() - retrievalCahePeriodMillis);
            }
        }
        return ++itemsCount;
    }

    private boolean isExpired(RepoResource repoResource) {
        long retrievalCahePeriodMillis = remoteRepo.getRetrievalCachePeriodSecs() * 1000;
        long age = repoResource.getAge();
        return age > retrievalCahePeriodMillis || age == -1;
    }
}