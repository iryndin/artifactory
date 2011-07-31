/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.WebstartAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.fs.VersionUnit;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.repo.ArchiveFileContent;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.rest.constant.RepositoriesRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.api.search.SavedSearchResults;
import org.artifactory.api.search.deployable.VersionUnitSearchControls;
import org.artifactory.api.search.deployable.VersionUnitSearchResult;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.tree.fs.ZipEntriesTree;
import org.artifactory.api.tree.fs.ZipEntryInfo;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.StatusHolder;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.info.InfoWriter;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.trash.Trashman;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenMetadataCalculator;
import org.artifactory.maven.MavenPluginsMetadataCalculator;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.Properties;
import org.artifactory.repo.*;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.repo.jcr.JcrLocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.service.mover.MoverConfig;
import org.artifactory.repo.service.mover.MoverConfigBuilder;
import org.artifactory.repo.service.mover.RepoPathMover;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.InternalArtifactoryResponse;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RequestContext;
import org.artifactory.resource.ResolvedResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskCallback;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.ZipUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.artifactory.repo.jcr.JcrHelper.safeGetNode;

/**
 * User: freds Date: Jul 21, 2008 Time: 8:10:12 PM
 */
@Service
@Reloadable(beanClass = InternalRepositoryService.class,
        initAfter = {JcrService.class, StorageInterceptors.class, InternalCentralConfigService.class})
public class RepositoryServiceImpl implements InternalRepositoryService {
    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    @Autowired
    private AclService aclService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private JcrService jcr;

    @Autowired
    private TaskService taskService;

    @Autowired
    private JcrRepoService jcrRepoService;

    @Autowired
    private InternalSearchService searchService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private UploadService uploadService;

    private VirtualRepo globalVirtualRepo;

    private Map<String, VirtualRepo> virtualRepositoriesMap = Maps.newLinkedHashMap();

    // a cache of all the repository keys
    private Set<String> allRepoKeysCache;

    // a semaphore to guard against parallel maven plugins metadata calculations
    private final Semaphore pluginsMDSemaphore = new Semaphore(1);
    // queue of repository keys that requires maven metadata plugins calculation
    private final Queue<String> pluginsMDQueue = new ConcurrentLinkedQueue<String>();

    public void init() {
        rebuildRepositories(null);
        try {
            //Dump info to the log
            InfoWriter.writeInfo();
        } catch (Exception e) {
            log.warn("Failed dumping system info", e);
        }
        getTransactionalMe().recalculateMavenMetadataOnMarkedFolders();
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        deleteOrphanRepos(oldDescriptor);
        rebuildRepositories(oldDescriptor);
        checkAndCleanChangedVirtualPomCleanupPolicy(oldDescriptor);
    }

    private void checkAndCleanChangedVirtualPomCleanupPolicy(CentralConfigDescriptor oldDescriptor) {
        Map<String, VirtualRepoDescriptor> oldVirtualDescriptors = oldDescriptor.getVirtualRepositoriesMap();
        List<VirtualRepoDescriptor> newVirtualDescriptors = getVirtualRepoDescriptors();
        for (VirtualRepoDescriptor newDescriptor : newVirtualDescriptors) {
            String repoKey = newDescriptor.getKey();
            VirtualRepoDescriptor oldVirtualDescriptor = oldVirtualDescriptors.get(repoKey);
            if (oldVirtualDescriptor != null && pomCleanUpPolicyChanged(newDescriptor, oldVirtualDescriptor)) {
                VirtualRepo virtualRepo = virtualRepositoryByKey(repoKey);
                log.info("Pom Repository Reference Cleanup Policy changed in '{}', cleaning repository cache. ",
                        repoKey);
                RepoPath rootPath = RepoPathImpl.repoRootPath(repoKey);
                virtualRepo.undeploy(rootPath, false);
            }
        }
    }

    private boolean pomCleanUpPolicyChanged(VirtualRepoDescriptor newDescriptor, VirtualRepoDescriptor oldDescriptor) {
        PomCleanupPolicy newPolicy = newDescriptor.getPomRepositoryReferencesCleanupPolicy();
        PomCleanupPolicy oldPolicy = oldDescriptor.getPomRepositoryReferencesCleanupPolicy();
        return !newPolicy.equals(oldPolicy);
    }

    private void deleteOrphanRepos(CentralConfigDescriptor oldDescriptor) {
        CentralConfigDescriptor currentDescriptor = centralConfigService.getDescriptor();
        Set<String> newRepoKeys = getConfigReposKeys(currentDescriptor);
        Set<String> oldRepoKeys = getConfigReposKeys(oldDescriptor);
        for (String key : oldRepoKeys) {
            if (!newRepoKeys.contains(key)) {
                log.warn("Removing the no-longer-referenced repository " + key);
                StatusHolder statusHolder = deleteOrphanRepo(key);
                if (statusHolder.isError()) {
                    log.warn("Error occurred during repo '{}' removal: {}", key, statusHolder.getStatusMsg());
                }
            }
        }
    }

    private StatusHolder deleteOrphanRepo(String repoKey) {
        BasicStatusHolder status = new BasicStatusHolder();
        RepoPath repoPath;
        RemoteRepo remoteRepo = remoteRepositoryByKey(repoKey);
        if (remoteRepo != null) {
            // for remote repos we have to delete the cache if exists
            if (!remoteRepo.isStoreArtifactsLocally()) {
                // if the cache repository node exists, delete it using jcr paths directly since there is no backing
                // repository(it is possible when a remote repo started with store artifacts locally and was later
                // changed not to)
                String cacheRepoJcrPath = JcrPath.get().getRepoJcrPath(repoKey + LocalCacheRepo.PATH_SUFFIX);
                if (jcr.itemNodeExists(cacheRepoJcrPath)) {
                    try {
                        Trashman trashman = jcr.getManagedSession().getOrCreateResource(Trashman.class);
                        trashman.addPathsToTrash(Arrays.asList(cacheRepoJcrPath), jcr);
                    } catch (Exception e) {
                        status.setError(
                                "Could not move remote repository cache node " + cacheRepoJcrPath + "  to trash.",
                                e, log);
                    }
                }
                return status;
            }
            // delete the local cache
            LocalCacheRepo localCacheRepo = remoteRepo.getLocalCacheRepo();
            repoPath = RepoPathImpl.repoRootPath(localCacheRepo.getKey());
        } else {
            repoPath = RepoPathImpl.repoRootPath(repoKey);
        }
        // delete repo content
        status = undeploy(repoPath);

        // delete the repo node
        StoringRepo storingRepo = storingRepositoryByKey(repoKey);
        JcrFsItem repoFolder = storingRepo.getRootFolder();
        jcrRepoService.trash(Arrays.asList(repoFolder));

        return status;
    }

    private Set<String> getConfigReposKeys(CentralConfigDescriptor descriptor) {
        Set<String> repoKeys = new HashSet<String>();
        repoKeys.addAll(descriptor.getLocalRepositoriesMap().keySet());
        repoKeys.addAll(descriptor.getRemoteRepositoriesMap().keySet());
        repoKeys.addAll(descriptor.getVirtualRepositoriesMap().keySet());
        return repoKeys;
    }

    public void destroy() {
        List<Repo> repos = Lists.newArrayList();
        repos.addAll(getVirtualRepositories());
        repos.addAll(getLocalAndRemoteRepositories());
        for (Repo repo : repos) {
            try {
                repo.destroy();
            } catch (Exception e) {
                log.error("Error while destroying the repository '{}'.", repo, e);
            }
        }
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    private void rebuildRepositories(CentralConfigDescriptor oldDescriptor) {
        //Create the repository objects from the descriptor
        CentralConfigDescriptor centralConfig = centralConfigService.getDescriptor();
        InternalRepositoryService transactionalMe = getTransactionalMe();

        //Local repos
        Map<String, LocalRepo> localRepositoriesMap = Maps.newLinkedHashMap();
        Map<String, LocalRepoDescriptor> localRepoDescriptorMap = centralConfig.getLocalRepositoriesMap();
        Map<String, LocalRepo> oldLocalRepos = null;
        if (oldDescriptor != null && globalVirtualRepo != null) {
            oldLocalRepos = globalVirtualRepo.getLocalRepositoriesMap();
        }
        for (LocalRepoDescriptor repoDescriptor : localRepoDescriptorMap.values()) {
            JcrLocalRepo oldLocalRepo = null;
            String key = repoDescriptor.getKey();
            if (oldLocalRepos != null) {
                LocalRepo oldRepo = oldLocalRepos.get(key);
                if (oldRepo != null) {
                    if (!(oldRepo instanceof JcrLocalRepo)) {
                        log.error("Reloading configuration did not find local repository " + key);
                    } else {
                        oldLocalRepo = (JcrLocalRepo) oldRepo;
                    }
                } else {
                    // This could be a new repo that is in the newly saved config but not in the global map yet.
                    // Only if we do not find it there as well then it is an error
                    LocalRepoDescriptor newLocalRepo = centralConfig.getLocalRepositoriesMap().get(key);
                    if (newLocalRepo == null) {
                        log.error("Reloading configuration did not find local repository " + key);
                    }

                }
            }
            LocalRepo repo = new JcrLocalRepo(transactionalMe, repoDescriptor, oldLocalRepo);
            try {
                repo.init();
            } catch (Exception e) {
                log.error("Failed to initialize local repository '{}'. Repository will be blacked-out", repo.getKey(),
                        e);
                ((LocalRepoDescriptor) repo.getDescriptor()).setBlackedOut(true);
            }
            localRepositoriesMap.put(repo.getKey(), repo);
        }

        //Remote repos
        Map<String, RemoteRepo> remoteRepositoriesMap = Maps.newLinkedHashMap();
        Map<String, RemoteRepoDescriptor> remoteRepoDescriptorMap = centralConfig.getRemoteRepositoriesMap();
        Map<String, RemoteRepo> oldRemoteRepos = null;
        if (oldDescriptor != null && globalVirtualRepo != null) {
            oldRemoteRepos = globalVirtualRepo.getRemoteRepositoriesMap();
        }
        for (RemoteRepoDescriptor repoDescriptor : remoteRepoDescriptorMap.values()) {
            RemoteRepo oldRemoteRepo = null;
            if (oldRemoteRepos != null) {
                oldRemoteRepo = oldRemoteRepos.get(repoDescriptor.getKey());
            }
            RemoteRepo repo = new HttpRepo(
                    transactionalMe, (HttpRepoDescriptor) repoDescriptor,
                    centralConfig.isOfflineMode(), oldRemoteRepo);
            try {
                repo.init();
            } catch (Exception e) {
                log.error("Failed to initialize remote repository '" + repo.getKey() + "'. " +
                        "Repository will be blacked-out!", e);
                ((HttpRepoDescriptor) repo.getDescriptor()).setBlackedOut(true);
            }
            remoteRepositoriesMap.put(repo.getKey(), repo);
        }

        // create on-the-fly repo descriptor to be used by the global virtual repo
        List<RepoDescriptor> localAndRemoteRepoDescriptors = new ArrayList<RepoDescriptor>();
        localAndRemoteRepoDescriptors.addAll(localRepoDescriptorMap.values());
        localAndRemoteRepoDescriptors.addAll(remoteRepoDescriptorMap.values());
        VirtualRepoDescriptor vrd = new VirtualRepoDescriptor();
        vrd.setRepositories(localAndRemoteRepoDescriptors);
        vrd.setArtifactoryRequestsCanRetrieveRemoteArtifacts(
                ConstantValues.artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts.getBoolean());
        vrd.setKey(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY);
        // create and init the global virtual repo
        globalVirtualRepo = new VirtualRepo(transactionalMe, vrd, localRepositoriesMap, remoteRepositoriesMap);
        // no need to call globalVirtualRepo.init()
        globalVirtualRepo.initStorage();

        virtualRepositoriesMap.clear();// we rebuild the virtual repo cache
        virtualRepositoriesMap.put(globalVirtualRepo.getKey(), globalVirtualRepo);

        // virtual repos init in 2 passes
        Map<String, VirtualRepoDescriptor> virtualRepoDescriptorMap =
                centralConfig.getVirtualRepositoriesMap();
        // 1. create the virtual repos
        WebstartAddon webstartAddon = addonsManager.addonByType(WebstartAddon.class);
        for (VirtualRepoDescriptor repoDescriptor : virtualRepoDescriptorMap.values()) {
            VirtualRepo repo = webstartAddon.createVirtualRepo(transactionalMe, repoDescriptor);
            virtualRepositoriesMap.put(repo.getKey(), repo);
        }

        // 2. call the init method only after all virtual repos exist
        for (VirtualRepo virtualRepo : virtualRepositoriesMap.values()) {
            virtualRepo.init();
        }

        initAllRepoKeysCache();
    }

    public List<ItemInfo> getChildrenDeeply(RepoPath path) {
        List<ItemInfo> result = Lists.newArrayList();
        if (path == null) {
            return result;
        }
        if (!hasChildren(path)) {
            return result;
        }
        List<ItemInfo> children = getChildren(path);
        for (ItemInfo child : children) {
            result.add(child);
            result.addAll(getChildrenDeeply(child.getRepoPath()));
        }
        return result;
    }

    public ModuleInfo getItemModuleInfo(RepoPath repoPath) {
        Repo repo = assertRepoKey(repoPath);
        return repo.getItemModuleInfo(repoPath.getPath());
    }

    private ModuleInfo getDescriptorModuleInfo(RepoPath repoPath) {
        Repo repo = assertRepoKey(repoPath);
        return repo.getDescriptorModuleInfo(repoPath.getPath());
    }

    public RepoPath getExplicitDescriptorPathByArtifact(RepoPath repoPath) {
        Repo repo = assertRepoKey(repoPath);

        RepoLayout repoLayout = repo.getDescriptor().getRepoLayout();
        if ((repoLayout == null) || !repoLayout.isDistinctiveDescriptorPathPattern()) {
            return repoPath;
        }

        ModuleInfo descriptorModuleInfo = getDescriptorModuleInfo(repoPath);
        if (descriptorModuleInfo.isValid()) {
            return repoPath;
        }

        ModuleInfo itemModuleInfo = getItemModuleInfo(repoPath);
        if (!itemModuleInfo.isValid()) {
            return repoPath;
        }

        String descriptorPath = ModuleInfoUtils.constructDescriptorPath(itemModuleInfo, repoLayout, true);
        return new RepoPathImpl(repoPath.getRepoKey(), descriptorPath);
    }

    private Repo assertRepoKey(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        Repo repo = repositoryByKey(repoKey);
        if (repo == null) {
            throw new IllegalArgumentException("Repository '" + repoKey + "' not found!");
        }
        return repo;
    }

    public boolean mkdirs(RepoPath folderRepoPath) {
        if (!exists(folderRepoPath)) {

            LocalRepo localRepo = getLocalRepository(folderRepoPath);

            JcrFolder jcrFolder = localRepo.getLockedJcrFolder(folderRepoPath.getPath(), true);
            return jcrFolder.exists() || jcrFolder.mkdirs();
        }

        return false;
    }

    public boolean virtualItemExists(RepoPath repoPath) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(repoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new RepositoryRuntimeException(
                    "Repository " + repoPath.getRepoKey() + " does not exists!");
        }
        return virtualRepo.virtualItemExists(repoPath.getPath());
    }

    public Repository getJcrHandle() {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        return jcr.getRepository();
    }

    public List<ItemInfo> getChildren(RepoPath repoPath) {
        List<ItemInfo> childrenInfo = Lists.newArrayList();

        LocalRepo repo = localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if ((repo != null) && repo.itemExists(repoPath.getPath())) {

            JcrFsItem item = repo.getJcrFsItem(repoPath);
            if (item.isDirectory()) {
                JcrFolder dir = (JcrFolder) item;
                List<JcrFsItem> children = dir.getItems();

                //Sort files by name
                Collections.sort(children);

                for (JcrFsItem child : children) {
                    //Check if we should return the child
                    String itemPath = child.getRelativePath();
                    RepoPath childRepoPath = new RepoPathImpl(child.getRepoKey(), itemPath);
                    boolean childReader = authService.canImplicitlyReadParentPath(childRepoPath);
                    if (!childReader) {
                        //Don't bother with stuff that we do not have read access to
                        continue;
                    }
                    childrenInfo.add(child.getInfo());
                }
            }
        }

        return childrenInfo;
    }

    @SuppressWarnings({"unchecked"})
    public List<String> getChildrenNames(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        LocalRepo repo = globalVirtualRepo.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            throw new RepositoryRuntimeException(
                    "Tried to get children of a non exiting local repository '" + repoKey + "'.");
        }
        List<String> childrenNames = repo.getChildrenNames(repoPath.getPath());
        List<String> authorizedChildrenNames = new ArrayList(childrenNames.size());
        for (String childName : childrenNames) {
            RepoPath childRepoPath = new RepoPathImpl(repoPath, childName);
            boolean childReader = authService.canImplicitlyReadParentPath(childRepoPath);
            if (childReader) {
                //Its enough that we have a single reader to say we have children
                authorizedChildrenNames.add(childName);
            }
        }
        return authorizedChildrenNames;
    }

    public boolean hasChildren(RepoPath repoPath) {
        return getChildrenNames(repoPath).size() > 0;
    }

    public VirtualRepo getGlobalVirtualRepo() {
        return globalVirtualRepo;
    }

    public List<VirtualRepo> getVirtualRepositories() {
        return new ArrayList<VirtualRepo>(virtualRepositoriesMap.values());
    }

    public List<LocalRepo> getLocalAndCachedRepositories() {
        return globalVirtualRepo.getLocalAndCachedRepositories();
    }

    public List<StoringRepo> getStoringRepositories() {
        List<StoringRepo> repoList = Lists.newArrayList();
        repoList.addAll(globalVirtualRepo.getLocalAndCachedRepositories());
        repoList.addAll(getVirtualRepositories());
        return repoList;
    }

    public List<RealRepo> getLocalAndRemoteRepositories() {
        return globalVirtualRepo.getLocalAndRemoteRepositories();
    }

    public List<LocalRepoDescriptor> getLocalAndCachedRepoDescriptors() {
        List<LocalRepo> localAndCached = globalVirtualRepo.getLocalAndCachedRepositories();
        ArrayList<LocalRepoDescriptor> result = Lists.newArrayList();
        for (LocalRepo localRepo : localAndCached) {
            result.add((LocalRepoDescriptor) localRepo.getDescriptor());
        }
        return result;
    }

    public List<RemoteRepoDescriptor> getRemoteRepoDescriptors() {
        List<RemoteRepo> remoteRepositories = globalVirtualRepo.getRemoteRepositories();
        ArrayList<RemoteRepoDescriptor> result = Lists.newArrayList();
        for (RemoteRepo remoteRepo : remoteRepositories) {
            result.add((RemoteRepoDescriptor) remoteRepo.getDescriptor());
        }
        return result;
    }

    public VirtualRepoDescriptor virtualRepoDescriptorByKey(String repoKey) {
        if (repoKey == null || repoKey.length() == 0) {
            return null;
        }
        if (VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY.equals(repoKey)) {
            return globalVirtualRepo.getDescriptor();
        }
        return centralConfigService.getDescriptor().getVirtualRepositoriesMap().get(repoKey);
    }

    public String getStringContent(org.artifactory.fs.FileInfo fileInfo) {
        LocalRepo repo = localOrCachedRepositoryByKey(fileInfo.getRepoKey());
        return repo.getTextFileContent(fileInfo.getRepoPath());
    }

    public ArchiveFileContent getArchiveFileContent(RepoPath archivePath, String sourceEntryPath) throws IOException {
        LocalRepo repo = localOrCachedRepositoryByKey(archivePath.getRepoKey());
        return repo.getArchiveFileContent(archivePath, sourceEntryPath);
    }

    /**
     * Import all the repositories under the passed folder which matches local or cached repository declared in the
     * configuration. Having empty directory for each repository is allowed and not an error. Nothing will be imported
     * for those.
     */
    public void importAll(ImportSettings settings) {
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(PermissionTargetInfo.ANY_REPO, settings, false, true);
        } else {
            //Import the local repositories
            List<LocalRepoDescriptor> repoList = getLocalAndCachedRepoDescriptors();
            importAll(repoList, Collections.<LocalRepoDescriptor>emptyList(), settings);
        }
    }

    /**
     * Import the artifacts under the folder passed directly in the repository named "repoKey". If no repository with
     * this repo key exists or if the folder passed is empty, the status will be set to error.
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importRepo(String repoKey, ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(repoKey, settings, false, true);
        } else {
            //Import each file seperately to avoid a long running transaction
            LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
            if (localRepo == null) {
                String msg = "The repo key " + repoKey + " is not a local or cached repository!";
                IllegalArgumentException ex = new IllegalArgumentException(msg);
                status.setError(msg, ex, log);
                return;
            }
            localRepo.importFrom(settings);
        }
    }

    public void exportTo(ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        status.setStatus("Exporting repositories...", log);
        if (TaskCallback.currentTaskToken() == null) {
            exportAsync(null, settings);
        } else {
            List<LocalRepoDescriptor> repos = settings.getRepositories();
            for (LocalRepoDescriptor localRepo : repos) {
                boolean stop = taskService.pauseOrBreak();
                if (stop) {
                    status.setError("Export was stopped", log);
                    return;
                }
                exportRepo(localRepo.getKey(), settings);
                if (status.isError() && settings.isFailFast()) {
                    return;
                }
            }

            if (settings.isIncremental()) {
                File repositoriesDir = JcrPath.get().getRepositoriesExportDir(settings.getBaseDir());
                cleanupIncrementalBackupDirectory(repositoriesDir, repos);
            }
        }
    }

    public void exportRepo(String repoKey, ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        if (TaskCallback.currentTaskToken() == null) {
            exportAsync(repoKey, settings);
        } else {
            //Check if we need to break/pause
            boolean stop = taskService.pauseOrBreak();
            if (stop) {
                status.setError("Export was stopped on " + repoKey, log);
                return;
            }
            File targetDir = JcrPath.get().getRepoExportDir(settings.getBaseDir(), repoKey);
            ExportSettings repoSettings = new ExportSettings(targetDir, settings);
            LocalRepo sourceRepo = localOrCachedRepositoryByKey(repoKey);
            sourceRepo.exportTo(repoSettings);
        }
    }

    /**
     * {@inheritDoc}
     */
    public MultiStatusHolder exportSearchResults(SavedSearchResults searchResults, ExportSettings baseSettings) {

        MultiStatusHolder statusHolder = baseSettings.getStatusHolder();
        statusHolder.setStatus("Started exporting search result '" + searchResults.getName() + "'.", log);

        File baseDir = baseSettings.getBaseDir();
        for (org.artifactory.fs.FileInfo searchResult : searchResults.getResults()) {
            RepoPath repoPath = searchResult.getRepoPath();
            ExportSettings settings = new ExportSettings(baseDir, baseSettings);
            StoringRepo storingRepo = storingRepositoryByKey(repoPath.getRepoKey());
            JcrFile jcrFile = storingRepo.getJcrFile(repoPath);
            jcrFile.exportTo(settings);
        }
        if (baseSettings.isCreateArchive()) {
            try {
                statusHolder.setStatus("Archiving exported search result '" + searchResults.getName() + "'.", log);
                String tempDir = System.getProperty("java.io.tmpdir");
                File tempArchive = new File(tempDir, baseDir.getName() + ".zip");
                // Create the archive
                ZipUtils.archive(baseDir, tempArchive, true);
                //Delete the exploded directory
                FileUtils.deleteDirectory(baseDir);
                //Copy the zip back into the deleted directory
                FileUtils.copyFile(tempArchive, new File(baseDir, tempArchive.getName()));
                //Delete the temporary zip
                FileUtils.deleteQuietly(tempArchive);
            } catch (IOException e) {
                statusHolder.setError("Unable to create zip archive", -1, e, log);
            }
        }
        statusHolder.setStatus("Finished exporting search result '" + searchResults.getName() + "'.", log);
        return statusHolder;
    }

    public org.artifactory.fs.ItemInfo getItemInfo(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        JcrFsItem item = localRepo.getJcrFsItem(repoPath);
        if (item != null) {
            return item.getInfo();
        }
        throw new ItemNotFoundRuntimeException("Item " + repoPath + " does not exists");
    }

    public org.artifactory.fs.FileInfo getFileInfo(RepoPath repoPath) {
        org.artifactory.fs.ItemInfo itemInfo = getItemInfo(repoPath);
        if (itemInfo instanceof org.artifactory.fs.FileInfo) {
            return (org.artifactory.fs.FileInfo) itemInfo;
        } else {
            throw new FileExpectedException(repoPath);
        }
    }

    public boolean exists(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        return localRepo.itemExists(repoPath.getPath());
    }

    public MetadataInfo getMetadataInfo(RepoPath repoPath, String metadataName) {
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            return null;
        }
        try {
            JcrFsItem fsItem = getFsItem(repoPath);
            if (fsItem == null) {
                return null;
            }

            MetadataDefinition metadataDefinition = fsItem.getMetadataDefinition(metadataName);
            if (metadataDefinition == null) {
                return null;
            }
            return metadataDefinition.getPersistenceHandler().getMetadataInfo(fsItem);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } finally {
            // release the read lock on the fsItem
            LockingHelper.releaseReadLock(repoPath);
        }
    }

    @SuppressWarnings({"unchecked"})
    public List<String> getMetadataNames(RepoPath repoPath) {
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            return Lists.newArrayList();
        }
        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem == null) {
            return null;
        }

        List<String> metadataNames = Lists.newArrayList();
        Set<MetadataDefinition<?>> existingMetadata;
        try {
            existingMetadata = fsItem.getExistingMetadata(false);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
        for (MetadataDefinition<?> metadata : existingMetadata) {
            metadataNames.add(metadata.getMetadataName());
        }
        return metadataNames;
    }

    @SuppressWarnings({"unchecked"})
    public <MD> MD getMetadata(RepoPath repoPath, Class<MD> metadataClass) {
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            return null;
        }
        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem == null) {
            return null;
        }
        // TODO: should use read lock?
        return (MD) fsItem.getMetadata(metadataClass);
    }

    public String getXmlMetadata(RepoPath repoPath, String metadataName) {
        try {
            if (!authService.canRead(repoPath)) {
                AccessLogger.downloadDenied(repoPath);
                return null;
            }
            JcrFsItem fsItem = getFsItem(repoPath);
            if (fsItem == null) {
                return null;
            }
            return fsItem.getXmlMetadata(metadataName);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } finally {
            // release the read lock on the fsItem
            LockingHelper.releaseReadLock(repoPath);
        }
    }

    public boolean hasMetadata(RepoPath repoPath, String metadataName) {
        try {
            JcrFsItem fsItem = getFsItem(repoPath);
            return fsItem != null && fsItem.hasMetadata(metadataName);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } finally {
            // release the read lock on the fsItem
            LockingHelper.releaseReadLock(repoPath);
        }
    }

    public <MD> boolean setMetadata(RepoPath repoPath, Class<MD> metadataClass, MD metadata) {
        if (!authService.canAnnotate(repoPath)) {
            AccessLogger.annotateDenied(repoPath);
            log.error("Cannot set metadata of type '{}' on '{}': lacking annotate permissions.",
                    metadataClass.getSimpleName(), repoPath.getId());
            return false;
        }

        LocalRepo repository = getLocalRepository(repoPath);
        JcrFsItem<?> fsItem = repository.getLockedJcrFsItem(repoPath);
        if (fsItem == null) {
            log.error("Cannot set metadata of type '{}' on '{}': unable to find the item within the repository.",
                    metadataClass.getSimpleName(), repoPath.getId());
            return false;
        }

        fsItem.setMetadata(metadataClass, metadata);
        return true;
    }

    public void setXmlMetadataLater(RepoPath repoPath, String metadataName, String metadataContent) {
        if (!authService.canAnnotate(repoPath)) {
            AccessLogger.annotateDenied(repoPath);
            return;
        }

        LocalRepo repository = getLocalRepository(repoPath);
        JcrFsItem fsItem = repository.getJcrFsItem(repoPath);
        if (fsItem == null) {
            return;
        }

        if (fsItem.isMutable()) {
            fsItem.setXmlMetadata(metadataName, metadataContent);
        } else {
            // we queue the metadata modifications and only commit them to jcr once the transaction is saved.
            fsItem.setXmlMetadataLater(metadataName, metadataContent);
            getTransactionalMe().updateDirtyState(repoPath);
            // once the update dirty state is called, it waits for a write lock, so we release the read lock to allow
            // the other thread to acquire it  
            LockingHelper.releaseReadLock(repoPath);
        }
    }

    public void setXmlMetadata(RepoPath repoPath, String metadataName, String metadataContent) {
        if (!authService.canAnnotate(repoPath)) {
            AccessLogger.annotateDenied(repoPath);
            return;
        }

        LocalRepo repository = getLocalRepository(repoPath);
        JcrFsItem fsItem = repository.getLockedJcrFsItem(repoPath);
        if (fsItem == null) {
            return;
        }

        fsItem.setXmlMetadata(metadataName, metadataContent);
    }

    public boolean removeMetadata(RepoPath repoPath, String metadataName) {
        if (!authService.canAnnotate(repoPath)) {
            AccessLogger.annotateDenied(repoPath);
            return false;
        }

        LocalRepo repository = getLocalRepository(repoPath);
        JcrFsItem<?> fsItem = repository.getLockedJcrFsItem(repoPath);
        if (fsItem == null) {
            return false;
        }

        fsItem.removeMetadata(metadataName);
        return true;
    }

    public MoveMultiStatusHolder move(RepoPath from, RepoPath to, boolean dryRun, boolean suppressLayouts,
            boolean failFast) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(from, to).copy(false).dryRun(dryRun).
                executeMavenMetadataCalculation(true).suppressLayouts(suppressLayouts).failFast(failFast);
        return moveOrCopy(configBuilder.build());
    }

    public MoveMultiStatusHolder move(RepoPath from, String targetLocalRepoKey, boolean dryRun) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(from, targetLocalRepoKey)
                .copy(false).dryRun(dryRun).executeMavenMetadataCalculation(true);
        return moveOrCopy(configBuilder.build());
    }

    public MoveMultiStatusHolder move(Set<RepoPath> pathsToMove, String targetLocalRepoKey,
            Properties properties, boolean dryRun, boolean failFast, boolean searchResults) {
        Set<RepoPath> pathsToMoveIncludingParents = aggregatePathsToMove(pathsToMove, targetLocalRepoKey, false);

        log.debug("The following paths will be moved: {}", pathsToMoveIncludingParents);
        // start moving each path separately, marking each folder or file's parent folder for metadata recalculation
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        RepoPathMover mover = getRepoPathMover();
        for (RepoPath pathToMove : pathsToMoveIncludingParents) {
            log.debug("Moving path: {} to {}", pathToMove, targetLocalRepoKey);
            mover.moveOrCopy(status, new MoverConfigBuilder(pathToMove, targetLocalRepoKey).copy(false).dryRun(dryRun)
                    .executeMavenMetadataCalculation(false).searchResult(searchResults).properties(properties).
                            failFast(failFast).build());
        }

        // done moving, launch async call to execute metadata recalculation on all marked folders
        getTransactionalMe().recalculateMavenMetadataOnMarkedFolders();
        return status;
    }

    public MoveMultiStatusHolder copy(RepoPath fromRepoPath, RepoPath targetRepoPath, boolean dryRun,
            boolean suppressLayouts, boolean failFast) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(fromRepoPath, targetRepoPath).copy(true).
                dryRun(dryRun).executeMavenMetadataCalculation(true).suppressLayouts(suppressLayouts).
                failFast(failFast);
        return moveOrCopy(configBuilder.build());
    }

    public MoveMultiStatusHolder copy(RepoPath from, String targetLocalRepoKey, boolean dryRun) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(from, targetLocalRepoKey)
                .copy(true).dryRun(dryRun).executeMavenMetadataCalculation(true);
        return moveOrCopy(configBuilder.build());
    }

    public MoveMultiStatusHolder copy(Set<RepoPath> pathsToCopy, String targetLocalRepoKey,
            Properties properties, boolean dryRun, boolean failFast, boolean searchResults) {
        Set<RepoPath> pathsToCopyIncludingParents = aggregatePathsToMove(pathsToCopy, targetLocalRepoKey, true);

        log.debug("The following paths will be copied: {}", pathsToCopyIncludingParents);
        //Start copying each path separately, marking each folder or file's parent folder for metadata recalculation
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        RepoPathMover mover = getRepoPathMover();
        for (RepoPath pathToCopy : pathsToCopyIncludingParents) {
            log.debug("Moving path: {} to {}", pathToCopy, targetLocalRepoKey);
            mover.moveOrCopy(status, new MoverConfigBuilder(pathToCopy, targetLocalRepoKey).copy(true).dryRun(dryRun)
                    .executeMavenMetadataCalculation(false).searchResult(searchResults).properties(properties).
                            failFast(failFast).build());
        }

        //Done copying, launch async call to execute metadata recalculation on all marked folders
        getTransactionalMe().recalculateMavenMetadataOnMarkedFolders();
        return status;
    }

    private MoveMultiStatusHolder moveOrCopy(MoverConfig config) {
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        getRepoPathMover().moveOrCopy(status, config);
        return status;
    }

    public StatusHolder deploy(RepoPath repoPath, InputStream inputStream) {
        try {
            ArtifactoryDeployRequest request =
                    new ArtifactoryDeployRequest(repoPath, inputStream, -1, System.currentTimeMillis());
            InternalArtifactoryResponse response = new InternalArtifactoryResponse();
            uploadService.process(request, response);
            return response.getStatusHolder();
        } catch (Exception e) {
            String msg = String.format("Cannot deploy to '{%s}'.", repoPath);
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        }
    }

    public FileInfo getVirtualFileInfo(RepoPath repoPath) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(repoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new IllegalArgumentException(repoPath.getRepoKey() + " is not a virtual repository.");
        }
        Set<LocalRepo> resolvedLocalRepos = virtualRepo.getResolvedLocalAndCachedRepos();
        for (LocalRepo resolvedLocalRepo : resolvedLocalRepos) {
            if (resolvedLocalRepo.itemExists(repoPath.getPath())) {
                return getFileInfo(resolvedLocalRepo.getRepoPath(repoPath.getPath()));
            }
        }

        throw new ItemNotFoundRuntimeException("Item " + repoPath + " does not exists");
    }

    public ItemInfo getVirtualItemInfo(RepoPath repoPath) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(repoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new IllegalArgumentException(repoPath.getRepoKey() + " is not a virtual repository.");
        }
        Set<LocalRepo> resolvedLocalRepos = virtualRepo.getResolvedLocalAndCachedRepos();
        for (LocalRepo resolvedLocalRepo : resolvedLocalRepos) {
            if (resolvedLocalRepo.itemExists(repoPath.getPath())) {
                return getItemInfo(resolvedLocalRepo.getRepoPath(repoPath.getPath()));
            }
        }

        throw new ItemNotFoundRuntimeException("Item " + repoPath + " does not exists");
    }

    public MetadataInfo getVirtualMetadataInfo(RepoPath repoPath, String metadataName) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(repoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new IllegalArgumentException(repoPath.getRepoKey() + " is not a virtual repository.");
        }
        Set<LocalRepo> resolvedLocalRepos = virtualRepo.getResolvedLocalRepos();
        for (LocalRepo resolvedLocalRepo : resolvedLocalRepos) {
            if (hasMetadata(resolvedLocalRepo.getRepoPath(repoPath.getPath()), metadataName)) {
                return getMetadataInfo(resolvedLocalRepo.getRepoPath(repoPath.getPath()), metadataName);
            }
        }

        throw new ItemNotFoundRuntimeException("Item " + repoPath + " does not exists");
    }

    public BasicStatusHolder undeploy(RepoPath repoPath, boolean calcMavenMetadata) {
        return undeploy(repoPath, calcMavenMetadata, false);
    }

    public BasicStatusHolder undeploy(RepoPath repoPath, boolean calcMavenMetadata, boolean pruneEmptyFolders) {
        String repoKey = repoPath.getRepoKey();
        StoringRepo storingRepo = storingRepositoryByKey(repoKey);
        BasicStatusHolder statusHolder = new BasicStatusHolder();
        if (storingRepo == null) {
            statusHolder.setError("Could find storing repository by key '" + repoKey + "'", log);
            return statusHolder;
        }
        assertDelete(storingRepo, repoPath.getPath(), false, statusHolder);
        if (!statusHolder.isError()) {
            storingRepo.undeploy(repoPath, calcMavenMetadata);
        }

        if (!repoPath.isRoot() && pruneEmptyFolders) {
            RepoPath parent = repoPath.getParent();
            if (!parent.isRoot() && getChildrenNames(parent).isEmpty()) {
                getTransactionalMe().undeploy(parent, false, pruneEmptyFolders);
            }
        }

        return statusHolder;
    }

    public BasicStatusHolder undeploy(RepoPath repoPath) {
        return undeploy(repoPath, true);
    }

    public StatusHolder undeployVersionUnits(Set<VersionUnit> versionUnits) {
        MultiStatusHolder multiStatusHolder = new MultiStatusHolder();
        InternalRepositoryService transactionalMe = getTransactionalMe();

        Set<RepoPath> pathsForMavenMetadataCalculation = Sets.newHashSet();

        for (VersionUnit versionUnit : versionUnits) {
            Set<RepoPath> repoPaths = versionUnit.getRepoPaths();

            for (RepoPath repoPath : repoPaths) {
                BasicStatusHolder holder = transactionalMe.undeploy(repoPath, false, true);
                multiStatusHolder.merge(holder);
            }

            for (RepoPath parent : versionUnit.getParents()) {
                if (transactionalMe.exists(parent)) {
                    pathsForMavenMetadataCalculation.add(parent);
                }
            }
        }


        for (RepoPath path : pathsForMavenMetadataCalculation) {
            /**
             * Check again to make sure parent exists, might have been removed through the iterations of the version
             * units
             */
            if (transactionalMe.exists(path)) {
                getTransactionalMe().calculateMavenMetadataAsync(path);

            }
        }

        return multiStatusHolder;
    }

    public int zap(RepoPath repoPath) {
        int zappedItems = 0;
        LocalRepo localRepo = getLocalRepository(repoPath);
        if (localRepo.isCache()) {
            LocalCacheRepo cache = (LocalCacheRepo) localRepo;
            zappedItems = cache.zap(repoPath);
        } else {
            log.warn("Got a zap request on a non-local-cache node '" + repoPath + "'.");
        }
        return zappedItems;
    }

    public List<org.artifactory.fs.FolderInfo> getWithEmptyChildren(org.artifactory.fs.FolderInfo folderInfo) {
        LocalRepo repository = getLocalRepository(folderInfo.getRepoPath());
        JcrFolder folder = (JcrFolder) repository.getJcrFsItem(folderInfo.getRepoPath());

        FolderCompactor compactor = ContextHelper.get().beanForType(FolderCompactor.class);
        List<JcrFolder> children;
        children = compactor.getFolderWithCompactedChildren(folder);
        List<org.artifactory.fs.FolderInfo> result = Lists.newArrayList();
        for (JcrFolder child : children) {
            result.add(child.getInfo());
        }
        return result;
    }

    public Set<String> getAllRepoKeys() {
        return allRepoKeysCache;
    }

    public List<RepoDescriptor> getLocalAndRemoteRepoDescriptors() {
        return globalVirtualRepo.getDescriptor().getRepositories();
    }

    public boolean isAnonAccessEnabled() {
        return authService.isAnonAccessEnabled();
    }

    public Repo repositoryByKey(String key) {
        Repo repo = null;
        if (globalVirtualRepo.getLocalRepositoriesMap().containsKey(key)) {
            repo = globalVirtualRepo.localRepositoryByKey(key);
        } else if (globalVirtualRepo.getLocalCacheRepositoriesMap().containsKey(key)) {
            repo = globalVirtualRepo.getLocalCacheRepositoriesMap().get(key);
        } else if (globalVirtualRepo.getRemoteRepositoriesMap().containsKey(key)) {
            repo = globalVirtualRepo.getRemoteRepositoriesMap().get(key);
        } else if (virtualRepositoriesMap.containsKey(key)) {
            repo = virtualRepositoriesMap.get(key);
        } else if (globalVirtualRepo.getKey().equals(key)) {
            repo = globalVirtualRepo;
        }
        return repo;
    }

    public LocalRepo localRepositoryByKey(String key) {
        return globalVirtualRepo.localRepositoryByKey(key);
    }

    public RemoteRepo remoteRepositoryByKey(String key) {
        return globalVirtualRepo.remoteRepositoryByKey(key);
    }

    public VirtualRepo virtualRepositoryByKey(String key) {
        return virtualRepositoriesMap.get(key);
    }

    public LocalRepo localOrCachedRepositoryByKey(String key) {
        return globalVirtualRepo.localOrCachedRepositoryByKey(key);
    }

    @SuppressWarnings({"unchecked"})
    public <R extends Repo> RepoRepoPath<R> getRepoRepoPath(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        R repo = (R) repositoryByKey(repoKey);
        if (repo == null) {
            throw new IllegalArgumentException("Repository '" + repoKey + "' not found!");
        }
        RepoRepoPath<R> rrp = new RepoRepoPath<R>(repo, repoPath);
        return rrp;
    }

    public StoringRepo storingRepositoryByKey(String key) {
        LocalRepo localRepo = localOrCachedRepositoryByKey(key);
        if (localRepo != null) {
            return localRepo;
        } else {
            return virtualRepositoryByKey(key);
        }
    }

    public List<LocalRepoDescriptor> getLocalRepoDescriptors() {
        return new ArrayList<LocalRepoDescriptor>(
                centralConfigService.getDescriptor().getLocalRepositoriesMap().values());
    }

    public List<LocalCacheRepoDescriptor> getCachedRepoDescriptors() {
        List<LocalCacheRepo> localAndCached = globalVirtualRepo.getLocalCaches();
        List<LocalCacheRepoDescriptor> result = new ArrayList<LocalCacheRepoDescriptor>();
        for (LocalRepo localRepo : localAndCached) {
            result.add((LocalCacheRepoDescriptor) localRepo.getDescriptor());
        }
        return result;
    }

    public RepoDescriptor repoDescriptorByKey(String key) {
        Repo repo = globalVirtualRepo.repositoryByKey(key);
        if (repo != null) {
            return repo.getDescriptor();
        }
        return null;
    }

    public LocalRepoDescriptor localRepoDescriptorByKey(String key) {
        LocalRepo localRepo = globalVirtualRepo.localRepositoryByKey(key);
        if (localRepo != null) {
            return (LocalRepoDescriptor) localRepo.getDescriptor();
        }
        return null;
    }

    public LocalRepoDescriptor localOrCachedRepoDescriptorByKey(String key) {
        LocalRepo localRepo = globalVirtualRepo.localOrCachedRepositoryByKey(key);
        if (localRepo != null) {
            return (LocalRepoDescriptor) localRepo.getDescriptor();
        }
        return null;
    }

    public RemoteRepoDescriptor remoteRepoDescriptorByKey(String key) {
        RemoteRepo remoteRepo = globalVirtualRepo.remoteRepositoryByKey(key);
        if (remoteRepo != null) {
            return (RemoteRepoDescriptor) remoteRepo.getDescriptor();
        }
        return null;
    }

    public List<VirtualRepoDescriptor> getVirtualRepoDescriptors() {
        ArrayList<VirtualRepoDescriptor> list = new ArrayList<VirtualRepoDescriptor>();
        list.add(globalVirtualRepo.getDescriptor());
        list.addAll(centralConfigService.getDescriptor().getVirtualRepositoriesMap().values());
        return list;
    }

    public Repo nonCacheRepositoryByKey(String key) {
        Repo repo = globalVirtualRepo.nonCacheRepositoryByKey(key);
        if (repo == null) {
            repo = virtualRepositoriesMap.get(key);
        }
        assert repo != null;
        return repo;
    }

    public void markBaseForMavenMetadataRecalculation(RepoPath basePath) {
        LocalRepo localRepo = localRepositoryByKeyFailIfNull(basePath);
        JcrFolder baseFolder = localRepo.getLockedJcrFolder(basePath, false);
        if (baseFolder == null) {
            throw new IllegalArgumentException("No folder found in " + basePath);
        }
        Node node = baseFolder.getNode();
        try {
            node.setProperty(JcrTypes.PROP_ARTIFACTORY_RECALC_MAVEN_METADATA, true);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to mark node for maven metadata recalculation.", e);
        }
    }

    public void removeMarkForMavenMetadataRecalculation(RepoPath basePath) throws ItemNotFoundException {
        LocalRepo localRepo = localRepositoryByKeyFailIfNull(basePath);
        JcrFolder baseFolder = localRepo.getJcrFolder(basePath);
        if (baseFolder == null) {
            throw new ItemNotFoundException("No folder found in " + basePath);
        }
        Node baseFolderNode = baseFolder.getNode();
        try {
            if (baseFolderNode.hasProperty(JcrTypes.PROP_ARTIFACTORY_RECALC_MAVEN_METADATA)) {
                baseFolderNode.getProperty(JcrTypes.PROP_ARTIFACTORY_RECALC_MAVEN_METADATA).remove();
            }
        } catch (RepositoryException e) {
            log.error("Failed to remove maven metadata recalc mark");
        }
    }

    public boolean treeNodeContainsMavenPlugins(JcrTreeNode treeNode) {
        if (treeNode == null) {
            return false;
        }

        if (treeNode.isFolder()) {

            Set<JcrTreeNode> children = treeNode.getChildren();
            for (JcrTreeNode child : children) {
                if (treeNodeContainsMavenPlugins(child)) {
                    return true;
                }
            }

        } else {

            Node node = jcr.getNode(JcrPath.get().getAbsolutePath(treeNode.getRepoPath()));
            Node packagingNodeText = safeGetNode(node, "artifactory:xml", "project", "packaging", "jcr:xmltext");
            if (packagingNodeText != null) {
                try {
                    log.debug("Found Maven Plugin pom at '{}'", node.getPath());
                    if (packagingNodeText.hasProperty("jcr:xmlcharacters")) {
                        return "maven-plugin".equals(packagingNodeText.getProperty("jcr:xmlcharacters").getString());
                    }
                } catch (RepositoryException e) {
                    log.error("Error while reading pom packaging value for " + JcrHelper.display(node) + ".", e);
                }
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void updateDirtyState(RepoPath repoPath) {
        LocalRepo repo = localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (repo == null) {
            log.debug("Not updating download counters - {} is not a local or cached repo resource (no store in use?)",
                    repoPath);
            return;
        }

        // If already write locked nothing to do the stats, metadata in queue will be updated
        if (!repo.isWriteLocked(repoPath)) {
            // Just write locking the file will save any dirty state upon commit (stats and metadata)
            repo.getLockedJcrFsItem(repoPath);
        }
    }

    // get all folders marked for maven metadata calculation and execute the metadata calculation

    public void recalculateMavenMetadataOnMarkedFolders() {
        try {
            //Sort the results to ensure nodes are prefetched and nodes.getSize() will not return -1
            String queryStr = "//element(*, " + JcrTypes.NT_ARTIFACTORY_FOLDER + ")[@" +
                    JcrTypes.PROP_ARTIFACTORY_RECALC_MAVEN_METADATA + "] order by @jcr:score descending";
            QueryResult result = jcr.executeQuery(JcrQuerySpec.xpath(queryStr).noLimit());
            NodeIterator nodes = result.getNodes();
            if (nodes.hasNext()) {
                log.info("Found {} nodes marked for maven metadata recalculation.", nodes.getSize());
            }
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String path = node.getPath();
                RepoPath repoPath = JcrPath.get().getRepoPath(path);
                calculateMavenMetadataAsync(repoPath);
            }
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed with metadata calculation", e);
        }
    }

    public void calculateMavenMetadataAsync(RepoPath baseFolderPath) {
        calculateMavenMetadata(baseFolderPath);
    }

    public void calculateMavenMetadata(RepoPath baseFolderPath) {
        if (baseFolderPath == null) {
            log.debug("Cannot calculate Maven metadata for a null path ");
            return;
        }
        LocalRepo localRepo = localRepositoryByKey(baseFolderPath.getRepoKey());
        if (localRepo == null) {
            log.debug("Couldn't find local non-cache repository for path '{}'.", baseFolderPath);
            return;
        }

        if (!localRepo.itemExists(baseFolderPath.getPath())) {
            log.debug("Couldn't find path '{}'.", baseFolderPath);
            return;
        }

        MavenMetadataCalculator metadataCalculator = new MavenMetadataCalculator();
        JcrTreeNode treeNode = metadataCalculator.calculate(baseFolderPath, new MultiStatusHolder());
        try {
            getTransactionalMe().removeMarkForMavenMetadataRecalculation(baseFolderPath);
        } catch (ItemNotFoundException e) {
            //Action is async. Item might have been removed
            log.debug("Failed to remove maven metadata calculation mark: " + e.getMessage());
        }

        if (getTransactionalMe().treeNodeContainsMavenPlugins(treeNode)) {
            // Calculate maven plugins metadata asynchronously if the poms contains some maven plugins
            getTransactionalMe().calculateMavenPluginsMetadataAsync(localRepo.getKey());
        }
    }

    public void calculateMavenPluginsMetadataAsync(String repoKey) {

        if (pluginsMDQueue.contains(repoKey)) {
            log.debug("Plugins maven metadata calculation for repo '{}' already waiting in queue", repoKey);
            return;
        }

        // add the repository key to the queue (there's a small chance that the same key will be added twice but it
        // doesn't worth locking again)
        log.debug("Adding '{}' to the plugins maven metadata calculation queue", repoKey);
        pluginsMDQueue.add(repoKey);

        // try to acquire the single lock to do the metadata calculation. If we don't get it, another thread is already
        // performing the job and it will also do the one just added to the queue
        if (!pluginsMDSemaphore.tryAcquire()) {
            log.debug("Plugins maven metadata calculation already running in another thread");
            return;
        }

        // ok i'm in, lets perform all the job in the queue
        try {
            String repoToCalculate;
            while ((repoToCalculate = pluginsMDQueue.poll()) != null) {
                log.debug("Calculating plugins maven metadata for {}.", repoToCalculate);
                // start with removing the repoKey from the queue (if another request for the same repo is made while
                // this one is executing, it shouldn't be rejected)
                pluginsMDQueue.remove(repoToCalculate);
                try {
                    LocalRepo localRepo = localRepositoryByKeyFailIfNull(RepoPathImpl.repoRootPath(repoToCalculate));
                    MavenPluginsMetadataCalculator calculator = new MavenPluginsMetadataCalculator();
                    calculator.calculate(localRepo);
                } catch (Exception e) {
                    log.error("Failed to calculate plugin maven metadata on repo '" + repoToCalculate + "':", e);
                }
            }
        } finally {
            pluginsMDSemaphore.release();
        }
    }

    /**
     * This method will delete and import all the local and cached repositories listed in the (newly loaded) config
     * file. This action is resource intensive and is done in multiple transactions to avoid out of memory exceptions.
     */
    public void importFrom(ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(null, settings, false, true);
        } else {
            status.setStatus("Importing repositories...", log);
            internalImportFrom(settings);
            status.setStatus("Finished importing repositories...", log);
        }
    }

    public void assertValidDeployPath(LocalRepo repo, String path) throws RepoRejectException {
        BasicStatusHolder status = repo.assertValidPath(path, false);

        if (!status.isError()) {
            RepoPath repoPath = new RepoPathImpl(repo.getKey(), path);
            // if it is metadata, assert annotate privileges. Maven metadata is treated as regular file
            // (needs deploy permissions).
            if (NamingUtils.isMetadata(path) && !MavenNaming.isMavenMetadata(path)) {
                if (!authService.canAnnotate(repoPath)) {
                    String msg = "User " + authService.currentUsername() + " is not permitted to annotate '" +
                            path + "' on '" + repoPath + "'.";
                    status.setError(msg, HttpStatus.SC_FORBIDDEN, log);
                    AccessLogger.annotateDenied(repoPath);
                }
            } else {
                //Assert deploy privileges
                boolean canDeploy = authService.canDeploy(repoPath);
                if (!canDeploy) {
                    String msg = "User " + authService.currentUsername() + " is not permitted to deploy '" +
                            path + "' into '" + repoPath + "'.";
                    status.setError(msg, HttpStatus.SC_FORBIDDEN, log);
                    AccessLogger.deployDenied(repoPath);
                }
            }
            if (!status.isError()) {
                assertDelete(repo, path, true, status);
            }
        }
        if (status.isError()) {
            if (status.getException() != null) {
                Throwable throwable = status.getException();
                if (throwable instanceof RepoRejectException) {
                    throw (RepoRejectException) throwable;
                }
                throw new RepoRejectException(throwable);
            }
            throw new RepoRejectException(status.getStatusMsg(), status.getStatusCode());
        }
    }

    public <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(RequestContext requestContext,
            RemoteRepo<T> remoteRepo, RepoResource res) throws IOException, RepositoryException,
            RepoRejectException {
        LocalCacheRepo localCache = remoteRepo.getLocalCacheRepo();
        RepoResource cachedResource = localCache.getInfo(requestContext);
        return remoteRepo.downloadAndSave(requestContext, res, cachedResource);
    }

    public RepoResource unexpireIfExists(LocalRepo localCacheRepo, String path) {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource == null) {
            return new UnfoundRepoResource(new RepoPathImpl(localCacheRepo.getKey(), path), "Object is not in cache");
        }
        return resource;
    }

    public ResourceStreamHandle unexpireAndRetrieveIfExists(RequestContext requestContext, LocalRepo localCacheRepo,
            String path) throws IOException, RepositoryException, RepoRejectException {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource != null && resource.isFound()) {
            return localCacheRepo.getResourceStreamHandle(requestContext, resource);
        }
        return null;
    }

    public ResourceStreamHandle getResourceStreamHandle(RequestContext requestContext, Repo repo, RepoResource res)
            throws IOException, RepoRejectException, RepositoryException {
        if (res instanceof ResolvedResource) {
            // resource already contains the content - just extract it and return a string resource handle
            String content = ((ResolvedResource) res).getContent();
            return new StringResourceStreamHandle(content);
        } else {
            RepoPath repoPath = res.getRepoPath();
            if (repo.isReal()) {
                //Permissions apply only to real repos
                StatusHolder holder = ((RealRepo) repo).checkDownloadIsAllowed(repoPath);
                if (holder.isError()) {
                    throw new RepoRejectException(holder.getStatusMsg(), holder.getStatusCode());
                }
            }
            return repo.getResourceStreamHandle(requestContext, res);
        }
    }

    public List<VersionUnit> getVersionUnitsUnder(RepoPath repoPath) {
        List<VersionUnit> versionUnits = Lists.newArrayList();
        try {
            VersionUnitSearchControls controls = new VersionUnitSearchControls(repoPath);
            ItemSearchResults<VersionUnitSearchResult> searchResults = searchService.searchVersionUnits(controls);
            for (VersionUnitSearchResult result : searchResults.getResults()) {
                versionUnits.add(result.getVersionUnit());
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Could not get version units under '" + repoPath + "'.", e);
        }

        return versionUnits;
    }

    public ArtifactCount getArtifactCount() throws RepositoryRuntimeException {
        return jcr.getArtifactCount();
    }

    public ArtifactCount getArtifactCount(String repoKey) throws RepositoryRuntimeException {
        return jcr.getArtifactCount(repoKey);
    }

    public List<VirtualRepoDescriptor> getVirtualReposContainingRepo(RepoDescriptor repoDescriptor) {
        RepoDescriptor descriptor = repoDescriptor;
        if (repoDescriptor instanceof LocalCacheRepoDescriptor) {
            //VirtualRepoResolver does not directly support local cache repos, so if the items descriptor is a cache,
            //We extract the caches remote repo, and use it instead
            descriptor = ((LocalCacheRepoDescriptor) repoDescriptor).getRemoteRepo();
        }

        List<VirtualRepoDescriptor> reposToDisplay = new ArrayList<VirtualRepoDescriptor>();
        List<VirtualRepoDescriptor> virtualRepos = getVirtualRepoDescriptors();
        for (VirtualRepoDescriptor virtualRepo : virtualRepos) {
            VirtualRepoResolver resolver = new VirtualRepoResolver(virtualRepo);
            if (resolver.contains(descriptor)) {
                reposToDisplay.add(virtualRepo);
            }
        }
        return reposToDisplay;
    }

    /**
     * Returns a list of local repo descriptors that the current user is permitted to deploy to.
     *
     * @return List of deploy-permitted local repos
     */
    public List<LocalRepoDescriptor> getDeployableRepoDescriptors() {
        // if the user is an admin user, simply return all the deployable descriptors without checking specific
        // permission targets.
        if (authService.isAdmin()) {
            return getLocalRepoDescriptors();
        }
        List<PermissionTargetInfo> permissionTargetInfos =
                aclService.getPermissionTargets(ArtifactoryPermission.DEPLOY);
        Set<LocalRepoDescriptor> permittedDescriptors = Sets.newHashSet();
        Map<String, LocalRepoDescriptor> descriptorMap = centralConfigService.getDescriptor().getLocalRepositoriesMap();
        for (PermissionTargetInfo permissionTargetInfo : permissionTargetInfos) {
            List<String> repoKeys = permissionTargetInfo.getRepoKeys();
            if (repoKeys.contains(PermissionTargetInfo.ANY_REPO) ||
                    repoKeys.contains(PermissionTargetInfo.ANY_LOCAL_REPO)) {
                // return the list of all local repositories
                return getLocalRepoDescriptors();
            }
            for (String repoKey : repoKeys) {
                LocalRepoDescriptor permittedDescriptor = descriptorMap.get(repoKey);
                if (permittedDescriptor != null) {
                    permittedDescriptors.add(permittedDescriptor);
                }
            }
        }
        return Lists.newArrayList(permittedDescriptors);
    }

    public boolean isLocalRepoPathAccepted(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (!StringUtils.hasLength(path)) {
            return true;
        }
        LocalRepo repo = getLocalOrCachedRepository(repoPath);
        return repo.accepts(path);
    }

    public boolean isLocalRepoPathDisplayable(RepoPath repoPath) {
        return (repoPath != null) && authService.canRead(repoPath) &&
                (isLocalRepoPathAccepted(repoPath) || authService.canAnnotate(repoPath));
    }

    public boolean isLocalRepoPathHandled(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (!StringUtils.hasLength(path)) {
            return true;
        }
        LocalRepo repo = getLocalOrCachedRepository(repoPath);
        return repo.handlesReleaseSnapshot(path);
    }

    public List<RemoteRepoDescriptor> getSharedRemoteRepoConfigs(String remoteUrl, Map<String, String> headersMap) {

        List<RemoteRepoDescriptor> remoteRepos = Lists.newArrayList();
        List<RepoDetails> remoteReposDetails = getSharedRemoteRepoDetails(remoteUrl, headersMap);
        boolean hasDefaultProxy = centralConfigService.defaultProxyDefined();
        ProxyDescriptor defaultProxy = centralConfigService.getMutableDescriptor().getDefaultProxy();
        for (RepoDetails remoteRepoDetails : remoteReposDetails) {
            String configurationUrl = remoteRepoDetails.getConfiguration();
            if (org.apache.commons.lang.StringUtils.isNotBlank(configurationUrl)) {
                RemoteRepoDescriptor remoteRepoConfig = getSharedRemoteRepoConfig(configurationUrl, headersMap);
                if (remoteRepoConfig != null) {
                    if (hasDefaultProxy && defaultProxy != null) {
                        ((HttpRepoDescriptor) remoteRepoConfig).setProxy(defaultProxy);
                    }
                    RepoLayout repoLayout = remoteRepoConfig.getRepoLayout();

                    //If there is no contained layout or if it doesn't exist locally, just add the default
                    if ((repoLayout == null) ||
                            centralConfigService.getDescriptor().getRepoLayout(repoLayout.getName()) == null) {
                        remoteRepoConfig.setRepoLayout(RepoLayoutUtils.MAVEN_2_DEFAULT);
                    }

                    RepoLayout remoteRepoLayout = remoteRepoConfig.getRemoteRepoLayout();
                    //If there is contained layout doesn't exist locally, remove it
                    if ((remoteRepoLayout != null) &&
                            centralConfigService.getDescriptor().getRepoLayout(remoteRepoLayout.getName()) == null) {
                        remoteRepoConfig.setRemoteRepoLayout(null);
                    }

                    remoteRepos.add(remoteRepoConfig);
                }
            }
        }

        return remoteRepos;
    }

    public ZipEntriesTree zipEntriesToTree(RepoPath zipPath) throws IOException {
        LocalRepo localRepo = getLocalOrCachedRepository(zipPath);
        JcrFile file = localRepo.getLocalJcrFile(zipPath.getPath());
        ZipInputStream zin = null;
        try {
            ZipEntriesTree tree;
            InputStream in = file.getStream();
            zin = new ZipInputStream(in);
            ZipEntry zipEntry;
            tree = new ZipEntriesTree();
            try {
                while ((zipEntry = zin.getNextEntry()) != null) {
                    tree.insert(new ZipEntryInfo(zipEntry));
                }
                // IllegalArgumentException is being thrown from: java.util.zip.ZipInputStream.getUTF8String on a
                // bad archive
            } catch (IllegalArgumentException e) {
                throw new IOException(
                        "An error occurred while reading entries from zip file: " + file.getAbsoluteFile());
            }
            return tree;
        } finally {
            IOUtils.closeQuietly(zin);
        }
    }

    public org.artifactory.fs.ItemInfo getLastModified(RepoPath pathToSearch) {
        if (pathToSearch == null) {
            throw new IllegalArgumentException("Repo path cannot be null.");
        }
        if (!exists(pathToSearch)) {
            throw new ItemNotFoundRuntimeException("Could not find item: " + pathToSearch.getId());
        }

        org.artifactory.fs.ItemInfo itemLastModified = collectLastModifiedRecursively(pathToSearch);
        return itemLastModified;
    }

    public void fixChecksums(RepoPath fileRepoPath) {
        LocalRepo localRepo = getLocalRepository(fileRepoPath);
        JcrFile file = localRepo.getLockedJcrFile(fileRepoPath, false);
        if (file == null) {
            throw new ItemNotFoundRuntimeException("File " + fileRepoPath + " does not exists");
        }
        org.artifactory.fs.FileInfo fileInfo = file.getInfo();
        ChecksumsInfo checksumsInfo = new ChecksumsInfo(fileInfo.getChecksumsInfo());   // work on a copy
        for (org.artifactory.checksum.ChecksumInfo checksumInfo : checksumsInfo.getChecksums()) {
            if (!checksumInfo.checksumsMatch()) {
                // replace inconsistent checksum with a trusted one
                fileInfo.addChecksumInfo(new ChecksumInfo(
                        checksumInfo.getType(), ChecksumInfo.TRUSTED_FILE_MARKER, checksumInfo.getActual()));
            }
        }
    }

    /**
     * Returns the latest modified item of the given file or folder (recursively)
     *
     * @param pathToSearch Repo path to search in
     * @return Latest modified item
     */
    private org.artifactory.fs.ItemInfo collectLastModifiedRecursively(RepoPath pathToSearch) {
        org.artifactory.fs.ItemInfo latestItem = getItemInfo(pathToSearch);

        if (latestItem.isFolder()) {
            List<ItemInfo> children = getChildren(pathToSearch);
            for (ItemInfo child : children) {
                org.artifactory.fs.ItemInfo itemInfo = collectLastModifiedRecursively(child.getRepoPath());
                if (itemInfo.getLastModified() > latestItem.getLastModified()) {
                    latestItem = itemInfo;
                }
            }
        }

        return latestItem;
    }

    private String importAsync(@Nullable String repoKey, ImportSettings settings, boolean deleteExistingRepo,
            boolean wait) {
        MultiStatusHolder status = settings.getStatusHolder();
        TaskBase task = TaskUtils.createManualTask(ImportJob.class, 0L);
        task.addAttribute(ImportJob.REPO_KEY, repoKey);
        task.addAttribute(ImportJob.DELETE_REPO, deleteExistingRepo);
        task.addAttribute(ImportSettings.class.getName(), settings);
        taskService.startTask(task, true);
        if (wait) {
            boolean completed = taskService.waitForTaskCompletion(task.getToken());
            if (!completed) {
                if (!status.isError()) {
                    // Add error of no completion
                    status.setError("The task " + task + " did not complete correctly.", log);
                }
            }
        }
        return task.getToken();
    }

    private void exportAsync(String repoKey, ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        TaskBase task = TaskUtils.createManualTask(ExportJob.class, 0L);
        if (repoKey != null) {
            task.addAttribute(ExportJob.REPO_KEY, repoKey);
        }
        task.addAttribute(ExportSettings.class.getName(), settings);
        taskService.startTask(task, true);
        boolean completed = taskService.waitForTaskCompletion(task.getToken());
        if (!completed) {
            if (!status.isError()) {
                // Add Error of no completion
                status.setError("The task " + task + " did not complete correctly", log);
            }
        }
    }

    public LocalRepo getLocalRepository(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
        if (localRepo == null) {
            throw new IllegalArgumentException("Repository " + repoKey + " is not a local repository");
        }
        return localRepo;
    }

    /**
     * Do the actual full import.
     *
     * @param settings
     * @return true if success, false otherwise
     */
    private boolean internalImportFrom(ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        File repoRootPath = new File(settings.getBaseDir(), JcrPath.get().getRepoJcrRootPath());
        //Keep the current list of repositories for deletion after or during import
        List<LocalRepoDescriptor> oldRepoList = getLocalAndCachedRepoDescriptors();
        //Import all local repositories
        List<LocalRepoDescriptor> newRepoList = settings.getRepositories();
        if (newRepoList.isEmpty()) {
            newRepoList = getLocalAndCachedRepoDescriptors();
        }
        ImportSettings repositoriesImportSettings = new ImportSettings(repoRootPath, settings);
        importAll(newRepoList, oldRepoList, repositoriesImportSettings);
        return !status.isError();
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private void importAll(List<LocalRepoDescriptor> newRepoList, List<LocalRepoDescriptor> oldRepoList,
            ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        List<String> tokens = new ArrayList<String>(newRepoList.size());
        File baseDir = settings.getBaseDir();
        List<String> children = new ArrayList<String>();
        String[] baseDirList = new String[]{};
        if (baseDir.list() != null) {
            baseDirList = baseDir.list();
        }
        List<String> listOfRepoKeys = Arrays.asList(baseDirList);
        children.addAll(listOfRepoKeys);
        int activeImports = 0;
        int maxParallelImports = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        for (LocalRepoDescriptor newLocalRepo : newRepoList) {
            File rootImportFolder = new File(settings.getBaseDir(), newLocalRepo.getKey());
            try {
                if (rootImportFolder.exists()) {
                    if (rootImportFolder.isDirectory()) {
                        ImportSettings repoSettings = new ImportSettings(rootImportFolder, settings);
                        boolean deleteExistingRepo = false;
                        if (oldRepoList.contains(newLocalRepo)) {
                            // Full repo delete with undeploy on root repo path
                            deleteExistingRepo = true;
                        }
                        // avoid spawning too many threads
                        boolean wait = activeImports == maxParallelImports;
                        if (!wait) {
                            activeImports++;
                        }
                        String importTaskToken =
                                importAsync(newLocalRepo.getKey(), repoSettings, deleteExistingRepo, wait);
                        if (!wait) {
                            tokens.add(importTaskToken);
                        }
                    }
                    children.remove(newLocalRepo.getKey());
                }
            } catch (Exception e) {
                status.setError("Could not import repository " + newLocalRepo + " from " + rootImportFolder, e, log);
                if (settings.isFailFast()) {
                    return;
                }
            }
        }

        if ((children.size() == baseDirList.length) && settings.isFailIfEmpty()) {
            status.setError("The selected directory did not contain any repositories.", log);
        } else {
            for (String unusedDir : children) {
                boolean isMetadata = unusedDir.contains("metadata");
                boolean isIndex = unusedDir.contains("index");
                if (!isMetadata && !isIndex) {
                    status.setWarning("The directory " + unusedDir + " does not match any repository key.", log);
                }
            }
        }

        for (String token : tokens) {
            try {
                taskService.waitForTaskCompletion(token);
            } catch (Exception e) {
                status.setError("error waiting for repository import completion", e, log);
                if (settings.isFailFast()) {
                    return;
                }
            }
        }
    }

    private void initAllRepoKeysCache() {
        Set<String> newKeys = new HashSet<String>();
        newKeys.addAll(globalVirtualRepo.getLocalRepositoriesMap().keySet());
        newKeys.addAll(globalVirtualRepo.getRemoteRepositoriesMap().keySet());
        newKeys.addAll(globalVirtualRepo.getLocalCacheRepositoriesMap().keySet());
        newKeys.addAll(virtualRepositoriesMap.keySet());
        allRepoKeysCache = newKeys;
    }

    private RepoResource internalUnexpireIfExists(LocalRepo repo, String path) {
        // Need to release the read lock first
        RepoPath repoPath = new RepoPathImpl(repo.getKey(), path);
        RepoPath fsItemRepoPath = RepoPathImpl.getLockingTargetRepoPath(repoPath);
        // Write lock auto upgrade supported LockingHelper.releaseReadLock(fsItemRepoPath);
        JcrFsItem fsItem = repo.getLockedJcrFsItem(fsItemRepoPath);
        if (fsItem != null) {
            log.debug("{}: falling back to using cache entry for resource info at '{}'.", this, path);
            //Reset the resource age so it is kept being cached
            fsItem.unexpire();
            return repo.getInfo(new NullRequestContext(repoPath));
        }
        return null;
    }

    private static InternalRepositoryService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(InternalRepositoryService.class);
    }

    private void assertDelete(StoringRepo repo, String path, boolean assertOverwrite, BasicStatusHolder statusHolder) {
        RepoPath repoPath = new RepoPathImpl(repo.getKey(), path);
        //Check that has delete rights to replace an exiting item
        if (repo.shouldProtectPathDeletion(path, assertOverwrite)) {
            if (!authService.canDelete(repoPath)) {
                AccessLogger.deleteDenied(repoPath);
                if (centralConfigService.getDescriptor().getSecurity().isHideUnauthorizedResources()) {
                    statusHolder.setError("Could not locate artifact '" + repoPath + "'.", HttpStatus.SC_NOT_FOUND,
                            log);
                } else {
                    statusHolder.setError("Not enough permissions to overwrite artifact '" + repoPath + "' (user '" +
                            authService.currentUsername() + "' needs DELETE permission).", HttpStatus.SC_FORBIDDEN,
                            log);
                }
            }
        }

        boolean isMetadata = NamingUtils.isMetadata(path);
        //For deletion (as opposed to overwrite), check that path actually exists
        if (!isMetadata && !assertOverwrite && !repo.itemExists(repoPath.getPath())) {
            statusHolder.setError("Could not locate artifact '" + repoPath + "' (Nothing to delete).",
                    HttpStatus.SC_NOT_FOUND, log);
        }
    }

    // remove export folders of repositories that are not part of the current backup included repositories
    // this cleanup is needed in incremental backup when a repository is excluded from the backup or removed

    private void cleanupIncrementalBackupDirectory(File targetDir,
            List<LocalRepoDescriptor> reposToBackup) {
        if (!targetDir.exists()) {
            log.debug("Repositories backup directory doesn't exist: {}", targetDir.getAbsolutePath());
            return; // nothing to clean
        }
        File[] childFiles = targetDir.listFiles();
        for (File childFile : childFiles) {
            String fileName = childFile.getName();
            if (fileName.endsWith(MetadataAware.METADATA_FOLDER)) {
                continue;  // skip metadata folders, will delete them with the actual folder if needed
            }
            boolean includedInBackup = false;
            for (LocalRepoDescriptor jcrFsItem : reposToBackup) {
                if (fileName.equals(jcrFsItem.getKey())) {
                    includedInBackup = true;
                    break;
                }
            }
            if (!includedInBackup) {
                log.info("Deleting {} from the incremental backup dir since it is not part " +
                        "of the backup included repositories", childFile.getAbsolutePath());
                boolean deleted = FileUtils.deleteQuietly(childFile);
                if (!deleted) {
                    log.warn("Failed to delete {}", childFile.getAbsolutePath());
                }
                // now delete the metadata folder of the repository is it exists
                File metadataFolder = new File(childFile.getParentFile(), childFile.getName() +
                        MetadataAware.METADATA_FOLDER);
                if (metadataFolder.exists()) {
                    deleted = FileUtils.deleteQuietly(metadataFolder);
                    if (!deleted) {
                        log.warn("Failed to delete metadata folder {}", metadataFolder.getAbsolutePath());
                    }
                }
            }
        }
    }

    private JcrFsItem getFsItem(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        // will place a read lock on the fsItem
        JcrFsItem fsItem = localRepo.getJcrFsItem(repoPath);
        if (fsItem == null) {
            log.debug("No file or folder found at {}", repoPath);
        }
        return fsItem;
    }

    private LocalRepo getLocalOrCachedRepository(RepoPath repoPath) {
        return globalVirtualRepo.localOrCachedRepositoryByKey(repoPath.getRepoKey());
    }

    /**
     * Returns an instance of the Repo Path Mover
     *
     * @return RepoPathMover
     */
    private RepoPathMover getRepoPathMover() {
        return ContextHelper.get().beanForType(RepoPathMover.class);
    }

    /**
     * Aggregates and unifies the given paths by parent
     *
     * @param pathsToMove        Paths to be moved\copied
     * @param targetLocalRepoKey Key of target local repo
     * @return Set of aggregated paths to move
     */
    private Set<RepoPath> aggregatePathsToMove(Set<RepoPath> pathsToMove, String targetLocalRepoKey, boolean copy) {
        // aggregate paths by parent repo path
        Multimap<RepoPath, RepoPath> pathsByParent = HashMultimap.create();
        for (RepoPath pathToMove : pathsToMove) {
            if (!pathToMove.getRepoKey().equals(targetLocalRepoKey)) {
                pathsByParent.put(pathToMove.getParent(), pathToMove);
            }
        }

        // now for each parent check if all its files are moved, and if they do, we will move
        // the parent folder and its children instead of just the children
        Set<RepoPath> pathsToMoveIncludingParents = new HashSet<RepoPath>();
        for (RepoPath parentPath : pathsByParent.keySet()) {
            Collection<RepoPath> children = pathsByParent.get(parentPath);
            if (!StringUtils.hasText(parentPath.getPath())) {
                // parent is the repository itself and cannot be moved, just add the children
                pathsToMoveIncludingParents.addAll(children);
            } else {
                // if the parent children count equals to the number of files to be moved, move the folder instead
                LocalRepo repository = getLocalRepository(parentPath);
                JcrFolder folder =
                        copy ? repository.getJcrFolder(parentPath) : repository.getLockedJcrFolder(parentPath, false);
                // get all the folder children using write lock
                List<JcrFsItem> folderChildren = jcrRepoService.getChildren(folder, !copy);
                if (folder != null && folderChildren.size() == children.size()) {
                    pathsToMoveIncludingParents.add(parentPath);
                } else {
                    pathsToMoveIncludingParents.addAll(children);
                }
            }
        }
        return pathsToMoveIncludingParents;
    }

    private String adjustRefererValue(Map<String, String> headersMap, String headerVal) {
        //Append the artifactory uagent to the referer
        if (headerVal == null) {
            //Fallback to host
            headerVal = headersMap.get("HOST");
            if (headerVal == null) {
                //Fallback to unknown
                headerVal = "UNKNOWN";
            }
        }
        if (!headerVal.startsWith("http")) {
            headerVal = "http://" + headerVal;
        }
        try {
            java.net.URL uri = new java.net.URL(headerVal);
            //Only use the uri up to the path part
            headerVal = uri.getProtocol() + "://" + uri.getAuthority();
        } catch (MalformedURLException e) {
            //Nothing
        }
        headerVal += "/" + HttpUtils.getArtifactoryUserAgent();
        return headerVal;
    }

    /**
     * Returns a list of shared repository details
     *
     * @param remoteUrl  URL of remote Artifactory instance
     * @param headersMap Map of headers to set for client
     * @return List of shared repository details
     */
    private List<RepoDetails> getSharedRemoteRepoDetails(String remoteUrl, Map<String, String> headersMap) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(remoteUrl);
        if (!remoteUrl.endsWith("/")) {
            urlBuilder.append("/");
        }
        urlBuilder.append(RestConstants.PATH_API).append("/").append(RepositoriesRestConstants.PATH_ROOT).
                append("?").append(RepositoriesRestConstants.PARAM_REPO_TYPE).append("=").
                append(RepoDetailsType.REMOTE.name());

        InputStream responseStream = null;
        try {
            responseStream = executeGetMethod(urlBuilder.toString(), headersMap);
            if (responseStream == null) {
                return Lists.newArrayList();
            }
            return JacksonReader.streamAsValueTypeReference(responseStream, new TypeReference<List<RepoDetails>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }

    /**
     * Returns the shared remote repository descriptor from the given configuration URL
     *
     * @param configUrl  URL of repository configuration
     * @param headersMap Map of headers to set for client
     * @return RemoteRepoDescriptor
     */
    private RemoteRepoDescriptor getSharedRemoteRepoConfig(String configUrl, Map<String, String> headersMap) {
        InputStream responseStream = null;
        try {
            responseStream = executeGetMethod(configUrl, headersMap);
            if (responseStream == null) {
                return null;
            }
            return JacksonReader.streamAsValueTypeReference(responseStream, new TypeReference<HttpRepoDescriptor>() {
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }

    /**
     * Executes an HTTP GET method
     *
     * @param url        URL to query
     * @param headersMap Map of headers to set for client
     * @return Input stream if execution was successfull, Null if not
     * @throws IOException
     */
    private InputStream executeGetMethod(String url, Map<String, String> headersMap) throws IOException {
        GetMethod getMethod = new GetMethod(url);

        //Append headers
        setHeader(getMethod, headersMap, "User-Agent");
        setHeader(getMethod, headersMap, "Referer");

        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();

        HttpClient client = new HttpClientConfigurator()
                .soTimeout(15000)
                .connectionTimeout(1500)
                .retry(0, false)
                .proxy(proxy).getClient();

        client.executeMethod(getMethod);
        if (getMethod.getStatusCode() == HttpStatus.SC_OK) {
            return getMethod.getResponseBodyAsStream();
        }

        return null;
    }

    /**
     * Sets the HTTP headers for the given method
     *
     * @param getMethod  Get method that should be set with the headers
     * @param headersMap Map of headers to set
     * @param headerKey  Key of header to set
     */
    private void setHeader(GetMethod getMethod, Map<String, String> headersMap, String headerKey) {
        String headerVal = headersMap.get(headerKey.toUpperCase());
        if ("Referer".equalsIgnoreCase(headerKey)) {
            headerVal = adjustRefererValue(headersMap, headerVal);
        }
        if (headerVal != null) {
            getMethod.setRequestHeader(headerKey, headerVal);
        }
    }

    private LocalRepo localRepositoryByKeyFailIfNull(RepoPath localRepoPath) {
        LocalRepo localRepo = localRepositoryByKey(localRepoPath.getRepoKey());
        if (localRepo == null) {
            throw new IllegalArgumentException("Couldn't find local non-cache repository for path " + localRepoPath);
        }
        return localRepo;
    }
}
