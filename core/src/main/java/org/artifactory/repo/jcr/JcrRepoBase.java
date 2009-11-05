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

package org.artifactory.repo.jcr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.FileExpectedException;
import org.artifactory.api.repo.exception.FolderExpectedException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.io.checksum.policy.ChecksumPolicy;
import org.artifactory.io.checksum.policy.ChecksumPolicyIgnoreAndGenerate;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenMetadataImportCalculator;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepoBase;
import org.artifactory.repo.context.NullRequestContext;
import org.artifactory.repo.context.RequestContext;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.resource.RepoResource;
import org.artifactory.resource.UnfoundRepoResource;
import org.artifactory.schedule.TaskService;
import org.artifactory.security.AccessLogger;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import javax.jcr.Node;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public abstract class JcrRepoBase<T extends LocalRepoDescriptor> extends RealRepoBase<T>
        implements LocalRepo<T>, StoringRepo<T> {
    private static final Logger log = LoggerFactory.getLogger(JcrRepoBase.class);

    private boolean anonAccessEnabled;
    private String tempFileRepoUrl;
    private JcrRepoService jcrRepoService;
    //Use a final policy that always generates checksums
    private final ChecksumPolicy defaultChecksumPolicy = new ChecksumPolicyIgnoreAndGenerate();

    StoringRepo<T> storageMixin = new StoringRepoMixin<T>(this);

    protected JcrRepoBase(InternalRepositoryService repositoryService) {
        super(repositoryService);
    }

    protected JcrRepoBase(InternalRepositoryService repositoryService, T descriptor) {
        super(repositoryService, descriptor);
    }

    public void init() {
        jcrRepoService = InternalContextHelper.get().getJcrRepoService();
        anonAccessEnabled = getRepositoryService().isAnonAccessEnabled();
        //Purge and recreate the (temp) repo dir
        final String key = getKey();
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File repoTmpDir = new File(artifactoryHome.getRepoTmpDir(), key);
        tempFileRepoUrl = repoTmpDir.toURI().toString();
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
        storageMixin.init();
    }

    public String getUrl() {
        return tempFileRepoUrl;
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

    public ChecksumPolicy getChecksumPolicy() {
        return defaultChecksumPolicy;
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
        boolean stop = taskService.blockIfPausedAndShouldBreak();
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
            if (taskService.blockIfPausedAndShouldBreak()) {
                status.setError("Import of " + getKey() + " was stopped", log);
                return;
            }
            //Import the folder (shallow) in a new tx
            RepoPath folderRepoPath = jcrRepoService.importFolder(this, currentFolder, settings);
            JcrFolder folder = getJcrFolder(folderRepoPath);
            // now import the folder's children
            folder.importChildren(settings, foldersToScan);
            LockingHelper.removeLockEntry(folderRepoPath);
        }
        if (!isCache()) {
            // calculate and set maven-metadata
            MavenMetadataImportCalculator metadataCalculator = new MavenMetadataImportCalculator();
            metadataCalculator.calculate(this.getRootFolder(), status);
            // if done, the maven metadata recalculation mark can be removed from the root node
            getRepositoryService().removeMarkForMavenMetadataRecalculation(rootRepoPath);
        }
        status.setStatus("Repository '" + getKey() + "' imported from " + baseDir + ".", log);
    }

    public String getTextFileContent(RepoPath repoPath) {
        String relativePath = repoPath.getPath();
        JcrFile jcrFile = getLocalJcrFile(relativePath);
        if (jcrFile != null) {
            try {
                InputStream is = new AutoCloseInputStream(jcrFile.getStream());
                return IOUtils.toString(is, "utf-8");
            } catch (IOException e) {
                throw new RuntimeException("Failed to read text file content from '" + relativePath + "'.", e);
            }
        }
        return "";
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
        storageMixin.undeploy(repoPath);
    }

    public RepoResource saveResource(RepoResource res, final InputStream in, Properties keyvals) throws IOException {
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
        return storageMixin.getInfo(new NullRequestContext(path));
    }

    public ResourceStreamHandle getResourceStreamHandle(final RepoResource res) throws IOException {
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
}