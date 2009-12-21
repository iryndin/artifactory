/*
 * This file is part of Artifactory.
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

package org.artifactory.repo.service;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.RepoResource;
import org.artifactory.spring.ReloadableBean;

import javax.jcr.Repository;
import java.io.IOException;
import java.util.List;

/**
 * User: freds Date: Jul 31, 2008 Time: 5:50:18 PM
 */
public interface InternalRepositoryService extends RepositoryService, ReloadableBean {

    boolean isAnonAccessEnabled();

    /**
     * @param key The repository key
     * @return Repository with the exact given key (no special meaning for remote/cache repo keys). Null if not found.
     */
    Repo repositoryByKey(String key);

    VirtualRepo virtualRepositoryByKey(String key);

    LocalRepo localOrCachedRepositoryByKey(String key);

    /**
     * Get a repository that can store artifacts. Will get either a local repo (regular or cache) or a virtual repo. If
     * remote repository key is given, the cache repository is returned.
     */
    StoringRepo storingRepositoryByKey(String key);

    List<RealRepo> getLocalAndRemoteRepositories();

    List<VirtualRepo> getVirtualRepositories();

    List<LocalRepo> getLocalAndCachedRepositories();

    /**
     * Returns a local non-cache repository by key
     *
     * @param key The repository key
     * @return Local non-cahce repository or null if not found
     */
    LocalRepo localRepositoryByKey(String key);

    RemoteRepo remoteRepositoryByKey(String key);

    Repo nonCacheRepositoryByKey(String key);

    StatusHolder assertValidPath(RealRepo repo, String path);

    @Lock(transactional = true)
    StatusHolder assertValidDeployPath(LocalRepo repo, String path);

    @Lock(transactional = true)
    void rebuildRepositories();

    @Lock(transactional = true)
    <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(
            RemoteRepo<T> remoteRepo, RepoResource res) throws IOException;

    @Lock(transactional = true)
    RepoResource unexpireIfExists(LocalRepo localCacheRepo, String path);

    @Lock(transactional = true)
    ResourceStreamHandle unexpireAndRetrieveIfExists(LocalRepo localCacheRepo, String path) throws IOException;

    @Lock(transactional = true)
    ResourceStreamHandle getResourceStreamHandle(Repo repo, RepoResource res) throws IOException, RepoAccessException;

    @Lock(transactional = true)
    void exportTo(ExportSettings settings);

    List<StoringRepo> getStoringRepositories();

    /**
     * Executes the maven metadata calculator on all the folders marked with the maven metadata recalculation flag. This
     * method is internal to the repository service and should execute during startup to make sure there are no folders
     * that the maven metadata wasn't recalculated on them (the recalculation might execute in metadata and interrupted
     * in the middle)
     */
    @Async(delayUntilAfterCommit = true, transactional = true)
    void recalculateMavenMetadataOnMarkedFolders();

    /**
     * Removes a mark for maven metadata recalculation if such exists.
     *
     * @param basePath Repo path to remove the mark from. Must be a local non-cache repository path.
     */
    @Lock(transactional = true)
    void removeMarkForMavenMetadataRecalculation(RepoPath basePath);

    @Async(delayUntilAfterCommit = true, transactional = true)
    void updateStats(RepoPath repoPath);

    Repository getJcrHandle();

    @Lock(transactional = true)
    void reload(CentralConfigDescriptor oldDescriptor);
}
