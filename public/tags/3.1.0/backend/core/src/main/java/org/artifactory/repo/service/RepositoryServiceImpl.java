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
import org.artifactory.addon.GemsAddon;
import org.artifactory.addon.NuGetAddon;
import org.artifactory.addon.WebstartAddon;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettingsImpl;
import org.artifactory.api.config.ImportSettingsImpl;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.jackson.JacksonReader;
import org.artifactory.api.maven.MavenMetadataService;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.api.module.VersionUnit;
import org.artifactory.api.repo.ArchiveFileContent;
import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.request.UploadService;
import org.artifactory.api.rest.constant.RepositoriesRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SavedSearchResults;
import org.artifactory.api.search.deployable.VersionUnitSearchControls;
import org.artifactory.api.search.deployable.VersionUnitSearchResult;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.storage.StorageQuotaInfo;
import org.artifactory.api.storage.StorageService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.common.StatusHolder;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.*;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.factory.xstream.XStreamInfoFactory;
import org.artifactory.fs.FileInfo;
import org.artifactory.fs.FolderInfo;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.MutableFileInfo;
import org.artifactory.fs.RepoResource;
import org.artifactory.fs.StatsInfo;
import org.artifactory.fs.ZipEntryInfo;
import org.artifactory.info.InfoWriter;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.mbean.MBeanRegistrationService;
import org.artifactory.md.Properties;
import org.artifactory.mime.NamingUtils;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.repo.*;
import org.artifactory.repo.cleanup.FolderPruningService;
import org.artifactory.repo.count.ArtifactCountRetriever;
import org.artifactory.repo.db.DbLocalRepo;
import org.artifactory.repo.db.importexport.DbRepoExportSearchHandler;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.local.PathDeletionContext;
import org.artifactory.repo.local.ValidDeployPathContext;
import org.artifactory.repo.mbean.ManagedRepository;
import org.artifactory.repo.service.mover.MoverConfig;
import org.artifactory.repo.service.mover.MoverConfigBuilder;
import org.artifactory.repo.service.mover.RepoPathMover;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.request.InternalArtifactoryResponse;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.NullRequestContext;
import org.artifactory.request.RepoRequests;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.ResolvedResource;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.sapi.common.BaseSettings;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.fs.MutableVfsFile;
import org.artifactory.sapi.fs.MutableVfsFolder;
import org.artifactory.sapi.fs.MutableVfsItem;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskCallback;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.security.ArtifactoryPermission;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.fs.lock.LockingHelper;
import org.artifactory.storage.fs.service.FileService;
import org.artifactory.storage.fs.service.ItemMetaInfo;
import org.artifactory.storage.fs.service.NodeMetaInfoService;
import org.artifactory.storage.fs.service.PropertiesService;
import org.artifactory.storage.fs.service.StatsService;
import org.artifactory.storage.fs.stats.StatsFlushJob;
import org.artifactory.util.HttpClientConfigurator;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.RepoLayoutUtils;
import org.artifactory.util.RepoPathUtils;
import org.artifactory.util.Tree;
import org.artifactory.version.CompoundVersionDetails;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Reloadable(beanClass = InternalRepositoryService.class,
        initAfter = {StorageInterceptors.class, InternalCentralConfigService.class, TaskService.class})
public class RepositoryServiceImpl implements InternalRepositoryService {
    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    private static final String REPOSITORIES_MBEAN_TYPE = "Repositories";

    @Autowired
    private AclService aclService;

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private MavenMetadataService mavenMetadataService;

    @Autowired
    private InternalSearchService searchService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private FileService fileService;

    @Autowired
    private FolderPruningService pruneService;

    private ArtifactCountRetriever artifactCountRetriever;

    private VirtualRepo globalVirtualRepo;

    private Map<String, VirtualRepo> virtualRepositoriesMap = Maps.newLinkedHashMap();

    // a cache of all the repository keys
    private Set<String> allRepoKeysCache;

    @Override
    public void init() {
        rebuildRepositories(null);
        HttpUtils.resetArtifactoryUserAgent();
        try {
            //Dump info to the log
            InfoWriter.writeInfo();
        } catch (Exception e) {
            log.warn("Failed dumping system info", e);
        }

        // register statistics flushing job
        TaskBase statsFlushTask = TaskUtils.createRepeatingTask(StatsFlushJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.statsFlushIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(ConstantValues.statsFlushIntervalSecs.getLong()));
        taskService.startTask(statsFlushTask, false);
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        HttpUtils.resetArtifactoryUserAgent();
        deleteOrphanRepos(oldDescriptor);
        rebuildRepositories(oldDescriptor);
        checkAndCleanChangedVirtualPomCleanupPolicy(oldDescriptor);
    }

    @Override
    @Async(authenticateAsSystem = true)
    public void onContextReady() {
        //
        registerRepositoriesMBeans();

        //
        GemsAddon gemsAddon = addonsManager.addonByType(GemsAddon.class);
        if (!gemsAddon.isDefault()) {
            for (LocalRepo localRepo : globalVirtualRepo.getLocalRepositories()) {
                if (!localRepo.isBlackedOut()) {
                    if (localRepo.getDescriptor().isEnableGemsSupport()) {
                        gemsAddon.afterRepoInit(localRepo.getKey());
                    }
                }
            }
            for (VirtualRepo virtualRepo : getVirtualRepositories()) {
                if (virtualRepo.getDescriptor().isEnableGemsSupport()) {
                    gemsAddon.afterRepoInit(virtualRepo.getKey());
                }
            }
        }

        //
        NuGetAddon nuGetAddon = addonsManager.addonByType(NuGetAddon.class);
        if (!nuGetAddon.isDefault()) {
            for (LocalRepo localRepo : globalVirtualRepo.getLocalRepositories()) {
                if (!localRepo.isBlackedOut()) {
                    if (localRepo.getDescriptor().isEnableNuGetSupport()) { //feature
                        nuGetAddon.afterRepoInit(localRepo.getKey());
                    }
                }
            }
        }
    }

    @Override
    public void onContextCreated() {
    }

    @Override
    public void onContextUnready() {
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
                RepoPath rootPath = InternalRepoPathFactory.repoRootPath(repoKey);
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
        Set<String> newRepoKeys = getConfigRepoKeys(currentDescriptor);
        Set<String> oldRepoKeys = getConfigRepoKeys(oldDescriptor);
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

    //TORE: [by YS] delete from the db directly - there's no need for permissions checks, events etc.
    private StatusHolder deleteOrphanRepo(String repoKey) {
        BasicStatusHolder status = new BasicStatusHolder();
        StoringRepo storingRepo = storingRepositoryByKey(repoKey);
        if (storingRepo == null) {
            status.warn("Repo not found for deletion: " + repoKey, log);
            return status;
        }

        MutableVfsFolder rootFolder = storingRepo.getMutableFolder(storingRepo.getRepoPath(""));
        if (rootFolder == null) {
            status.warn("Root folder not found for deletion: " + repoKey, log);
            return status;
        }

        rootFolder.deleteIncludingRoot();
        return status;
    }

    private Set<String> getConfigRepoKeys(CentralConfigDescriptor descriptor) {
        Set<String> repoKeys = new HashSet<>();
        repoKeys.addAll(descriptor.getLocalRepositoriesMap().keySet());
        repoKeys.addAll(descriptor.getRemoteRepositoriesMap().keySet());
        repoKeys.addAll(descriptor.getVirtualRepositoriesMap().keySet());
        return repoKeys;
    }

    @Override
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

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    private void rebuildRepositories(CentralConfigDescriptor oldDescriptor) {
        if (globalVirtualRepo != null) {
            // stop remote repo online monitors
            for (RemoteRepo remoteRepo : globalVirtualRepo.getRemoteRepositories()) {
                remoteRepo.cleanupResources();
            }
        }

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
            DbLocalRepo<LocalRepoDescriptor> oldLocalRepo = null;
            String key = repoDescriptor.getKey();
            if (oldLocalRepos != null) {
                LocalRepo oldRepo = oldLocalRepos.get(key);
                if (oldRepo != null) {
                    if (!(oldRepo instanceof DbLocalRepo)) {
                        log.error("Reloading configuration did not find local repository " + key);
                    } else {
                        //noinspection unchecked
                        oldLocalRepo = (DbLocalRepo<LocalRepoDescriptor>) oldRepo;
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
            LocalRepo repo = new DbLocalRepo<>(repoDescriptor, transactionalMe, oldLocalRepo);
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
        NuGetAddon nuGetAddon = addonsManager.addonByType(NuGetAddon.class);
        for (RemoteRepoDescriptor repoDescriptor : remoteRepoDescriptorMap.values()) {
            RemoteRepo oldRemoteRepo = null;
            if (oldRemoteRepos != null) {
                oldRemoteRepo = oldRemoteRepos.get(repoDescriptor.getKey());
            }
            RemoteRepo repo = nuGetAddon.createRemoteRepo(transactionalMe, repoDescriptor,
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
        List<RepoDescriptor> localAndRemoteRepoDescriptors = new ArrayList<>();
        localAndRemoteRepoDescriptors.addAll(localRepoDescriptorMap.values());
        localAndRemoteRepoDescriptors.addAll(remoteRepoDescriptorMap.values());
        VirtualRepoDescriptor vrd = new VirtualRepoDescriptor();
        vrd.setRepositories(localAndRemoteRepoDescriptors);
        vrd.setArtifactoryRequestsCanRetrieveRemoteArtifacts(
                ConstantValues.artifactoryRequestsToGlobalCanRetrieveRemoteArtifacts.getBoolean());
        vrd.setKey(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY);
        // create and init the global virtual repo
        globalVirtualRepo = new VirtualRepo(vrd, transactionalMe, localRepositoriesMap, remoteRepositoriesMap);
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

    @Override
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

    @Override
    public ModuleInfo getItemModuleInfo(RepoPath repoPath) {
        Repo repo = assertRepoKey(repoPath);
        return repo.getItemModuleInfo(repoPath.getPath());
    }

    private ModuleInfo getDescriptorModuleInfo(RepoPath repoPath) {
        Repo repo = assertRepoKey(repoPath);
        return repo.getDescriptorModuleInfo(repoPath.getPath());
    }

    @Override
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
        return InternalRepoPathFactory.create(repoPath.getRepoKey(), descriptorPath);
    }

    private Repo assertRepoKey(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        Repo repo = repositoryByKey(repoKey);
        if (repo == null) {
            throw new IllegalArgumentException("Repository '" + repoKey + "' not found!");
        }
        return repo;
    }

    @Override
    public boolean mkdirs(RepoPath folderRepoPath) {
        StoringRepo storingRepo = storingRepositoryByKey(folderRepoPath.getRepoKey());
        if (!storingRepo.itemExists(folderRepoPath.getPath())) {
            MutableVfsFolder folder = storingRepo.createOrGetFolder(folderRepoPath);
            return folder.isNew();
        }
        return false;
    }

    @Override
    public boolean virtualItemExists(RepoPath repoPath) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(repoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new RepositoryRuntimeException(
                    "Repository " + repoPath.getRepoKey() + " does not exists!");
        }
        return virtualRepo.virtualItemExists(repoPath.getPath());
    }

    @Override
    @Nonnull
    public MutableVfsItem getMutableItem(RepoPath repoPath) {
        //TORE: [by YS] should be storing repo once interfaces refactoring is done
        LocalRepo localRepo = localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (localRepo != null) {
            MutableVfsItem mutableFsItem = localRepo.getMutableFsItem(repoPath);
            if (mutableFsItem != null) {
                return mutableFsItem;
            }
        }
        throw new ItemNotFoundRuntimeException(repoPath);
    }

    private MutableVfsFile getMutableFile(RepoPath repoPath) {
        MutableVfsItem mutableItem = getMutableItem(repoPath);
        if (!(mutableItem instanceof MutableVfsFile)) {
            throw new FileExpectedException(repoPath);
        }
        return (MutableVfsFile) mutableItem;
    }

    @Override
    @Nullable
    public StatsInfo getStatsInfo(RepoPath repoPath) {
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            return null;
        }
        return statsService.getStats(repoPath);
    }

    @Override
    public long getArtifactCount(RepoPath repoPath) {
        return fileService.getFilesCount(repoPath);
    }

    @Override
    public long getNodesCount(RepoPath repoPath) {
        return fileService.getNodesCount(repoPath);
    }

    @Override
    public List<FileInfo> searchFilesWithBadChecksum(ChecksumType type) {
        return fileService.searchFilesWithBadChecksum(type);
    }

    @Override
    @Nonnull
    public List<ItemInfo> getChildren(RepoPath repoPath) {
        List<ItemInfo> filteredChildren = Lists.newArrayList();

        LocalRepo repo = localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (repo != null && repo.itemExists(repoPath.getPath())) {
            List<ItemInfo> children = fileService.loadChildren(repoPath);
            for (ItemInfo child : children) {
                //Check if we should return the child
                boolean childReader = authService.canImplicitlyReadParentPath(child.getRepoPath());
                if (!childReader) {
                    //Don't bother with stuff that we do not have read access to
                    continue;
                }
                filteredChildren.add(child);
            }
        }

        //Sort files by name
        Collections.sort(filteredChildren);
        return filteredChildren;
    }

    //todo [mamo] make more efficient - no real need for ItemInfo
    @Override
    public List<String> getChildrenNames(RepoPath repoPath) {
        List<ItemInfo> childrenInfo = getChildren(repoPath);
        List<String> childrenNames = Lists.newArrayListWithCapacity(childrenInfo.size());
        for (ItemInfo itemInfo : childrenInfo) {
            childrenNames.add(itemInfo.getName());
        }
        return childrenNames;
    }

    @Override
    public boolean hasChildren(RepoPath repoPath) {
        return fileService.hasChildren(repoPath);
    }

    @Override
    public VirtualRepo getGlobalVirtualRepo() {
        return globalVirtualRepo;
    }

    @Override
    public void saveFileInternal(RepoPath fileRepoPath, InputStream is) throws RepoRejectException, IOException {
        try {
            MutableFileInfo fileInfo = new XStreamInfoFactory().createFileInfo(fileRepoPath);
            fileInfo.createTrustedChecksums();
            SaveResourceContext saveContext = new SaveResourceContext.Builder(new FileResource(fileInfo), is).build();
            StoringRepo storingRepo = storingRepositoryByKey(fileRepoPath.getRepoKey());
            if (storingRepo == null) {
                throw new IllegalArgumentException("Storing repo for '" + fileRepoPath + "' not found");
            }
            saveResource(storingRepo, saveContext);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Override
    public List<VirtualRepo> getVirtualRepositories() {
        return new ArrayList<>(virtualRepositoriesMap.values());
    }

    @Override
    public List<LocalRepo> getLocalAndCachedRepositories() {
        return globalVirtualRepo.getLocalAndCachedRepositories();
    }

    @Override
    public List<RealRepo> getLocalAndRemoteRepositories() {
        return globalVirtualRepo.getLocalAndRemoteRepositories();
    }

    @Override
    public List<LocalRepoDescriptor> getLocalAndCachedRepoDescriptors() {
        List<LocalRepo> localAndCached = globalVirtualRepo.getLocalAndCachedRepositories();
        ArrayList<LocalRepoDescriptor> result = Lists.newArrayList();
        for (LocalRepo localRepo : localAndCached) {
            result.add((LocalRepoDescriptor) localRepo.getDescriptor());
        }
        return result;
    }

    @Override
    public List<RemoteRepoDescriptor> getRemoteRepoDescriptors() {
        List<RemoteRepo> remoteRepositories = globalVirtualRepo.getRemoteRepositories();
        ArrayList<RemoteRepoDescriptor> result = Lists.newArrayList();
        for (RemoteRepo remoteRepo : remoteRepositories) {
            result.add((RemoteRepoDescriptor) remoteRepo.getDescriptor());
        }
        return result;
    }

    @Override
    public VirtualRepoDescriptor virtualRepoDescriptorByKey(String repoKey) {
        if (repoKey == null || repoKey.length() == 0) {
            return null;
        }
        if (VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY.equals(repoKey)) {
            return globalVirtualRepo.getDescriptor();
        }
        return centralConfigService.getDescriptor().getVirtualRepositoriesMap().get(repoKey);
    }

    @Override
    public String getStringContent(FileInfo fileInfo) {
        return getStringContent(fileInfo.getRepoPath());
    }

    @Override
    public String getStringContent(RepoPath repoPath) {
        LocalRepo repo = localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (repo == null) {
            throw new IllegalArgumentException("Local repository for '" + repoPath + "' doesn't exist");
        }
        return repo.getTextFileContent(repoPath);
    }

    @Override
    public ResourceStreamHandle getResourceStreamHandle(RepoPath repoPath) {
        LocalRepo repo = localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (repo == null) {
            throw new IllegalArgumentException("Local repository for '" + repoPath + "' doesn't exist");
        }
        // Recreate the repo path for remote stream handle request
        if (repo.isCache() && !repo.getKey().equals(repoPath.getRepoKey())) {
            repoPath = InternalRepoPathFactory.cacheRepoPath(repoPath);
        }
        return repo.getFileContent(repoPath);
    }

    @Override
    public ArchiveFileContent getArchiveFileContent(RepoPath archivePath, String sourceEntryPath) throws IOException {
        LocalRepo repo = localOrCachedRepositoryByKey(archivePath.getRepoKey());
        return new ArchiveContentRetriever().getArchiveFileContent(repo, archivePath, sourceEntryPath);
    }

    /**
     * Import all the repositories under the passed folder which matches local or cached repository declared in the
     * configuration. Having empty directory for each repository is allowed and not an error. Nothing will be imported
     * for those.
     */
    @Override
    public void importAll(ImportSettingsImpl settings) {
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(PermissionTargetInfo.ANY_REPO, settings, false, null);
        } else {
            //Import the local repositories
            importAll(getLocalAndCacheRepoKeys(), Collections.<String>emptyList(), settings);
        }
    }

    /**
     * Import the artifacts under the folder passed directly in the repository named "repoKey". If no repository with
     * this repo key exists or if the folder passed is empty, the status will be set to error.
     */
    @Override
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importRepo(String repoKey, ImportSettingsImpl settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(repoKey, settings, false, null);
        } else {
            //Import each file separately to avoid a long running transaction
            LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
            if (localRepo == null) {
                String msg = "The repo key " + repoKey + " is not a local or cached repository!";
                IllegalArgumentException ex = new IllegalArgumentException(msg);
                status.error(msg, ex, log);
                return;
            }
            localRepo.importFrom(settings);
        }
    }

    @Override
    public void exportTo(ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        status.status("Exporting repositories...", log);
        if (TaskCallback.currentTaskToken() == null) {
            exportAsync(BaseSettings.FULL_SYSTEM, settings);
        } else {
            List<String> repoKeys = settings.getRepositories();
            for (String repoKey : repoKeys) {
                boolean stop = taskService.pauseOrBreak();
                if (stop) {
                    status.error("Export was stopped", log);
                    return;
                }
                exportRepo(repoKey, settings);
                if (status.isError() && settings.isFailFast()) {
                    return;
                }
            }

            if (settings.isIncremental()) {
                File repositoriesDir = getRepositoriesExportDir(settings.getBaseDir());
                cleanupIncrementalBackupDirectory(repositoriesDir, repoKeys);
            }
        }
    }

    @Override
    public void exportRepo(String repoKey, ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        if (TaskCallback.currentTaskToken() == null) {
            exportAsync(repoKey, settings);
        } else {
            //Check if we need to break/pause
            boolean stop = taskService.pauseOrBreak();
            if (stop) {
                status.error("Export was stopped on " + repoKey, log);
                return;
            }
            LocalRepo sourceRepo = localOrCachedRepositoryByKey(repoKey);
            if (sourceRepo == null) {
                status.error("Export cannot be done on non existing repository " + repoKey, log);
                return;
            }
            File targetDir = getRepoExportDir(settings.getBaseDir(), repoKey);
            ExportSettingsImpl repoSettings = new ExportSettingsImpl(targetDir, settings);
            sourceRepo.exportTo(repoSettings);
        }
    }

    private File getRepoExportDir(File exportDir, String repoKey) {
        return new File(getRepositoriesExportDir(exportDir), repoKey);
    }

    private File getRepositoriesExportDir(File exportDir) {
        // the directory under the base export dir that contains the exported repositories
        return new File(exportDir, "repositories");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MutableStatusHolder exportSearchResults(SavedSearchResults searchResults, ExportSettingsImpl baseSettings) {
        return new DbRepoExportSearchHandler(searchResults, baseSettings).export();
    }

    @Override
    @Nonnull
    public ItemInfo getItemInfo(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        VfsItem item = localRepo.getImmutableFsItem(repoPath);
        if (item != null) {
            return item.getInfo();
        }
        throw new ItemNotFoundRuntimeException("Item " + repoPath + " does not exist");
    }

    @Override
    @Nonnull
    public FileInfo getFileInfo(RepoPath repoPath) {
        ItemInfo itemInfo = getItemInfo(repoPath);
        if (itemInfo instanceof FileInfo) {
            return (FileInfo) itemInfo;
        } else {
            throw new FileExpectedException(repoPath);
        }
    }

    @Override
    @Nonnull
    public FolderInfo getFolderInfo(RepoPath repoPath) {
        ItemInfo itemInfo = getItemInfo(repoPath);
        if (itemInfo instanceof FolderInfo) {
            return (FolderInfo) itemInfo;
        } else {
            throw new FolderExpectedException(repoPath);
        }
    }

    @Override
    public boolean exists(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
        return localRepo != null && localRepo.itemExists(repoPath.getPath());
    }

    @Override
    public ItemMetaInfo getItemMetaInfo(RepoPath repoPath) {
        return ContextHelper.get().beanForType(NodeMetaInfoService.class).getNodeMetaInfo(repoPath);
    }

    @Override
    public boolean hasProperties(RepoPath repoPath) {
        MutableVfsItem mutableSessionItem = LockingHelper.getIfWriteLockedByMe(repoPath);
        if (mutableSessionItem != null) {
            return mutableSessionItem.getProperties().isEmpty();
        } else {
            return ContextHelper.get().beanForType(PropertiesService.class).hasProperties(repoPath);
        }
    }

    @Override
    @Nullable
    public Properties getProperties(RepoPath repoPath) {
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            return null;
        }

        MutableVfsItem mutableItem = LockingHelper.getIfWriteLockedByMe(repoPath);
        if (mutableItem != null) {
            return mutableItem.getProperties();
        } else {
            return ContextHelper.get().beanForType(PropertiesService.class).getProperties(repoPath);
        }
    }

    @Override
    public boolean setProperties(RepoPath repoPath, Properties properties) {
        if (!assertCanAnnotate(repoPath, "Properties")) {
            return false;
        }
        LocalRepo repository = getLocalRepository(repoPath);
        MutableVfsItem mutableItem = repository.getMutableFsItem(repoPath);
        if (mutableItem == null) {
            log.warn("Cannot set properties on '{}': Item not found.", repoPath);
            return false;
        }

        mutableItem.setProperties(properties);

        ReplicationAddon replicationAddon = addonsManager.addonByType(ReplicationAddon.class);
        replicationAddon.offerLocalReplicationPropertiesChangeEvent(repoPath);
        return true;
    }

    @Override
    public boolean removeProperties(RepoPath repoPath) {
        return setProperties(repoPath, new PropertiesImpl());
    }

    private boolean assertCanAnnotate(RepoPath repoPath, String metadataName) {
        if (!authService.canAnnotate(repoPath)) {
            AccessLogger.annotateDenied(repoPath);
            log.error("Cannot set '{}' on '{}': lacking annotate permissions.", metadataName, repoPath.getId());
            return false;
        }
        return true;
    }

    @Override
    public MoveMultiStatusHolder moveWithoutMavenMetadata(RepoPath from, RepoPath to, boolean dryRun, boolean suppressLayouts,
            boolean failFast) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(from, to).copy(false).dryRun(dryRun).
                executeMavenMetadataCalculation(false).suppressLayouts(suppressLayouts).failFast(failFast);
        return moveOrCopy(configBuilder.build());
    }

    @Override
    public MoveMultiStatusHolder move(RepoPath from, RepoPath to, boolean dryRun, boolean suppressLayouts,
            boolean failFast) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(from, to).copy(false).dryRun(dryRun).
                executeMavenMetadataCalculation(true).suppressLayouts(suppressLayouts).failFast(failFast);
        return moveOrCopy(configBuilder.build());
    }

    @Override
    public MoveMultiStatusHolder move(RepoPath from, String targetLocalRepoKey, boolean dryRun) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(from, targetLocalRepoKey)
                .copy(false).dryRun(dryRun).executeMavenMetadataCalculation(true);
        return moveOrCopy(configBuilder.build());
    }

    @Override
    public MoveMultiStatusHolder move(Set<RepoPath> pathsToMove, String targetLocalRepoKey,
            Properties properties, boolean dryRun, boolean failFast) {
        Set<RepoPath> pathsToMoveIncludingParents = aggregatePathsToMove(pathsToMove, targetLocalRepoKey, false);

        log.debug("The following paths will be moved: {}", pathsToMoveIncludingParents);
        // start moving each path separately, marking each folder or file's parent folder for metadata recalculation
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        RepoPathMover mover = getRepoPathMover();
        for (RepoPath pathToMove : pathsToMoveIncludingParents) {
            log.debug("Moving path: {} to {}", pathToMove, targetLocalRepoKey);
            mover.moveOrCopy(status, new MoverConfigBuilder(pathToMove, targetLocalRepoKey).copy(false).dryRun(dryRun)
                    .executeMavenMetadataCalculation(false).pruneEmptyFolders(true).properties(properties).
                            failFast(failFast).build());
        }
        mavenMetadataService.calculateMavenMetadataAsyncNonRecursive(status.getCandidatesForMavenMetadataCalculation());
        return status;
    }

    @Override
    public MoveMultiStatusHolder copy(RepoPath fromRepoPath, RepoPath targetRepoPath, boolean dryRun,
            boolean suppressLayouts, boolean failFast) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(fromRepoPath, targetRepoPath).copy(true).
                dryRun(dryRun).executeMavenMetadataCalculation(true).suppressLayouts(suppressLayouts).
                failFast(failFast);
        return moveOrCopy(configBuilder.build());
    }

    @Override
    public MoveMultiStatusHolder copy(RepoPath from, String targetLocalRepoKey, boolean dryRun) {
        MoverConfigBuilder configBuilder = new MoverConfigBuilder(from, targetLocalRepoKey)
                .copy(true).dryRun(dryRun).executeMavenMetadataCalculation(true);
        return moveOrCopy(configBuilder.build());
    }

    @Override
    public MoveMultiStatusHolder copy(Set<RepoPath> pathsToCopy, String targetLocalRepoKey,
            Properties properties, boolean dryRun, boolean failFast) {
        Set<RepoPath> pathsToCopyIncludingParents = aggregatePathsToMove(pathsToCopy, targetLocalRepoKey, true);

        log.debug("The following paths will be copied: {}", pathsToCopyIncludingParents);
        //Start copying each path separately, marking each folder or file's parent folder for metadata recalculation
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        RepoPathMover mover = getRepoPathMover();
        for (RepoPath pathToCopy : pathsToCopyIncludingParents) {
            log.debug("Copying path: {} to {}", pathToCopy, targetLocalRepoKey);
            mover.moveOrCopy(status, new MoverConfigBuilder(pathToCopy, targetLocalRepoKey).copy(true).dryRun(dryRun)
                    .executeMavenMetadataCalculation(false).pruneEmptyFolders(false).properties(properties).
                            failFast(failFast).build());
        }
        mavenMetadataService.calculateMavenMetadataAsyncNonRecursive(status.getCandidatesForMavenMetadataCalculation());

        return status;
    }

    private MoveMultiStatusHolder moveOrCopy(MoverConfig config) {
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        getRepoPathMover().moveOrCopy(status, config);
        return status;
    }

    @Override
    public StatusHolder deploy(RepoPath repoPath, InputStream inputStream) {
        try {
            ArtifactoryDeployRequest request = new ArtifactoryDeployRequestBuilder(repoPath)
                    .inputStream(inputStream).build();
            InternalArtifactoryResponse response = new InternalArtifactoryResponse();
            uploadService.upload(request, response);
            return response.getStatusHolder();
        } catch (Exception e) {
            String msg = String.format("Cannot deploy to '{%s}'.", repoPath);
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        }
    }

    @Override
    public FileInfo getVirtualFileInfo(RepoPath virtualRepoPath) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(virtualRepoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new IllegalArgumentException(virtualRepoPath.getRepoKey() + " is not a virtual repository.");
        }
        Set<LocalRepo> resolvedLocalRepos = virtualRepo.getResolvedLocalAndCachedRepos();
        for (LocalRepo resolvedLocalRepo : resolvedLocalRepos) {
            if (resolvedLocalRepo.itemExists(virtualRepoPath.getPath())) {
                return getFileInfo(resolvedLocalRepo.getRepoPath(virtualRepoPath.getPath()));
            }
        }

        throw new ItemNotFoundRuntimeException("Item " + virtualRepoPath + " does not exists");
    }

    @Override
    public ItemInfo getVirtualItemInfo(RepoPath virtualRepoPath) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(virtualRepoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new IllegalArgumentException(virtualRepoPath.getRepoKey() + " is not a virtual repository.");
        }
        Set<LocalRepo> resolvedLocalRepos = virtualRepo.getResolvedLocalAndCachedRepos();
        for (LocalRepo resolvedLocalRepo : resolvedLocalRepos) {
            if (resolvedLocalRepo.itemExists(virtualRepoPath.getPath())) {
                return getItemInfo(resolvedLocalRepo.getRepoPath(virtualRepoPath.getPath()));
            }
        }

        throw new ItemNotFoundRuntimeException("Item " + virtualRepoPath + " does not exists");
    }

    @Override
    public BasicStatusHolder undeploy(RepoPath repoPath, boolean calcMavenMetadata) {
        return undeploy(repoPath, calcMavenMetadata, false);
    }

    @Override
    public BasicStatusHolder undeploy(RepoPath repoPath, boolean calcMavenMetadata, boolean pruneEmptyFolders) {
        String repoKey = repoPath.getRepoKey();
        StoringRepo storingRepo = storingRepositoryByKey(repoKey);
        BasicStatusHolder statusHolder = new BasicStatusHolder();
        if (storingRepo == null) {
            statusHolder.error("Could find storing repository by key '" + repoKey + "'", log);
            return statusHolder;
        }
        PathDeletionContext pathDeletionContext = new PathDeletionContext.Builder(storingRepo, repoPath.getPath(),
                statusHolder).assertOverwrite(false).build();
        assertDelete(pathDeletionContext);
        if (!statusHolder.isError()) {
            storingRepo.undeploy(repoPath, calcMavenMetadata);
        }

        if (pruneEmptyFolders && !repoPath.isRoot()) {
            pruneService.prune(repoPath.getParent());
        }

        return statusHolder;
    }

    @Override
    public BasicStatusHolder undeploy(RepoPath repoPath) {
        return undeploy(repoPath, true);
    }

    @Override
    public StatusHolder undeployVersionUnits(Set<VersionUnit> versionUnits) {
        MultiStatusHolder multiStatusHolder = new MultiStatusHolder();
        InternalRepositoryService transactionalMe = getTransactionalMe();

        Set<RepoPath> pathsForMavenMetadataCalculation = Sets.newHashSet();

        for (VersionUnit versionUnit : versionUnits) {
            Set<RepoPath> repoPaths = versionUnit.getRepoPaths();
            for (RepoPath repoPath : repoPaths) {
                BasicStatusHolder holder = transactionalMe.undeploy(repoPath, false, true);
                multiStatusHolder.merge(holder);

                if (NamingUtils.isPom(repoPath.getPath())) {
                    // We need to re-calculate the artifact id folder (which is the grandparent of the pom file)
                    RepoPath grandparentFolder = RepoPathUtils.getAncestor(repoPath, 2);
                    if (grandparentFolder != null) {
                        pathsForMavenMetadataCalculation.add(grandparentFolder);
                    }
                }
            }
        }

        for (RepoPath path : pathsForMavenMetadataCalculation) {
            // Check to make sure of existence, might have been removed through the iterations of the version units
            if (exists(path)) {
                mavenMetadataService.calculateMavenMetadataAsync(path, true);
            }
        }

        return multiStatusHolder;
    }

    @Override
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

    @Override
    public List<FolderInfo> getWithEmptyChildren(FolderInfo folderInfo) {
        FolderCompactor compactor = ContextHelper.get().beanForType(FolderCompactor.class);
        return compactor.getFolderWithCompactedChildren(folderInfo);
    }

    @Override
    public Set<String> getAllRepoKeys() {
        return allRepoKeysCache;
    }

    @Override
    public List<RepoDescriptor> getLocalAndRemoteRepoDescriptors() {
        return globalVirtualRepo.getDescriptor().getRepositories();
    }

    @Override
    public boolean isAnonAccessEnabled() {
        return authService.isAnonAccessEnabled();
    }

    @Override
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

    @Override
    public LocalRepo localRepositoryByKey(String key) {
        return globalVirtualRepo.localRepositoryByKey(key);
    }

    @Override
    public RemoteRepo remoteRepositoryByKey(String key) {
        return globalVirtualRepo.remoteRepositoryByKey(key);
    }

    @Override
    public VirtualRepo virtualRepositoryByKey(String key) {
        return virtualRepositoriesMap.get(key);
    }

    @Override
    @Nullable
    public LocalRepo localOrCachedRepositoryByKey(String key) {
        return globalVirtualRepo.localOrCachedRepositoryByKey(key);
    }

    @Override
    public RealRepo localOrRemoteRepositoryByKey(String key) {
        return globalVirtualRepo.localOrRemoteRepositoryByKey(key);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <R extends Repo> RepoRepoPath<R> getRepoRepoPath(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        R repo = (R) repositoryByKey(repoKey);
        if (repo == null) {
            throw new IllegalArgumentException("Repository '" + repoKey + "' not found!");
        }
        RepoRepoPath<R> rrp = new RepoRepoPath<>(repo, repoPath);
        return rrp;
    }

    @Override
    public StoringRepo storingRepositoryByKey(String key) {
        LocalRepo localRepo = localOrCachedRepositoryByKey(key);
        if (localRepo != null) {
            return localRepo;
        } else {
            return virtualRepositoryByKey(key);
        }
    }

    @Override
    public boolean isWriteLocked(RepoPath repoPath) {
        StoringRepo storingRepo = storingRepositoryByKey(repoPath.getRepoKey());
        if (storingRepo != null) {
            return storingRepo.isWriteLocked(repoPath);
        }
        return false;
    }

    @Override
    public List<LocalRepoDescriptor> getLocalRepoDescriptors() {
        return new ArrayList<>(centralConfigService.getDescriptor().getLocalRepositoriesMap().values());
    }

    @Override
    public List<LocalCacheRepoDescriptor> getCachedRepoDescriptors() {
        List<LocalCacheRepo> localAndCached = globalVirtualRepo.getLocalCaches();
        List<LocalCacheRepoDescriptor> result = new ArrayList<>();
        for (LocalRepo localRepo : localAndCached) {
            result.add((LocalCacheRepoDescriptor) localRepo.getDescriptor());
        }
        return result;
    }

    @Override
    public RepoDescriptor repoDescriptorByKey(String key) {
        Repo repo = globalVirtualRepo.repositoryByKey(key);
        if (repo != null) {
            return repo.getDescriptor();
        }
        return null;
    }

    @Override
    public LocalRepoDescriptor localRepoDescriptorByKey(String key) {
        LocalRepo localRepo = globalVirtualRepo.localRepositoryByKey(key);
        if (localRepo != null) {
            return (LocalRepoDescriptor) localRepo.getDescriptor();
        }
        return null;
    }

    @Override
    public LocalRepoDescriptor localOrCachedRepoDescriptorByKey(String key) {
        LocalRepo localRepo = globalVirtualRepo.localOrCachedRepositoryByKey(key);
        if (localRepo != null) {
            return (LocalRepoDescriptor) localRepo.getDescriptor();
        }
        return null;
    }

    @Override
    public RemoteRepoDescriptor remoteRepoDescriptorByKey(String key) {
        RemoteRepo remoteRepo = globalVirtualRepo.remoteRepositoryByKey(key);
        if (remoteRepo != null) {
            return (RemoteRepoDescriptor) remoteRepo.getDescriptor();
        }
        return null;
    }

    @Override
    public List<VirtualRepoDescriptor> getVirtualRepoDescriptors() {
        ArrayList<VirtualRepoDescriptor> list = new ArrayList<>();
        list.add(globalVirtualRepo.getDescriptor());
        list.addAll(centralConfigService.getDescriptor().getVirtualRepositoriesMap().values());
        return list;
    }

    @Override
    public Repo nonCacheRepositoryByKey(String key) {
        Repo repo = globalVirtualRepo.nonCacheRepositoryByKey(key);
        if (repo == null) {
            repo = virtualRepositoriesMap.get(key);
        }
        assert repo != null;
        return repo;
    }

    /**
     * This method will delete and import all the local and cached repositories listed in the (newly loaded) config
     * file. This action is resource intensive and is done in multiple transactions to avoid out of memory exceptions.
     */
    @Override
    public void importFrom(ImportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(BaseSettings.FULL_SYSTEM, settings, false, null);
        } else {
            status.status("Importing repositories...", log);
            internalImportFrom(settings);
            status.status("Finished importing repositories...", log);
        }
    }

    @Override
    public boolean isRemoteAssumedOffline(@Nonnull String remoteRepoKey) {
        RemoteRepo remoteRepo = remoteRepositoryByKey(remoteRepoKey);
        if (remoteRepo == null) {
            return false;
        }
        return remoteRepo.isAssumedOffline();
    }

    @Override
    public long getRemoteNextOnlineCheck(String remoteRepoKey) {
        RemoteRepo remoteRepo = remoteRepositoryByKey(remoteRepoKey);
        if (remoteRepo == null) {
            return 0;
        }
        return remoteRepo.getNextOnlineCheckMillis();
    }

    @Override
    public void resetAssumedOffline(String remoteRepoKey) {
        RemoteRepo remoteRepo = remoteRepositoryByKey(remoteRepoKey);
        if (remoteRepo != null) {
            remoteRepo.resetAssumedOffline();
        }
    }

    @Override
    public void assertValidDeployPath(ValidDeployPathContext validDeployPathContext) throws RepoRejectException {
        LocalRepo repo = validDeployPathContext.getRepo();
        RepoPath repoPath = validDeployPathContext.getRepoPath();
        String path = repoPath.getPath();
        if (!repo.getKey().equals(repoPath.getRepoKey())) {
            // the repo path should point to the given repo (e.g, in case the repo path points to the remote repo)
            repoPath = InternalRepoPathFactory.create(repo.getKey(), path, repoPath.isFolder());
        }
        BasicStatusHolder status = repo.assertValidPath(repoPath, false);
        if (!status.isError()) {
            // if it is metadata, assert annotate privileges. Maven metadata is treated as regular file
            // (needs deploy permissions).
            if (NamingUtils.isMetadata(path)) {
                if (!authService.canAnnotate(repoPath)) {
                    String msg = "User " + authService.currentUsername() + " is not permitted to annotate '" +
                            path + "' on '" + repoPath + "'.";
                    status.error(msg, HttpStatus.SC_FORBIDDEN, log);
                    AccessLogger.annotateDenied(repoPath);
                }
            } else {
                //Assert deploy privileges
                boolean canDeploy = authService.canDeploy(repoPath);
                if (!canDeploy) {
                    String msg = "User " + authService.currentUsername() + " is not permitted to deploy '" +
                            path + "' into '" + repoPath + "'.";
                    status.error(msg, HttpStatus.SC_FORBIDDEN, log);
                    AccessLogger.deployDenied(repoPath);
                }
            }
            if (!status.isError()) {
                PathDeletionContext pathDeletionContext = new PathDeletionContext.Builder(repo, path, status)
                        .assertOverwrite(true).requestSha1(validDeployPathContext.getRequestSha1())
                        .forceExpiryCheck(validDeployPathContext.isForceExpiryCheck()).build();
                assertDelete(pathDeletionContext);
            }

            if (!status.isError()) {
                // Assert that we don't exceed the user configured maximum storage size
                assertStorageQuota(status, validDeployPathContext.getContentLength());
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

    private void assertStorageQuota(MutableStatusHolder statusHolder, long contentLength) {
        StorageQuotaInfo info = storageService.getStorageQuotaInfo(contentLength);
        if (info == null) {
            return;
        }

        if (info.isLimitReached()) {
            // Note: don't display the disk usage in the status holder - this message is written back to the user
            statusHolder.error(
                    "Datastore disk usage is too high. Contact your Artifactory administrator to add additional " +
                            "storage space or change the disk quota limits.", HttpStatus.SC_REQUEST_TOO_LONG, log);

            log.error(info.getErrorMessage());
        } else if (info.isWarningLimitReached()) {
            log.warn(info.getWarningMessage());
        }
    }

    @Override
    public <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(InternalRequestContext requestContext,
            RemoteRepo<T> remoteRepo, RepoResource res) throws IOException, RepoRejectException {
        return remoteRepo.downloadAndSave(requestContext, res);
    }

    @Override
    public RepoResource unexpireIfExists(LocalRepo localCacheRepo, String path) {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource == null) {
            return new UnfoundRepoResource(InternalRepoPathFactory.create(localCacheRepo.getKey(), path),
                    "Object is not in cache");
        }
        return resource;
    }

    @Override
    public ResourceStreamHandle unexpireAndRetrieveIfExists(InternalRequestContext requestContext,
            LocalRepo localCacheRepo, String path) throws IOException, RepoRejectException {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource != null && resource.isFound()) {
            return localCacheRepo.getResourceStreamHandle(requestContext, resource);
        }
        return null;
    }

    @Override
    public ResourceStreamHandle getResourceStreamHandle(InternalRequestContext requestContext, Repo repo,
            RepoResource res) throws IOException, RepoRejectException {
        if (res instanceof ResolvedResource) {
            RepoRequests.logToContext("The requested resource is already resolved - using a string resource handle");
            // resource already contains the content - just extract it and return a string resource handle
            String content = ((ResolvedResource) res).getContent();
            return new StringResourceStreamHandle(content);
        } else {
            RepoRequests.logToContext("The requested resource isn't pre-resolved");
            RepoPath repoPath = res.getRepoPath();
            if (repo.isReal()) {
                RepoRequests.logToContext("Target repository isn't virtual - verifying that downloading is allowed");
                //Permissions apply only to real repos
                StatusHolder holder = ((RealRepo) repo).checkDownloadIsAllowed(repoPath);
                if (holder.isError()) {
                    RepoRequests.logToContext("Download isn't allowed - received status {} and message '%s'",
                            holder.getStatusCode(), holder.getStatusMsg());
                    throw new RepoRejectException(holder.getStatusMsg(), holder.getStatusCode());
                }
            }
            return repo.getResourceStreamHandle(requestContext, res);
        }
    }

    @Override
    public RepoResource saveResource(StoringRepo repo, SaveResourceContext saveContext)
            throws IOException, RepoRejectException {
        return repo.saveResource(saveContext);
    }

    @Override
    public List<VersionUnit> getVersionUnitsUnder(RepoPath repoPath) {
        List<VersionUnit> versionUnits = Lists.newArrayList();
        VersionUnitSearchControls controls = new VersionUnitSearchControls(repoPath);
        ItemSearchResults<VersionUnitSearchResult> searchResults = searchService.searchVersionUnits(controls);
        for (VersionUnitSearchResult result : searchResults.getResults()) {
            versionUnits.add(result.getVersionUnit());
        }
        return versionUnits;
    }

    @Override
    public long getArtifactCount() throws RepositoryRuntimeException {
        if (artifactCountRetriever == null) {
            artifactCountRetriever = new ArtifactCountRetriever();
        }
        return artifactCountRetriever.getCount();
    }

    @Override
    public List<VirtualRepoDescriptor> getVirtualReposContainingRepo(RepoDescriptor repoDescriptor) {
        RepoDescriptor descriptor = repoDescriptor;
        if (repoDescriptor instanceof LocalCacheRepoDescriptor) {
            //VirtualRepoResolver does not directly support local cache repos, so if the items descriptor is a cache,
            //We extract the caches remote repo, and use it instead
            descriptor = ((LocalCacheRepoDescriptor) repoDescriptor).getRemoteRepo();
        }

        List<VirtualRepoDescriptor> reposToDisplay = new ArrayList<>();
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
     * Returns a list of local (non-cache) repo descriptors that the current user is permitted to deploy to.
     *
     * @return List of deploy-permitted local repos
     */
    @Override
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

    @Override
    public boolean isRepoPathAccepted(RepoPath repoPath) {
        if (repoPath.isRoot()) {
            return true;
        }
        LocalRepo repo = getLocalOrCachedRepository(repoPath);
        return repo.accepts(repoPath);
    }

    @Override
    public boolean isRepoPathVisible(RepoPath repoPath) {
        return (repoPath != null) && authService.canRead(repoPath) &&
                (isRepoPathAccepted(repoPath) || authService.canAnnotate(repoPath));
    }

    @Override
    public boolean isRepoPathHandled(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (repoPath.isRoot()) {
            return true;
        }
        LocalRepo repo = getLocalOrCachedRepository(repoPath);
        return repo.handlesReleaseSnapshot(path);
    }

    @Override
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

    @Override
    public Tree<ZipEntryInfo> zipEntriesToTree(RepoPath zipPath) throws IOException {
        LocalRepo localRepo = getLocalOrCachedRepository(zipPath);
        VfsFile file = localRepo.getImmutableFile(zipPath);
        ZipInputStream zin = null;
        try {
            Tree<ZipEntryInfo> tree;
            zin = new ZipInputStream(file.getStream());
            ZipEntry zipEntry;
            tree = InfoFactoryHolder.get().createZipEntriesTree();
            try {
                while ((zipEntry = zin.getNextEntry()) != null) {
                    tree.insert(InfoFactoryHolder.get().createZipEntry(zipEntry));
                }
                // IllegalArgumentException is being thrown from: java.util.zip.ZipInputStream.getUTF8String on a
                // bad archive
            } catch (IllegalArgumentException e) {
                throw new IOException(
                        "An error occurred while reading entries from zip file: " + file.getRepoPath());
            }
            return tree;
        } finally {
            IOUtils.closeQuietly(zin);
        }
    }

    @Override
    public ItemInfo getLastModified(RepoPath pathToSearch) {
        if (pathToSearch == null) {
            throw new IllegalArgumentException("Repo path cannot be null.");
        }
        if (!exists(pathToSearch)) {
            throw new ItemNotFoundRuntimeException("Could not find item: " + pathToSearch.getId());
        }

        ItemInfo itemLastModified = collectLastModifiedRecursively(pathToSearch);
        return itemLastModified;
    }

    @Override
    public void touch(RepoPath repoPath) {
        if (repoPath == null) {
            throw new IllegalArgumentException("Repo path cannot be null.");
        }
        LocalRepo localOrCachedRepository = getLocalOrCachedRepository(repoPath);
        if (localOrCachedRepository == null) {
            throw new IllegalArgumentException(repoPath + " is not local or cache repository path");
        }
        MutableVfsItem mutableFsItem = localOrCachedRepository.getMutableFsItem(repoPath);
        if (mutableFsItem == null) {
            throw new ItemNotFoundRuntimeException("Could not find item: " + repoPath.getId());
        }
        mutableFsItem.setModified(System.currentTimeMillis());
    }

    @Override
    public void fixChecksums(RepoPath fileRepoPath) {
        MutableVfsFile mutableFile = getMutableFile(fileRepoPath);
        FileInfo fileInfo = mutableFile.getInfo();
        ChecksumsInfo checksumsInfo = fileInfo.getChecksumsInfo();
        for (ChecksumInfo checksumInfo : checksumsInfo.getChecksums()) {
            if (!checksumInfo.checksumsMatch()) {
                mutableFile.setClientChecksum(checksumInfo.getType(), ChecksumInfo.TRUSTED_FILE_MARKER);
            }
        }
    }

    /**
     * Returns the latest modified item of the given file or folder (recursively)
     *
     * @param pathToSearch Repo path to search in
     * @return Latest modified item
     */
    private ItemInfo collectLastModifiedRecursively(RepoPath pathToSearch) {
        ItemInfo latestItem = getItemInfo(pathToSearch);

        if (latestItem.isFolder()) {
            List<ItemInfo> children = getChildren(pathToSearch);
            for (ItemInfo child : children) {
                ItemInfo itemInfo = collectLastModifiedRecursively(child.getRepoPath());
                if (itemInfo.getLastModified() > latestItem.getLastModified()) {
                    latestItem = itemInfo;
                }
            }
        }

        return latestItem;
    }

    private String importAsync(@Nonnull String repoKey, ImportSettings settings, boolean deleteExistingRepo,
            Semaphore activeImportsGate) {
        MutableStatusHolder status = settings.getStatusHolder();
        TaskBase task = TaskUtils.createManualTask(ImportJob.class, 0L);
        task.addAttribute(Task.REPO_KEY, repoKey);
        task.addAttribute(ImportJob.DELETE_REPO, deleteExistingRepo);
        task.addAttribute(ImportSettingsImpl.class.getName(), settings);
        if (activeImportsGate != null) {
            activeImportsGate.acquireUninterruptibly();
            task.addAttribute(Semaphore.class.getName(), activeImportsGate);
        }
        taskService.startTask(task, true);
        if (activeImportsGate == null) {
            // No gate, need to wait
            boolean completed = taskService.waitForTaskCompletion(task.getToken(), Long.MAX_VALUE);
            if (!completed) {
                if (!status.isError()) {
                    // Add error of no completion
                    status.error("The task " + task + " did not complete correctly.", log);
                }
            }
        }
        return task.getToken();
    }

    private void exportAsync(@Nonnull String repoKey, ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        TaskBase task = TaskUtils.createManualTask(ExportJob.class, 0L);
        task.addAttribute(Task.REPO_KEY, repoKey);
        task.addAttribute(ExportSettingsImpl.class.getName(), settings);
        taskService.startTask(task, true);
        boolean completed = taskService.waitForTaskCompletion(task.getToken());
        if (!completed) {
            if (!status.isError()) {
                // Add Error of no completion
                status.error("The task " + task + " did not complete correctly", log);
            }
        }
    }

    @Override
    public LocalRepo getLocalRepository(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
        if (localRepo == null) {
            throw new IllegalArgumentException("Repository '" + repoKey + "' is not a local repository");
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
        MutableStatusHolder status = settings.getStatusHolder();
        File repoRootPath = getRepositoriesExportDir(settings.getBaseDir());
        //Keep the current list of repositories for deletion after or during import
        List<String> oldRepoList = getLocalAndCacheRepoKeys();
        //Import all local repositories
        List<String> newRepoList = settings.getRepositories();
        if (newRepoList.isEmpty()) {
            newRepoList = new ArrayList<>(oldRepoList);
        }
        ImportSettingsImpl repositoriesImportSettings = new ImportSettingsImpl(repoRootPath, settings);
        importAll(newRepoList, oldRepoList, repositoriesImportSettings);
        return !status.isError();
    }

    private List<String> getLocalAndCacheRepoKeys() {
        List<String> result = new ArrayList<>();
        for (LocalRepoDescriptor localRepoDescriptor : getLocalAndCachedRepoDescriptors()) {
            result.add(localRepoDescriptor.getKey());
        }
        return result;
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private void importAll(List<String> newRepoList, List<String> oldRepoList, ImportSettingsImpl settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        List<String> tokens = new ArrayList<>(newRepoList.size());
        File baseDir = settings.getBaseDir();
        String[] baseDirList = new String[]{};
        if (baseDir.list() != null) {
            baseDirList = baseDir.list();
        }
        List<String> listOfRepoKeys = Arrays.asList(baseDirList);
        List<String> children = new ArrayList<>(listOfRepoKeys);

        // avoid spawning too many threads
        int maxParallelImports = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
        Semaphore activeImportsGate = new Semaphore(maxParallelImports);

        for (String newLocalRepoKey : newRepoList) {
            File rootImportFolder = new File(settings.getBaseDir(), newLocalRepoKey);
            try {
                if (rootImportFolder.exists()) {
                    if (rootImportFolder.isDirectory()) {
                        ImportSettings repoSettings = new ImportSettingsImpl(rootImportFolder, settings);
                        boolean deleteExistingRepo = false;
                        if (oldRepoList.contains(newLocalRepoKey)) {
                            // Full repo delete with undeploy on root repo path
                            deleteExistingRepo = true;
                        }
                        String importTaskToken =
                                importAsync(newLocalRepoKey, repoSettings, deleteExistingRepo, activeImportsGate);
                        tokens.add(importTaskToken);
                    }
                    children.remove(newLocalRepoKey);
                }
            } catch (Exception e) {
                status.error("Could not import repository " + newLocalRepoKey + " from " + rootImportFolder, e, log);
                if (settings.isFailFast()) {
                    return;
                }
            }
        }

        if ((children.size() == baseDirList.length) && settings.isFailIfEmpty()) {
            status.error("The selected directory did not contain any repositories.", log);
        } else {
            for (String unusedDir : children) {
                boolean isMetadata = unusedDir.contains("metadata");
                boolean isIndex = unusedDir.contains("index");
                if (!isMetadata && !isIndex) {
                    status.warn("The directory " + unusedDir + " does not match any repository key.", log);
                }
            }
        }

        for (String token : tokens) {
            try {
                taskService.waitForTaskCompletion(token);
            } catch (Exception e) {
                status.error("error waiting for repository import completion", e, log);
                if (settings.isFailFast()) {
                    return;
                }
            }
        }
    }

    private void initAllRepoKeysCache() {
        Set<String> newKeys = new HashSet<>();
        newKeys.addAll(globalVirtualRepo.getLocalRepositoriesMap().keySet());
        newKeys.addAll(globalVirtualRepo.getRemoteRepositoriesMap().keySet());
        newKeys.addAll(globalVirtualRepo.getLocalCacheRepositoriesMap().keySet());
        newKeys.addAll(virtualRepositoriesMap.keySet());
        allRepoKeysCache = newKeys;
    }

    private RepoResource internalUnexpireIfExists(LocalRepo repo, String path) {
        // Need to release the read lock first
        RepoPath repoPath = InternalRepoPathFactory.create(repo.getKey(), path);
        RepoPath fsItemRepoPath = NamingUtils.getLockingTargetRepoPath(repoPath);
        // Write lock auto upgrade supported LockingHelper.releaseReadLock(fsItemRepoPath);
        MutableVfsItem fsItem = repo.getMutableFsItem(fsItemRepoPath);
        if (fsItem != null) {
            log.debug("{}: falling back to using cache entry for resource info at '{}'.", this, path);
            //Reset the resource age so it is kept being cached
            fsItem.setUpdated(System.currentTimeMillis());
            return repo.getInfo(new NullRequestContext(repoPath));
        }
        return null;
    }

    private static InternalRepositoryService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(InternalRepositoryService.class);
    }

    private void assertDelete(PathDeletionContext pathDeletionContext) {
        StoringRepo repo = pathDeletionContext.getRepo();
        String path = pathDeletionContext.getPath();
        BasicStatusHolder status = pathDeletionContext.getStatus();
        RepoPath repoPath = InternalRepoPathFactory.create(repo.getKey(), path);
        //Check that has delete rights to replace an exiting item
        if (repo.shouldProtectPathDeletion(pathDeletionContext)) {
            if (!authService.canDelete(repoPath)) {
                AccessLogger.deleteDenied(repoPath);
                if (centralConfigService.getDescriptor().getSecurity().isHideUnauthorizedResources()) {
                    status.error("Could not locate artifact '" + repoPath + "'.", HttpStatus.SC_NOT_FOUND, log);
                } else {
                    status.error("Not enough permissions to overwrite artifact '" + repoPath + "' (user '" +
                            authService.currentUsername() + "' needs DELETE permission).", HttpStatus.SC_FORBIDDEN,
                            log);
                }
            }
        }

        //For deletion (as opposed to overwrite), check that path actually exists
        if (!pathDeletionContext.isAssertOverwrite() && !repo.itemExists(repoPath.getPath())) {
            status.error("Could not locate artifact '" + repoPath + "' (Nothing to delete).",
                    HttpStatus.SC_NOT_FOUND, log);
        }
    }

    // remove export folders of repositories that are not part of the current backup included repositories
    // this cleanup is needed in incremental backup when a repository is excluded from the backup or removed

    private void cleanupIncrementalBackupDirectory(File targetDir,
            List<String> reposToBackup) {
        if (!targetDir.exists()) {
            log.debug("Repositories backup directory doesn't exist: {}", targetDir.getAbsolutePath());
            return; // nothing to clean
        }
        File[] childFiles = targetDir.listFiles();
        for (File childFile : childFiles) {
            String fileName = childFile.getName();
            if (fileName.endsWith(METADATA_FOLDER)) {
                continue;  // skip metadata folders, will delete them with the actual folder if needed
            }
            boolean includedInBackup = false;
            for (String repoKey : reposToBackup) {
                if (fileName.equals(repoKey)) {
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
                        METADATA_FOLDER);
                if (metadataFolder.exists()) {
                    deleted = FileUtils.deleteQuietly(metadataFolder);
                    if (!deleted) {
                        log.warn("Failed to delete metadata folder {}", metadataFolder.getAbsolutePath());
                    }
                }
            }
        }
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
        Set<RepoPath> pathsToMoveIncludingParents = new HashSet<>();
        for (RepoPath parentPath : pathsByParent.keySet()) {
            Collection<RepoPath> children = pathsByParent.get(parentPath);
            if (parentPath.isRoot()) {
                // parent is the repository itself and cannot be moved, just add the children
                pathsToMoveIncludingParents.addAll(children);
            } else {
                // if the parent children count equals to the number of files to be moved, move the folder instead
                LocalRepo repository = getLocalRepository(parentPath);
                VfsFolder folder =
                        copy ? repository.getImmutableFolder(parentPath) : repository.getMutableFolder(parentPath);
                // get all the folder children using write lock
                List<VfsItem> folderChildren = folder.getImmutableChildren();
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
            URL uri = new URL(headerVal);
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
                .connectionTimeout(15000)
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

    private void registerRepositoriesMBeans() {
        MBeanRegistrationService registrationService = ContextHelper.get().beanForType(MBeanRegistrationService.class);
        registrationService.unregisterAll(REPOSITORIES_MBEAN_TYPE);
        for (LocalRepoDescriptor descriptor : getLocalAndCachedRepoDescriptors()) {
            registrationService.register(new ManagedRepository(descriptor), REPOSITORIES_MBEAN_TYPE,
                    descriptor.getKey());
        }
    }
}
