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
import org.apache.commons.collections15.set.ListOrderedSet;
import org.apache.log4j.Logger;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoBase;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@XmlType(name = "VirtualRepoType", propOrder = {"key", "repositories"})
public class VirtualRepo implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(VirtualRepo.class);

    private static final long serialVersionUID = 1L;

    public static final String GLOBAL_VIRTUAL_REPO_KEY = "repo";

    private String key;
    @XmlTransient
    private OrderedMap<String, LocalRepo> localRepositoriesMap =
            new ListOrderedMap<String, LocalRepo>();
    @XmlTransient
    private OrderedMap<String, RemoteRepo> remoteRepositoriesMap =
            new ListOrderedMap<String, RemoteRepo>();

    public VirtualRepo() {
    }

    public VirtualRepo(String key, OrderedMap<String, LocalRepo> localRepositoriesMap,
            OrderedMap<String, RemoteRepo> remoteRepositoriesMap) {
        this.key = key;
        this.localRepositoriesMap = localRepositoriesMap;
        this.remoteRepositoriesMap = remoteRepositoriesMap;
    }

    @XmlID
    @XmlElement(required = true)
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlIDREF
    @XmlElementWrapper(name = "repositories")
    @XmlElement(name = "repositoryRef", type = RepoBase.class, namespace = CentralConfig.NS)
    public Set<Repo> getRepositories() {
        return getLocalAndRemoteRepositories();
    }

    public List<Repo> getRepositoriesList() {
        ArrayList<Repo> list = new ArrayList<Repo>();
        list.addAll(getRepositories());
        return list;
    }

    public void setRepositories(Set<Repo> repositories) {
        //Split the repositories into local and remote
        for (Repo repo : repositories) {
            String repoKey = repo.getKey();
            if (repo.isLocal()) {
                LocalRepo localRepo = localRepositoriesMap.put(repoKey, (LocalRepo) repo);
                //Test for repositories with the same key
                if (localRepo != null) {
                    //Throw an error since jaxb swallows exceptions
                    throw new Error(
                            "Duplicate local repository key in virtual repository configuration: "
                                    + key + ".");
                }
            } else {
                RemoteRepo remoteRepo = remoteRepositoriesMap.put(repoKey, (RemoteRepo) repo);
                //Test for repositories with the same key
                if (remoteRepo != null) {
                    //Throw an error since jaxb swallows exceptions
                    throw new Error(
                            "Duplicate remote repository key in virtual repository configuration: "
                                    + key + ".");
                }
            }
        }
    }

    public synchronized Set<Repo> getLocalAndRemoteRepositories() {
        ListOrderedSet<Repo> repos = new ListOrderedSet<Repo>();
        repos.addAll(localRepositoriesMap.values());
        repos.addAll(remoteRepositoriesMap.values());
        return repos;
    }

    public synchronized List<RemoteRepo> getRemoteRepositories() {
        return new ArrayList<RemoteRepo>(remoteRepositoriesMap.values());
    }

    public synchronized List<LocalCacheRepo> getLocalCaches() {
        List<LocalCacheRepo> localCaches = new ArrayList<LocalCacheRepo>();
        for (RemoteRepo repo : remoteRepositoriesMap.values()) {
            if (repo.isStoreArtifactsLocally()) {
                localCaches.add(repo.getLocalCacheRepo());
            }
        }
        return localCaches;
    }

    public synchronized List<LocalRepo> getLocalRepositories() {
        return new ArrayList<LocalRepo>(localRepositoriesMap.values());
    }

    public synchronized List<LocalRepo> getLocalAndCachedRepositories() {
        List<LocalRepo> localRepos = getLocalRepositories();
        List<LocalCacheRepo> localCaches = getLocalCaches();
        List<LocalRepo> repos = new ArrayList<LocalRepo>(localRepos);
        repos.addAll(localCaches);
        return repos;
    }

    public LocalRepo localRepositoryByKey(String key) {
        return localRepositoriesMap.get(key);
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
            RemoteRepo remoteRepo = null;
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
}