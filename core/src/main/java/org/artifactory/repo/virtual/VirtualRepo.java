/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.RepoRejectionException;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyIgnoreAndGenerate;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataDefinition;
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
import org.artifactory.repo.virtual.interceptor.PomInterceptor;
import org.artifactory.resource.RepoResource;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VirtualRepo extends RepoBase<VirtualRepoDescriptor> implements StoringRepo<VirtualRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(VirtualRepo.class);

    private OrderedMap<String, LocalRepo> localRepositoriesMap = newOrderedMap();
    private OrderedMap<String, RemoteRepo> remoteRepositoriesMap = newOrderedMap();
    private OrderedMap<String, LocalCacheRepo> localCacheRepositoriesMap = newOrderedMap();
    private OrderedMap<String, VirtualRepo> virtualRepositoriesMap = newOrderedMap();

    StoringRepo<VirtualRepoDescriptor> storageMixin = new StoringRepoMixin<VirtualRepoDescriptor>(this, null);

    //Use a final policy that always generates checksums
    private final ChecksumPolicy defaultChecksumPolicy = new ChecksumPolicyIgnoreAndGenerate();
    protected VirtualRepoDownloadStrategy downloadStrategy = new VirtualRepoDownloadStrategy(this);
    private PomInterceptor pomInterceptor;

    public VirtualRepo(InternalRepositoryService repositoryService, VirtualRepoDescriptor descriptor) {
        super(repositoryService);
        setDescriptor(descriptor);
        pomInterceptor = ContextHelper.get().beanForType(PomInterceptor.class);
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

    public VirtualRepoItem getVirtualRepoItem(RepoPath repoPath) {
        Set<LocalRepo> localAndCahcedRepos = getResolvedLocalAndCachedRepos();
        //Add paths from all children virtual repositories
        VirtualRepoItem item = null;
        for (LocalRepo repo : localAndCahcedRepos) {
            if (repo.itemExists(repoPath.getPath())) {
                JcrFsItem fsItem = repo.getLocalJcrFsItem(repoPath.getPath());
                if (item == null) {
                    // use the item info from the first found item
                    item = new VirtualRepoItem(fsItem.getInfo());
                }
                item.addRepoKey(repo.getKey());
            }
        }
        return item;
    }

    public List<RealRepo> getSearchRepositoriesList() {
        List<RealRepo> localAndRemoteRepos = Lists.newArrayList();
        localAndRemoteRepos.addAll(getResolvedLocalRepos());
        localAndRemoteRepos.addAll(getResolvedRemoteRepos());
        return localAndRemoteRepos;
    }

    public Set<String> getChildrenNamesDeeply(RepoPath folderPath) {
        String path = folderPath.getPath();
        Set<LocalRepo> localAndCahcedRepos = getResolvedLocalAndCachedRepos();
        Set<String> children = Sets.newHashSet();
        for (LocalRepo repo : localAndCahcedRepos) {
            if (!repo.itemExists(path)) {
                continue;
            }
            JcrFsItem fsItem = repo.getLocalJcrFsItem(path);
            if (!fsItem.isFolder()) {
                log.warn("Expected folder but got file: {}", new RepoPath(repo.getKey(), path));
                continue;
            }
            JcrFolder dir = (JcrFolder) fsItem;
            List<JcrFsItem> items = dir.getItems();

            for (JcrFsItem item : items) {
                children.add(item.getName());
            }
        }
        return children;
    }

    private List<VirtualRepo> getResolvedVirtualRepos() {
        //Add items from contained virtual repositories
        List<VirtualRepo> virtualRepos = new ArrayList<VirtualRepo>();
        //Assemble the virtual repo deep search lists
        resolveVirtualRepos(virtualRepos);
        return virtualRepos;
    }

    private void resolveVirtualRepos(List<VirtualRepo> repos) {
        if (repos.contains(this)) {
            return;
        }
        repos.add(this);
        List<VirtualRepo> childrenVirtualRepos = getVirtualRepositories();
        for (VirtualRepo childVirtualRepo : childrenVirtualRepos) {
            if (!repos.contains(childVirtualRepo)) {
                childVirtualRepo.resolveVirtualRepos(repos);
            }
        }
    }

    public Set<RemoteRepo> getResolvedRemoteRepos() {
        Set<RemoteRepo> resolvedRemoteRepos = Sets.newLinkedHashSet();
        for (VirtualRepo vrepo : getResolvedVirtualRepos()) {
            for (RemoteRepo rrepo : vrepo.getRemoteRepositories()) {
                resolvedRemoteRepos.add(rrepo);
            }
        }
        return resolvedRemoteRepos;
    }

    /**
     * @return Recursively resolved list of all the local non-cache repos of this virtual repo and nested virtual
     *         repos.
     */
    public Set<LocalRepo> getResolvedLocalRepos() {
        Set<LocalRepo> resolvedLocalRepos = Sets.newLinkedHashSet();
        for (VirtualRepo repo : getResolvedVirtualRepos()) {
            for (LocalRepo localRepo : repo.getLocalRepositories()) {
                resolvedLocalRepos.add(localRepo);
            }
        }
        return resolvedLocalRepos;
    }

    /**
     * @return Recursively resolved list of all the cache repos of this virtual repo and nested virtual repos.
     */
    public Set<LocalCacheRepo> getResolvedLocalCachedRepos() {
        Set<LocalCacheRepo> resolvedLocalRepos = Sets.newLinkedHashSet();
        for (VirtualRepo repo : getResolvedVirtualRepos()) {
            for (LocalCacheRepo localRepo : repo.getLocalCaches()) {
                resolvedLocalRepos.add(localRepo);
            }
        }
        return resolvedLocalRepos;
    }

    /**
     * @return Recursively resolved list of all the local and cache repos of this virtual repo and nested virtual
     *         repos.
     */
    private Set<LocalRepo> getResolvedLocalAndCachedRepos() {
        Set<LocalRepo> localAndCahcedRepos = Sets.newLinkedHashSet();
        localAndCahcedRepos.addAll(getResolvedLocalRepos());
        localAndCahcedRepos.addAll(getResolvedLocalCachedRepos());
        return localAndCahcedRepos;
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

    private <K, V> OrderedMap<K, V> newOrderedMap() {
        return new ListOrderedMap<K, V>();
    }

    /**
     * This method is called when a resource was found in the searchable repositories, before returning it to the
     * client. This method will call a list of interceptors that might alter the returned resource and cache it
     * locally.
     *
     * @param context       The request context
     * @param foundResource The resource that was found in the searchable repositories.
     * @return Original or transformed resource
     */
    protected RepoResource interceptBeforeReturn(RequestContext context, RepoResource foundResource) {
        if (pomInterceptor != null && MavenNaming.isPom(context.getResourcePath())) {
            foundResource = pomInterceptor.onBeforeReturn(this, context, foundResource);
        }
        return foundResource;
    }


    public PomCleanupPolicy getPomRepositoryReferencesCleanupPolicy() {
        return getDescriptor().getPomRepositoryReferencesCleanupPolicy();
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
        undeploy(repoPath, false);
    }

    public void undeploy(RepoPath repoPath, boolean calcMavenMetadata) {
        storageMixin.undeploy(repoPath, calcMavenMetadata);
    }

    public RepoResource saveResource(RepoResource res, final InputStream in, Properties keyvals) throws IOException,
            RepoRejectionException {
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

    public ResourceStreamHandle getResourceStreamHandle(RepoResource res) throws IOException, RepositoryException,
            RepoRejectionException {
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

    public MetadataDefinition<FileInfo> getFileInfoMd() {
        return storageMixin.getFileInfoMd();
    }

    public MetadataDefinition<FolderInfo> getFolderInfoMd() {
        return storageMixin.getFolderInfoMd();
    }

    public boolean isWriteLocked(RepoPath path) {
        return storageMixin.isWriteLocked(path);
    }

    public StoringRepo<VirtualRepoDescriptor> getStorageMixin() {
        return storageMixin;
    }

    public <T> MetadataDefinition<T> getMetadataDefinition(Class<T> clazz) {
        return storageMixin.getMetadataDefinition(clazz);
    }

    public MetadataDefinition getMetadataDefinition(String metadataName, boolean createIfEmpty) {
        return storageMixin.getMetadataDefinition(metadataName, createIfEmpty);
    }

    public Set<MetadataDefinition<?>> getAllMetadataDefinitions(boolean includeInternal) {
        return storageMixin.getAllMetadataDefinitions(includeInternal);
    }
}
