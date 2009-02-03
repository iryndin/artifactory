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
package org.artifactory.repo.virtual;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoBase;
import org.artifactory.repo.service.InternalRepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class VirtualRepo extends RepoBase<VirtualRepoDescriptor>
        implements Repo<VirtualRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(VirtualRepo.class);

    private OrderedMap<String, LocalRepo> localRepositoriesMap =
            new ListOrderedMap<String, LocalRepo>();
    private OrderedMap<String, RemoteRepo> remoteRepositoriesMap =
            new ListOrderedMap<String, RemoteRepo>();
    private OrderedMap<String, VirtualRepo> virtualRepositoriesMap =
            new ListOrderedMap<String, VirtualRepo>();

    private OrderedMap<String, VirtualRepo> searchableVirtualRepositories =
            new ListOrderedMap<String, VirtualRepo>();
    private OrderedMap<String, LocalRepo> searchableLocalRepositories =
            new ListOrderedMap<String, LocalRepo>();
    private OrderedMap<String, LocalCacheRepo> searchableLocalCacheRepositories =
            new ListOrderedMap<String, LocalCacheRepo>();
    private OrderedMap<String, RemoteRepo> searchableRemoteRepositories =
            new ListOrderedMap<String, RemoteRepo>();

    protected VirtualRepo(InternalRepositoryService repositoryService) {
        super(repositoryService);
    }

    public VirtualRepo(InternalRepositoryService repositoryService,
            VirtualRepoDescriptor descriptor) {
        this(repositoryService);
        setDescriptor(descriptor);
    }

    /**
     * Special ctor for the default global repo
     */
    public VirtualRepo(InternalRepositoryService service, VirtualRepoDescriptor descriptor,
            OrderedMap<String, LocalRepo> localRepositoriesMap,
            OrderedMap<String, RemoteRepo> remoteRepositoriesMap) {
        this(service);
        this.localRepositoriesMap = localRepositoriesMap;
        this.remoteRepositoriesMap = remoteRepositoriesMap;
        setDescriptor(descriptor);
    }

    /**
     * Must be called after all repositories were built because we save references to other repositories.
     */
    @SuppressWarnings({"unchecked"})
    public void init() {
        //Split the repositories into local, remote and virtual
        List<RepoDescriptor> repositories = getDescriptor().getRepositories();
        for (RepoDescriptor repoDescriptor : repositories) {
            String key = repoDescriptor.getKey();
            Repo repo = getRepositoryService().nonCacheRepositoryByKey(key);
            if (repoDescriptor.isReal()) {
                RealRepoDescriptor realRepoDescriptor = (RealRepoDescriptor) repoDescriptor;
                if (realRepoDescriptor.isLocal()) {
                    localRepositoriesMap.put(key, (LocalRepo) repo);
                } else {
                    remoteRepositoriesMap.put(key, (RemoteRepo) repo);
                }
            } else {
                // it is a virtual repository
                virtualRepositoriesMap.put(key, (VirtualRepo) repo);
            }
        }
    }

    /**
     * Another init method to assemble the search repositories. Must be called after the init() method!
     */
    public void initSearchRepositoryLists() {
        deeplyAssembleSearchRepositoryLists(
                searchableVirtualRepositories,
                searchableLocalRepositories,
                searchableLocalCacheRepositories,
                searchableRemoteRepositories);
    }

    public List<RealRepo> getLocalAndRemoteRepositories() {
        List<RealRepo> repos = new ArrayList<RealRepo>();
        repos.addAll(localRepositoriesMap.values());
        repos.addAll(remoteRepositoriesMap.values());
        return repos;
    }

    public List<VirtualRepo> getVirtualRepositories() {
        return new ArrayList<VirtualRepo>(virtualRepositoriesMap.values());
    }

    public List<RemoteRepo> getRemoteRepositories() {
        return new ArrayList<RemoteRepo>(remoteRepositoriesMap.values());
    }

    public List<LocalCacheRepo> getLocalCaches() {
        List<LocalCacheRepo> localCaches = new ArrayList<LocalCacheRepo>();
        for (RemoteRepo remoteRepo : remoteRepositoriesMap.values()) {
            if (remoteRepo.isStoreArtifactsLocally()) {
                localCaches.add(remoteRepo.getLocalCacheRepo());
            }
        }
        return localCaches;
    }

    public List<LocalRepo> getLocalRepositories() {
        return new ArrayList<LocalRepo>(localRepositoriesMap.values());
    }

    public List<LocalRepo> getLocalAndCachedRepositories() {
        List<LocalRepo> localRepos = getLocalRepositories();
        List<LocalCacheRepo> localCaches = getLocalCaches();
        List<LocalRepo> repos = new ArrayList<LocalRepo>(localRepos);
        repos.addAll(localCaches);
        return repos;
    }

    public OrderedMap<String, VirtualRepo> getSearchableVirtualRepositories() {
        return searchableVirtualRepositories;
    }

    public OrderedMap<String, LocalRepo> getSearchableLocalRepositories() {
        return searchableLocalRepositories;
    }

    public OrderedMap<String, LocalCacheRepo> getSearchableLocalCacheRepositories() {
        return searchableLocalCacheRepositories;
    }

    public OrderedMap<String, RemoteRepo> getSearchableRemoteRepositories() {
        return searchableRemoteRepositories;
    }

    public boolean isArtifactoryRequestsCanRetrieveRemoteArtifacts() {
        return getDescriptor().isArtifactoryRequestsCanRetrieveRemoteArtifacts();
    }

    public LocalRepo localRepositoryByKey(String key) {
        return localRepositoriesMap.get(key);
    }

    public Repo nonCacheRepositoryByKey(String key) {
        Repo repo = localRepositoryByKey(key);
        if (repo != null) {
            return repo;
        }
        repo = remoteRepositoryByKey(key);
        if (repo != null) {
            return repo;
        }
        return virtualRepositoriesMap.get(key);
    }

    public OrderedMap<String, LocalRepo> getLocalRepositoriesMap() {
        return localRepositoriesMap;
    }

    public OrderedMap<String, RemoteRepo> getRemoteRepositoriesMap() {
        return remoteRepositoriesMap;
    }

    public OrderedMap<String, VirtualRepo> getVirtualRepositoriesMap() {
        return virtualRepositoriesMap;
    }

    /**
     * Gets a local or cache repository by key
     *
     * @param key The key for a cache can either be the remote repository one or the cache one(ends with "-cache")
     * @return
     */
    public LocalRepo localOrCachedRepositoryByKey(String key) {
        LocalRepo localRepo = localRepositoryByKey(key);
        if (localRepo == null) {
            //Try to get cached repositories
            int idx = key.lastIndexOf(LocalCacheRepo.PATH_SUFFIX);
            RemoteRepo remoteRepo;
            //Get the cache either by <remote-repo-name> or by <remote-repo-name>-cache
            if (idx > 1 && idx + LocalCacheRepo.PATH_SUFFIX.length() == key.length()) {
                remoteRepo = remoteRepositoryByKey(key.substring(0, idx));
            } else {
                remoteRepo = remoteRepositoryByKey(key);
            }
            if (remoteRepo != null && remoteRepo.isStoreArtifactsLocally()) {
                localRepo = remoteRepo.getLocalCacheRepo();
            }
        }
        return localRepo;
    }

    public RemoteRepo remoteRepositoryByKey(String key) {
        return remoteRepositoriesMap.get(key);
    }

    public List<VirtualRepoItem> getChildrenDeeply(String path) {
        //Add items from contained virtual repositories
        OrderedMap<String, VirtualRepo> virtualRepos =
                new ListOrderedMap<String, VirtualRepo>();
        //Assemble the virtual repo deep search lists
        deeplyAssembleSearchRepositoryLists(
                virtualRepos, new ListOrderedMap<String, LocalRepo>(),
                new ListOrderedMap<String, LocalCacheRepo>(),
                new ListOrderedMap<String, RemoteRepo>());
        //Add paths from all children virtual repositories
        List<VirtualRepoItem> items = new ArrayList<VirtualRepoItem>();
        for (VirtualRepo repo : virtualRepos.values()) {
            List<VirtualRepoItem> repoItems = repo.getChildren(path);
            items.addAll(repoItems);
        }
        return items;
    }

    public List<VirtualRepoItem> getChildren(String path) {
        //Collect the items under the virtual directory viewed from all local repositories
        List<LocalRepo> repoList = getLocalAndCachedRepositories();
        SortedMap<String, VirtualRepoItem> children = new TreeMap<String, VirtualRepoItem>();
        for (final LocalRepo repo : repoList) {
            if (!repo.itemExists(path)) {
                continue;
            }
            JcrFolder dir = (JcrFolder) repo.getJcrFsItem(path);
            List<JcrFsItem> items = dir.getItems();
            for (JcrFsItem item : items) {
                String itemPath = item.getRelativePath();
                VirtualRepoItem repoItem = new VirtualRepoItem(item.getInfo());
                //Check if we already have this item
                if (!children.containsKey(itemPath)) {
                    //Initialize
                    children.put(itemPath, repoItem);
                } else {
                    repoItem = children.get(itemPath);
                }
                //Add the current repo to the list of repositories
                String key = repo.getKey();
                repoItem.getRepoKeys().add(key);
            }
        }
        List<VirtualRepoItem> items = new ArrayList<VirtualRepoItem>(children.values());
        return items;
    }

    private void deeplyAssembleSearchRepositoryLists(
            OrderedMap<String, VirtualRepo> searchableVirtualRepositories,
            OrderedMap<String, LocalRepo> searchableLocalRepositories,
            OrderedMap<String, LocalCacheRepo> searchableLocalCacheRepositories,
            OrderedMap<String, RemoteRepo> searchableRemoteRepositories) {
        searchableVirtualRepositories.put(getKey(), this);
        //Add its local repositories
        searchableLocalRepositories.putAll(getLocalRepositoriesMap());
        //Add the caches
        List<LocalCacheRepo> allCaches = getLocalCaches();
        for (LocalCacheRepo cache : allCaches) {
            searchableLocalCacheRepositories.put(cache.getKey(), cache);
        }
        //Add the remote repositories
        searchableRemoteRepositories.putAll(getRemoteRepositoriesMap());
        //Add any contained virtual repo
        List<VirtualRepo> childrenVirtualRepos = getVirtualRepositories();
        //Avoid infinite loop - stop if already processed virtual repo is encountered
        for (VirtualRepo childVirtualRepo : childrenVirtualRepos) {
            String key = childVirtualRepo.getKey();
            if (searchableVirtualRepositories.get(key) != null) {
                String virtualRepoKeys = "";
                List<String> list = new ArrayList<String>(searchableVirtualRepositories.keySet());
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    virtualRepoKeys += "'" + list.get(i) + "'";
                    if (i < size - 1) {
                        virtualRepoKeys += ", ";
                    }
                }
                log.warn(
                        "Repositories list assembly has been truncated to avoid recursive loop " +
                                "on the virtual repo '" + key +
                                "'. Already processed virtual repositories: " + virtualRepoKeys +
                                ".");
                return;
            } else {
                childVirtualRepo.deeplyAssembleSearchRepositoryLists(
                        searchableVirtualRepositories,
                        searchableLocalRepositories,
                        searchableLocalCacheRepositories,
                        searchableRemoteRepositories);
            }
        }
    }
}
