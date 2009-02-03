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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
import org.artifactory.api.common.PackagingType;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.repo.DeployableArtifact;
import org.artifactory.api.repo.DirectoryItem;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.VirtualRepoItem;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.RepoAccessException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.config.CentralConfigServiceImpl;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
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
import org.artifactory.repo.JcrRepo;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.index.IndexerManagerImpl;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.interceptor.UniqueSnapshotsCleanerJcrInterceptor;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.SimpleRepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.artifactory.utils.PathUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.jcr.Repository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;

/**
 * User: freds Date: Jul 21, 2008 Time: 8:10:12 PM
 */
@Service
public class RepositoryServiceImpl implements InternalRepositoryService {
    private static final Logger LOGGER =
            LogManager.getLogger(RepositoryServiceImpl.class);

    @Autowired
    private AuthorizationService authService;

    @Autowired
    private CentralConfigServiceImpl centralConfig;

    @Autowired
    private Timer timer;

    @Autowired
    private JcrService jcr;

    @Autowired
    private MetadataService mdService;

    private WorkingCopyCommitter workingCopyCommitter;

    @Autowired
    private IndexerManagerImpl indexerManager;

    private VirtualRepo globalVirtualRepo;
    private OrderedMap<String, VirtualRepo> virtualRepositoriesMap =
            new ListOrderedMap<String, VirtualRepo>();

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addPostInit(InternalRepositoryService.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[]{CentralConfigServiceImpl.class, JcrService.class};
    }

    public void init() {
        InternalRepositoryService transactionalMe = getTransactionalMe();
        transactionalMe.rebuildRepositories();
        //Start the wc committer thread
        restartWorkingCopyCommitter();
    }

    @Transactional
    public void rebuildRepositories() {
        //Create the repository objects from the descriptor
        CentralConfigDescriptor descriptor = centralConfig.getDescriptor();
        InternalRepositoryService transactionalMe = getTransactionalMe();

        //Local repos
        OrderedMap<String, LocalRepo> localRepositoriesMap =
                new ListOrderedMap<String, LocalRepo>();
        OrderedMap<String, LocalRepoDescriptor> localRepoDescriptorMap =
                descriptor.getLocalRepositoriesMap();
        for (LocalRepoDescriptor repoDescriptor : localRepoDescriptorMap.values()) {
            LocalRepo repo = new JcrRepo(transactionalMe, repoDescriptor);
            repo.init();
            localRepositoriesMap.put(repo.getKey(), repo);
        }

        //Remote repos
        OrderedMap<String, RemoteRepo> remoteRepositoriesMap =
                new ListOrderedMap<String, RemoteRepo>();
        OrderedMap<String, RemoteRepoDescriptor> remoteRepoDescriptorMap =
                descriptor.getRemoteRepositoriesMap();
        for (RemoteRepoDescriptor repoDescriptor : remoteRepoDescriptorMap.values()) {
            RemoteRepo repo = new HttpRepo(transactionalMe, (HttpRepoDescriptor) repoDescriptor);
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
                descriptor.getVirtualRepositoriesMap();
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
    }

    @Transactional
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

    @Transactional
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
            item = repo.getFsItem(path);
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
        //Add the .. link if necessary
        if (withPseudoUpDirItem && StringUtils.hasLength(path)) {
            DirectoryItem upDirItem = new DirectoryItem(
                    DirectoryItem.UP, dir.getParentFolder().getInfo());
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

    @Transactional
    @SuppressWarnings({"unchecked"})
    //TODO: [by yl] why we get an unchecked warning at all?!
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

    @Transactional
    public boolean hasChildren(RepoPath repoPath) {
        List<String> childrenNames = getChildrenNames(repoPath);
        for (String childName : childrenNames) {
            RepoPath childRepoPath =
                    new RepoPath(repoPath.getRepoKey(), repoPath.getPath() + "/" + childName);
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
                centralConfig.getDescriptor().getLocalRepositoriesMap().values());
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    @Transactional
    public void deploy(RepoDescriptor targetRepo, DeployableArtifact deployableArtifact,
            boolean deployPom, File uploadedFile) throws RepoAccessException {
        if (!deployableArtifact.isValid()) {
            throw new IllegalArgumentException(
                    "Artifact deployment submission attempt ignored - form not valid.");
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
        String path = ArtifactResource.getPath(groupId, artifactId, version, classifier, packaging);
        StatusHolder statusHolder = assertValidDeployPath(localRepo, path);
        if (statusHolder.isError()) {
            throw new IllegalArgumentException(statusHolder.getStatusMsg());
        }
        Artifact artifact;
        File pomFile = null;
        try {
            InternalArtifactoryContext context = InternalContextHelper.get();
            Maven maven = context.beanForType(Maven.class);
            artifact = maven.createArtifact(groupId, artifactId, version, classifier, packaging);
            RepoPath repoPath = new RepoPath(targetRepo.getKey(), path);
            if (!authService.canDeploy(repoPath)) {
                AccessLogger.deployDenied(repoPath);
                throw new RepoAccessException(
                        "Not enough permissions to deploy artifact '" + artifact + "'.",
                        repoPath, "deploy", authService.currentUsername());
            }
            Model model = (Model) deployableArtifact.getModel();
            //Handle pom deployment
            if (deployPom && !packaging.equalsIgnoreCase(PackagingType.pom.name())) {
                // TODO: Probable bug: If model not null the value entered by the user are ignored
                if (model == null) {
                    model = new Model();
                    model.setModelVersion("4.0.0");
                    model.setGroupId(groupId);
                    model.setArtifactId(artifactId);
                    model.setVersion(version);
                    model.setPackaging(packaging);
                    model.setDescription("Auto generated POM");
                    deployableArtifact.setModel(model);
                }
                //Create the pom file in the uploads dir
                //Artifact pomArtifact = maven.createArtifact(
                //        groupId, artifactId, version, classifier, PackagingType.pom.name());
                String pomFileName = uploadedFile.getName() + ".pom";
                //Create the upload folder evey time (e.g., in case it has been reaped)
                pomFile = new File(ArtifactoryHome.getTmpUploadsDir(), pomFileName);
                //Write the pom to the file
                OutputStreamWriter osw = null;
                try {
                    FileUtils.forceMkdir(ArtifactoryHome.getTmpUploadsDir());
                    MavenXpp3Writer writer = new MavenXpp3Writer();
                    osw = new OutputStreamWriter(new FileOutputStream(pomFile), "utf-8");
                    writer.write(osw, model);
                } catch (Exception e) {
                    final String msg = "Cannot save Pom file " + pomFile.getName() +
                            ". Cause: " + e.getMessage();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(msg, e);
                    }
                    throw new RepositoryRuntimeException(msg, e);
                } finally {
                    IOUtils.closeQuietly(osw);
                }
                //Add project metadata that will trigger additional deployment of the pom file
                ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pomFile);
                artifact.addMetadata(metadata);
            }
            //Add the latest version metadata for plugins.
            //With regular maven deploy this is handled automatically by the
            //AddPluginArtifactMetadataMojo, as part of the "maven-plugin" packaging lifecycle.
            if (model != null && "maven-plugin".equals(model.getPackaging())) {
                //Set the current deployed version as the latest
                Versioning versioning = new Versioning();
                versioning.setLatest(version);
                versioning.updateTimestamp();
                ArtifactRepositoryMetadata metadata =
                        new ArtifactRepositoryMetadata(artifact, versioning);
                artifact.addMetadata(metadata);
            }
            maven.deploy(uploadedFile, artifact, localRepo);
        } catch (ArtifactDeploymentException e) {
            final String msg = "Cannot deploy file " + uploadedFile.getName() +
                    ". Cause: " + e.getMessage();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(msg, e);
            }
            throw new RepositoryRuntimeException(msg, e);
        } finally {
            if (pomFile != null) {
                pomFile.delete();
            }
        }
    }

    @Transactional(readOnly = true)
    public boolean pomExists(RepoDescriptor targetRepo, DeployableArtifact deployableArtifact) {
        if (!deployableArtifact.isValid()) {
            return false;
        }
        String groupId = deployableArtifact.getGroup();
        String artifactId = deployableArtifact.getArtifact();
        String version = deployableArtifact.getVersion();
        String packaging = PackagingType.pom.name();
        String path = ArtifactResource.getPath(groupId, artifactId, version, null, packaging);
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
        return centralConfig.getDescriptor().getVirtualRepositoriesMap().get(repoKey);
    }

    @Transactional
    public String getContent(ItemInfo itemInfo) {
        LocalRepo repo = localOrCachedRepositoryByKey(itemInfo.getRepoKey());
        return repo.getPomContent(itemInfo);
    }

    /**
     * Import all the repositories under the passed folder which matches local or cached repository
     * declared in the configuration. Having empty directory for each repository is allowed and not
     * an error. Nothing will be imported for those.
     *
     * @param settings
     * @param status
     */
    @Transactional
    public void importAll(ImportSettings settings, StatusHolder status) {
        //Import the local repositories
        List<LocalRepoDescriptor> repoList = getLocalAndCachedRepoDescriptors();
        importAll(repoList, Collections.<LocalRepoDescriptor>emptyList(), settings, status);
    }

    /**
     * Import the artifacts under the folder passed directly in the repository named "repoKey". If
     * no repository with this rpo key exists or if the folder passed is empty, the status will be
     * set to error.
     *
     * @param repoKey
     * @param settings
     * @param status
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void importRepo(String repoKey, ImportSettings settings, StatusHolder status) {
        //Import each file seperately to avoid a long running transaction
        LocalRepo localRepo = localOrCachedRepositoryByKey(repoKey);
        if (localRepo == null) {
            String msg = "The repo key " + repoKey + " is not a local or cached repoitory!";
            IllegalArgumentException ex = new IllegalArgumentException(msg);
            status.setError(msg, ex, LOGGER);
            return;
        }
        localRepo.importFrom(settings, status);
    }

    @Transactional
    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Exporting repositories...");
        List<LocalRepoDescriptor> repos = settings.getReposToExport();
        if (repos.isEmpty()) {
            repos = getLocalAndCachedRepoDescriptors();
        }
        for (LocalRepoDescriptor localRepo : repos) {
            exportRepo(localRepo.getKey(), settings, status);
        }
    }

    @Transactional(readOnly = true)
    public void exportRepo(String repoKey, ExportSettings settings, StatusHolder status) {
        File targetDir = JcrPath.get().getRepoExportDir(settings.getBaseDir(), repoKey);
        ExportSettings repoSettings = new ExportSettings(targetDir, settings);
        LocalRepo sourceRepo = localOrCachedRepositoryByKey(repoKey);
        sourceRepo.exportTo(repoSettings, status);
    }

    /**
     * Throws exception if the item does not exist
     *
     * @param repoPath
     * @return
     */
    @Transactional
    public ItemInfo getItemInfo(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        String absPath = localRepo.getRepoRootPath() + "/" + repoPath.getPath();
        ItemInfo fromCache = mdService.getInfoFromCache(absPath, FileInfo.class);
        if (fromCache != null) {
            return fromCache;
        }
        fromCache = mdService.getInfoFromCache(absPath, FolderInfo.class);
        if (fromCache != null) {
            return fromCache;
        }
        JcrFsItem fsItem = localRepo.getFsItem(repoPath.getPath());
        ItemInfo result = fsItem.getInfo();
        return result;
    }

    @Transactional
    public boolean exists(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        return localRepo.itemExists(repoPath.getPath());
    }

    @Transactional
    @SuppressWarnings({"unchecked"})
    public <MD> MD getXmlMetdataObject(RepoPath repoPath, Class<MD> metadataClass) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        JcrFsItem fsItem = localRepo.getFsItem(repoPath.getPath());
        MD result = (MD) fsItem.getXmlMetdataObject(metadataClass);
        return result;
    }

    @Transactional
    public void undeploy(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        StatusHolder statusHolder = assertDelete(localRepo, repoPath.getPath(), new StatusHolder());
        if (statusHolder.isError()) {
            throw new IllegalArgumentException(statusHolder.getStatusMsg());
        }
        localRepo.undeploy(repoPath.getPath());
    }

    @Transactional
    public void zap(RepoPath repoPath) {
        LocalRepo localRepo = getLocalRepository(repoPath);
        if (localRepo.isCache()) {
            LocalCacheRepo cache = (LocalCacheRepo) localRepo;
            cache.expire(repoPath.getPath());
        } else {
            LOGGER.warn("Got a zap request on a non-local-cache node '" + repoPath + "'.");
        }
    }

    @Transactional
    public MavenArtifactInfo getMavenArtifactInfo(ItemInfo itemInfo) {
        String repoKey = itemInfo.getRepoKey();
        LocalRepo localRepo = localRepositoryByKey(repoKey);
        if (localRepo == null) {
            throw new IllegalArgumentException(
                    "Repository " + repoKey + " is not a local repository");
        }
        JcrFsItem fsItem = localRepo.getFsItem(itemInfo.getRelPath());
        if (fsItem.isDirectory()) {
            return null;
        }
        ArtifactResource result = new ArtifactResource(((JcrFile) fsItem).getInfo());
        return result.getMavenArtifactInfo();
    }

    @Transactional(readOnly = true)
    public List<FolderInfo> getWithEmptyChildren(FolderInfo folderInfo) {
        LocalRepo repository = getLocalRepository(folderInfo.getRepoPath());
        JcrFolder folder = (JcrFolder) repository.getFsItem(folderInfo.getRelPath());
        List<JcrFolder> children = folder.withEmptyChildren();
        List<FolderInfo> result = new ArrayList<FolderInfo>(children.size());
        for (JcrFolder child : children) {
            result.add(child.getInfo());
        }
        return result;
    }

    public List<String> getAllRepoKeys() {
        List<String> result = new ArrayList<String>();
        result.addAll(globalVirtualRepo.getLocalRepositoriesMap().keySet());
        result.addAll(globalVirtualRepo.getRemoteRepositoriesMap().keySet());
        for (LocalCacheRepo cacheRepo : globalVirtualRepo.getLocalCaches()) {
            result.add(cacheRepo.getKey());
        }
        result.add(globalVirtualRepo.getKey());
        result.addAll(virtualRepositoriesMap.keySet());
        return result;
    }

    /**
     * Get the artifact model from a jar or pom file
     *
     * @param uploadedFile .jar or .pom file
     * @return null if no pom found
     * @throws java.io.IOException if uploaded file is unreadable
     */
    @SuppressWarnings({"OverlyComplexMethod"})
    @Transactional
    public DeployableArtifact getDeployableArtifact(File uploadedFile) {
        DeployableArtifact result = new DeployableArtifact();
        String fileName = uploadedFile.getName();
        if (StringUtils.endsWithIgnoreCase(fileName, PackagingType.jar.name())) {
            //JAR
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
                        result.setPackaging(PackagingType.jar.name());
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
        } else if (StringUtils.endsWithIgnoreCase(fileName, PackagingType.pom.name())
                || StringUtils.endsWithIgnoreCase(fileName, ".xml")) {
            //POM
            try {
                FileInputStream in = new FileInputStream(uploadedFile);
                try {
                    readModel(in, result);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                result.setPackaging(PackagingType.pom.name());
            } catch (Exception e) {
                //Ignore exception - not every xml is a pom
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Failed to read maven model from '" + fileName + "'. Cause: " +
                            e.getMessage() + ".");
                }
                result.setPackaging("xml");
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
        Matcher matcher = MavenUtils.artifactMatcher(fileName);
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

    private static void readModel(InputStream is, DeployableArtifact da)
            throws IOException, XmlPullParserException {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new InputStreamReader(is, "utf-8"));
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
        da.setModel(model);
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

    public void deleteFullRepo(LocalRepoDescriptor repo, StatusHolder status) {
        status.setStatus("Fully removing repository '" + repo + "'.");
        LocalRepo localRepo = localOrCachedRepositoryByKey(repo.getKey());
        localRepo.delete();
        status.setStatus("Repository '" + localRepo.getKey() + "' fully deleted.");
    }

    public List<VirtualRepoDescriptor> getVirtualRepoDescriptors() {
        ArrayList<VirtualRepoDescriptor> list = new ArrayList<VirtualRepoDescriptor>();
        list.add(globalVirtualRepo.getDescriptor());
        list.addAll(centralConfig.getDescriptor().getVirtualRepositoriesMap().values());
        return list;
    }

    public List<RealRepo> getLocalAndRemoteRepositories() {
        return globalVirtualRepo.getLocalAndRemoteRepositories();
    }

    /**
     * This method will delete and import all the local and cached repositories listed in the (newly
     * loaded) config file. This action is resource intensive and is done in multiple transactions
     * to avoid out of memory exceptions.
     */
    public void importFrom(ImportSettings settings, StatusHolder status) {
        //TODO: [by yl]  need to use a service sync fw
        stopWorkingCopyCommitter();
        //Pause the scheduler
        indexerManager.unschedule();
        //Remove anything under the wc folder
        File workingCopyDir = ArtifactoryHome.getWorkingCopyDir();
        try {
            FileUtils.deleteDirectory(workingCopyDir);
            FileUtils.forceMkdir(workingCopyDir);
        } catch (IOException e) {
            LOGGER.error("Failed to recreate working copy dir: " + e.getMessage());
        }
        try {
            InternalRepositoryService transactionalMe = getTransactionalMe();
            //A new config file was loaded, refresh and init
            transactionalMe.rebuildRepositories();
            File repoRootPath = new File(settings.getBaseDir(), JcrPath.get().getRepoJcrRootPath());
            //Keep the current list of repositories for deletion after or during import
            List<LocalRepoDescriptor> oldRepoList = getLocalAndCachedRepoDescriptors();
            //Import all local repositories
            List<LocalRepoDescriptor> newRepoList = settings.getReposToImport();
            if (newRepoList.isEmpty()) {
                newRepoList = getLocalAndCachedRepoDescriptors();
            }
            ImportSettings repositoriesImportSettings = new ImportSettings(repoRootPath, settings);
            importAll(newRepoList, oldRepoList, repositoriesImportSettings, status);
        } finally {
            //Reschedule the indexer
            indexerManager.init();
            //Trigger the wc import thread to run now
            restartWorkingCopyCommitter();
        }
    }

    private void importAll(List<LocalRepoDescriptor> newRepoList,
            List<LocalRepoDescriptor> oldRepoList, ImportSettings settings, StatusHolder status) {
        for (LocalRepoDescriptor newLocalRepo : newRepoList) {
            File repoPath = new File(settings.getBaseDir(), newLocalRepo.getKey());
            try {
                if (repoPath.exists() && repoPath.isDirectory() && repoPath.list().length > 0) {
                    if (oldRepoList.contains(newLocalRepo)) {
                        deleteFullRepo(newLocalRepo, status);
                    }
                    ImportSettings repoSettings =
                            new ImportSettings(repoPath, settings);
                    importRepo(
                            newLocalRepo.getKey(), repoSettings, status);
                } else {
                    status.setStatus("The directory " + repoPath +
                            " does not exists or is empty. No import done on " + newLocalRepo);
                }
            } catch (Exception e) {
                status.setStatus(
                        "Could not import repository " + newLocalRepo + " from " + repoPath, e);
            }
        }
    }

    public StatusHolder assertValidPath(RealRepo repo, String path) {
        StatusHolder statusHolder = new StatusHolder();
        statusHolder.setLogging(false);
        if (repo.isBlackedOut()) {
            statusHolder.setError("The repository '" + repo.getKey() +
                    "' is blacked out and cannot accept artifact '" + path +
                    "'.", HttpStatus.SC_FORBIDDEN);
        } else if (!repo.handles(path)) {
            statusHolder.setError(
                    "The repository '" + repo.getKey() + "' rejected the artifact '" + path +
                            "' due to its snapshot/release handling policy.",
                    HttpStatus.SC_FORBIDDEN);
        } else if (!repo.accepts(path)) {
            statusHolder.setError(
                    "The repository '" + repo.getKey() + "' rejected the artifact '" + path +
                            "' due to its include/exclude patterns settings.",
                    HttpStatus.SC_FORBIDDEN);

        }
        return statusHolder;
    }

    public StatusHolder assertValidDeployPath(LocalRepo repo, String path) {
        StatusHolder statusHolder = assertValidPath(repo, path);
        if (!statusHolder.isError()) {
            assertDelete(repo, path, statusHolder);

        }
        return statusHolder;
    }

    /**
     * Trigger the wc import thread to run now
     */
    public void restartWorkingCopyCommitter() {
        //Stop and reschedule the working copy committer task to run now
        synchronized (this) {
            stopWorkingCopyCommitter();
            workingCopyCommitter = new WorkingCopyCommitter(InternalContextHelper.get());
            //Reschedule at fixed delay (serial execution) from now + 30 secs (to let the original
            //tx commit), every 20 minutes.
            timer.schedule(workingCopyCommitter, new Date(System.currentTimeMillis() + 30000),
                    1200000);//1200000
        }
    }

    public synchronized void stopWorkingCopyCommitter() {
        //TODO: [by yl] Need to really stop it with a synchronizer, not just unschedule
        if (workingCopyCommitter != null) {
            workingCopyCommitter.cancel();
        }
    }

    @Transactional
    public <T extends RemoteRepoDescriptor> ResourceStreamHandle downloadAndSave(
            RemoteRepo<T> remoteRepo, RepoResource res) throws IOException {
        RepoResource targetResource = retrieveInfo(remoteRepo.getLocalCacheRepo(), res.getPath());
        return remoteRepo.downloadAndSave(res, targetResource);
    }

    @Transactional
    public RepoResource unexpireIfExists(LocalRepo<LocalCacheRepoDescriptor> localCacheRepo,
            String path) {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path, false);
        if (resource == null) {
            return new UnfoundRepoResource(localCacheRepo, path);
        }
        return resource;
    }

    @Transactional
    public ResourceStreamHandle unexpireAndRetrieveIfExists(
            LocalRepo<LocalCacheRepoDescriptor> localCacheRepo, String path)
            throws RepoAccessException, IOException {
        RepoResource resource = internalUnexpireIfExists(localCacheRepo, path, true);
        if (resource != null) {
            return localCacheRepo.getResourceStreamHandle(resource);
        }
        return null;
    }

    @Transactional
    public ResourceStreamHandle getResourceStreamHandle(RealRepo repo, RepoResource res)
            throws RepoAccessException, IOException {
        return repo.getResourceStreamHandle(res);
    }

    private RepoResource internalUnexpireIfExists(
            LocalRepo<LocalCacheRepoDescriptor> repo, String path, boolean retrieveInfo) {
        JcrFile file = repo.getLockedJcrFile(path);
        if (file != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(this +
                        ": falling back to using cache entry for resource info at '" +
                        path + "'.");
            }
            // TODO: Change this mechanism since the last updated is used for artifact popularity measurement
            //Reset the resource age so it is kept being cached
            file.setLastUpdated(System.currentTimeMillis());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unexpired '" + path + "' from local cache '" +
                        repo.getKey() + "'.");
            }
            if (retrieveInfo) {
                return createRepoResource(repo, path, file.getLockedInfo());
            } else {
                return repo.getInfo(path);
            }
        }
        return null;
    }

    @Transactional
    public RepoResource retrieveInfo(LocalRepo repo, String path) throws FileExpectedException {
        //Check that it not a folder, since earlier check in getInfo() is not called on repo
        //browsing from the UI (uses: RemoteRepoBase.getResourceStreamHandle())
        FileInfo file = jcr.getFileInfo(repo, path);
        return createRepoResource(repo, path, file);
    }

    private static RepoResource createRepoResource(LocalRepo repo, String path, FileInfo fileInfo) {
        if (fileInfo != null) {
            RepoResource localRes = new SimpleRepoResource(fileInfo);
            return localRes;
        }
        return new UnfoundRepoResource(repo, path);
    }

    private static InternalRepositoryService getTransactionalMe() {
        InternalRepositoryService transactionalMe =
                InternalContextHelper.get().beanForType(
                        InternalRepositoryService.class);
        return transactionalMe;
    }

    private StatusHolder assertDelete(LocalRepo repo, String path, StatusHolder statusHolder) {
        //Check that has delete rights to replace an exiting item
        if (repo.itemExists(path) && repo.shouldProtectPathDeletion(path)) {
            RepoPath repoPath = new RepoPath(repo.getKey(), path);
            if (!authService.canDelete(repoPath)) {
                AccessLogger.deleteDenied(repoPath);
                statusHolder.setError(
                        "Not enough permissions to overwrite artifact '" + repoPath +
                                "' (needs delete permission).", HttpStatus.SC_FORBIDDEN);
            }
        }
        return statusHolder;
    }
}
