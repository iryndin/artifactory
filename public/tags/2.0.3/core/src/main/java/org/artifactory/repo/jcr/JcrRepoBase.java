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
package org.artifactory.repo.jcr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.BaseSettings;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.ItemNotFoundException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.io.StringResourceStreamHandle;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyException;
import org.artifactory.io.checksum.policy.ChecksumPolicyIgnoreAndGenerate;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockEntry;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataService;
import org.artifactory.maven.MavenMetadataCalculator;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepoBase;
import org.artifactory.repo.interceptor.LocalRepoInterceptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.resource.FileResource;
import org.artifactory.resource.MetadataResource;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.worker.StatsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class JcrRepoBase<T extends LocalRepoDescriptor> extends RealRepoBase<T> implements LocalRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(JcrRepoBase.class);

    private final JcrFileCreator jcrFileCreator = new JcrFileCreator();
    private final JcrFolderCreator jcrFolderCreator = new JcrFolderCreator();
    private boolean anonAccessEnabled;
    private String tempFileRepoUrl;
    private String repoRootPath;
    private LockEntry rootLockEntry;
    private Map<RepoPath, LockEntry> locks;
    private Map<RepoPath, JcrFsItem> fsItemCache;
    private LocalRepoInterceptor localRepoInterceptor;
    private JcrRepoService jcrService;
    private ChecksumPolicy defaultChecksumPolicy = new ChecksumPolicyIgnoreAndGenerate();

    protected JcrRepoBase(InternalRepositoryService repositoryService) {
        super(repositoryService);
    }

    protected JcrRepoBase(InternalRepositoryService repositoryService, T descriptor) {
        super(repositoryService, descriptor);
    }

    @Override
    public void setDescriptor(T descriptor) {
        super.setDescriptor(descriptor);
        repoRootPath = JcrPath.get().getRepoJcrPath(getKey());
    }

    public String getUrl() {
        return tempFileRepoUrl;
    }

    public void init() {
        jcrService = InternalContextHelper.get().getJcrRepoService();
        anonAccessEnabled = getRepositoryService().isAnonAccessEnabled();
        //Purge and recreate the (temp) repo dir
        final String key = getKey();
        File repoTmpDir = new File(ArtifactoryHome.getDataDir(), "tmp/" + key);
        try {
            tempFileRepoUrl = repoTmpDir.toURI().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Temporary directory for repo " + key +
                    " has a Malformed URL.", e);
        }
        if (repoTmpDir.exists()) {
            try {
                FileUtils.deleteDirectory(repoTmpDir);
            } catch (IOException e) {
                //Ignore
            }
        }
        boolean result = repoTmpDir.mkdirs();
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create local repository directory: " + repoTmpDir.getAbsolutePath());
        }
        localRepoInterceptor = getRepositoryService().getLocalRepoInterceptor();
        initCaches();
        //Create the repo node if it doesn't exist
        JcrFolder rootJcrFolder = new JcrFolder(new RepoPath(getKey(), ""), this);
        rootLockEntry = getLockEntry(rootJcrFolder);
        fsItemCache.put(rootJcrFolder.getRepoPath(), rootJcrFolder);
        rootJcrFolder.mkdir();
    }

    @SuppressWarnings({"unchecked"})
    private void initCaches() {
        CacheService cacheService = InternalContextHelper.get().beanForType(CacheService.class);
        this.fsItemCache = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.fsItemCache);
        this.locks = cacheService.getRepositoryCache(getKey(), ArtifactoryCache.locks);
    }

    public String getRepoRootPath() {
        return repoRootPath;
    }

    public SnapshotVersionBehavior getSnapshotVersionBehavior() {
        return getDescriptor().getSnapshotVersionBehavior();
    }

    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public String getAbsolutePath(Node node) {
        try {
            return PathUtils.trimTrailingSlashes(node.getPath());
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to retrieve node's absolute path:" + node,
                    e);
        }
    }

    public JcrFolder getRootFolder() {
        JcrFsItem result = LockingHelper.getIfLockedByMe(rootLockEntry.getRepoPath());
        if (result != null) {
            return (JcrFolder) result;
        }
        LockingHelper.readLock(rootLockEntry);
        return (JcrFolder) rootLockEntry.getFsItem();
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getJcrFsItem(String relPath) {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getJcrFsItem(repoPath);
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getJcrFsItem(RepoPath repoPath) {
        return internalGetFsItem(new JcrFsItemLocator(repoPath, true));
    }

    /**
     * {@inheritDoc}
     */
    public JcrFsItem getJcrFsItem(Node node) {
        return internalGetFsItem(new JcrFsItemLocator(node, true));
    }

    public JcrFile getJcrFile(String relPath) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getJcrFile(repoPath);
    }

    public JcrFile getJcrFile(RepoPath repoPath) throws FileExpectedException {
        JcrFsItem item = getJcrFsItem(repoPath);
        if (item != null && !item.isFile()) {
            throw new FileExpectedException(repoPath);
        }
        return (JcrFile) item;
    }

    public JcrFolder getJcrFolder(String relPath) throws FolderExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getJcrFolder(repoPath);
    }

    public JcrFolder getJcrFolder(RepoPath repoPath) throws FolderExpectedException {
        JcrFsItem item = getJcrFsItem(repoPath);
        if (item != null && !item.isDirectory()) {
            throw new FolderExpectedException(repoPath);
        }
        return (JcrFolder) item;
    }

    public JcrFsItem getLockedJcrFsItem(String relPath) {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFsItem(repoPath);
    }

    public JcrFsItem getLockedJcrFsItem(RepoPath repoPath) {
        JcrFsItem fsItem = internalGetFsItem(new JcrFsItemLocator(repoPath, false));
        if (fsItem != null) {
            return internalGetLockedJcrFsItem(fsItem, getCreator(fsItem));
        }
        return null;
    }

    public JcrFsItem getLockedJcrFsItem(Node node) {
        JcrFsItem fsItem = internalGetFsItem(new JcrFsItemLocator(node, false));
        if (fsItem != null) {
            return internalGetLockedJcrFsItem(fsItem, getCreator(fsItem));
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private <T extends JcrFsItem<? extends ItemInfo>> FsItemCreator<T> getCreator(T item) {
        if (item.isDirectory()) {
            return (FsItemCreator<T>) jcrFolderCreator;
        } else {
            return (FsItemCreator<T>) jcrFileCreator;
        }
    }

    public JcrFile getLockedJcrFile(String relPath, boolean createIfMissing) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFile(repoPath, createIfMissing);
    }

    public JcrFile getLockedJcrFile(RepoPath repoPath, boolean createIfMissing) throws FileExpectedException {
        return internalGetLockedJcrFsItem(repoPath, createIfMissing, jcrFileCreator);
    }

    public JcrFolder getLockedJcrFolder(String relPath, boolean createIfMissing) throws FolderExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), relPath);
        return getLockedJcrFolder(repoPath, createIfMissing);
    }

    public JcrFolder getLockedJcrFolder(RepoPath repoPath, boolean createIfMissing) throws FolderExpectedException {
        return internalGetLockedJcrFsItem(repoPath, createIfMissing, jcrFolderCreator);
    }

    public void updateCache(JcrFsItem fsItem) {
        RepoPath repoPath = fsItem.getRepoPath();
        if (fsItem.isDeleted()) {
            fsItemCache.remove(repoPath);
        } else {
            if (fsItem.isMutable()) {
                log.error("Cannot add object " + fsItem + " into cache, it is mutable...");
            }
            fsItemCache.put(repoPath, fsItem);
        }
    }

    protected final void assertRepoPath(RepoPath repoPath) {
        if (!getKey().equals(repoPath.getRepoKey())) {
            throw new IllegalArgumentException(
                    "Trying to retrieve resource " + repoPath + " from local repo " + getKey());
        }
    }

    /**
     * Get from cache, or load from JCR. Read lock according to locator.
     *
     * @param locator
     * @return null if item does not exists, a JcrFsItem otherwise
     */
    private JcrFsItem internalGetFsItem(JcrFsItemLocator locator) {
        RepoPath repoPath = locator.getRepoPath();
        assertRepoPath(repoPath);
        // First check if we have already the write lock
        JcrFsItem item = LockingHelper.getIfLockedByMe(repoPath);
        if (item != null) {
            return item;
        }
        JcrFsItem fsItem = fsItemCache.get(repoPath);
        if (fsItem == null) {
            fsItem = locator.getFsItem();
            if (fsItem != null) {
                fsItemCache.put(repoPath, fsItem);
            }
        }
        locator.lock(fsItem);
        return fsItem;
    }

    private LockEntry getLockEntry(JcrFsItem fsItem) {
        RepoPath repoPath = fsItem.getRepoPath();
        LockEntry lockEntry = locks.get(repoPath);
        if (lockEntry == null) {
            lockEntry = locks.put(repoPath, new LockEntry(fsItem));
        }
        return lockEntry;
    }

    public RepoResource getInfo(String path) throws FileExpectedException {
        RepoPath repoPath = new RepoPath(getKey(), path);
        StatusHolder statusHolder = allowsDownload(repoPath);
        if (statusHolder.isError()) {
            return new UnfoundRepoResource(repoPath, statusHolder.getStatusMsg());
        }
        // TODO: Why there is a need for lock here?
        //If we are dealing with metadata will return the md container item
        JcrFsItem item = getLockedJcrFsItem(path);
        if (item == null) {
            return new UnfoundRepoResource(repoPath, "file not found");
        }
        if (item.isDeleted() || !item.exists()) {
            item.setDeleted(true);
            throw new ItemNotFoundException("File " + repoPath + " was deleted during download!");
        }
        RepoResource localRes;
        //When requesting a property/metadata return a special resouce class that contains the parent node
        //path and the metadata name.
        if (NamingUtils.isMetadata(path)) {
            String metadataName = NamingUtils.getMetadataName(path);
            boolean metadataExists = item.hasXmlMetdata(metadataName);
            if (metadataExists) {
                MetadataInfo metadataInfo = getMetadataService().getMetadataInfo(item, metadataName);
                if (MavenNaming.isSnapshotMavenMetadata(path)) {
                    // this is hack - for snapshot maven metadata use the last updated time of the folder
                    // the cache repo will use this value to determine if the resource is expired
                    metadataInfo.setLastModified(item.getInfo().getInernalXmlInfo().getLastUpdated());
                }
                localRes = new MetadataResource(metadataInfo);
            } else {
                return new UnfoundRepoResource(repoPath, "metadata not found");
            }
        } else {
            if (item.isDirectory()) {
                throw new FileExpectedException(repoPath);
            }
            localRes = new FileResource((FileInfo) item.getInfo());
        }
        return localRes;
    }

    public StatusHolder allowsDownload(RepoPath repoPath) {
        StatusHolder status = assertValidPath(repoPath);
        if (status.isError()) {
            return status;
        }
        if (anonAccessEnabled) {
            return status;
        }
        AuthorizationService authService = getAuthorizationService();
        boolean canRead = authService.canRead(repoPath);
        if (!canRead) {
            status.setError("Download request for repo:path '" + repoPath + "' is forbidden for user '" +
                    authService.currentUsername() + "'.", log);
            AccessLogger.downloadDenied(repoPath);
        }
        return status;
    }

    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
        log.debug("Transferring {} directly to user from {}.", res, this);
        String relPath = res.getRepoPath().getPath();
        //If we are dealing with metadata will return the md container item
        JcrFsItem item = getJcrFsItem(relPath);
        //If resource does not exist throw an IOException
        if (item == null) {
            throw new IOException("Could not get resource stream. Path not found: " + res + ".");
        }
        if (item.isDeleted() || !item.exists()) {
            item.setDeleted(true);
            throw new ItemNotFoundException("Could not get resource stream. Item " + item + " was deleted!");
        }
        ResourceStreamHandle handle;
        if (res.isMetadata()) {
            String medtadataName = res.getInfo().getName();
            String xmlMetdata = item.getXmlMetdata(medtadataName);
            if (xmlMetdata == null) {
                throw new IOException("Could not get resource stream. Stream not found: " + res + ".");
            } else {
                handle = new StringResourceStreamHandle(xmlMetdata);
            }
        } else if (item.isFile()) {
            JcrFile jcrFile = (JcrFile) item;
            final InputStream is = jcrFile.getStream();
            if (is == null) {
                throw new IOException("Could not get resource stream. Stream not found: " + item + ".");
            }
            //Update the stats
            getRepositoryService().publish(new StatsMessage(jcrFile));
            handle = new SimpleResourceStreamHandle(is);
        } else {
            throw new IOException("Could not get resource stream from a folder " + res + ".");
        }
        return handle;
    }

    public String getChecksum(String checksumFilePath) throws IOException {
        //For checksums return the property directly
        String resourcePath = checksumFilePath.substring(0, checksumFilePath.lastIndexOf("."));
        RepoResource resource = getInfo(resourcePath);
        if (resource == null) {
            throw new IOException("Could not get resource stream. Path not found: " + resourcePath + ".");
        }
        String extension = '.' + PathUtils.getExtension(checksumFilePath);
        ChecksumType checksumType = ChecksumType.forExtension(extension);
        if (checksumType == null) {
            throw new IllegalArgumentException("Checksum type not found for path " + checksumFilePath);
        }
        return getChecksumPolicy().getChecksum(checksumType, resource.getInfo().getChecksums());
    }

    public void undeploy(RepoPath repoPath) {
        //TODO: [by yl] Replace with real undeploy
        /**
         * Undeploy rules:
         * jar - remove pom, all jar classifiers and update metadata
         * pom - if packaging is jar remove pom and jar, else remove pom and update metadata
         * metadata - remove pom, jar and classifiers
         * version dir - update versions metadata in containing dir
         * plugin pom - update plugins in maven-metadata.xml in the groupId directory above
         */
        if (!PathUtils.hasText(repoPath.getPath())) {
            // Delete the all repo
            delete();
        } else {
            JcrFsItem fsItem = getLockedJcrFsItem(repoPath);
            if (fsItem != null && !fsItem.isDeleted()) {
                fsItem.delete();
                //Move the deleted item to the trash
                jcrService.trash(Collections.singletonList(fsItem));
            }
        }
    }

    public Model getModel(ArtifactResource res) {
        String pom = getPomContent(res);
        if (pom == null) {
            return null;
        }
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            Model model = reader.read(new StringReader(pom));
            return model;
        } catch (Exception e) {
            log.warn("Failed to read pom from '" + pom + "'.", e);
            return null;
        }
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        TaskService taskService = InternalContextHelper.get().getTaskService();
        //Check if we need to break/pause
        boolean stop = taskService.blockIfPausedAndShouldBreak();
        if (stop) {
            status.setError("Export was stopped on " + this, log);
            return;
        }
        LockingHelper.readLock(rootLockEntry);
        File dir = settings.getBaseDir();
        status.setStatus("Exporting repository '" + getKey() + "' to '" + dir.getAbsolutePath() + "'.", log);
        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create export directory '" + dir.getAbsolutePath() + "'.", e);
        }
        JcrFolder folder = getRootFolder();
        folder.exportTo(settings, status);
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        JcrFolder rootFolder = getLockedRootFolder();
        File baseDir = settings.getBaseDir();
        status.setStatus("Importing repository '" + getKey() + "' from " + baseDir + ".", log);
        LinkedList<RepoPath> foldersToScan = new LinkedList<RepoPath>();
        RepoPath currentFolder;
        foldersToScan.add(rootFolder.getRepoPath());
        TaskService taskService = InternalContextHelper.get().getTaskService();
        while ((currentFolder = foldersToScan.poll()) != null) {
            if (taskService.blockIfPausedAndShouldBreak()) {
                status.setError("Import of " + getKey() + " was stopped", log);
                return;
            }
            //Import the folder (shallow) in a new tx
            RepoPath folderRepoPath = jcrService.importFolder(this, currentFolder, settings, status);
            JcrFolder folder = getJcrFolder(folderRepoPath);
            folder.importChildren(settings, status, foldersToScan);
            LockingHelper.removeLockEntry(folderRepoPath);
        }
        status.setStatus("Repository '" + getKey() + "' imported from " + baseDir + ".", log);
    }

    private JcrFolder getLockedRootFolder() {
        JcrFolder rootFolder = new JcrFolder((JcrFolder) rootLockEntry.getFsItem(), this);
        LockingHelper.writeLock(new LockEntry(rootLockEntry, rootFolder));
        return rootFolder;
    }

    protected long getTimeToWait(BaseSettings settings) {
        long timeToWait = ConstantsValue.failFastLockTimeoutSecs.getLong();
        if (!settings.isFailFast()) {
            timeToWait = ConstantsValue.lockTimeoutSecs.getLong();
        }
        return timeToWait;
    }

    /**
     * Create the resource in the local repository
     *
     * @param res the destination resource definition
     * @param in  the stream to save at the location
     */
    public RepoResource saveResource(RepoResource res, final InputStream in) throws IOException {
        log.debug("Saving resource '{}' into repository '{}'.", res, this);
        RepoPath repoPath = new RepoPath(getKey(), res.getRepoPath().getPath());
        try {
            if (res.isMetadata()) {
                //If we are dealing with metadata return the containing fsitem
                RepoPath metadataContainerRepoPath = RepoPath.getMetadataContainerRepoPath(repoPath);
                JcrFsItem metadataAware = getLockedJcrFsItem(metadataContainerRepoPath);
                if (metadataAware == null) {
                    //If we cannot find the container, assume it's a folder and create it on demand
                    metadataAware = getLockedJcrFolder(metadataContainerRepoPath, true);
                    metadataAware.mkdirs();
                }
                MetadataService metadataService = getMetadataService();
                String metadataName = res.getInfo().getName();
                metadataService.setXmlMetadata(metadataAware, metadataName, in);
            } else {
                //Create the parent folder
                JcrFile jcrFile = getLockedJcrFile(repoPath, true);
                // set the file extension checksums (only needed if the file is currently being download)
                jcrFile.getInfo().setChecksums(((FileResource) res).getInfo().getChecksums());
                JcrFolder jcrFolder = getLockedJcrFolder(jcrFile.getParentRepoPath(), true);
                jcrFolder.mkdirs();
                //Deploy
                long lastModified = res.getInfo().getLastModified();
                BufferedInputStream bis = new BufferedInputStream(in);
                jcrFile.fillData(lastModified, bis);
                AccessLogger.deployed(jcrFile.getRepoPath());
                //If the resource has no size specified, update the size
                //(this can happen if we established the resource based on a HEAD request that failed to
                //return the content-length).
                res = new FileResource(jcrFile.getInfo());
                //Cleanup old snapshots etc.
                localRepoInterceptor.afterResourceSave(res, this);
            }
            return res;
        } catch (Exception e) {
            //Unwrap any IOException and throw it
            Throwable ioCause = ExceptionUtils.getCauseOfTypes(e, IOException.class);
            if (ioCause != null) {
                log.warn("IO error while trying to save resource {}'': {}",
                        res.getRepoPath(), ioCause.getMessage());
                throw (IOException) ioCause;
            }
            // throw back ChecksumPolicyException if it is the cause
            Throwable checksumCause = ExceptionUtils.getCauseOfTypes(e, ChecksumPolicyException.class);
            if (checksumCause != null) {
                throw (ChecksumPolicyException) checksumCause;
            }
            throw new RuntimeException("Failed to save resource '" + res.getRepoPath() + "'.", e);
        }
    }

    public String getPomContent(ArtifactResource res) {
        return getPomContent(res.getRepoPath().getPath());
    }

    public String getPomContent(ItemInfo itemInfo) {
        if (itemInfo.isFolder()) {
            // TODO: Try to extract a POM from the folder
            throw new IllegalArgumentException(
                    "Item " + itemInfo + " is not a maven artifact with a POM");
        }
        String relPath = itemInfo.getRelPath();
        return getPomContent(relPath);
    }

    public boolean shouldProtectPathDeletion(String path) {
        //Snapshots should generally be overridable, except for unique ones
        return (!MavenNaming.isSnapshot(path) || !MavenNaming.isNonUniqueSnapshot(path)) &&
                !(NamingUtils.isChecksum(path) || NamingUtils.isMetadata(path));
    }

    private String getPomContent(String relPath) {
        String relativePath;
        if (!relPath.endsWith(".pom")) {
            File file = new File(relPath);
            String fileName = file.getName();
            int dotIdx = fileName.lastIndexOf(".");
            if (dotIdx < 0) {
                return "No content found.";
            }
            String pomFileName = fileName.substring(0, dotIdx) + ".pom";
            relativePath = new File(file.getParent(), pomFileName).getPath();
        } else {
            relativePath = relPath;
        }
        JcrFile jcrFile;
        try {
            jcrFile = getLockedJcrFile(relativePath, true);
        } catch (FileExpectedException e) {
            throw new RuntimeException("Cannot read a POM from a folder name " + relPath, e);
        }
        if (jcrFile != null) {
            InputStream is = null;
            try {
                is = jcrFile.getStream();
                return IOUtils.toString(is, "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read pom from '" + relativePath + "'.", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return null;
    }

    public boolean itemExists(String relPath) {
        if (relPath.length() > 0) {
            JcrService jcr = InternalContextHelper.get().getJcrService();
            return jcr.itemNodeExists(repoRootPath + "/" + relPath);
        } else {
            //The repo itself
            return true;
        }
    }

    public List<String> getChildrenNames(String relPath) {
        return jcrService.getChildrenNames(repoRootPath + "/" + relPath);
    }

    public boolean isCache() {
        return false;
    }

    public void delete() {
        JcrFolder rootFolder = getLockedRootFolder();
        List<JcrFsItem> children = jcrService.getChildren(rootFolder, true);
        for (JcrFsItem child : children) {
            child.delete();
        }
        //Move the deleted item to the trash
        jcrService.trash(children);
    }

    public ChecksumPolicy getChecksumPolicy() {
        return defaultChecksumPolicy;
    }

    public void onDelete(JcrFsItem fsItem) {
        AccessLogger.deleted(fsItem.getRepoPath());
        //Calculate the for a folder metadata
        MavenMetadataCalculator mavenMetadataCalculator =
                jcrService.getManagedSession().getOrCreateResource(MavenMetadataCalculator.class);
        mavenMetadataCalculator.addDeletedItem(fsItem);
    }

    @SuppressWarnings({"unchecked"})
    private <T extends JcrFsItem<? extends ItemInfo>> T internalGetLockedJcrFsItem(
            RepoPath repoPath, boolean createIfMissing, FsItemCreator<T> creator) throws FileExpectedException {
        // First check if we have already the write lock
        T item = (T) LockingHelper.getIfLockedByMe(repoPath);
        if (item != null) {
            creator.checkItemType(item);
            return item;
        }
        // get or create the JcrFile
        item = (T) internalGetFsItem(new JcrFsItemLocator(repoPath, false));
        if (item != null) {
            creator.checkItemType(item);
        }
        if (item == null) {
            if (createIfMissing) {
                // Create the empty JcrFile from repoPath
                item = creator.newFsItem(repoPath, this);
            } else {
                return null;
            }
        }
        return internalGetLockedJcrFsItem(item, creator);
    }

    @SuppressWarnings({"unchecked"})
    private <T extends JcrFsItem<? extends ItemInfo>> T internalGetLockedJcrFsItem(T item, FsItemCreator<T> creator) {
        // First check if we have already the write lock
        T result = (T) LockingHelper.getIfLockedByMe(item.getRepoPath());
        if (result != null) {
            return result;
        }
        T original = item;
        if (!original.isMutable()) {
            // Do a copy constructor to start modifying it
            item = creator.newFsItem(original, this);
        }
        // Create a lock entry with new item
        LockEntry lockEntry = new LockEntry(getLockEntry(original), item);
        // acquire the write lock
        LockingHelper.writeLock(lockEntry);
        return item;
    }

    private class JcrFsItemLocator {
        private final RepoPath repoPath;
        private final Node node;
        private final boolean acquireReadLock;

        JcrFsItemLocator(RepoPath repoPath, boolean acquireReadLock) {
            //If we are dealing with metadata return the containing fsitem
            this.repoPath = RepoPath.getMetadataContainerRepoPath(repoPath);
            this.node = null;
            this.acquireReadLock = acquireReadLock;
        }

        JcrFsItemLocator(Node node, boolean acquireReadLock) {
            this.repoPath = JcrPath.get().getRepoPath(getAbsolutePath(node));
            this.node = node;
            this.acquireReadLock = acquireReadLock;
        }

        public RepoPath getRepoPath() {
            return repoPath;
        }

        public JcrFsItem getFsItem() {
            if (node != null) {
                return jcrService.getFsItem(node, JcrRepoBase.this);
            }
            if (repoPath != null) {
                return jcrService.getFsItem(repoPath, JcrRepoBase.this);
            }
            throw new IllegalArgumentException("Need either repoPath or node");
        }

        public void lock(JcrFsItem fsItem) {
            if (acquireReadLock && fsItem != null) {
                LockEntry lockEntry = getLockEntry(fsItem);
                if (!fsItem.isMutable()) {
                    lockEntry.setFsItem(fsItem);
                }
                LockingHelper.readLock(lockEntry);
            }
        }
    }

    private static interface FsItemCreator<T extends JcrFsItem<? extends ItemInfo>> {
        public boolean checkItemType(JcrFsItem item);

        public T newFsItem(RepoPath repoPath, LocalRepo repo);

        public T newFsItem(JcrFsItem copy, LocalRepo repo);

        public T newFsItem(Node node, LocalRepo repo);
    }

    private static class JcrFileCreator implements FsItemCreator<JcrFile> {
        public boolean checkItemType(JcrFsItem item) {
            if (item.isDirectory()) {
                throw new FileExpectedException(item.getRepoPath());
            }
            return true;
        }

        public JcrFile newFsItem(RepoPath repoPath, LocalRepo repo) {
            return new JcrFile(repoPath, repo);
        }

        public JcrFile newFsItem(JcrFsItem copy, LocalRepo repo) {
            return new JcrFile((JcrFile) copy, repo);
        }

        public JcrFile newFsItem(Node node, LocalRepo repo) {
            return new JcrFile(node, repo);
        }
    }

    private static class JcrFolderCreator implements FsItemCreator<JcrFolder> {
        public boolean checkItemType(JcrFsItem item) {
            if (!item.isDirectory()) {
                throw new FolderExpectedException(item.getRepoPath());
            }
            return true;
        }

        public JcrFolder newFsItem(RepoPath repoPath, LocalRepo repo) {
            return new JcrFolder(repoPath, repo);
        }

        public JcrFolder newFsItem(JcrFsItem copy, LocalRepo repo) {
            return new JcrFolder((JcrFolder) copy, repo);
        }

        public JcrFolder newFsItem(Node node, LocalRepo repo) {
            return new JcrFolder(node, repo);
        }
    }
}
