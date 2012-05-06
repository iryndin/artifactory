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

package org.artifactory.repo.virtual;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyIgnoreAndGenerate;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoBase;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.SaveResourceContext;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.jcr.StoringRepoMixin;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.interceptor.VirtualRepoInterceptor;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.RequestTraceLogger;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.sapi.fs.VfsFolder;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VirtualRepo extends RepoBase<VirtualRepoDescriptor> implements StoringRepo<VirtualRepoDescriptor> {
    private static final Logger log = LoggerFactory.getLogger(VirtualRepo.class);

    private Map<String, LocalRepo> localRepositoriesMap = Maps.newLinkedHashMap();
    private Map<String, RemoteRepo> remoteRepositoriesMap = Maps.newLinkedHashMap();
    private Map<String, LocalCacheRepo> localCacheRepositoriesMap = Maps.newLinkedHashMap();
    private Map<String, VirtualRepo> virtualRepositoriesMap = Maps.newLinkedHashMap();

    StoringRepo<VirtualRepoDescriptor> storageMixin = new StoringRepoMixin<VirtualRepoDescriptor>(this, null);

    //Use a final policy that always generates checksums
    private final ChecksumPolicy defaultChecksumPolicy = new ChecksumPolicyIgnoreAndGenerate();
    protected VirtualRepoDownloadStrategy downloadStrategy = new VirtualRepoDownloadStrategy(this);

    // List of interceptors for various download resolution points
    private Collection<VirtualRepoInterceptor> interceptors;

    public VirtualRepo(InternalRepositoryService repositoryService, VirtualRepoDescriptor descriptor) {
        super(repositoryService);
        setDescriptor(descriptor);
    }

    /**
     * Special ctor for the default global repo
     */
    public VirtualRepo(InternalRepositoryService service, VirtualRepoDescriptor descriptor,
            Map<String, LocalRepo> localRepositoriesMap,
            Map<String, RemoteRepo> remoteRepositoriesMap) {
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
    @Override
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
        interceptors = ContextHelper.get().beansForType(VirtualRepoInterceptor.class).values();
        initStorage();
    }

    @Override
    public boolean isSuppressPomConsistencyChecks() {
        return false;
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

    public VirtualRepoDownloadStrategy getDownloadStrategy() {
        return downloadStrategy;
    }

    /**
     * Returns a local non-cache repository by key
     *
     * @param key The repository key
     * @return Local non-cache repository or null if not found
     */
    public LocalRepo localRepositoryByKey(String key) {
        return localRepositoriesMap.get(key);
    }

    public Repo repositoryByKey(String key) {
        Repo repo = localOrCachedRepositoryByKey(key);
        if (repo != null) {
            return repo;
        }
        repo = remoteRepositoryByKey(key);
        if (repo != null) {
            return repo;
        }
        return virtualRepositoriesMap.get(key);
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

    public Map<String, LocalRepo> getLocalRepositoriesMap() {
        return localRepositoriesMap;
    }

    public Map<String, LocalCacheRepo> getLocalCacheRepositoriesMap() {
        return localCacheRepositoriesMap;
    }

    public Map<String, RemoteRepo> getRemoteRepositoriesMap() {
        return remoteRepositoriesMap;
    }

    public Map<String, VirtualRepo> getVirtualRepositoriesMap() {
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
            RemoteRepo remoteRepo = remoteRepositoryByRemoteOrCacheKey(key);
            if (remoteRepo != null && remoteRepo.isStoreArtifactsLocally()) {
                localRepo = remoteRepo.getLocalCacheRepo();
            }
        }
        return localRepo;
    }

    /**
     * Gets a local or remote repository by key
     *
     * @param key The key for a remote can either be the remote repository one or the cache one(ends with "-cache")
     */
    public RealRepo localOrRemoteRepositoryByKey(String key) {
        LocalRepo localRepo = localRepositoryByKey(key);
        if (localRepo != null) {
            return localRepo;
        }
        return remoteRepositoryByRemoteOrCacheKey(key);
    }

    private RemoteRepo remoteRepositoryByRemoteOrCacheKey(String key) {
        //Try to get cached repositories
        int idx = key.lastIndexOf(LocalCacheRepo.PATH_SUFFIX);
        RemoteRepo remoteRepo;
        //Get the cache either by <remote-repo-name> or by <remote-repo-name>-cache
        if (idx > 1 && idx + LocalCacheRepo.PATH_SUFFIX.length() == key.length()) {
            remoteRepo = remoteRepositoryByKey(key.substring(0, idx));
        } else {
            remoteRepo = remoteRepositoryByKey(key);
        }
        return remoteRepo;
    }

    public RemoteRepo remoteRepositoryByKey(String key) {
        return remoteRepositoriesMap.get(key);
    }

    public VirtualRepoItem getVirtualRepoItem(RepoPath repoPath) {
        Set<LocalRepo> localAndCachedRepos = getResolvedLocalAndCachedRepos();
        //Add paths from all children virtual repositories
        VirtualRepoItem item = null;
        for (LocalRepo repo : localAndCachedRepos) {
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
        Set<LocalRepo> localAndCachedRepos = getResolvedLocalAndCachedRepos();
        Set<String> children = Sets.newHashSet();
        for (LocalRepo repo : localAndCachedRepos) {
            if (!repo.itemExists(path)) {
                continue;
            }
            JcrFsItem fsItem = repo.getLocalJcrFsItem(path);
            if (!fsItem.isFolder()) {
                log.warn("Expected folder but got file: {}", InternalRepoPathFactory.create(repo.getKey(), path));
                continue;
            }
            JcrFolder dir = (JcrFolder) fsItem;
            List<JcrFsItem> items = dir.getJcrItems();

            for (JcrFsItem item : items) {
                if (!MavenNaming.NEXUS_INDEX_DIR.equals(item.getName()) ||
                        MavenNaming.isIndex(item.getPath())) {  // don't include the index dir in the listing
                    children.add(item.getName());
                }
            }
        }
        return children;
    }

    public List<VirtualRepo> getResolvedVirtualRepos() {
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
    public Set<LocalRepo> getResolvedLocalAndCachedRepos() {
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

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public boolean isCache() {
        return false;
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
    protected RepoResource interceptBeforeReturn(InternalRequestContext context, RepoResource foundResource) {
        for (VirtualRepoInterceptor interceptor : interceptors) {
            RequestTraceLogger.log("Intercepting found resource with '%s'", interceptor.getClass().getSimpleName());
            foundResource = interceptor.onBeforeReturn(this, context, foundResource);
        }
        return foundResource;
    }

    public RepoResource interceptGetInfo(InternalRequestContext context, RepoPath repoPath,
            List<RealRepo> repositories) {
        for (VirtualRepoInterceptor interceptor : interceptors) {
            RequestTraceLogger.log("Intercepting info request with '%s'", interceptor.getClass().getSimpleName());
            RepoResource repoResource = interceptor.interceptGetInfo(this, context, repoPath, repositories);
            if (repoResource != null) {
                RequestTraceLogger.log("Info request was intercepted by '%s'",
                        interceptor.getClass().getSimpleName());
                return repoResource;
            }
        }
        return null;
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

    @Override
    public ChecksumPolicy getChecksumPolicy() {
        return defaultChecksumPolicy;
    }

    @Override
    public VfsFolder getRootFolder() {
        return storageMixin.getRootFolder();
    }

    @Override
    public JcrFolder getLockedRootFolder() {
        return storageMixin.getLockedRootFolder();
    }

    @Override
    public String getRepoRootPath() {
        return storageMixin.getRepoRootPath();
    }

    @Override
    public void undeploy(RepoPath repoPath) {
        undeploy(repoPath, false);
    }

    @Override
    public void undeploy(RepoPath repoPath, boolean calcMavenMetadata) {
        storageMixin.undeploy(repoPath, calcMavenMetadata);
    }

    @Override
    public RepoResource saveResource(SaveResourceContext context) throws IOException, RepoRejectException {
        return storageMixin.saveResource(context);
    }

    @Override
    public boolean shouldProtectPathDeletion(String path, boolean assertOverwrite) {
        return storageMixin.shouldProtectPathDeletion(path, assertOverwrite);
    }

    @Override
    public boolean itemExists(String relPath) {
        return storageMixin.itemExists(relPath);
    }

    @Override
    public List<String> getChildrenNames(String relPath) {
        return storageMixin.getChildrenNames(relPath);
    }

    @Override
    public void onDelete(JcrFsItem fsItem) {
        storageMixin.onDelete(fsItem);
    }

    @Override
    public void onCreate(JcrFsItem fsItem) {
        storageMixin.onCreate(fsItem);
    }

    @Override
    public void updateCache(JcrFsItem fsItem) {
        storageMixin.updateCache(fsItem);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrFsItem getJcrFsItem(RepoPath repoPath) {
        return storageMixin.getJcrFsItem(repoPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrFsItem getJcrFsItem(Node node) {
        return storageMixin.getJcrFsItem(node);
    }

    @Override
    public JcrFile getJcrFile(RepoPath repoPath) throws FileExpectedException {
        return storageMixin.getJcrFile(repoPath);
    }

    @Override
    public JcrFolder getJcrFolder(RepoPath repoPath) throws FolderExpectedException {
        return storageMixin.getJcrFolder(repoPath);
    }

    @Override
    public JcrFsItem getLockedJcrFsItem(RepoPath repoPath) {
        return storageMixin.getLockedJcrFsItem(repoPath);
    }

    @Override
    public JcrFsItem getLockedJcrFsItem(Node node) {
        return storageMixin.getLockedJcrFsItem(node);
    }

    @Override
    public JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException {
        return storageMixin.getLockedJcrFile(repoPath, createIfMissing);
    }

    @Override
    public JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException {
        return storageMixin.getLockedJcrFolder(repoPath, createIfMissing);
    }

    @Override
    public RepoResource getInfo(InternalRequestContext context) throws FileExpectedException {
        return downloadStrategy.getInfo(context);
    }

    @Override
    public ResourceStreamHandle getResourceStreamHandle(InternalRequestContext requestContext, RepoResource res)
            throws IOException, RepositoryException, RepoRejectException {
        return storageMixin.getResourceStreamHandle(requestContext, res);
    }

    @Override
    public String getChecksum(String checksumFilePath, RepoResource res) throws IOException {
        return storageMixin.getChecksum(checksumFilePath, res);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JcrFsItem getLocalJcrFsItem(String relPath) {
        return storageMixin.getLocalJcrFsItem(relPath);
    }

    @Override
    public JcrFsItem getLockedJcrFsItem(String relPath) {
        return storageMixin.getLockedJcrFsItem(relPath);
    }

    @Override
    public JcrFile getLocalJcrFile(String relPath) throws FileExpectedException {
        return storageMixin.getLocalJcrFile(relPath);
    }

    @Override
    public JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException {
        return storageMixin.getLockedJcrFile(relPath, createIfMissing);
    }

    @Override
    public JcrFolder getLocalJcrFolder(String relPath) throws FolderExpectedException {
        return storageMixin.getLocalJcrFolder(relPath);
    }

    @Override
    public JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException {
        return storageMixin.getLockedJcrFolder(relPath, createIfMissing);
    }

    @Override
    public boolean isWriteLocked(RepoPath path) {
        return storageMixin.isWriteLocked(path);
    }

    @Override
    public StoringRepo<VirtualRepoDescriptor> getStorageMixin() {
        return storageMixin;
    }

    @Override
    public void clearCaches() {
        storageMixin.clearCaches();
    }
}
