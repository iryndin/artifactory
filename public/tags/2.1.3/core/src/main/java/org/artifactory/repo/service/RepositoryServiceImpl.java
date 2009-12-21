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

import com.google.common.collect.Lists;
import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.WebstartAddon;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderAdditionalInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.FolderInfoImpl;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenUnitInfo;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.DeployService;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.ItemNotFoundRuntimeException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.rest.RepositoriesRestConstants;
import org.artifactory.api.rest.RestConstants;
import org.artifactory.api.security.AclService;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.backup.BackupJob;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoResolver;
import org.artifactory.info.InfoWriter;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.ItemInfoProxy;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.jcr.trash.Trashman;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenMetadataImportCalculator;
import org.artifactory.repo.HttpRepo;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoDetails;
import org.artifactory.repo.RepoDetailsType;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.repo.interceptor.RepoInterceptors;
import org.artifactory.repo.jcr.JcrLocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.StringResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskCallback;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.HttpClientUtils;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.ZipUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.artifactory.api.repo.DirectoryItem.UP;

/**
 * User: freds Date: Jul 21, 2008 Time: 8:10:12 PM
 */
@Service
@Reloadable(beanClass = InternalRepositoryService.class,
        initAfter = {JcrService.class, MetadataService.class, InternalCacheService.class, RepoInterceptors.class})
public class RepositoryServiceImpl implements InternalRepositoryService {
    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    public static final String PROP_ARTIFACTORY_RECALC_MAVEN_METADATA = "artifactory:recalcMavenMetadata";

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
    private DeployService deployService;

    private VirtualRepo globalVirtualRepo;

    private OrderedMap<String, VirtualRepo> virtualRepositoriesMap = new ListOrderedMap<String, VirtualRepo>();

    // a cache of all the repository keys
    private Set<String> allRepoKeysCache;

    public void init() {
        InternalRepositoryService transactionalMe = getTransactionalMe();
        transactionalMe.rebuildRepositories();
        try {
            //Dump info to the log
            InfoWriter.writeInfo();
        } catch (Exception e) {
            log.warn("Failed dumping system info", e);
        }
        transactionalMe.recalculateMavenMetadataOnMarkedFolders();
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        InternalRepositoryService transactionalMe = getTransactionalMe();
        deleteOrphanRepos(oldDescriptor);
        transactionalMe.rebuildRepositories();
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
        StatusHolder status = new StatusHolder();
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
            repoPath = new RepoPath(localCacheRepo.getKey(), "");
        } else {
            repoPath = new RepoPath(repoKey, "");
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
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void rebuildRepositories() {
        //Create the repository objects from the descriptor
        CentralConfigDescriptor centralConfig = centralConfigService.getDescriptor();
        InternalRepositoryService transactionalMe = getTransactionalMe();

        //Local repos
        OrderedMap<String, LocalRepo> localRepositoriesMap = new ListOrderedMap<String, LocalRepo>();
        OrderedMap<String, LocalRepoDescriptor> localRepoDescriptorMap = centralConfig.getLocalRepositoriesMap();
        for (LocalRepoDescriptor repoDescriptor : localRepoDescriptorMap.values()) {
            LocalRepo repo = new JcrLocalRepo(transactionalMe, repoDescriptor);
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
        OrderedMap<String, RemoteRepo> remoteRepositoriesMap = new ListOrderedMap<String, RemoteRepo>();
        OrderedMap<String, RemoteRepoDescriptor> remoteRepoDescriptorMap = centralConfig.getRemoteRepositoriesMap();
        for (RemoteRepoDescriptor repoDescriptor : remoteRepoDescriptorMap.values()) {
            RemoteRepo repo = new HttpRepo(
                    transactionalMe, (HttpRepoDescriptor) repoDescriptor,
                    centralConfig.isOfflineMode());
            try {
                repo.init();
            } catch (Exception e) {
                log.error("Failed to initialize remote repository '{}'. Repository will be blacked-out!", repo.getKey(),
                        e);
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
        OrderedMap<String, VirtualRepoDescriptor> virtualRepoDescriptorMap =
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

    public List<VirtualRepoItem> getVirtualRepoItems(RepoPath repoPath) {
        VirtualRepo virtualRepo = virtualRepositoryByKey(repoPath.getRepoKey());
        if (virtualRepo == null) {
            throw new RepositoryRuntimeException(
                    "Repository " + repoPath.getRepoKey() + " does not exists!");
        }
        //Get a deep children view of the virtual repository (including contained virtual repos)
        List<VirtualRepoItem> children = virtualRepo.getChildrenDeeply(repoPath);
        List<VirtualRepoItem> result = new ArrayList<VirtualRepoItem>(children.size());
        for (VirtualRepoItem child : children) {
            //Do not add or check hidden items
            String childPath = child.getPath();
            //Security - check that we can return the child
            List<String> repoKeys = child.getRepoKeys();
            Iterator<String> iter = repoKeys.iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                RepoPath childRepoPath = new RepoPath(key, childPath);
                boolean childReader = authService.canRead(childRepoPath);
                if (!childReader) {
                    //Don't bother with stuff that we do not have read access to
                    iter.remove();
                }
            }
            if (repoKeys.size() > 0) {
                result.add(child);
            }
        }
        return result;
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

    public List<DirectoryItem> getDirectoryItems(RepoPath repoPath, boolean withPseudoUpDirItem) {
        final String repoKey = repoPath.getRepoKey();
        final LocalRepo repo = globalVirtualRepo.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            return null;
        }

        String path = repoPath.getPath();
        //List the local repository directory
        if (!repo.itemExists(path)) {
            return null;
        }

        JcrFsItem item = repo.getJcrFsItem(repoPath);
        if (!item.isDirectory()) {
            return null;
        }

        JcrFolder dir = (JcrFolder) item;
        List<JcrFsItem> children = dir.getItems();
        //Sort files by name
        Collections.sort(children);
        List<DirectoryItem> dirItems = new ArrayList<DirectoryItem>();

        //Add the .. link
        if (withPseudoUpDirItem) {
            DirectoryItem upDirItem;
            if (StringUtils.hasLength(path)) {
                upDirItem = new DirectoryItem(UP, dir.getParentFolder().getInfo());
            } else {
                upDirItem = new DirectoryItem(UP, new FolderInfoImpl(new RepoPath("", "/")));
            }

            dirItems.add(upDirItem);
        }

        for (JcrFsItem child : children) {
            //Check if we should return the child
            String itemPath = child.getRelativePath();
            RepoPath childRepoPath = new RepoPath(child.getRepoKey(), itemPath);
            boolean childReader = authService.canImplicitlyReadParentPath(childRepoPath);
            if (!childReader) {
                //Don't bother with stuff that we do not have read access to
                continue;
            }
            ItemInfo info = ItemInfoProxy.create(child.getInfo());
            dirItems.add(new DirectoryItem(info));
        }
        return dirItems;
    }

    @SuppressWarnings({"unchecked"})
    public List<String> getChildrenNames(RepoPath repoPath) {
        final String repoKey = repoPath.getRepoKey();
        final LocalRepo repo = globalVirtualRepo.localOrCachedRepositoryByKey(repoKey);
        if (repo == null) {
            throw new RepositoryRuntimeException(
                    "Tried to get children of a non exiting local repository '" + repoKey + "'.");
        }
        String path = repoPath.getPath();
        return repo.getChildrenNames(path);
    }

    public boolean hasChildren(RepoPath repoPath) {
        List<String> childrenNames = getChildrenNames(repoPath);
        for (String childName : childrenNames) {
            RepoPath childRepoPath =
                    new RepoPath(repoPath.getRepoKey(), repoPath.getPath() + "/" + childName);
            boolean childReader = authService.canImplicitlyReadParentPath(childRepoPath);
            if (childReader) {
                //Its enough that we have a single reader to say we have children
                return true;
            }
        }
        return false;
    }

    public boolean pomExists(RepoDescriptor targetRepo, MavenArtifactInfo artifactInfo) {
        if (!artifactInfo.isValid()) {
            return false;
        }

        MavenArtifactInfo pomArtifactInfo = new MavenArtifactInfo(artifactInfo);
        pomArtifactInfo.setType(MavenArtifactInfo.POM);
        pomArtifactInfo.setClassifier(MavenUnitInfo.NA);
        String path = pomArtifactInfo.getPath();

        //Sanity check
        if (targetRepo == null) {
            throw new IllegalArgumentException("Empty target repository illegal for deployment.");
        }
        LocalRepo localRepo = globalVirtualRepo.localRepositoryByKey(targetRepo.getKey());
        if (localRepo == null) {
            throw new IllegalArgumentException(
                    "Target repository " + targetRepo + " does not exists.");
        }

        //If a pom is already deployed (or a folder by the same name exists), default value
        //should be not to override it
        return localRepo.itemExists(path);
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

    public List<LocalRepoDescriptor> getLocalAndCachedRepoDescriptors() {
        List<LocalRepo> localAndCached = globalVirtualRepo.getLocalAndCachedRepositories();
        ArrayList<LocalRepoDescriptor> result = new ArrayList<LocalRepoDescriptor>();
        for (LocalRepo localRepo : localAndCached) {
            result.add((LocalRepoDescriptor) localRepo.getDescriptor());
        }
        return result;
    }

    public List<RemoteRepoDescriptor> getRemoteRepoDescriptors() {
        List<RemoteRepo> remoteRepositories = globalVirtualRepo.getRemoteRepositories();
        ArrayList<RemoteRepoDescriptor> result = new ArrayList<RemoteRepoDescriptor>();
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

    public String getTextFileContent(FileInfo fileInfo) {
        LocalRepo repo = localOrCachedRepositoryByKey(fileInfo.getRepoKey());
        return repo.getTextFileContent(fileInfo.getRepoPath());
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
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.importStarting();
                //Import the local repositories
                List<LocalRepoDescriptor> repoList = getLocalAndCachedRepoDescriptors();
                importAll(repoList, Collections.<LocalRepoDescriptor>emptyList(), settings);
            } finally {
                tasks.importEnded();
            }
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
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.importStarting();
                //Import each file seperately to avoid a long running transaction
                LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
                if (localRepo == null) {
                    String msg = "The repo key " + repoKey + " is not a local or cached repoitory!";
                    IllegalArgumentException ex = new IllegalArgumentException(msg);
                    status.setError(msg, ex, log);
                    return;
                }
                localRepo.importFrom(settings);
            } finally {
                tasks.importEnded();
            }
        }
    }

    public void exportTo(ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        status.setStatus("Exporting repositories...", log);
        if (TaskCallback.currentTaskToken() == null) {
            exportAsync(null, settings);
        } else {
            List<LocalRepoDescriptor> repos = settings.getRepositories();
            if (repos.isEmpty()) {
                repos = getLocalAndCachedRepoDescriptors();
            }
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
                File repositoriesDir =
                        JcrPath.get().getRepositoriesExportDir(settings.getBaseDir());
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
    public MultiStatusHolder exportSearchResults(List<FileInfo> searchResults, File baseDir, boolean includeMetadata,
            boolean m2Compatible, boolean archive) {
        MultiStatusHolder statusHolder = new MultiStatusHolder();
        for (FileInfo searchResult : searchResults) {
            RepoPath repoPath = searchResult.getRepoPath();
            ExportSettings settings = new ExportSettings(new File(baseDir, repoPath.getRepoKey()));
            settings.setM2Compatible(m2Compatible);
            settings.setIncludeMetadata(includeMetadata);
            StoringRepo storingRepo = storingRepositoryByKey(repoPath.getRepoKey());
            JcrFile jcrFile = new JcrFile(repoPath, storingRepo);
            jcrFile.exportTo(settings);
            if (settings.getStatusHolder().isError()) {
                statusHolder = settings.getStatusHolder();
            }
        }
        if (archive) {
            try {
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
        return statusHolder;
    }

    /**
     * Throws exception if the item does not exist
     *
     * @param repoPath
     * @return
     */
    public ItemInfo getItemInfo(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        JcrFsItem item = localRepo.getJcrFsItem(repoPath);
        if (item != null) {
            return item.getInfo();
        }
        throw new ItemNotFoundRuntimeException("Item " + repoPath + " does not exists");
    }

    public boolean exists(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        return localRepo.itemExists(repoPath.getPath());
    }

    @SuppressWarnings({"unchecked"})
    public <MD> MD getXmlMetdataObject(RepoPath repoPath, Class<MD> metadataClass) {
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            return null;
        }
        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem == null) {
            return null;
        }
        // TODO: should use read lock?
        return (MD) fsItem.getXmlMetdataObject(metadataClass);
    }

    public <MD> void setXmlMetadataObject(RepoPath repoPath, MD xstreamable) {
        if (!authService.canAnnotate(repoPath)) {
            AccessLogger.annotateDenied(repoPath);
            return;
        }

        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem == null) {
            return;
        }

        fsItem.setXmlMetadata(xstreamable);
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
            return fsItem.getXmlMetdata(metadataName);
        } finally {
            // release the raed lock on the fsItem
            LockingHelper.releaseReadLock(repoPath);
        }
    }

    public void setXmlMetadata(RepoPath repoPath, String metadataName, String metadataContent) {
        if (!authService.canAnnotate(repoPath)) {
            AccessLogger.annotateDenied(repoPath);
            return;
        }

        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem == null) {
            return;
        }

        fsItem.setXmlMetadata(metadataName, metadataContent);
    }

    @SuppressWarnings({"unchecked"})
    public List<String> getXmlMetadataNames(RepoPath repoPath, boolean excludeSystemMetadata) {
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            return Collections.emptyList();
        }
        JcrFsItem fsItem = getFsItem(repoPath);
        if (fsItem == null) {
            return null;
        }
        List<String> names = fsItem.getXmlMetadataNames();
        if (excludeSystemMetadata) {
            List<String> filteredNames = new ArrayList<String>();
            for (String name : names) {
                if (StatsInfo.ROOT.equals(name) || FileAdditionalInfo.ROOT.equals(name) ||
                        FolderAdditionalInfo.ROOT.equals(name)) {
                    continue;
                }
                filteredNames.add(name);
            }
            return filteredNames;
        } else {
            return names;
        }
    }

    public boolean hasXmlMetadata(RepoPath repoPath, String metadataName) {
        try {
            LocalRepo localRepo = getLocalRepository(repoPath);
            // will place a read lock on the fsItem
            JcrFsItem fsItem = localRepo.getJcrFsItem(repoPath);
            if (fsItem == null) {
                log.debug("No file or folder found in {}", repoPath);
                return false;
            }
            return fsItem.hasXmlMetadata(metadataName);
        } finally {
            // release the read lock on the fsItem
            LockingHelper.releaseReadLock(repoPath);
        }
    }

    public MoveMultiStatusHolder move(RepoPath fromRepoPath, String targetLocalRepoKey, boolean dryRun) {
        RepoPathMover mover = getRepoPathMover();
        MoveMultiStatusHolder status = mover.moveOrCopy(
                new MoverConfigBuilder(fromRepoPath, targetLocalRepoKey).setCopy(false).setDryRun(dryRun).
                        setExecuteMavenMetadataCalculation(true).build());
        if (status.noItemsMoved()) {
            status.setError("No items were moved during this operation.", log);
        }
        return status;
    }

    public MoveMultiStatusHolder move(Set<RepoPath> pathsToMove, String targetLocalRepoKey, boolean dryRun) {
        Set<RepoPath> pathsToMoveIncludingParents = aggregatePathsToMove(pathsToMove, targetLocalRepoKey);

        log.debug("The following paths will be moved: {}", pathsToMoveIncludingParents);
        // start moving each path separately, marking each folder or file's parent folder for metadata recalculation
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        RepoPathMover mover = getRepoPathMover();
        for (RepoPath pathToMove : pathsToMoveIncludingParents) {
            log.debug("Moving path: {} to {}", pathToMove, targetLocalRepoKey);
            MoveMultiStatusHolder result = mover.moveOrCopy(
                    new MoverConfigBuilder(pathToMove, targetLocalRepoKey).setCopy(false).setDryRun(dryRun)
                            .setExecuteMavenMetadataCalculation(false).setSearchResult(true).build());
            status.merge(result);
        }

        if (status.noItemsMoved()) {
            status.setError("No items were moved during this operation.", log);
        }
        // done moving, launch async call to execute metadata recalculation on all marked folders
        getTransactionalMe().recalculateMavenMetadataOnMarkedFolders();
        return status;
    }

    public MoveMultiStatusHolder copy(RepoPath fromRepoPath, String targetLocalRepoKey, boolean dryRun) {
        RepoPathMover mover = getRepoPathMover();
        MoveMultiStatusHolder status = mover.moveOrCopy(
                new MoverConfigBuilder(fromRepoPath, targetLocalRepoKey).setCopy(true).setDryRun(dryRun).
                        setExecuteMavenMetadataCalculation(true).build());
        if (status.noItemsMoved()) {
            status.setError("No items were copied during this operation.", log);
        }
        return status;
    }

    public MoveMultiStatusHolder copy(Set<RepoPath> pathsToCopy, String targetLocalRepoKey, boolean dryRun) {
        Set<RepoPath> pathsToCopyIncludingParents = aggregatePathsToMove(pathsToCopy, targetLocalRepoKey);

        log.debug("The following paths will be copied: {}", pathsToCopyIncludingParents);
        //Start copying each path separately, marking each folder or file's parent folder for metadata recalculation
        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        RepoPathMover mover = getRepoPathMover();
        for (RepoPath pathToCopy : pathsToCopyIncludingParents) {
            log.debug("Moving path: {} to {}", pathToCopy, targetLocalRepoKey);
            MoveMultiStatusHolder result = mover.moveOrCopy(
                    new MoverConfigBuilder(pathToCopy, targetLocalRepoKey).setCopy(true).setDryRun(dryRun)
                            .setExecuteMavenMetadataCalculation(false).setSearchResult(true).build());
            status.merge(result);
        }

        if (status.noItemsMoved()) {
            status.setError("No items were copied during this operation.", log);
        }
        //Done copying, launch async call to execute metadata recalculation on all marked folders
        getTransactionalMe().recalculateMavenMetadataOnMarkedFolders();
        return status;
    }

    public StatusHolder undeploy(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        StoringRepo storingRepo = storingRepositoryByKey(repoKey);

        if (storingRepo == null) {
            throw new IllegalArgumentException("Could find storing repository by key '" + repoKey + "'");
        }

        StatusHolder statusHolder = new StatusHolder();
        assertDelete(storingRepo, repoPath.getPath(), false, statusHolder);
        if (!statusHolder.isError()) {
            storingRepo.undeploy(repoPath);
        }
        return statusHolder;
    }

    public StatusHolder undeployPaths(List<RepoPath> repoPaths) {
        MultiStatusHolder multiStatusHolder = new MultiStatusHolder();
        InternalRepositoryService internalMe = getTransactionalMe();
        for (RepoPath repoPath : repoPaths) {
            StatusHolder holder = internalMe.undeploy(repoPath);
            multiStatusHolder.merge(holder);
        }

        return multiStatusHolder;
    }

    public void zap(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        if (localRepo.isCache()) {
            LocalCacheRepo cache = (LocalCacheRepo) localRepo;
            cache.zap(repoPath);
        } else {
            log.warn("Got a zap request on a non-local-cache node '" + repoPath + "'.");
        }
    }

    public MavenArtifactInfo getMavenArtifactInfo(ItemInfo itemInfo) {
        String repoKey = itemInfo.getRepoKey();
        LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
        if (localRepo == null) {
            throw new IllegalArgumentException("Repository " + repoKey + " is not a local repository");
        }
        JcrFsItem fsItem = localRepo.getJcrFsItem(itemInfo.getRepoPath());
        if (fsItem.isDirectory()) {
            return null;
        }
        ArtifactResource result = new ArtifactResource(((JcrFile) fsItem).getInfo());
        return result.getMavenInfo();
    }

    public List<FolderInfo> getWithEmptyChildren(FolderInfo folderInfo) {
        LocalRepo repository = getLocalRepository(folderInfo.getRepoPath());
        JcrFolder folder = (JcrFolder) repository.getJcrFsItem(folderInfo.getRepoPath());

        FolderCompactor compactor = ContextHelper.get().beanForType(FolderCompactor.class);
        List<JcrFolder> children = compactor.getFolderWithCompactedChildren(folder);
        List<FolderInfo> result = new ArrayList<FolderInfo>(children.size());
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

    public StoringRepo storingRepositoryByKey(String key) {
        LocalRepo localRepo = localOrCachedRepositoryByKey(key);
        if (localRepo != null) {
            return localRepo;
        } else {
            return virtualRepositoryByKey(key);
        }
    }

    public List<StoringRepo> getStoringRepositories() {
        List<StoringRepo> repoList = new ArrayList<StoringRepo>();
        repoList.addAll(globalVirtualRepo.getLocalAndCachedRepositories());
        repoList.addAll(getVirtualRepositories());
        return repoList;
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

    public List<RealRepo> getLocalAndRemoteRepositories() {
        return globalVirtualRepo.getLocalAndRemoteRepositories();
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
            node.setProperty(PROP_ARTIFACTORY_RECALC_MAVEN_METADATA, true);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to mark node for maven metadata recalculation");
        }
    }

    public void removeMarkForMavenMetadataRecalculation(RepoPath basePath) {
        LocalRepo localRepo = localRepositoryByKeyFailIfNull(basePath);
        JcrFolder baseFolder = localRepo.getJcrFolder(basePath);
        if (baseFolder == null) {
            throw new IllegalArgumentException("No folder found in " + basePath);
        }
        Node baseFolderNode = baseFolder.getNode();
        try {
            if (baseFolderNode.hasProperty(PROP_ARTIFACTORY_RECALC_MAVEN_METADATA)) {
                baseFolderNode.getProperty(PROP_ARTIFACTORY_RECALC_MAVEN_METADATA).remove();
            }
        } catch (RepositoryException e) {
            log.error("Failed to remove maven metadata recalc mark");
        }
    }

    public void updateStats(RepoPath repoPath) {
        LocalRepo repo = localOrCachedRepositoryByKey(repoPath.getRepoKey());
        if (repo == null) {
            log.debug("Not updating download counters - {} is not a local or cached repo resource (no store in use?)",
                    repoPath);
            return;
        }
        JcrFile lockedJcrFile = repo.getLockedJcrFile(repoPath, false);
        if (lockedJcrFile != null) {
            lockedJcrFile.updateDownloadStats();
        }
    }

    public void calculateStats() {
    }

    // get all folders marked for maven metadata calculation and execute the metadata calculation

    public void recalculateMavenMetadataOnMarkedFolders() {
        try {
            String queryStr = "//element(*, " + JcrFolder.NT_ARTIFACTORY_FOLDER + ")[@" +
                    PROP_ARTIFACTORY_RECALC_MAVEN_METADATA + "= true()]";
            QueryResult result = jcr.executeXpathQuery(queryStr);
            NodeIterator nodes = result.getNodes();
            if (nodes.hasNext()) {
                log.info("Found {} nodes marked for maven metadata recalculation", nodes.getSize());
            }
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                String path = node.getPath();
                RepoPath repoPath = JcrPath.get().getRepoPath(path);
                calculateMavenMetadata(repoPath);
            }
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed with metadata calculation", e);
        }
    }

    public void calculateMavenMetadata(RepoPath baseFolderPath) {
        LocalRepo localRepo = localRepositoryByKeyFailIfNull(baseFolderPath);
        JcrFolder baseFolder = localRepo.getJcrFolder(baseFolderPath);
        if (baseFolder == null) {
            throw new IllegalArgumentException("No folder found in " + baseFolderPath);
        }

        MavenMetadataImportCalculator metadataCalculator = new MavenMetadataImportCalculator();
        StatusHolder status = new StatusHolder();
        metadataCalculator.calculate(baseFolder, status);
        removeMarkForMavenMetadataRecalculation(baseFolderPath);
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
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.importStarting();
                internalImportFrom(settings);
            } finally {
                tasks.importEnded();
            }
            status.setStatus("Finished importing repositories...", log);
        }
    }

    public StatusHolder assertValidPath(RealRepo repo, String path) {
        return repo.assertValidPath(new RepoPath(repo.getKey(), path));
    }

    public StatusHolder assertValidDeployPath(LocalRepo repo, String path) {
        StatusHolder status = assertValidPath(repo, path);
        if (status.isError()) {
            return status;
        }

        //Assert deploy privileges
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        boolean canDeploy = authService.canDeploy(repoPath);
        if (!canDeploy) {
            String msg = "User " + authService.currentUsername() + " is not permitted to deploy '" +
                    path + "' into '" + repoPath + "'.";
            status.setError(msg, HttpStatus.SC_FORBIDDEN, log);
            AccessLogger.deployDenied(repoPath);
        }
        if (!status.isError()) {
            assertDelete(repo, path, true, status);
        }
        return status;
    }

    public <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(
            RemoteRepo<T> remoteRepo, RepoResource res) throws IOException {
        LocalCacheRepo localCache = remoteRepo.getLocalCacheRepo();
        String path = res.getRepoPath().getPath();
        RepoResource repoResource = localCache.getInfo(new NullRequestContext(path));
        return remoteRepo.downloadAndSave(res, repoResource);
    }

    public RepoResource unexpireIfExists(LocalRepo localCacheRepo, String path) {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource == null) {
            return new UnfoundRepoResource(new RepoPath(localCacheRepo.getKey(), path), "Object is not in cache");
        }
        return resource;
    }

    public ResourceStreamHandle unexpireAndRetrieveIfExists(LocalRepo localCacheRepo, String path)
            throws IOException {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource != null && resource.isFound()) {
            return localCacheRepo.getResourceStreamHandle(resource);
        }
        return null;
    }

    public ResourceStreamHandle getResourceStreamHandle(Repo repo, RepoResource res)
            throws IOException, RepoAccessException {
        if (res instanceof StringResource) {
            // resource already contains the content - just extract it and return a string resource handle
            String content = ((StringResource) res).getContent();
            return new StringResourceStreamHandle(content);
        } else {
            RepoPath repoPath = res.getRepoPath();
            if (repo.isReal()) {
                //Permissions apply only to real repos
                StatusHolder holder = ((RealRepo) repo).checkDownloadIsAllowed(repoPath);
                if (holder.isError()) {
                    throw new RepoAccessException(holder.getStatusMsg(), repoPath, "download",
                            authService.currentUsername());
                }
            }
            return repo.getResourceStreamHandle(res);
        }
    }

    public List<DeployableUnit> getDeployableUnitsUnder(RepoPath repoPath) {
        try {
            return jcr.getDeployableUnitsUnder(repoPath);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public ArtifactCount getArtifactCount() throws RepositoryRuntimeException {
        try {
            return jcr.getArtifactCount();
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public ArtifactCount getArtifactCount(String repoKey) throws RepositoryRuntimeException {
        try {
            return jcr.getArtifactCount(repoKey);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    }

    public List<VirtualRepoDescriptor> getVirtualReposContainingRepo(RepoDescriptor repoDescriptor) {
        RepoDescriptor descriptor = repoDescriptor;
        if (repoDescriptor instanceof LocalCacheRepoDescriptor) {
            //VirtualRepoResolver does not directly support local cache repos, so if the items descriptor is a cache,
            //We extract the caches remote repo, and use it insted
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
     * Returns a list of local repo descriptors that the user is permitted to deploy on
     *
     * @return List<LocalRepoDescriptor> - List of deploy-permitted local repos
     */
    public List<LocalRepoDescriptor> getDeployableRepoDescriptors() {
        OrderedMap<String, LocalRepoDescriptor> descriptorMap =
                centralConfigService.getDescriptor().getLocalRepositoriesMap();
        List<PermissionTargetInfo> permissionTargetInfos =
                aclService.getPermissionTargets(ArtifactoryPermission.DEPLOY);
        List<LocalRepoDescriptor> permittedDescriptors = new ArrayList<LocalRepoDescriptor>();
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
        return permittedDescriptors;
    }

    public boolean isRepoPathAccepted(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (!StringUtils.hasLength(path)) {
            return true;
        }
        LocalRepo repo = getLocalOrCachedRepository(repoPath);
        return repo.accepts(repoPath);
    }

    public boolean isRepoPathHandled(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (!StringUtils.hasLength(path)) {
            return true;
        }
        LocalRepo repo = getLocalOrCachedRepository(repoPath);
        return repo.handles(path);
    }

    public List<RemoteRepoDescriptor> getSharedRemoteRepoConfigs(String remoteUrl, Map<String, String> headersMap) {

        List<RemoteRepoDescriptor> remoteRepos = Lists.newArrayList();
        List<RepoDetails> remoteReposDetails = getSharedRemoteRepoDetails(remoteUrl, headersMap);

        for (RepoDetails remoteRepoDetails : remoteReposDetails) {
            String configurationUrl = remoteRepoDetails.getConfiguration();
            if (org.apache.commons.lang.StringUtils.isNotBlank(configurationUrl)) {
                RemoteRepoDescriptor remoteRepoConfig = getSharedRemoteRepoConfig(configurationUrl, headersMap);
                if (remoteRepoConfig != null) {
                    remoteRepos.add(remoteRepoConfig);
                }
            }
        }

        return remoteRepos;
    }

    private String importAsync(String repoKey, ImportSettings settings, boolean deleteExistingRepo, boolean wait) {
        MultiStatusHolder status = settings.getStatusHolder();
        QuartzTask task = new QuartzTask(ImportJob.class, "Import");
        task.addAttribute(ImportJob.REPO_KEY, repoKey);
        task.addAttribute(ImportJob.DELETE_REPO, deleteExistingRepo);
        task.addAttribute(ImportSettings.class.getName(), settings);
        taskService.startTask(task);
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
        QuartzTask task = new QuartzTask(ExportJob.class, "Export");
        if (repoKey != null) {
            task.addAttribute(ExportJob.REPO_KEY, repoKey);
        }
        task.addAttribute(ExportSettings.class.getName(), settings);
        taskService.startTask(task);
        boolean completed = taskService.waitForTaskCompletion(task.getToken());
        if (!completed) {
            if (!status.isError()) {
                // Add Error of no completion
                status.setError("The task " + task + " did not complete correctly", log);
            }
        }
    }

    private LocalRepo getLocalRepository(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
        if (localRepo == null) {
            throw new IllegalArgumentException("Repository " + repoKey + " is not a local repository");
        }
        return localRepo;
    }

    private class ImportExportTasks {
        void importStarting() {
            taskService.cancelTasks(ExportJob.class, true);
            taskService.stopTasks(IndexerJob.class, false);
            taskService.stopTasks(BackupJob.class, true);
            taskService.stopTasks(ArtifactCleanupJob.class, true);
        }

        void importEnded() {
            taskService.resumeTasks(IndexerJob.class);
            taskService.resumeTasks(BackupJob.class);
            taskService.resumeTasks(ArtifactCleanupJob.class);
        }
    }

    /**
     * Do the actual full import.
     *
     * @param settings
     * @return true if success, false othewise
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
        int executionCounter = 0;
        for (LocalRepoDescriptor newLocalRepo : newRepoList) {
            File rootImportFolder = new File(settings.getBaseDir(), newLocalRepo.getKey());
            try {
                if (rootImportFolder.exists()) {
                    if (rootImportFolder.isDirectory() && rootImportFolder.list().length > 0) {
                        ImportSettings repoSettings = new ImportSettings(rootImportFolder, settings);
                        boolean deleteExistingRepo = false;
                        if (oldRepoList.contains(newLocalRepo)) {
                            // Full repo delete with undeploy on root repo path
                            deleteExistingRepo = true;
                        }
                        // avoid spawning too many threads
                        boolean wait = executionCounter % 2 == 0;
                        String importTaskToken =
                                importAsync(newLocalRepo.getKey(), repoSettings, deleteExistingRepo, wait);
                        tokens.add(importTaskToken);
                        executionCounter++;
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
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        RepoPath fsItemRepoPath = RepoPath.getMetadataContainerRepoPath(repoPath);
        LockingHelper.releaseReadLock(fsItemRepoPath);
        JcrFsItem fsItem = repo.getLockedJcrFsItem(fsItemRepoPath);
        if (fsItem != null) {
            log.debug("{}: falling back to using cache entry for resource info at '{}'.", this, path);
            //TODO: Change this mechanism since the last updated is used for artifact popularity measurement
            //Reset the resource age so it is kept being cached
            fsItem.setLastUpdated(System.currentTimeMillis());
            log.debug("Unexpired '{}' from local cache '{}'.", path, repo.getKey());
            return repo.getInfo(new NullRequestContext(path));
        }
        return null;
    }

    private static InternalRepositoryService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(InternalRepositoryService.class);
    }

    private void assertDelete(StoringRepo repo, String path, boolean assertOverwrite, StatusHolder statusHolder) {
        RepoPath repoPath = new RepoPath(repo.getKey(), path);
        //Check that has delete rights to replace an exiting item
        if (repo.shouldProtectPathDeletion(path, assertOverwrite)) {
            if (!authService.canDelete(repoPath)) {
                AccessLogger.deleteDenied(repoPath);
                statusHolder.setError(
                        "Not enough permissions to overwrite artifact '" + repoPath +
                                "' (user '" + authService.currentUsername() + "' needs DELETE permission).",
                        HttpStatus.SC_FORBIDDEN, log);
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
        File[] childFiles = targetDir.listFiles();
        for (File childFile : childFiles) {
            String fileName = childFile.getName();
            if (fileName.endsWith(ItemInfo.METADATA_FOLDER)) {
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
                        ItemInfo.METADATA_FOLDER);
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
    private Set<RepoPath> aggregatePathsToMove(Set<RepoPath> pathsToMove, String targetLocalRepoKey) {
        // aggregate paths by parent repo path
        MultiMap<RepoPath, RepoPath> pathsByParent = new MultiHashMap<RepoPath, RepoPath>();
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
                JcrFolder folder = (JcrFolder) repository.getLockedJcrFsItem(parentPath);
                // get all the folder children using write lock
                List<JcrFsItem> folderChildren = jcrRepoService.getChildren(folder, true);
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
        urlBuilder.append(RestConstants.PATH_API).append("/").append(RepositoriesRestConstants.REPOSITORIES_PATH_ROOT).
                append("?").append(RepositoriesRestConstants.REPOSITORIES_PARAM_REPO_TYPE).append("=").
                append(RepoDetailsType.REMOTE.name());

        InputStream responseStream = null;
        try {
            responseStream = executeGetMethod(urlBuilder.toString(), headersMap);
            if (responseStream == null) {
                return Lists.newArrayList();
            }
            JsonParser jsonParser = JacksonFactory.createJsonParser(responseStream);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(jsonParser, new TypeReference<List<RepoDetails>>() {
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
            JsonParser jsonParser = JacksonFactory.createJsonParser(responseStream);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(jsonParser, new TypeReference<HttpRepoDescriptor>() {
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

        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        // Set the socket data timeout
        clientParams.setSoTimeout(15000);
        // Set the connection timeout
        clientParams.setConnectionManagerTimeout(1500);

        // Don't retry
        clientParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));

        //Update the proxy settings
        ProxyDescriptor proxy = InternalContextHelper.get().getCentralConfig().getDescriptor().getDefaultProxy();
        HttpClientUtils.configureProxy(client, proxy);

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