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

package org.artifactory.repo.service.mover;

import org.apache.commons.io.IOUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenMetadataService;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.StatusEntry;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.maven.PomTargetPathValidator;
import org.artifactory.md.Properties;
import org.artifactory.mime.MavenNaming;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.cleanup.FolderPruningService;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.fs.MutableVfsFile;
import org.artifactory.sapi.fs.MutableVfsFolder;
import org.artifactory.sapi.fs.MutableVfsItem;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.artifactory.storage.fs.lock.LockingHelper;
import org.artifactory.storage.fs.session.StorageSessionHolder;
import org.artifactory.util.RepoPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * The abstract repo path mover implementation
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseRepoPathMover {
    private static final Logger log = LoggerFactory.getLogger(BaseRepoPathMover.class);

    private InternalRepositoryService repositoryService;
    private MavenMetadataService mavenMetadataService;

    protected AuthorizationService authorizationService;
    protected StorageInterceptors storageInterceptors;

    protected final boolean copy;
    protected final boolean dryRun;
    protected final boolean executeMavenMetadataCalculation;
    protected final boolean failFast;
    protected final Properties properties;
    protected final String targetLocalRepoKey;

    private final boolean pruneEmptyFolders;
    protected final MoveMultiStatusHolder status;
    protected ArtifactoryContext artifactoryContext;

    protected BaseRepoPathMover(MoveMultiStatusHolder status, MoverConfig moverConfig) {
        this.status = status;
        artifactoryContext = ContextHelper.get();
        authorizationService = artifactoryContext.getAuthorizationService();
        repositoryService = artifactoryContext.beanForType(InternalRepositoryService.class);
        storageInterceptors = artifactoryContext.beanForType(StorageInterceptors.class);
        mavenMetadataService = artifactoryContext.beanForType(MavenMetadataService.class);

        copy = moverConfig.isCopy();
        dryRun = moverConfig.isDryRun();
        executeMavenMetadataCalculation = moverConfig.isExecuteMavenMetadataCalculation();
        failFast = moverConfig.isFailFast();
        pruneEmptyFolders = moverConfig.isPruneEmptyFolders();
        properties = initProperties(moverConfig);
        targetLocalRepoKey = moverConfig.getTargetLocalRepoKey();

        // don't output to the logger if executing in dry run
        this.status.setActivateLogging(!dryRun);
    }

    protected Properties initProperties(MoverConfig moverConfig) {
        Properties properties = moverConfig.getProperties();
        if (properties == null) {
            properties = (Properties) InfoFactoryHolder.get().createProperties();
        }
        return properties;
    }

    /**
     * <ul> <li>If not in a dry run.</li> <li>If not pruning empty folders (if true it will happen at a
     * later stage).</li> <li>If not copying (no source removal when copying).</li> <li>If not on the root item (a
     * repo).</li> <li>If not containing any children and items have been moved (children have actually been
     * moved).</li> </ul>
     */
    protected boolean shouldRemoveSourceFolder(VfsFolder sourceFolder) {
        return !dryRun && !pruneEmptyFolders && !copy && !sourceFolder.getRepoPath().isRoot() &&
                !sourceFolder.hasChildren() && status.getMovedCount() != 0;
    }

    protected void handleFile(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {
        if (canMove(source, targetRrp)) {
            if (!dryRun) {
                moveFile((VfsFile) source, targetRrp);
            } else {
                status.itemMoved();
            }
        }
    }

    protected boolean canMove(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {
        RepoPath sourceRepoPath = source.getRepoPath();

        LocalRepo targetRepo = targetRrp.getRepo();
        RepoPath targetRepoPath = targetRrp.getRepoPath();
        String targetPath = targetRepoPath.getPath();

        // snapshot/release policy is enforced only on files since it only has a meaning on files
        if (source.isFile() && !targetRepo.handlesReleaseSnapshot(targetPath)) {
            status.warn("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath
                    + "' due to its snapshot/release handling policy.", log);
            return false;
        }

        if (!targetRepo.accepts(targetRepoPath)) {
            status.warn("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath
                    + "' due to its include/exclude patterns.", log);
            return false;
        }

        // permission checks
        if (!copy && !authorizationService.canDelete(sourceRepoPath)) {
            status.warn("User doesn't have permissions to move '" + sourceRepoPath + "'. " +
                    "Needs delete permissions.", log);
            return false;
        }

        if (contains(targetRrp)) {
            if (!authorizationService.canDelete(targetRepoPath)) {
                status.warn("User doesn't have permissions to override '" + targetRepoPath + "'. " +
                        "Needs delete permissions.", log);
                return false;
            }

            // don't allow moving/copying folder to file
            if (source.isFolder()) {
                VfsItem targetFsItem = targetRepo.getMutableFsItem(targetRepoPath);
                if (targetFsItem != null && targetFsItem.isFile()) {
                    status.warn("Can't move folder under file '" + targetRepoPath + "'. ", log);
                    return false;
                }
            }
        } else if (!authorizationService.canDeploy(targetRepoPath)) {
            status.warn("User doesn't have permissions to create '" + targetRepoPath + "'. " +
                    "Needs write permissions.", log);
            return false;
        }

        if (source.isFile() && NamingUtils.isPom(sourceRepoPath.getPath()) && NamingUtils.isPom(targetPath) &&
                !((RealRepoDescriptor) targetRepo.getDescriptor()).isSuppressPomConsistencyChecks()) {
            ModuleInfo moduleInfo = targetRepo.getItemModuleInfo(targetPath);
            InputStream resourceStream = null;
            try {
                resourceStream = ((VfsFile) source).getStream();
                new PomTargetPathValidator(targetPath, moduleInfo).validate(resourceStream, false);
            } catch (Exception e) {
                status.warn("Failed to validate target path of pom: " + targetPath, e, log);
                return false;
            } finally {
                IOUtils.closeQuietly(resourceStream);
            }
        }

        // all tests passed
        return true;
    }

    protected void moveFile(VfsFile sourceFile, RepoRepoPath<LocalRepo> targetRrp) {
        assertNotDryRun();
        LocalRepo targetRepo = targetRrp.getRepo();

        MutableVfsItem targetItem = targetRepo.getMutableFsItem(targetRrp.getRepoPath());
        if (targetItem != null) {
            // target repository already contains file or folder with the same name, delete it
            log.debug("File {} already exists in target repository. Overriding.", targetRrp.getRepoPath());
            targetItem.delete();
            saveSession();
        }

        MutableVfsFile targetFile = targetRepo.createOrGetFile(targetRrp.getRepoPath());

        RepoPath targetRepoPath = targetRrp.getRepoPath();
        if (copy) {
            StatusEntry lastError = status.getLastError();
            storageInterceptors.beforeCopy(sourceFile, targetRepoPath, status, properties);
            if (status.getCancelException(lastError) != null) {
                return;
            }
            log.debug("Copying file {} to {}", sourceFile, targetFile);
            copyVfsFile(sourceFile, targetFile);
            storageInterceptors.afterCopy(sourceFile, targetFile, status, properties);
        } else {
            StatusEntry lastError = status.getLastError();
            storageInterceptors.beforeMove(sourceFile, targetRepoPath, status, properties);
            if (status.getCancelException(lastError) != null) {
                return;
            }
            log.debug("Moving file from {} to {}", sourceFile, targetFile);
            moveVfsFile(sourceFile, targetFile);
            storageInterceptors.afterMove(sourceFile, targetFile, status, properties);
        }
        saveSession();
        LockingHelper.removeLockEntry(sourceFile.getRepoPath());
        status.itemMoved();
    }

    private void copyVfsFile(VfsFile sourceFile, MutableVfsFile targetFile) {
        // copy the info and the properties only (stats and watches are not required)
        targetFile.useData(sourceFile.getSha1(), sourceFile.getMd5(), sourceFile.length());
        targetFile.fillInfo(sourceFile.getInfo());
        targetFile.setProperties(sourceFile.getProperties());
    }

    private void moveVfsFile(VfsFile sourceFile, MutableVfsFile targetFile) {
        copyVfsFile(sourceFile, targetFile);
        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(sourceFile.getRepoKey());
        MutableVfsFile mutableSourceFile = localRepo.getMutableFile(sourceFile.getRepoPath());
        mutableSourceFile.delete();
    }

    protected void assertNotDryRun() {
        if (dryRun) {
            throw new IllegalStateException("Method call is not allowed in dry run");
        }
    }

    protected boolean contains(RepoRepoPath<LocalRepo> rrp) {
        return rrp.getRepo().itemExists(rrp.getRepoPath().getPath());
    }

    protected void clearEmptyDirsAndCalcMetadata(RepoRepoPath<LocalRepo> targetRrp, VfsItem sourceItem) {
        if (!dryRun) {
            clearEmptySourceDirs(sourceItem);
            if (calcMetadata()) {
                RepoPath folderRepoPath = getFolderRepoPath(sourceItem);
                if (folderRepoPath != null) {
                    calculateMavenMetadata(targetRrp, folderRepoPath);
                }
            }
        }
    }

    protected void clearEmptySourceDirs(VfsItem sourceItem) {
        RepoPath sourceRepoPath = getFolderRepoPath(sourceItem);
        if (sourceRepoPath == null) {
            return;
        }

        if (!pruneEmptyFolders || copy) {
            // cleanup only in search results or promotion after move
            return;
        }

        FolderPruningService pruningService = ContextHelper.get().beanForType(FolderPruningService.class);
        pruningService.prune(sourceRepoPath);
    }

    protected RepoPath getFolderRepoPath(VfsItem sourceItem) {
        RepoPath sourceRepoPath = sourceItem.getRepoPath();
        if (sourceItem.isFile()) {
            //If the item is a file, just calculate the parent folder
            sourceRepoPath = sourceRepoPath.getParent();
        }

        if (sourceRepoPath == null || sourceRepoPath.isRoot()) {
            // cleanup only for non root folders
            return null;
        }
        return sourceRepoPath;
    }

    protected boolean calcMetadata() {
        return true;
    }

    protected boolean calcMetadataOnSource() {
        return true;
    }

    protected boolean calcMetadataOnTarget() {
        return true;
    }

    protected void calculateMavenMetadata(RepoRepoPath<LocalRepo> targetRrp, RepoPath sourceFolderRepoPath) {
        assertNotDryRun();

        if (calcMetadataOnTarget()) {
            LocalRepo targetLocalRepo = targetRrp.getRepo();
            VfsItem fsItem = targetLocalRepo.getImmutableFsItem(targetRrp.getRepoPath());
            if (fsItem == null) {
                log.debug("Target item doesn't exist. Skipping maven metadata recalculation.");
                return;
            }

            // start calculation from the parent folder of the target path (unless it's the root)
            RepoPath folderForMetadataCalculation =
                    fsItem.getRepoPath().isRoot() ? fsItem.getRepoPath() : fsItem.getRepoPath().getParent();
            if (executeMavenMetadataCalculation) {
                mavenMetadataService.calculateMavenMetadataAsync(folderForMetadataCalculation, true);
                if (MavenNaming.isPom(fsItem.getRepoPath().getPath())) {
                    // for pom files we need to trigger metadata calculation on the grandparent non-recursively -
                    // potential new version and snapshot.
                    RepoPath grandparentFolder = RepoPathUtils.getAncestor(fsItem.getRepoPath(), 2);
                    mavenMetadataService.calculateMavenMetadataAsync(grandparentFolder, false);
                }
            } else {
                status.addToMavenMetadataCandidates(folderForMetadataCalculation);
                if (MavenNaming.isPom(fsItem.getRepoPath().getPath())) {
                    // for pom files we need to trigger metadata calculation on the grandparent non-recursively -
                    // potential new version and snapshot.
                    RepoPath grandparentFolder = RepoPathUtils.getAncestor(fsItem.getRepoPath(), 2);
                    status.addToMavenMetadataCandidates(grandparentFolder);
                }
            }
        }

        if (calcMetadataOnSource()) {
            // recalculate the source repository only if it's not a cache repo and not copy
            RepoPath sourceForMetadataCalculation = sourceFolderRepoPath.getParent();
            Repo sourceRepo = repositoryService.repositoryByKey(sourceFolderRepoPath.getRepoKey());
            if (!copy && sourceRepo != null && !sourceRepo.isCache() && sourceForMetadataCalculation != null) {
                if (executeMavenMetadataCalculation) {
                    mavenMetadataService.calculateMavenMetadataAsync(sourceForMetadataCalculation, true);
                } else {
                    status.addToMavenMetadataCandidates(sourceForMetadataCalculation);
                }
            }
        }
    }

    protected boolean errorsOrWarningsOccurredAndFailFast() {
        return (status.hasWarnings() || status.hasErrors()) && failFast;
    }

    protected void deleteAndReplicateEvent(VfsFolder folder) {
        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(folder.getRepoKey());
        if (localRepo != null) {
            MutableVfsFolder mutableFolder = localRepo.getMutableFolder(folder.getRepoPath());
            mutableFolder.delete();
            artifactoryContext.beanForType(AddonsManager.class).addonByType(ReplicationAddon.class)
                    .offerLocalReplicationDeleteEvent(folder.getRepoPath());
        }
    }

    protected void saveSession() {
        StorageSessionHolder.getSession().save();
    }

}
