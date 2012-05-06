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

package org.artifactory.repo.jcr;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.util.CloseableThreadLocal;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.YumAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.repo.ArchiveFileContent;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.common.StatusHolder;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.fs.RepoResource;
import org.artifactory.io.NullResourceStreamHandle;
import org.artifactory.io.SimpleResourceStreamHandle;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepoBase;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.SaveResourceContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.request.InternalRequestContext;
import org.artifactory.request.RequestTraceLogger;
import org.artifactory.resource.ResourceStreamHandle;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.schedule.TaskService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.ExceptionUtils;
import org.artifactory.util.PathUtils;
import org.artifactory.util.ZipUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.jcr.ItemNotFoundException;
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

    @Override
    public void init() {
        jcrRepoService = InternalContextHelper.get().getJcrRepoService();
        anonAccessEnabled = getRepositoryService().isAnonAccessEnabled();
        storageMixin.init();
    }

    @Override
    public StoringRepo<T> getStorageMixin() {
        return storageMixin;
    }

    @Override
    public boolean isCache() {
        return false;
    }

    @Override
    public SnapshotVersionBehavior getMavenSnapshotVersionBehavior() {
        return getDescriptor().getSnapshotVersionBehavior();
    }

    @Override
    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    @Override
    public StatusHolder checkDownloadIsAllowed(RepoPath repoPath) {
        BasicStatusHolder status = assertValidPath(repoPath.getPath(), true);
        if (status.isError()) {
            return status;
        }
        AuthorizationService authService = getAuthorizationService();
        boolean canRead = authService.canRead(repoPath);
        if (!canRead) {
            status.setError("Download request for repo:path '" + repoPath + "' is forbidden for user '" +
                    authService.currentUsername() + "'.", HttpStatus.SC_FORBIDDEN, log);
            AccessLogger.downloadDenied(repoPath);
        }
        return status;
    }

    @Override
    public void exportTo(ExportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        TaskService taskService = InternalContextHelper.get().getTaskService();
        //Check if we need to break/pause
        boolean stop = taskService.pauseOrBreak();
        if (stop) {
            status.setError("Export was stopped on " + this, log);
            return;
        }
        // Acquire read lock early
        VfsFolder folder = getRootFolder();
        File dir = settings.getBaseDir();
        status.setStatus("Exporting repository '" + getKey() + "' to '" + dir.getAbsolutePath() + "'.", log);
        try {
            FileUtils.forceMkdir(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create export directory '" + dir.getAbsolutePath() + "'.", e);
        }
        folder.exportTo(settings);
    }

    @Override
    public void importFrom(ImportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
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
                CloseableThreadLocal.closeAllThreadLocal();
            }
        }
        if (!isCache()) {
            try {
                // calculate and set maven-metadata
                getRepositoryService().calculateMavenMetadata(this.getRootFolder().getRepoPath());
            } catch (Exception e) {
                // remove the maven metadata recalculation mark to not try it over and over again
                try {
                    getRepositoryService().removeMarkForMavenMetadataRecalculation(rootRepoPath);
                } catch (ItemNotFoundException infe) {
                    //Action is async. Item might have been removed
                    status.setDebug("Failed to remove maven metadata calculation mark: " + e.getMessage(), log);
                }
                status.setError("Failed to calculate maven metadata on imported repo: " + getKey(), e, log);
            }
            T descriptor = getDescriptor();
            if (descriptor.isCalculateYumMetadata()) {
                AddonsManager addonsManager = StorageContextHelper.get().beanForType(AddonsManager.class);
                addonsManager.addonByType(YumAddon.class).requestAsyncRepositoryYumMetadataCalculation(descriptor);
            }
        }
        status.setStatus("Repository '" + getKey() + "' imported from " + baseDir + ".", log);
    }

    @Override
    public String getTextFileContent(RepoPath repoPath) {
        String relativePath = repoPath.getPath();
        JcrFile jcrFile = getLocalJcrFile(relativePath);
        if (jcrFile != null) {
            InputStream is = null;
            try {
                is = jcrFile.getStream();
                return IOUtils.toString(is, "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Could not read text file content from '" + relativePath + "'.", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        return "";
    }

    /**
     * Note: Handle is to be closed by clients to avoid stream leaks!
     *
     * @param repoPath
     * @return
     */
    @Override
    public ResourceStreamHandle getFileContent(RepoPath repoPath) {
        String relativePath = repoPath.getPath();
        JcrFile jcrFile = getLocalJcrFile(relativePath);
        if (jcrFile != null) {
            InputStream is = jcrFile.getStream();
            return new SimpleResourceStreamHandle(is, jcrFile.getSize());
        } else {
            return new NullResourceStreamHandle();
        }
    }

    @Override
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
                        failureReason = "Source jar not found.";
                    } else if (!getAuthorizationService()
                            .canRead(InternalRepoPathFactory.create(getKey(), sourcesJarPath))) {
                        failureReason = "No read permissions for the source jar.";
                    } else {
                        List<String> alternativeExtensions = null;
                        if ("java".equalsIgnoreCase(PathUtils.getExtension(sourceEntryPath))) {
                            alternativeExtensions = Lists.newArrayList("groovy", "fx");
                        }

                        jis = new ZipInputStream(jcrFile.getStream());
                        ZipEntry zipEntry = ZipUtils.locateEntry(jis, sourceEntryPath, alternativeExtensions);
                        if (zipEntry == null) {
                            failureReason = "Source file not found.";
                        } else {
                            found = true;   // source entry was found in the jar
                            int maxAllowedSize = 1024 * 1024;
                            if (zipEntry.getSize() > maxAllowedSize) {
                                failureReason = String.format(
                                        "Source file is too big to render: file size: %s, max size: %s.",
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
            return new ArchiveFileContent(content, InternalRepoPathFactory.create(getKey(), sourceJarPath),
                    sourceEntryPath);
        } else {
            return ArchiveFileContent.contentNotFound(failureReason);
        }
    }

    private boolean isTextFile(String fileName) {
        return NamingUtils.isViewable(fileName) || isLicenseFile(fileName);
    }

    private boolean isLicenseFile(String fileName) {
        String licenseFileNames = ConstantValues.archiveLicenseFileNames.getString();
        Set<String> possibleLicenseFileNames = Sets.newHashSet(
                Iterables.transform(Sets.newHashSet(StringUtils.split(licenseFileNames, ",")),
                        new Function<String, String>() {
                            @Override
                            public String apply(@Nullable String input) {
                                return StringUtils.isBlank(input) ? input : StringUtils.trim(input);
                            }
                        }
                )
        );
        return possibleLicenseFileNames.contains(PathUtils.getFileName(fileName));
    }

    //STORING REPO MIXIN

    @Override
    public void setDescriptor(T descriptor) {
        super.setDescriptor(descriptor);
        storageMixin.setDescriptor(descriptor);
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
        undeploy(repoPath, true);
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
        final String path = context.getResourcePath();
        RepoPath repoPath = InternalRepoPathFactory.create(getKey(), path);
        StatusHolder statusHolder = checkDownloadIsAllowed(repoPath);
        if (statusHolder.isError()) {
            RequestTraceLogger.log("Download denied (%s) - returning unfound resource", statusHolder.getStatusMsg());
            return new UnfoundRepoResource(repoPath, statusHolder.getStatusMsg(), statusHolder.getStatusCode());
        }
        return storageMixin.getInfo(context);
    }

    @Override
    public ResourceStreamHandle getResourceStreamHandle(InternalRequestContext requestContext, final RepoResource res)
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
    public void clearCaches() {
        storageMixin.clearCaches();
    }
}
