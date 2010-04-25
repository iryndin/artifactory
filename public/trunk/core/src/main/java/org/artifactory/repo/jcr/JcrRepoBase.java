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

package org.artifactory.repo.jcr;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.ArchiveFileContent;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.RepoRejectionException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepoBase;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.util.ZipUtils;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class JcrRepoBase<T extends LocalRepoDescriptor> extends RealRepoBase<T>
        implements LocalRepo<T>, StoringRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(JcrRepoBase.class);

    private boolean anonAccessEnabled;
    private JcrRepoService jcrRepoService;

    private final StoringRepo<T> storageMixin;

    protected JcrRepoBase(InternalRepositoryService repositoryService, StoringRepo<T> oldStoringRepo) {
        super(repositoryService);
        storageMixin = new StoringRepoMixin<T>(this, oldStoringRepo);
    }

    protected JcrRepoBase(InternalRepositoryService repositoryService, T descriptor,
            StoringRepo<T> oldStoringRepo) {
        super(repositoryService, descriptor);
        storageMixin = new StoringRepoMixin<T>(this, oldStoringRepo);
    }

    public void init() {
        jcrRepoService = InternalContextHelper.get().getJcrRepoService();
        anonAccessEnabled = getRepositoryService().isAnonAccessEnabled();
        storageMixin.init();
    }

    public StoringRepo<T> getStorageMixin() {
        return storageMixin;
    }

    public boolean isCache() {
        return false;
    }

    public SnapshotVersionBehavior getSnapshotVersionBehavior() {
        return getDescriptor().getSnapshotVersionBehavior();
    }

    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public StatusHolder checkDownloadIsAllowed(RepoPath repoPath) {
        StatusHolder status = assertValidPath(repoPath);
        if (status.isError()) {
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

    public void exportTo(ExportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        TaskService taskService = InternalContextHelper.get().getTaskService();
        //Check if we need to break/pause
        boolean stop = taskService.pauseOrBreak();
        if (stop) {
            status.setError("Export was stopped on " + this, log);
            return;
        }
        // Acquire read lock early
        JcrFolder folder = getRootFolder();
        File dir = settings.getBaseDir();
        status.setStatus("Exporting repository '" + getKey() + "' to '" + dir.getAbsolutePath() + "'.", log);
        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create export directory '" + dir.getAbsolutePath() + "'.", e);
        }
        folder.exportTo(settings);
    }

    public void importFrom(ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        File baseDir = settings.getBaseDir();
        if (baseDir == null || !baseDir.isDirectory()) {
            status.setError("Error Import: Cannot import null, non existent folder or non directory file '"
                    + baseDir + "'.", log);
            return;
        }

        status.setStatus("Importing repository '" + getKey() + "' from " + baseDir + ".", log);

        JcrFolder rootFolder = getLockedRootFolder();
        RepoPath rootRepoPath = rootFolder.getRepoPath();
        if (!isCache()) {
            // mark the root node for metadata recalculation in case the process will fail
            getRepositoryService().markBaseForMavenMetadataRecalculation(rootRepoPath);
        }
        LinkedList<RepoPath> foldersToScan = new LinkedList<RepoPath>();
        foldersToScan.add(rootRepoPath);
        TaskService taskService = InternalContextHelper.get().getTaskService();
        RepoPath currentFolder;
        while ((currentFolder = foldersToScan.poll()) != null) {
            if (taskService.pauseOrBreak()) {
                status.setError("Import of " + getKey() + " was stopped", log);
                return;
            }
            //Import the folder (shallow) in a new tx
            //if fail simply log and continue the loop
            RepoPath folderRepoPath = null;
            try {
                folderRepoPath = jcrRepoService.importFolder(this, currentFolder, settings);
                JcrFolder folder = getJcrFolder(folderRepoPath);
                // now import the folder's children
                folder.importChildren(settings, foldersToScan);
            } catch (RepositoryRuntimeException e) {
                if (ExceptionUtils.getRootCause(e) instanceof PathNotFoundException) {
                    status.setError("Could not get or create folder: " + currentFolder, e, log);
                } else {
                    throw e;
                }
            } finally {
                LockingHelper.removeLockEntry(folderRepoPath);
            }
        }
        if (!isCache()) {
            try {
                // calculate and set maven-metadata
                getRepositoryService().calculateMavenMetadata(this.getRootFolder().getRepoPath());
            } catch (Exception e) {
                // remove the maven metadata recalculation mark to not try it over and over again
                getRepositoryService().removeMarkForMavenMetadataRecalculation(rootRepoPath);
                status.setError("Failed to calculate maven metadata on imported repo: " + getKey(), e, log);
            }
        }
        status.setStatus("Repository '" + getKey() + "' imported from " + baseDir + ".", log);
    }

    public String getTextFileContent(RepoPath repoPath) {
        String relativePath = repoPath.getPath();
        JcrFile jcrFile = getLocalJcrFile(relativePath);
        if (jcrFile != null) {
            InputStream is = null;
            try {
                is = jcrFile.getStream();
                return IOUtils.toString(is, "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read text file content from '" + relativePath + "'.", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return "";
    }

    public ArchiveFileContent getArchiveFileContent(RepoPath archivePath, String archiveEntryPath) throws IOException {
        String content = null;
        String sourceJarPath = null;
        List<String> searchList = null;
        String failureReason = null;
        ZipInputStream jis = null;
        String sourceEntryPath = null;
        try {
            if (archiveEntryPath.endsWith(".class")) {
                sourceEntryPath = NamingUtils.javaSourceNameFromClassName(archiveEntryPath);
                // locate the sources jar and find the source file in it
                String sourceJarName = PathUtils.stripExtension(archivePath.getName()) + "-sources." +
                        PathUtils.getExtension(archivePath.getName());
                String sourcesJarPath = archivePath.getParent().getPath() + "/" + sourceJarName;
                // search in the sources file first
                searchList = Lists.newArrayList(sourcesJarPath, archivePath.getPath());
            } else if (isTextFile(archiveEntryPath)) {
                // read directly from this archive
                searchList = Lists.newArrayList(archivePath.getPath());
                sourceEntryPath = archiveEntryPath;
            } else {
                failureReason = "View source for " + archiveEntryPath + " is not supported";
            }

            if (searchList != null) {
                boolean found = false;
                for (int i = 0; i < searchList.size() && !found; i++) {
                    String sourcesJarPath = searchList.get(i);
                    log.debug("Looking for {} source in {}", sourceEntryPath, sourceJarPath);
                    JcrFile jcrFile = getLocalJcrFile(sourcesJarPath);
                    if (jcrFile == null) {
                        failureReason = "Sources jar not found";
                    } else if (!getAuthorizationService().canRead(new RepoPath(getKey(), sourcesJarPath))) {
                        failureReason = "No read permissions for the sources jar";
                    } else {
                        List<String> alternativeExtensions = null;
                        if ("java".equalsIgnoreCase(PathUtils.getExtension(sourceEntryPath))) {
                            alternativeExtensions = Lists.newArrayList("groovy");
                        }

                        jis = new ZipInputStream(jcrFile.getStream());
                        ZipEntry zipEntry = ZipUtils.locateEntry(jis, sourceEntryPath, alternativeExtensions);
                        if (zipEntry == null) {
                            failureReason = "Source file not found";
                        } else {
                            found = true;   // source entry was found in the jar
                            int maxAllowedSize = 1024 * 1024;
                            if (zipEntry.getSize() > maxAllowedSize) {
                                failureReason = String.format(
                                        "Source file is too big to display: File size: %s, Max size: %s",
                                        zipEntry.getSize(), maxAllowedSize);

                            } else {
                                // read the current entry (the source entry path)
                                content = IOUtils.toString(jis, "UTF-8");
                                sourceEntryPath = zipEntry.getName();
                                sourceJarPath = sourcesJarPath;
                            }
                        }
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(jis);
        }

        if (content != null) {
            return new ArchiveFileContent(content, new RepoPath(getKey(), sourceJarPath), sourceEntryPath);
        } else {
            return ArchiveFileContent.contentNotFound(failureReason);
        }
    }

    private boolean isTextFile(String fileName) {
        return NamingUtils.isViewable(fileName);
    }

    //STORING REPO MIXIN

    @Override
    public void setDescriptor(T descriptor) {
        super.setDescriptor(descriptor);
        storageMixin.setDescriptor(descriptor);
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
        undeploy(repoPath, true);
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
        final String path = context.getResourcePath();
        RepoPath repoPath = new RepoPath(getKey(), path);
        StatusHolder statusHolder = checkDownloadIsAllowed(repoPath);
        if (statusHolder.isError()) {
            return new UnfoundRepoResource(repoPath, statusHolder.getStatusMsg());
        }
        return storageMixin.getInfo(context);
    }

    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException, RepositoryException,
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
