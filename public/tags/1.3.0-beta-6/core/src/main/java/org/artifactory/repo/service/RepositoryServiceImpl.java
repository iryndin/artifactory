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
package org.artifactory.repo.service;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.DeployableUnit;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.mime.PackagingType;
import org.artifactory.api.repo.ArtifactCount;
import org.artifactory.api.repo.DeployableArtifact;
import org.artifactory.api.repo.DirectoryItem;
import static org.artifactory.api.repo.DirectoryItem.UP;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.ItemNotFoundException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.backup.BackupJob;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.info.InfoWriter;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.jcr.schedule.WorkingCopyCommitter;
import org.artifactory.maven.Maven;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.HttpRepo;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.interceptor.UniqueSnapshotsCleanerJcrInterceptor;
import org.artifactory.repo.jcr.JcrRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskCallback;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.utils.PathUtils;
import org.artifactory.worker.WorkMessage;
import org.artifactory.worker.WorkMessagesSession;
import org.artifactory.worker.WorkerService;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;

/**
 * User: freds Date: Jul 21, 2008 Time: 8:10:12 PM
 */
@Service
public class RepositoryServiceImpl implements InternalRepositoryService {
    private static final Logger log = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    private static final String POM = ContentType.mavenPom.getDefaultExtension();
    private static final String JAR = ContentType.javaArchive.getDefaultExtension();
    private static final String XML = ContentType.applicationXml.getDefaultExtension();

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private CentralConfigService centralConfigService;

    @Autowired
    private JcrService jcr;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MetadataService mdService;

    @Autowired
    private WorkerService workerService;

    @Autowired
    private TaskService taskService;

    private VirtualRepo globalVirtualRepo;

    private OrderedMap<String, VirtualRepo> virtualRepositoriesMap =
            new ListOrderedMap<String, VirtualRepo>();

    // a cache of all the repositories keys
    private List<String> allRepoKeysCache;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(InternalRepositoryService.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{JcrService.class,
                MetadataService.class,
                InternalCacheService.class};
    }

    public void init() {
        InternalRepositoryService transactionalMe = getTransactionalMe();
        transactionalMe.rebuildRepositories();
        try {
            //Dump info to the log
            InfoWriter.writeInfo();
        } catch (Exception e) {
            log.warn("Failed dumping system info", e);
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // TODO: See if we can be smarter and reinit only the repos that changed
        taskService.stopTasks(WorkingCopyCommitter.class, true);
        InternalRepositoryService transactionalMe = getTransactionalMe();
        transactionalMe.rebuildRepositories();
        taskService.resumeTasks(WorkingCopyCommitter.class);
    }

    public void destroy() {
    }

    public void rebuildRepositories() {
        //Create the repository objects from the descriptor
        CentralConfigDescriptor centralConfig = centralConfigService.getDescriptor();
        InternalRepositoryService transactionalMe = getTransactionalMe();

        //Local repos
        OrderedMap<String, LocalRepo> localRepositoriesMap =
                new ListOrderedMap<String, LocalRepo>();
        OrderedMap<String, LocalRepoDescriptor> localRepoDescriptorMap =
                centralConfig.getLocalRepositoriesMap();
        for (LocalRepoDescriptor repoDescriptor : localRepoDescriptorMap.values()) {
            LocalRepo repo = new JcrRepo(transactionalMe, repoDescriptor);
            repo.init();
            localRepositoriesMap.put(repo.getKey(), repo);
        }

        //Remote repos
        OrderedMap<String, RemoteRepo> remoteRepositoriesMap =
                new ListOrderedMap<String, RemoteRepo>();
        OrderedMap<String, RemoteRepoDescriptor> remoteRepoDescriptorMap =
                centralConfig.getRemoteRepositoriesMap();
        for (RemoteRepoDescriptor repoDescriptor : remoteRepoDescriptorMap.values()) {
            RemoteRepo repo = new HttpRepo(transactionalMe,
                    (HttpRepoDescriptor) repoDescriptor, centralConfig.isOfflineMode());
            repo.init();
            remoteRepositoriesMap.put(repo.getKey(), repo);
        }

        // create on-the-fly repo descriptor to be used by the global virtual repo
        List<RepoDescriptor> localAndRemoteRepoDescriptors = new ArrayList<RepoDescriptor>();
        localAndRemoteRepoDescriptors.addAll(localRepoDescriptorMap.values());
        localAndRemoteRepoDescriptors.addAll(remoteRepoDescriptorMap.values());
        VirtualRepoDescriptor vrd = new VirtualRepoDescriptor();
        vrd.setRepositories(localAndRemoteRepoDescriptors);
        vrd.setKey(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY);
        // create and init the global virtual repo
        globalVirtualRepo =
                new VirtualRepo(transactionalMe, vrd, localRepositoriesMap, remoteRepositoriesMap);
        // no need to call globalVirtualRepo.init()
        globalVirtualRepo.initSearchRepositoryLists();

        // virtual repos init in 3 passes
        OrderedMap<String, VirtualRepoDescriptor> virtualRepoDescriptorMap =
                centralConfig.getVirtualRepositoriesMap();
        virtualRepositoriesMap.clear();// we rebuild the virtual repo cache
        // 1. create the virtual repos
        for (VirtualRepoDescriptor repoDescriptor : virtualRepoDescriptorMap.values()) {
            VirtualRepo repo = new VirtualRepo(transactionalMe, repoDescriptor);
            virtualRepositoriesMap.put(repo.getKey(), repo);
        }

        // 2. call the init method only after all virtual repos exist
        for (VirtualRepo virtualRepo : virtualRepositoriesMap.values()) {
            virtualRepo.init();
        }

        // 3. call initSearchRepositoryLists of all the virtual repos
        for (VirtualRepo virtualRepo : virtualRepositoriesMap.values()) {
            virtualRepo.initSearchRepositoryLists();
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
        List<VirtualRepoItem> children = virtualRepo.getChildrenDeeply(repoPath.getPath());
        List<VirtualRepoItem> result = new ArrayList<VirtualRepoItem>(children.size());
        for (VirtualRepoItem child : children) {
            //Do not add or check hidden items
            String childPath = child.getPath();
            if (MavenUtils.isHidden(childPath)) {
                continue;
            }
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

    public Repository getRepository() {
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
        JcrFsItem item;
        if (repo.itemExists(path)) {
            item = repo.getJcrFsItem(repoPath);
            if (!item.isDirectory()) {
                return null;
            }
        } else {
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
                upDirItem = new DirectoryItem(UP, new FolderInfo(new RepoPath("", "/")));
            }

            dirItems.add(upDirItem);
        }

        for (JcrFsItem child : children) {
            //Check if we should return the child
            String itemPath = child.getRelativePath();
            RepoPath childRepoPath = new RepoPath(child.getRepoKey(), itemPath);
            boolean childReader = authService.canRead(childRepoPath);
            if (!childReader) {
                //Don't bother with stuff that we do not have read access to
                continue;
            }
            ItemInfo info = child.getInfo();
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
            RepoPath childRepoPath = new RepoPath(repoPath.getRepoKey(), repoPath.getPath() + "/" + childName);
            boolean childReader = authService.canRead(childRepoPath);
            if (childReader) {
                //Its enough that we have a single reader to say we have children
                return true;
            }
        }
        return false;
    }

    public List<LocalRepoDescriptor> getLocalRepoDescriptors() {
        return new ArrayList<LocalRepoDescriptor>(
                centralConfigService.getDescriptor().getLocalRepositoriesMap().values());
    }

    public void deploy(RepoDescriptor targetRepo, DeployableArtifact deployableArtifact,
            boolean forceDeployPom, File uploadedFile) throws RepoAccessException {
        if (!deployableArtifact.isValid()) {
            throw new IllegalArgumentException("Artifact deployment submission attempt ignored - form not valid.");
        }
        //Sanity check
        if (targetRepo == null) {
            throw new IllegalArgumentException("No target repository found for deployment.");
        }
        final LocalRepo localRepo = globalVirtualRepo.localRepositoryByKey(targetRepo.getKey());
        if (localRepo == null) {
            throw new IllegalArgumentException("No target repository found for deployment.");
        }
        //Check acceptance according to include/exclude patterns
        String groupId = deployableArtifact.getGroup();
        String artifactId = deployableArtifact.getArtifact();
        String version = deployableArtifact.getVersion();
        String classifier = deployableArtifact.getClassifier();
        String packaging = deployableArtifact.getPackaging();

        MavenArtifactInfo artifactInfo = new MavenArtifactInfo(groupId, artifactId, version, classifier, packaging);
        String path = artifactInfo.getPath();
        StatusHolder statusHolder = assertValidDeployPath(localRepo, path);
        if (statusHolder.isError()) {
            throw new IllegalArgumentException(statusHolder.getStatusMsg());
        }
        File pomFile = null;
        try {
            InternalArtifactoryContext context = InternalContextHelper.get();
            Maven maven = context.beanForType(Maven.class);
            Artifact artifact = maven.createArtifact(groupId, artifactId, version, classifier, packaging);
            RepoPath repoPath = new RepoPath(targetRepo.getKey(), path);
            if (!authService.canDeploy(repoPath)) {
                AccessLogger.deployDenied(repoPath);
                throw new RepoAccessException("Not enough permissions to deploy artifact '" + artifact + "'.",
                        repoPath, "deploy", authService.currentUsername());
            }

            //Check if we already have a string model for the pom (set from the jar internal pom or during regular pom
            //deployment)
            String originalPomAsString = deployableArtifact.getPomAsString();
            //Create the model
            Model model;
            if (originalPomAsString == null) {
                //Build the model based on user provided values
                model = generateDefaultPom(groupId, artifactId, version, packaging);
            } else {
                //Build the model based on the string and pacth it with user values
                model = stringToMavenModel(originalPomAsString);
                model.setGroupId(groupId);
                model.setArtifactId(artifactId);
                model.setVersion(version);
            }
            String pomString = mavenModelToString(model);
            deployableArtifact.setPomAsString(pomString);

            //Handle extra pom deployment - add the metadata with the gnerated pom file to the artifact
            if (forceDeployPom && !POM.equalsIgnoreCase(packaging)) {
                pomFile = addPomFileMetadata(uploadedFile, pomFile, artifact, pomString);
            }

            //Add plugin metadata
            if (model != null && "maven-plugin".equals(model.getPackaging())) {
                addPluginVersioningMetadata(version, artifact);
            }
            maven.deploy(uploadedFile, artifact, localRepo);
        } catch (ArtifactDeploymentException e) {
            String msg = "Cannot deploy file " + uploadedFile.getName() + ". Cause: " + e.getMessage();
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        } finally {
            if (pomFile != null) {
                pomFile.delete();
            }
        }
    }

    private Model generateDefaultPom(String groupId, String artifactId, String version, String packaging) {
        Model model = new Model();
        model.setModelVersion("4.0.0");
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        model.setPackaging(packaging);
        model.setDescription("Artifactory auto generated POM");
        return model;
    }

    private File addPomFileMetadata(File uploadedFile, File pomFile, Artifact artifact, String pomAsString) {
        //Create the pom file in the uploads dir
        String pomFileName = uploadedFile.getName() + "." + POM;
        //Create the upload folder every time (e.g., in case it has been reaped)
        pomFile = new File(ArtifactoryHome.getTmpUploadsDir(), pomFileName);
        //Write the pom to the file
        OutputStreamWriter osw = null;
        try {
            FileUtils.forceMkdir(ArtifactoryHome.getTmpUploadsDir());
            osw = new OutputStreamWriter(new FileOutputStream(pomFile), "utf-8");
            IOUtils.write(pomAsString, osw);
        } catch (Exception e) {
            String msg = "Cannot save Pom file " + pomFile.getName() + ". Cause: " + e.getMessage();
            log.debug(msg, e);
            throw new RepositoryRuntimeException(msg, e);
        } finally {
            IOUtils.closeQuietly(osw);
        }
        //Add project metadata that will trigger additional deployment of the pom file
        ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
        artifact.addMetadata(metadata);
        return pomFile;
    }

    private void addPluginVersioningMetadata(String version, Artifact artifact) {
        //Add the latest version metadata for plugins.
        //With regular maven deploy this is handled automatically by the
        //AddPluginArtifactMetadataMojo, as part of the "maven-plugin" packaging lifecycle.
        Versioning versioning = new Versioning();
        versioning.setLatest(version); //Set the current deployed version as the latest
        versioning.updateTimestamp();
        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata(artifact, versioning);
        artifact.addMetadata(metadata);
    }

    public boolean pomExists(RepoDescriptor targetRepo, DeployableArtifact deployableArtifact) {
        if (!deployableArtifact.isValid()) {
            return false;
        }
        String groupId = deployableArtifact.getGroup();
        String artifactId = deployableArtifact.getArtifact();
        String version = deployableArtifact.getVersion();
        MavenArtifactInfo artifactInfo = new MavenArtifactInfo(groupId, artifactId, version, null, POM);
        String path = artifactInfo.getPath();

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

    public LocalRepoInterceptor getLocalRepoInterceptor() {
        return new UniqueSnapshotsCleanerJcrInterceptor();
    }

    public VirtualRepo getGlobalVirtualRepo() {
        return globalVirtualRepo;
    }

    public Collection<VirtualRepo> getDeclaredVirtualRepositories() {
        return virtualRepositoriesMap.values();
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

    public VirtualRepoDescriptor virtualRepoDescriptorByKey(String repoKey) {
        if (repoKey == null || repoKey.length() == 0) {
            return null;
        }
        if (VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY.equals(repoKey)) {
            return globalVirtualRepo.getDescriptor();
        }
        return centralConfigService.getDescriptor().getVirtualRepositoriesMap().get(repoKey);
    }

    public String getContent(ItemInfo itemInfo) {
        LocalRepo repo = localOrCachedRepositoryByKey(itemInfo.getRepoKey());
        return repo.getPomContent(itemInfo);
    }

    /**
     * Import all the repositories under the passed folder which matches local or cached repository declared in the
     * configuration. Having empty directory for each repository is allowed and not an error. Nothing will be imported
     * for those.
     *
     * @param settings
     * @param status
     */
    public void importAll(ImportSettings settings, StatusHolder status) {
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(PermissionTargetInfo.ANY_REPO, settings, status, null, true);
        } else {
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.startImport();
                //Import the local repositories
                List<LocalRepoDescriptor> repoList = getLocalAndCachedRepoDescriptors();
                importAll(repoList, Collections.<LocalRepoDescriptor>emptyList(), settings, status);
            } finally {
                tasks.endImport();
            }
        }
    }

    /**
     * Import the artifacts under the folder passed directly in the repository named "repoKey". If no repository with
     * this rpo key exists or if the folder passed is empty, the status will be set to error.
     *
     * @param repoKey
     * @param settings
     * @param status
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importRepo(String repoKey, ImportSettings settings, StatusHolder status) {
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(repoKey, settings, status, null, true);
        } else {
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.startImport();
                //Import each file seperately to avoid a long running transaction
                LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
                if (localRepo == null) {
                    String msg = "The repo key " + repoKey + " is not a local or cached repoitory!";
                    IllegalArgumentException ex = new IllegalArgumentException(msg);
                    status.setError(msg, ex, log);
                    return;
                }
                localRepo.importFrom(settings, status);
            } finally {
                tasks.endImport();
            }
        }
    }

    private String importAsync(String repoKey, ImportSettings settings, StatusHolder status,
            RepoPath deleteRepo, boolean wait) {
        QuartzTask task = new QuartzTask(ImportJob.class, "Import");
        task.addAttribute(ImportJob.REPO_KEY, repoKey);
        task.addAttribute(ImportJob.DELETE_REPO, deleteRepo);
        task.addAttribute(ImportSettings.class.getName(), settings);
        task.addAttribute(StatusHolder.class.getName(), status);
        taskService.startTask(task);
        if (wait) {
            boolean completed = taskService.waitForTaskCompletion(task.getToken());
            if (!completed) {
                if (!status.isError()) {
                    // Add Error of no completion
                    status.setError("The task " + task + " did not complete correctly", log);
                }
            }
        }
        return task.getToken();
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Exporting repositories...", log);
        if (TaskCallback.currentTaskToken() == null) {
            exportAsync(null, settings, status);
        } else {
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.startExport();
                List<LocalRepoDescriptor> repos = settings.getRepositories();
                if (repos.isEmpty()) {
                    repos = getLocalAndCachedRepoDescriptors();
                }
                for (LocalRepoDescriptor localRepo : repos) {
                    boolean stop = taskService.blockIfPausedAndShouldBreak();
                    if (stop) {
                        status.setError("Export was stopped", log);
                        return;
                    }
                    exportRepo(localRepo.getKey(), settings, status);
                    if (status.isError() && settings.isFailFast()) {
                        return;
                    }
                }
            } finally {
                tasks.endExport();
            }
        }
    }

    public void exportRepo(String repoKey, ExportSettings settings, StatusHolder status) {
        if (TaskCallback.currentTaskToken() == null) {
            exportAsync(repoKey, settings, status);
        } else {
            //Check if we need to break/pause
            boolean stop = taskService.blockIfPausedAndShouldBreak();
            if (stop) {
                status.setError("Export was stopped on " + repoKey, log);
                return;
            }
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.startExport();
                File targetDir = JcrPath.get().getRepoExportDir(settings.getBaseDir(), repoKey);
                ExportSettings repoSettings = new ExportSettings(targetDir, settings);
                LocalRepo sourceRepo = localOrCachedRepositoryByKey(repoKey);
                sourceRepo.exportTo(repoSettings, status);
            } finally {
                tasks.endExport();
            }
        }
    }

    private void exportAsync(String repoKey, ExportSettings settings, StatusHolder status) {
        QuartzTask task = new QuartzTask(ExportJob.class, "Export");
        if (repoKey != null) {
            task.addAttribute(ExportJob.REPO_KEY, repoKey);
        }
        task.addAttribute(ExportSettings.class.getName(), settings);
        task.addAttribute(StatusHolder.class.getName(), status);
        taskService.startTask(task);
        boolean completed = taskService.waitForTaskCompletion(task.getToken());
        if (!completed) {
            if (!status.isError()) {
                // Add Error of no completion
                status.setError("The task " + task + " did not complete correctly", log);
            }
        }
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
        throw new ItemNotFoundException("Item " + repoPath + " does not exists");
    }

    public boolean exists(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        return localRepo.itemExists(repoPath.getPath());
    }

    @SuppressWarnings({"unchecked"})
    public <MD> MD getXmlMetdataObject(RepoPath repoPath, Class<MD> metadataClass) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        JcrFsItem fsItem = localRepo.getJcrFsItem(repoPath);
        MD result = (MD) fsItem.getXmlMetdataObject(metadataClass);
        return result;
    }

    public void undeploy(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        StatusHolder statusHolder = new StatusHolder();
        assertDelete(localRepo, repoPath.getPath(), statusHolder);
        if (statusHolder.isError()) {
            throw new IllegalArgumentException(statusHolder.getStatusMsg());
        }
        localRepo.undeploy(repoPath);
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
        LocalRepo localRepo = localRepositoryByKey(repoKey);
        if (localRepo == null) {
            throw new IllegalArgumentException(
                    "Repository " + repoKey + " is not a local repository");
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
        List<JcrFolder> children = folder.withEmptyChildren();
        List<FolderInfo> result = new ArrayList<FolderInfo>(children.size());
        for (JcrFolder child : children) {
            result.add(child.getInfo());
        }
        return result;
    }

    public List<String> getAllRepoKeys() {
        return allRepoKeysCache;
    }

    private void initAllRepoKeysCache() {
        ArrayList<String> newKeys = new ArrayList<String>();
        newKeys.addAll(globalVirtualRepo.getLocalRepositoriesMap().keySet());
        newKeys.addAll(globalVirtualRepo.getRemoteRepositoriesMap().keySet());
        for (LocalCacheRepo cacheRepo : globalVirtualRepo.getLocalCaches()) {
            newKeys.add(cacheRepo.getKey());
        }
        newKeys.add(globalVirtualRepo.getKey());
        newKeys.addAll(virtualRepositoriesMap.keySet());
        allRepoKeysCache = newKeys;
    }

    /**
     * Get the artifact model from a jar or pom file
     *
     * @param uploadedFile .jar or .pom file
     * @return null if no pom found
     * @throws java.io.IOException if uploaded file is unreadable
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    public DeployableArtifact getDeployableArtifact(File uploadedFile) {
        DeployableArtifact result = new DeployableArtifact();
        final ContentType ct = PackagingType.getContentType(uploadedFile);
        String fileName = uploadedFile.getName();
        if (ct.isJarVariant()) {
            //JAR variant
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(new FileInputStream(uploadedFile));
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    //Look for pom.xml in META-INF/maven/
                    String name = entry.getName();
                    if (name.startsWith("META-INF/maven/") && name.endsWith("pom.xml")) {
                        int size = (int) entry.getSize();
                        //Sanity check
                        if (size < 0) {
                            return null;
                        }
                        //Read the uncompressed content
                        try {
                            readModel(jis, result);
                        } catch (Exception e) {
                            throw new RepositoryRuntimeException(
                                    "Failed to read maven model from '" + entry.getName()
                                            + "'. Cause: " + e.getMessage() + ".", e);
                        }
                        result.setPackaging(JAR);
                        return result;
                    }
                }
            } catch (IOException e) {
                throw new RepositoryRuntimeException(
                        "Failed to read maven model from '" + uploadedFile
                                + "'. Cause: " + e.getMessage() + ".", e);
            } finally {
                IOUtils.closeQuietly(jis);
            }
        } else if (ct.isXml()) {
            //POM
            try {
                FileInputStream in = new FileInputStream(uploadedFile);
                try {
                    readModel(in, result);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                result.setPackaging(POM);
            } catch (Exception e) {
                //Ignore exception - not every xml is a pom
                if (log.isDebugEnabled()) {
                    log.debug("Failed to read maven model from '" + fileName + "'. Cause: " +
                            e.getMessage() + ".");
                }
                result.setPackaging(XML);
            }

        } else {
            //Other extension
            String extension = PathUtils.getExtension(fileName);
            if (extension != null) {
                result.setPackaging(extension);
            }
        }
        //Calculate the classifier according to the version in the file name
        boolean classifierSet = false;
        if (result.hasVersion()) {
            String version = result.getVersion();
            int versionBeginIdx = fileName.lastIndexOf(version);
            int classifierBeginIdx = versionBeginIdx + version.length();
            int extBeginIdx = fileName.lastIndexOf('.');
            if (versionBeginIdx > 0 && classifierBeginIdx < extBeginIdx &&
                    fileName.charAt(classifierBeginIdx) == '-') {
                String classif = fileName.substring(classifierBeginIdx + 1, extBeginIdx);
                result.setClassifier(classif);
            }
            classifierSet = true;
        }
        //Try to guess the artifactId and version properties from the uploadedFile name by regexp
        Matcher matcher = MavenNaming.artifactMatcher(fileName);
        if (matcher.matches()) {
            if (!result.hasClassifer() && !classifierSet) {
                result.setClassifier(matcher.group(5));
            }
            if (!result.hasArtifact()) {
                result.setArtifact(matcher.group(1));
            }
            if (!result.hasVersion()) {
                result.setVersion(matcher.group(2));
            }
        }
        //Complete values by falling back to dumb defaults
        if (!StringUtils.hasText(result.getArtifact())) {
            result.setArtifact(fileName);
        }
        if (!StringUtils.hasText(result.getGroup())) {
            //If we have no group, set it to be the same as the artifact name
            result.setGroup(result.getArtifact());
        }
        if (!StringUtils.hasText(result.getVersion())) {
            result.setVersion(fileName);
        }
        return result;
    }

    private LocalRepo getLocalRepository(RepoPath repoPath) {
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepo = localRepositoryByKey(repoKey);
        if (localRepo == null) {
            throw new IllegalArgumentException(
                    "Repository " + repoKey + " is not a local repository");
        }
        return localRepo;
    }

    private void readModel(InputStream is, DeployableArtifact da) throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        InputStreamReader pomStream = new InputStreamReader(is, "utf-8");
        Model model = reader.read(pomStream);

        Parent parent = model.getParent();
        String groupId = model.getGroupId();
        if (groupId == null && parent != null) {
            groupId = parent.getGroupId();
        }
        da.setGroup(groupId);
        da.setArtifact(model.getArtifactId());
        String version = model.getVersion();
        if (version == null && parent != null) {
            version = parent.getVersion();
        }
        da.setVersion(version);
        da.setPomAsString(mavenModelToString(model));
    }

    private String mavenModelToString(Model model) {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        StringWriter stringWriter = new StringWriter();
        try {
            writer.write(stringWriter, model);
        } catch (IOException e) {
            throw new RepositoryRuntimeException("Failed to convert maven model to string", e);
        }
        return stringWriter.toString();
    }

    private Model stringToMavenModel(String pomAsString) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        StringReader pomStream = new StringReader(pomAsString);
        try {
            return reader.read(pomStream);
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed to convert string to maven model", e);
        }
    }

    public List<RepoDescriptor> getLocalAndRemoteRepoDescriptors() {
        return globalVirtualRepo.getDescriptor().getRepositories();
    }

    public boolean isAnonAccessEnabled() {
        return authService.isAnonAccessEnabled();
    }

    public LocalRepo localRepositoryByKey(String key) {
        return globalVirtualRepo.localOrCachedRepositoryByKey(key);
    }

    public RemoteRepo remoteRepositoryByKey(String key) {
        return globalVirtualRepo.remoteRepositoryByKey(key);
    }

    public VirtualRepo virtualRepositoryByKey(String key) {
        VirtualRepo repo = virtualRepositoriesMap.get(key);
        if (repo == null && VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY.equals(key)) {
            repo = globalVirtualRepo;
        }
        return repo;
    }

    public LocalRepo localOrCachedRepositoryByKey(String key) {
        return globalVirtualRepo.localOrCachedRepositoryByKey(key);
    }

    public LocalRepoDescriptor localOrCachedRepoDescriptorByKey(String key) {
        LocalRepo localRepo = globalVirtualRepo.localOrCachedRepositoryByKey(key);
        if (localRepo != null) {
            return (LocalRepoDescriptor) localRepo.getDescriptor();
        }
        return null;
    }

    public Repo nonCacheRepositoryByKey(String key) {
        Repo repo = globalVirtualRepo.nonCacheRepositoryByKey(key);
        if (repo == null) {
            repo = virtualRepositoriesMap.get(key);
        }
        assert repo != null;
        return repo;
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

    private class ImportExportTasks {
        void startImport() {
            taskService.stopTasks(WorkingCopyCommitter.class, true);
            taskService.cancelTasks(ExportJob.class, true);
            taskService.stopTasks(IndexerJob.class, true);
            taskService.stopTasks(BackupJob.class, true);
        }

        void endImport() {
            taskService.resumeTasks(WorkingCopyCommitter.class);
            taskService.resumeTasks(IndexerJob.class);
            taskService.resumeTasks(BackupJob.class);
        }

        void startExport() {
            taskService.stopTasks(WorkingCopyCommitter.class, true);
        }

        void endExport() {
            taskService.resumeTasks(WorkingCopyCommitter.class);
        }
    }

    /**
     * This method will delete and import all the local and cached repositories listed in the (newly loaded) config
     * file. This action is resource intensive and is done in multiple transactions to avoid out of memory exceptions.
     */
    public void importFrom(ImportSettings settings, StatusHolder status) {
        if (TaskCallback.currentTaskToken() == null) {
            importAsync(null, settings, status, null, true);
        } else {
            status.setStatus("Importing repositories...", log);
            ImportExportTasks tasks = new ImportExportTasks();
            try {
                tasks.startImport();
                internalImportFrom(settings, status);
            } finally {
                tasks.endImport();
            }
            status.setStatus("Finished importing repositories...", log);
        }
    }

    /**
     * Do the actual full import.
     *
     * @param settings
     * @param status
     * @return true if success, false othewise
     */
    private boolean internalImportFrom(ImportSettings settings, StatusHolder status) {
        //Remove anything under the wc folder
        File workingCopyDir = ArtifactoryHome.getWorkingCopyDir();
        try {
            FileUtils.deleteDirectory(workingCopyDir);
            FileUtils.forceMkdir(workingCopyDir);
        } catch (IOException e) {
            status.setError("Failed to recreate working copy dir " + workingCopyDir, e,
                    log);
            return false;
        }
        InternalRepositoryService transactionalMe = getTransactionalMe();
        //A new config file was loaded, refresh and init
        transactionalMe.rebuildRepositories();
        File repoRootPath =
                new File(settings.getBaseDir(), JcrPath.get().getRepoJcrRootPath());
        //Keep the current list of repositories for deletion after or during import
        List<LocalRepoDescriptor> oldRepoList = getLocalAndCachedRepoDescriptors();
        //Import all local repositories
        List<LocalRepoDescriptor> newRepoList = settings.getRepositories();
        if (newRepoList.isEmpty()) {
            newRepoList = getLocalAndCachedRepoDescriptors();
        }
        ImportSettings repositoriesImportSettings =
                new ImportSettings(repoRootPath, settings);
        importAll(newRepoList, oldRepoList, repositoriesImportSettings, status);
        return !status.isError();
    }

    private void importAll(List<LocalRepoDescriptor> newRepoList,
            List<LocalRepoDescriptor> oldRepoList, ImportSettings settings, StatusHolder status) {
        List<String> tokens = new ArrayList<String>(newRepoList.size());
        for (LocalRepoDescriptor newLocalRepo : newRepoList) {
            File rootImportFolder = new File(settings.getBaseDir(), newLocalRepo.getKey());
            try {
                if (rootImportFolder.exists() && rootImportFolder.isDirectory() && rootImportFolder.list().length > 0) {
                    ImportSettings repoSettings = new ImportSettings(rootImportFolder, settings);
                    RepoPath deleteRepo = null;
                    if (oldRepoList.contains(newLocalRepo)) {
                        // Full repo delete with undeploy on root repo path
                        deleteRepo = new RepoPath(newLocalRepo.getKey(), "");
                    }
                    tokens.add(importAsync(newLocalRepo.getKey(), repoSettings, status, deleteRepo, false));
                } else {
                    String message = "The directory " + rootImportFolder +
                            " does not exists or is empty. No import done on " + newLocalRepo;
                    int statusCode = StatusHolder.CODE_OK;
                    if (settings.isFailIfEmpty()) {
                        statusCode = StatusHolder.CODE_INTERNAL_ERROR;
                    }
                    status.setStatus(statusCode, message, log);
                }
            } catch (Exception e) {
                status.setError(
                        "Could not import repository " + newLocalRepo + " from " + rootImportFolder, e,
                        log);
                if (settings.isFailFast()) {
                    return;
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

    public StatusHolder assertValidPath(RealRepo repo, String path) {
        StatusHolder statusHolder = new StatusHolder();
        statusHolder.setActivateLogging(false);
        if (repo.isBlackedOut()) {
            statusHolder.setError(
                    "The repository '" + repo.getKey() + "' is blacked out and cannot accept artifact '" + path +
                            "'.", HttpStatus.SC_FORBIDDEN, log);
        } else if (!repo.handles(path)) {
            statusHolder.setError(
                    "The repository '" + repo.getKey() + "' rejected the artifact '" + path +
                            "' due to its snapshot/release handling policy.", HttpStatus.SC_FORBIDDEN, log);
        } else if (!repo.accepts(path)) {
            statusHolder.setError(
                    "The repository '" + repo.getKey() + "' rejected the artifact '" + path +
                            "' due to its include/exclude patterns settings.", HttpStatus.SC_FORBIDDEN, log);
        }
        return statusHolder;
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
            String msg = "Upload rejected: not permitted to deploy '" + path + "' into '" + repoPath + "'.";
            status.setError(msg, HttpStatus.SC_FORBIDDEN, log);
            AccessLogger.deployDenied(repoPath);
        }
        if (!status.isError()) {
            assertDelete(repo, path, status);
        }
        return status;
    }

    public <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(
            RemoteRepo<T> remoteRepo, RepoResource res) throws IOException {
        LocalCacheRepo localCache = remoteRepo.getLocalCacheRepo();
        String path = res.getRepoPath().getPath();
        JcrFile lockedJcrFile = localCache.getLockedJcrFile(path, true);
        RepoResource targetResource;
        if (lockedJcrFile.isCreatedFromJcr()) {
            targetResource = new SimpleRepoResource(lockedJcrFile.getInfo());
        } else {
            targetResource = new UnfoundRepoResource(lockedJcrFile.getRepoPath(), "transient");
        }
        return remoteRepo.downloadAndSave(res, targetResource);
    }

    public RepoResource unexpireIfExists(LocalRepo<LocalCacheRepoDescriptor> localCacheRepo,
            String path) {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource == null) {
            return new UnfoundRepoResource(new RepoPath(localCacheRepo.getKey(), path),
                    "Object is not in cache");
        }
        return resource;
    }

    public ResourceStreamHandle unexpireAndRetrieveIfExists(
            LocalRepo<LocalCacheRepoDescriptor> localCacheRepo, String path)
            throws IOException {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path);
        if (resource != null && resource.isFound()) {
            return localCacheRepo.getResourceStreamHandle(resource);
        }
        return null;
    }

    public ResourceStreamHandle getResourceStreamHandle(RealRepo repo, RepoResource res)
            throws IOException, RepoAccessException {
        RepoPath repoPath = res.getRepoPath();
        if (!authService.canRead(repoPath)) {
            AccessLogger.downloadDenied(repoPath);
            throw new RepoAccessException(
                    "Not enough permissions to download artifact '" + repoPath + "'.",
                    repoPath, "download", authService.currentUsername());
        }
        return repo.getResourceStreamHandle(res);
    }

    public String getMetadataProperty(RealRepo repo, String path) throws IOException {
        return repo.getMetadataProperty(path);
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

    private RepoResource internalUnexpireIfExists(
            LocalRepo<LocalCacheRepoDescriptor> repo, String path) {
        JcrFile file = repo.getLockedJcrFile(path, false);
        if (file != null) {
            if (log.isDebugEnabled()) {
                log.debug(this +
                        ": falling back to using cache entry for resource info at '" +
                        path + "'.");
            }
            //TODO: Change this mechanism since the last updated is used for artifact popularity
            //measurement
            //Reset the resource age so it is kept being cached
            file.setLastUpdated(System.currentTimeMillis());
            if (log.isDebugEnabled()) {
                log.debug("Unexpired '" + path + "' from local cache '" +
                        repo.getKey() + "'.");
            }
            return repo.getInfo(path);
        }
        return null;
    }

    public void publish(WorkMessage message) {
        if (message.publishAfterCommit()) {
            jcr.getSessionResource(WorkMessagesSession.class).addWorkMessage(message);
        } else {
            workerService.publish(message);
        }
    }

    public void executeMessage(WorkMessage message) {
        message.execute();
    }

    private static InternalRepositoryService getTransactionalMe() {
        InternalRepositoryService transactionalMe =
                InternalContextHelper.get().beanForType(
                        InternalRepositoryService.class);
        return transactionalMe;
    }

    private void assertDelete(LocalRepo repo, String path, StatusHolder statusHolder) {
        //Check that has delete rights to replace an exiting item
        if (repo.itemExists(path) && repo.shouldProtectPathDeletion(path)) {
            RepoPath repoPath = new RepoPath(repo.getKey(), path);
            if (!authService.canDelete(repoPath)) {
                AccessLogger.deleteDenied(repoPath);
                statusHolder.setError(
                        "Not enough permissions to overwrite artifact '" + repoPath +
                                "' (needs delete permission).", HttpStatus.SC_FORBIDDEN, log);
            }
        }
    }
}
