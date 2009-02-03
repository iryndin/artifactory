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
import org.apache.log4j.Logger;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoBase;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@XmlType(name = "VirtualRepoType",
        propOrder = {"artifactoryRequestsCanRetrieveRemoteArtifacts", "repositories"})
@XmlAccessorType(XmlAccessType.PROPERTY)
public class VirtualRepo extends RepoBase implements Repo {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepo.class);

    private static final long serialVersionUID = 1L;

    public static final String GLOBAL_VIRTUAL_REPO_KEY = "repo";

    /*@XmlElement(name = "repositories")
    @XmlJavaTypeAdapter(RepositoriesListAdapter.class)*/
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "UnusedDeclaration"})
    @XmlIDREF
    @XmlElementWrapper(name = "repositories")
    @XmlElement(name = "repositoryRef", type = RepoBase.class, required = false)
    //JAXB only field
    private List<Repo> repositories;

    private boolean artifactoryRequestsCanRetrieveRemoteArtifacts;

    @XmlTransient
    private OrderedMap<String, LocalRepo> localRepositoriesMap =
            new ListOrderedMap<String, LocalRepo>();
    @XmlTransient
    private OrderedMap<String, RemoteRepo> remoteRepositoriesMap =
            new ListOrderedMap<String, RemoteRepo>();
    @XmlTransient
    private OrderedMap<String, VirtualRepo> virtualRepositoriesMap =
            new ListOrderedMap<String, VirtualRepo>();

    public VirtualRepo() {
    }

    /**
     * Used for the global virtual repo
     *
     * @param key
     * @param localRepositoriesMap
     * @param remoteRepositoriesMap
     */
    public VirtualRepo(String key, OrderedMap<String, LocalRepo> localRepositoriesMap,
                       OrderedMap<String, RemoteRepo> remoteRepositoriesMap) {
        setKey(key);
        this.localRepositoriesMap = localRepositoriesMap;
        this.remoteRepositoriesMap = remoteRepositoriesMap;
    }

    public boolean isReal() {
        return false;
    }

    public void init() {
        //Split the repositories into local and remote
        for (Repo repo : repositories) {
            String repoKey = repo.getKey();
            if (repo.isReal()) {
                RealRepo realRepo = (RealRepo) repo;
                if (realRepo.isLocal()) {
                    LocalRepo localRepo = localRepositoriesMap.put(repoKey, (LocalRepo) realRepo);
                    //Test for repositories with the same key
                    if (localRepo != null) {
                        //Throw an error since jaxb swallows exceptions
                        throw new Error(
                                "Duplicate local repository key " + repoKey +
                                        " in virtual repository configuration: " + getKey() + ".");
                    }
                } else {
                    RemoteRepo remoteRepo =
                            remoteRepositoriesMap.put(repoKey, (RemoteRepo) realRepo);
                    //Test for repositories with the same key
                    if (remoteRepo != null) {
                        //Throw an error since jaxb swallows exceptions
                        throw new Error(
                                "Duplicate virtual remote key " + repoKey +
                                        " in virtual repository configuration: " + getKey() + ".");
                    }
                }
            } else {
                VirtualRepo virtualRepo = virtualRepositoriesMap.put(repoKey, (VirtualRepo) repo);
                //Test for repositories with the same key
                if (virtualRepo != null) {
                    //Throw an error since jaxb swallows exceptions
                    throw new Error(
                            "Duplicate virtual repository key " + repoKey +
                                    " in virtual repository configuration: " + getKey() + ".");
                }
            }
        }
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
        for (RemoteRepo repo : remoteRepositoriesMap.values()) {
            if (repo.isStoreArtifactsLocally()) {
                localCaches.add(repo.getLocalCacheRepo());
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

    @XmlElement(defaultValue = "false", required = false)
    public boolean isArtifactoryRequestsCanRetrieveRemoteArtifacts() {
        return artifactoryRequestsCanRetrieveRemoteArtifacts;
    }

    public void setArtifactoryRequestsCanRetrieveRemoteArtifacts(
            boolean artifactoryRequestsCanRetrieveRemoteArtifacts) {
        this.artifactoryRequestsCanRetrieveRemoteArtifacts =
                artifactoryRequestsCanRetrieveRemoteArtifacts;
    }

    //PERF: [by yl] Cache this data locally
    public void deeplyAssembleRepositoryLists(
            OrderedMap<String, VirtualRepo> virtualRepos,
            OrderedMap<String, LocalRepo> localRepos,
            OrderedMap<String, LocalCacheRepo> localCacheRepos,
            OrderedMap<String, RemoteRepo> remoteRepos) {
        virtualRepos.put(getKey(), this);
        //Add its local repositories
        localRepos.putAll(getLocalRepositoriesMap());
        //Add the caches
        List<LocalCacheRepo> allCaches = getLocalCaches();
        for (LocalCacheRepo cache : allCaches) {
            localCacheRepos.put(cache.getKey(), cache);
        }
        //Add the remote repositories
        remoteRepos.putAll(getRemoteRepositoriesMap());
        //Add any contained virtual repo
        List<VirtualRepo> childrenVirtualRepos = getVirtualRepositories();
        //Avoid infinite loop - stop if already processed virtual repo is encountered
        for (VirtualRepo childVirtualRepo : childrenVirtualRepos) {
            String key = childVirtualRepo.getKey();
            if (virtualRepos.get(key) != null) {
                String virtualRepoKeys = "";
                List<String> list = new ArrayList<String>(virtualRepos.keySet());
                int size = list.size();
                for (int i = 0; i < size; i++) {
                    virtualRepoKeys += "'" + list.get(i) + "'";
                    if (i < size - 1) {
                        virtualRepoKeys += ", ";
                    }
                }
                LOGGER.warn(
                        "Repositories list assembly has been truncated to avoid recursive loop " +
                                "on the virtual repo '" + key +
                                "'. Already processed virtual repositories: " + virtualRepoKeys +
                                ".");
                return;
            } else {
                childVirtualRepo.deeplyAssembleRepositoryLists(
                        virtualRepos, localRepos, localCacheRepos, remoteRepos);
            }
        }
    }

    public LocalRepo localRepositoryByKey(String key) {
        return localRepositoriesMap.get(key);
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
     * @param key The key for a cache can either be the remote repository one or the cache one(ends
     *            with "-cache")
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
        deeplyAssembleRepositoryLists(
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

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public List<VirtualRepoItem> getChildren(String path) {
        //Collect the items under the virtual directory viewed from all local repositories
        List<LocalRepo> repoList = getLocalAndCachedRepositories();
        SortedMap<String, VirtualRepoItem> children = new TreeMap<String, VirtualRepoItem>();
        for (final LocalRepo repo : repoList) {
            if (!repo.itemExists(path)) {
                continue;
            }
            JcrFolder dir = (JcrFolder) repo.getFsItem(path);
            List<JcrFsItem> items = dir.getItems();
            for (JcrFsItem item : items) {
                String itemPath = item.getRelativePath();
                VirtualRepoItem repoItem = new VirtualRepoItem(item);
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
}
