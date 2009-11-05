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

package org.artifactory.repo.virtual;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyIgnoreAndGenerate;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoBase;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.jcr.StoringRepoMixin;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;
import org.slf4j.Logger;

import javax.jcr.Node;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class VirtualRepo extends RepoBase<VirtualRepoDescriptor> implements StoringRepo<VirtualRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(VirtualRepo.class);

    private OrderedMap<String, LocalRepo> localRepositoriesMap = newOrderedMap();
    private OrderedMap<String, RemoteRepo> remoteRepositoriesMap = newOrderedMap();
    private OrderedMap<String, LocalCacheRepo> localCacheRepositoriesMap = newOrderedMap();
    private OrderedMap<String, VirtualRepo> virtualRepositoriesMap = newOrderedMap();

    private OrderedMap<String, VirtualRepo> searchableVirtualRepositories = newOrderedMap();
    private OrderedMap<String, LocalRepo> searchableLocalRepositories = newOrderedMap();
    private OrderedMap<String, LocalCacheRepo> searchableLocalCacheRepositories = newOrderedMap();
    private OrderedMap<String, RemoteRepo> searchableRemoteRepositories = newOrderedMap();

    StoringRepo<VirtualRepoDescriptor> storageMixin = new StoringRepoMixin<VirtualRepoDescriptor>(this);

    //Use a final policy that always generates checksums
    private final ChecksumPolicy defaultChecksumPolicy = new ChecksumPolicyIgnoreAndGenerate();
    protected VirtualRepoDownloadStrategy downloadStrategy = new VirtualRepoDownloadStrategy(this);

    public VirtualRepo(InternalRepositoryService repositoryService, VirtualRepoDescriptor descriptor) {
        super(repositoryService);
        setDescriptor(descriptor);
    }

    /**
     * Special ctor for the default global repo
     */
    public VirtualRepo(InternalRepositoryService service, VirtualRepoDescriptor descriptor,
            OrderedMap<String, LocalRepo> localRepositoriesMap,
            OrderedMap<String, RemoteRepo> remoteRepositoriesMap) {
        super(service);
        this.localRepositoriesMap = localRepositoriesMap;
        this.remoteRepositoriesMap = remoteRepositoriesMap;
        for (RemoteRepo remoteRepo : remoteRepositoriesMap.values()) {
            if (remoteRepo.isStoreArtifactsLocally()) {
                LocalCacheRepo localCacheRepo = remoteRepo.getLocalCacheRepo();
                localCacheRepositoriesMap.put(localCacheRepo.getKey(), localCacheRepo);
            }
        }
        setDescriptor(descriptor);
    }

    /**
     * Must be called after all repositories were built because we save references to other repositories.
     */
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
                    RemoteRepo remoteRepo = (RemoteRepo) repo;
                    remoteRepositoriesMap.put(key, remoteRepo);
                    if (remoteRepo.isStoreArtifactsLocally()) {
                        LocalCacheRepo localCacheRepo = remoteRepo.getLocalCacheRepo();
                        localCacheRepositoriesMap.put(localCacheRepo.getKey(), localCacheRepo);
                    }
                }
            } else {
                // it is a virtual repository
                virtualRepositoriesMap.put(key, (VirtualRepo) repo);
            }
        }
        initStorage();
    }

    public void initStorage() {
        storageMixin.init();
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
        return new ArrayList<LocalCacheRepo>(localCacheRepositoriesMap.values());
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

    /**
     * Returns a local non-cache repository by key
     *
     * @param key The repository key
     * @return Local non-cahce repository or null if not found
     */
    public LocalRepo localRepositoryByKey(String key) {
        return localRepositoriesMap.get(key);
    }

    /**
     * Returns a non-cached repository by repo key. Non cached might be non-cached local repo, remote repo or virtual.
     *
     * @param key The repository key
     * @return Non-cache repository or null if not found
     */
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

    public OrderedMap<String, LocalCacheRepo> getLocalCacheRepositoriesMap() {
        return localCacheRepositoriesMap;
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
            JcrFolder dir = (JcrFolder) repo.getLocalJcrFsItem(path);
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
        return new ArrayList<VirtualRepoItem>(children.values());
    }

    /**
     * Inidicates if the given path exists
     *
     * @param relPath Relative path to item
     * @return True if repo path exists, false if not
     */
    public boolean virtualItemExists(String relPath) {

        //Check if item exists in each repo
        List<LocalRepo> repoList = getLocalAndCachedRepositories();
        for (LocalRepo localRepo : repoList) {
            if (localRepo.itemExists(relPath)) {
                return true;
            }
        }

        return false;
    }

    public boolean isLocal() {
        return false;
    }

    public boolean isCache() {
        return false;
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
        searchableLocalCacheRepositories.putAll(getLocalCacheRepositoriesMap());
        //Add the remote repositories
        searchableRemoteRepositories.putAll(getRemoteRepositoriesMap());
        //Add any contained virtual repo
        List<VirtualRepo> childrenVirtualRepos = getVirtualRepositories();
        //Avoid infinite loop - stop if already processed virtual repo is encountered
        for (VirtualRepo childVirtualRepo : childrenVirtualRepos) {
            String key = childVirtualRepo.getKey();
            if (searchableVirtualRepositories.get(key) != null) {
                log.warn("Repositories list assembly has been truncated to avoid recursive loop " +
                        "on the virtual repo '{}'. Already processed virtual repositories: {}.",
                        key, searchableVirtualRepositories.keySet());
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

    private <K, V> OrderedMap<K, V> newOrderedMap() {
        return new ListOrderedMap<K, V>();
    }

    //STORING REPO MIXIN

    @Override
    public void setDescriptor(VirtualRepoDescriptor descriptor) {
        super.setDescriptor(descriptor);
        storageMixin.setDescriptor(descriptor);
    }

    public ChecksumPolicy getChecksumPolicy() {
        return defaultChecksumPolicy;
    }

    public JcrFolder getRootFolder() {
        return storageMixin.getRootFolder();
    }

    public JcrFolder getLockedRootFolder() {
        return storageMixin.getLockedRootFolder();
    }

    public String getRepoRootPath() {
        return storageMixin.getRepoRootPath();
    }

    public void undeploy(RepoPath repoPath) {
        storageMixin.undeploy(repoPath);
    }

    public RepoResource saveResource(RepoResource res, final InputStream in, Properties keyvals) throws IOException {
        return storageMixin.saveResource(res, in, keyvals);
    }

    public boolean shouldProtectPathDeletion(String path, boolean assertOverwrite) {
        return storageMixin.shouldProtectPathDeletion(path, assertOverwrite);
    }

    public boolean itemExists(String relPath) {
        return storageMixin.itemExists(relPath);
    }

    public List<String> getChildrenNames(String relPath) {
        return storageMixin.getChildrenNames(relPath);
    }

    public void onDelete(JcrFsItem fsItem) {
        storageMixin.onDelete(fsItem);
    }

    public void onCreate(JcrFsItem fsItem) {
        storageMixin.onCreate(fsItem);
    }

    public void updateCache(JcrFsItem fsItem) {
        storageMixin.updateCache(fsItem);
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getJcrFsItem(RepoPath repoPath) {
        return storageMixin.getJcrFsItem(repoPath);
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getJcrFsItem(Node node) {
        return storageMixin.getJcrFsItem(node);
    }

    public JcrFile getJcrFile(RepoPath repoPath) throws FileExpectedException {
        return storageMixin.getJcrFile(repoPath);
    }

    public JcrFolder getJcrFolder(RepoPath repoPath) throws FolderExpectedException {
        return storageMixin.getJcrFolder(repoPath);
    }

    public JcrFsItem getLockedJcrFsItem(RepoPath repoPath) {
        return storageMixin.getLockedJcrFsItem(repoPath);
    }

    public JcrFsItem getLockedJcrFsItem(Node node) {
        return storageMixin.getLockedJcrFsItem(node);
    }

    public JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException {
        return storageMixin.getLockedJcrFile(repoPath, createIfMissing);
    }

    public JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException {
        return storageMixin.getLockedJcrFolder(repoPath, createIfMissing);
    }

    public RepoResource getInfo(RequestContext context) throws FileExpectedException {
        return downloadStrategy.getInfo(context);
    }

    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
        return storageMixin.getResourceStreamHandle(res);
    }

    public String getChecksum(String checksumFilePath, RepoResource res) throws IOException {
        return storageMixin.getChecksum(checksumFilePath, res);
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getLocalJcrFsItem(String relPath) {
        return storageMixin.getLocalJcrFsItem(relPath);
    }

    public JcrFsItem getLockedJcrFsItem(String relPath) {
        return storageMixin.getLockedJcrFsItem(relPath);
    }

    public JcrFile getLocalJcrFile(String relPath) throws FileExpectedException {
        return storageMixin.getLocalJcrFile(relPath);
    }

    public JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException {
        return storageMixin.getLockedJcrFile(relPath, createIfMissing);
    }

    public JcrFolder getLocalJcrFolder(String relPath) throws FolderExpectedException {
        return storageMixin.getLocalJcrFolder(relPath);
    }

    public JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException {
        return storageMixin.getLockedJcrFolder(relPath, createIfMissing);
    }
}
