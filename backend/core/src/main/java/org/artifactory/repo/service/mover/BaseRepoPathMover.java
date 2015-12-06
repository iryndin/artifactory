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

import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
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
import org.artifactory.repo.*;
import org.artifactory.repo.cleanup.FolderPruningService;
import org.artifactory.repo.db.DbLocalRepo;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.fs.*;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.fs.lock.LockingHelper;
import org.artifactory.storage.fs.session.StorageSessionHolder;
import org.artifactory.util.RepoPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The abstract repo path mover implementation
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseRepoPathMover {
    private static final Logger log = LoggerFactory.getLogger(BaseRepoPathMover.class);
    protected final boolean copy;
    protected final boolean dryRun;
    protected final boolean executeMavenMetadataCalculation;
    protected final boolean failFast;
    protected final boolean unixStyleBehavior;
    protected final Properties properties;
    protected final MoveMultiStatusHolder status;
    private final boolean pruneEmptyFolders;
    protected AuthorizationService authorizationService;
    protected StorageInterceptors storageInterceptors;
    protected ArtifactoryContext artifactoryContext;
    protected InternalRepositoryService repositoryService;
    private MavenMetadataService mavenMetadataService;
    protected final ReplicationAddon replicationAddon;


    protected BaseRepoPathMover(MoveMultiStatusHolder status, MoverConfig moverConfig) {
        this.status = status;
        artifactoryContext = ContextHelper.get();
        authorizationService = artifactoryContext.getAuthorizationService();
        repositoryService = artifactoryContext.beanForType(InternalRepositoryService.class);
        storageInterceptors = artifactoryContext.beanForType(StorageInterceptors.class);
        mavenMetadataService = artifactoryContext.beanForType(MavenMetadataService.class);
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        replicationAddon = addonsManager.addonByType(ReplicationAddon.class);
        copy = moverConfig.isCopy();
        dryRun = moverConfig.isDryRun();
        executeMavenMetadataCalculation = moverConfig.isExecuteMavenMetadataCalculation();
        failFast = moverConfig.isFailFast();
        unixStyleBehavior = moverConfig.isUnixStyleBehavior();
        pruneEmptyFolders = moverConfig.isPruneEmptyFolders();
        properties = initProperties(moverConfig);

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

    protected MoveMultiStatusHolder moveOrCopy(VfsItem sourceItem, RepoRepoPath<LocalRepo> targetRrp) {
        // See org.artifactory.repo.service.mover.MoverConfig,
        // copy(Set<RepoPath> pathsToCopy, String targetLocalRepoKey, ...)
        // move(Set<RepoPath> pathsToCopy, String targetLocalRepoKey, ...)
        if (unixStyleBehavior) {
            // if the target is a directory and it exists we move/copy the source UNDER the target directory (ie, we
            // don't replace it - this is the default unix filesystem behavior).
            VfsItem targetFsItem = targetRrp.getRepo().getMutableFsItem(targetRrp.getRepoPath());
            if (targetFsItem != null && targetFsItem.isFolder()) {
                String adjustedPath = targetRrp.getRepoPath().getPath() + "/" + sourceItem.getName();
                targetRrp = new RepoRepoPath<>(targetRrp.getRepo(),
                        InternalRepoPathFactory.create(targetRrp.getRepoPath().getRepoKey(), adjustedPath));
            }
        }
        // ok start moving
        moveCopyRecursive(sourceItem, targetRrp);

        // recalculate maven metadata on affected repositories
        if (!dryRun) {
            clearEmptySourceDirs(sourceItem);
            RepoPath folderRepoPath = getFolderRepoPath(sourceItem);
            if (folderRepoPath != null) {
                calculateMavenMetadata(targetRrp, folderRepoPath);
            }
        }
        return status;
    }

    /**
     * move / copy in multi tx mode
     *
     * @param sourceItem source item to be copy or move
     * @param targetRrp  - target repo
     * @return - move/copy status holder
     */
    protected MoveMultiStatusHolder moveOrCopyMultiTx(VfsItem sourceItem, RepoRepoPath<LocalRepo> targetRrp) {
        Map<String, String> folderMoverMap = Maps.newHashMap();
        // ok start moving
        moveCopyRecursiveMultiTx(sourceItem, targetRrp, folderMoverMap);
        // recalculate maven metadata on affected repositories
        if (!dryRun) {
            clearEmptySourceDirs(sourceItem);
            RepoPath folderRepoPath = getFolderRepoPath(sourceItem);
            if (folderRepoPath != null) {
                calculateMavenMetadata(targetRrp, folderRepoPath);
            }
        }
        return status;
    }


    /**
     * move / copy recursive procedure in atomic tx
     * @param source - source of copy move
     * @param targetRrp - target path
     */
    protected void moveCopyRecursive(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {
        if (errorsOrWarningsOccurredAndFailFast()) {
            return;
        }
        if (source.isFolder()) {
            handleDir((VfsFolder) source, targetRrp);
        } else {
            // the source is a file
            handleFile(source, targetRrp);
        }
    }

    /**
     * move / copy recursive procedure in multi tx
     *
     * @param source    - source of copy move
     * @param targetRrp - target path
     */
    protected void moveCopyRecursiveMultiTx(VfsItem source, RepoRepoPath<LocalRepo> targetRrp, Map<String, String> folderMoverMap) {
        if (errorsOrWarningsOccurredAndFailFast()) {
            return;
        }
        if (source.isFolder()) {
            handleDirMultiTx((VfsFolder) source, targetRrp,folderMoverMap);
        } else {
            // the source is a file
            handleFileMultiTx(source, targetRrp);
        }
    }

    /**
     * This method copies the source folder to the target folder including the folder metadata, excluding children
     *
     * @param sourceFolder src
     * @param targetRrp    dest
     * @return the new folder dest created
     */
    public MutableVfsFolder shallowCopyDirectory(VfsFolder sourceFolder, RepoRepoPath<LocalRepo> targetRrp) {
        assertNotDryRun();
        LocalRepo targetRepo = targetRrp.getRepo();
        RepoPath targetRepoPath = InternalRepoPathFactory.create(targetRepo.getKey(),
                targetRrp.getRepoPath().getPath());
        MutableVfsFolder targetFolder = targetRepo.getMutableFolder(targetRepoPath);
        if (targetFolder == null) {
            log.debug("Creating target folder {}", targetRepoPath);
            targetFolder = targetRepo.createOrGetFolder(targetRepoPath);
        } else {
            log.debug("Target folder {} already exist", targetRepoPath);
        }
        status.folderMoved();
        targetFolder.fillInfo(sourceFolder.getInfo());
        // copy relevant metadata from source to target
        log.debug("Copying folder metadata to {}", targetRepoPath);
        targetFolder.setProperties(sourceFolder.getProperties());
        replicationAddon.offerLocalReplicationPropertiesChangeEvent(targetRepoPath);
        return targetFolder;
    }

    protected abstract void beforeOperationOnFolder(VfsItem sourceItem, RepoPath targetRepoPath);

    protected abstract void afterOperationOnFolder(VfsItem sourceItem, RepoRepoPath<LocalRepo> targetRrp, VfsFolder targetFolder);

    protected abstract void beforeOperationOnFile(VfsItem sourceItem, RepoPath targetRepoPath);

    protected abstract void operationOnFile(VfsFile sourceItem, RepoRepoPath<LocalRepo> targetRrp);

    protected abstract VfsItem getSourceItem(VfsItem sourceItem);


    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    protected void handleDir(VfsFolder source, RepoRepoPath<LocalRepo> targetRrp) {
        MutableVfsFolder targetFolder = null;
        RepoPath targetRepoPath = targetRrp.getRepoPath();
        if (canMove(source, targetRrp)) {
            if (!dryRun) {
                StatusEntry lastError = status.getLastError();
                //call interceptors before move or copy operation
                beforeOperationOnFolder(source, targetRepoPath);
                if (status.getCancelException(lastError) != null) {
                    return;
                }
                targetFolder = shallowCopyDirectory(source, targetRrp);
            }
        } else if (!contains(targetRrp) ||
                targetRrp.getRepo().getImmutableFsItem(targetRrp.getRepoPath()).isFile()) {
            // target repo doesn't accept this path and it doesn't already contain it OR the target is a file
            // so there is no point to continue to the children
            status.error("Cannot create/override the path '" + targetRrp.getRepoPath() + "'. " +
                    "Skipping this path and all its children.", log);
            return;
        }
        List<VfsItem> children = source.getImmutableChildren();
        RepoPath originalRepoPath = targetRrp.getRepoPath();
        for (VfsItem child : children) {
            // update the cached object with the child's repo path.
            targetRrp = new RepoRepoPath<>(targetRrp.getRepo(),
                    InternalRepoPathFactory.create(originalRepoPath, child.getName()));
            // recursive call with the child
            moveCopyRecursive(child, targetRrp);
        }
        saveSession();  // save the session before checking if the folder is empty
        //call interceptors after move or copy operation
        afterOperationOnFolder(source, targetRrp, targetFolder);
        deleteAndReplicateAfterMoveEvent(targetFolder, children.size());
    }

    /***
     * delete folder and send replication event
     *
     * @param targetFolder  - target folder
     * @param numOfChildren - folder children number
     */
    protected void deleteAndReplicateAfterMoveEvent(MutableVfsFolder targetFolder, int numOfChildren) {
        if (shouldRemoveTargetFolder(targetFolder, numOfChildren)) {
            deleteAndReplicateEvent(targetFolder); // target folder is empty remove it immediately
        }
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    protected void handleDirMultiTx(VfsFolder source, RepoRepoPath<LocalRepo> targetRrp, Map<String, String> folderMoverMap) {
        LockableMover lockableMover = InternalContextHelper.get().beanForType(LockableMover.class);
        VfsFolderRepo vfsFolderRepo = lockableMover.moveCopyFolder(source, targetRrp, this, status);
        if (vfsFolderRepo == null || vfsFolderRepo.getVfsItem() == null) {
            return;
        }
        MutableVfsFolder targetFolder = (MutableVfsFolder) vfsFolderRepo.getVfsItem();
        targetRrp = vfsFolderRepo.getRepoRepoPath();
        if (targetFolder == null) {
            return;
        }
        RepoPath originalRepoPath = targetRrp.getRepoPath();
        // add folder repo path to folder creation map
        folderMoverMap.put(targetRrp.getRepoPath().toString(), originalRepoPath.toString());
        List<VfsItem> children = source.getImmutableChildren();
        for (VfsItem child : children) {
            if (isFolderCreatedAlready(folderMoverMap, child)) {
                // update the cached object with the child's repo path.
                targetRrp = new RepoRepoPath<>(targetRrp.getRepo(),
                        InternalRepoPathFactory.create(originalRepoPath, child.getName()));
                // recursive call with the child
                moveCopyRecursiveMultiTx(child, targetRrp, folderMoverMap);
            }
        }
        //call interceptors after move or copy operation
        lockableMover = InternalContextHelper.get().beanForType(LockableMover.class);
        lockableMover.postFolderProcessing(source, targetFolder, targetRrp, this, properties, children.size());
    }

    /**
     * check weather folder was created already to prevent recursive folder creation
     *
     * @param folderMoverMap - hold folder created Path
     * @param child          - child to be created
     * @return - if true folder was not created already
     */
    private boolean isFolderCreatedAlready(Map<String, String> folderMoverMap, VfsItem child) {
        return folderMoverMap.get(child.getRepoPath().toString()) == null;
    }

    protected boolean validateDryRun(RepoRepoPath<LocalRepo> targetRrp) {
        if (dryRun && (!contains(targetRrp) ||
                targetRrp.getRepo().getImmutableFsItem(targetRrp.getRepoPath()).isFile())) {
            // target repo doesn't accept this path and it doesn't already contain it OR the target is a file
            // so there is no point to continue to the children
            status.error("Cannot create/override the path '" + targetRrp.getRepoPath() + "'. " +
                    "Skipping this path and all its children.", log);
            return true;

        }
        return false;
    }

    protected RepoRepoPath<LocalRepo> processUnixStyle(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {
        if (unixStyleBehavior) {
            // if the target is a directory and it exists we move/copy the source UNDER the target directory (ie, we
            // don't replace it - this is the default unix filesystem behavior).
            VfsItem targetFsItem = targetRrp.getRepo().getMutableFsItem(targetRrp.getRepoPath());
            if (targetFsItem != null && targetFsItem.isFolder()) {
                String adjustedPath = targetRrp.getRepoPath().getPath() + "/" + source.getName();
                targetRrp = new RepoRepoPath<>(targetRrp.getRepo(),
                        InternalRepoPathFactory.create(targetRrp.getRepoPath().getRepoKey(), adjustedPath));
            }
        }
        return targetRrp;
    }

    /**
     * If not in a dry run, If not pruning empty folders (if true it will happen at a later stage),
     * If not copying (no source removal when copying), If not on the root item (a repo),
     * If not containing any children and folders or artifacts were moved.
     */
    protected boolean shouldRemoveSourceFolder(VfsFolder sourceFolder) {
        return !dryRun && !copy && !sourceFolder.getRepoPath().isRoot() && !sourceFolder.hasChildren()
                && !pruneEmptyFolders && (status.getMovedFoldersCount() != 0 || status.getMovedArtifactsCount() != 0);
    }

    //If not containing any children and items have been moved (children have actually been moved)
    protected boolean shouldRemoveTargetFolder(MutableVfsFolder targetFolder, int childrenSize) {
        return !dryRun && targetFolder != null && !targetFolder.getRepoPath().isRoot() && !targetFolder.hasChildren()
                && childrenSize != 0;
    }

    protected void handleFile(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {
        if (canMove(source, targetRrp)) {
            if (!dryRun) {
                moveFile((VfsFile) source, targetRrp);
            } else {
                status.artifactMoved();
            }
        }
    }

    protected void handleFileMultiTx(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {
        if (canMove(source, targetRrp)) {
            if (!dryRun) {
                moveFileMultiTx((VfsFile) source, targetRrp);
            } else {
                status.artifactMoved();
            }
        }
    }

    protected void afterFolderCopy(VfsItem sourceItem, RepoRepoPath<LocalRepo> targetRrp, VfsFolder targetFolder, Properties properties) {
        RepoPath repoPath = targetRrp.getRepoPath();
        DbLocalRepo targetRepo = (DbLocalRepo) targetRrp.getRepo();
        if (!dryRun && copy && targetRepo.isPathPatternValid(repoPath, repoPath.getPath())) {
            storageInterceptors.afterCopy(sourceItem, targetFolder, status, properties);
        }
    }

    protected void copyFile(VfsFile sourceItem, RepoRepoPath<LocalRepo> targetRrp, Properties properties) {
        MutableVfsFile targetFile = getMutableVfsTargetFile(targetRrp, targetRrp.getRepo());
        log.debug("Copying file {} to {}", sourceItem, targetFile);
        // copy file
        copyVfsFile(sourceItem, targetFile);
        // call after copy interceptors
        storageInterceptors.afterCopy(sourceItem, targetFile, status, properties);
    }


    protected void afterMoveFolder(VfsItem sourceItem, VfsFolder targetFolder, Properties properties) {
        if (shouldRemoveSourceFolder((VfsFolder) sourceItem)) {
            storageInterceptors.afterMove(sourceItem, targetFolder, status, properties);
            deleteAndReplicateEvent((VfsFolder) sourceItem);
        }
    }

    protected void moveFile(VfsFile sourceItem, RepoRepoPath<LocalRepo> targetRrp, Properties properties) {
        MutableVfsFile targetFile = getMutableVfsTargetFile(targetRrp, targetRrp.getRepo());
        log.debug("Moving file from {} to {}", sourceItem, targetFile);
        moveVfsFile(sourceItem, targetFile);
        storageInterceptors.afterMove(sourceItem, targetFile, status, properties);
    }

    private void moveVfsFile(VfsFile sourceFile, MutableVfsFile targetFile) {
        copyVfsFile(sourceFile, targetFile);
        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(sourceFile.getRepoKey());
        MutableVfsFile mutableSourceFile =
                localRepo != null ? localRepo.getMutableFile(sourceFile.getRepoPath()) : null;
        if (mutableSourceFile != null) {
            mutableSourceFile.delete();
        } else {
            log.error("About to delete {} but it is null", sourceFile.getRepoPath());
        }
    }
    protected boolean canMove(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {
        RepoPath sourceRepoPath = source.getRepoPath();

        LocalRepo targetRepo = targetRrp.getRepo();
        RepoPath targetRepoPath = targetRrp.getRepoPath();
        String targetPath = targetRepoPath.getPath();

        // snapshot/release policy is enforced only on files since it only has a meaning on files
        if (source.isFile() && !targetRepo.handlesReleaseSnapshot(targetPath)) {
            status.error("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath
                    + "' due to its snapshot/release handling policy.", HttpStatus.SC_BAD_REQUEST, log);
            return false;
        }

        if (!targetRepo.accepts(targetRepoPath)) {
            status.error("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath
                    + "' due to its include/exclude patterns.", HttpStatus.SC_FORBIDDEN, log);
            return false;
        }

        // permission checks
        if (!copy && !authorizationService.canDelete(sourceRepoPath)) {
            status.error("User doesn't have permissions to move '" + sourceRepoPath + "'. " +
                    "Needs delete permissions.", HttpStatus.SC_FORBIDDEN, log);
            return false;
        }

        if (contains(targetRrp)) {
            if (!authorizationService.canDelete(targetRepoPath)) {
                status.error("User doesn't have permissions to override '" + targetRepoPath + "'. " +
                        "Needs delete permissions.", HttpStatus.SC_UNAUTHORIZED, log);
                return false;
            }

            // don't allow moving/copying folder to file
            if (source.isFolder()) {
                VfsItem targetFsItem = targetRepo.getMutableFsItem(targetRepoPath);
                if (targetFsItem != null && targetFsItem.isFile()) {
                    status.error("Can't move folder under file '" + targetRepoPath + "'. ", HttpStatus.SC_BAD_REQUEST,
                            log);
                    return false;
                }
            }
        } else if (!authorizationService.canDeploy(targetRepoPath)) {
            status.error("User doesn't have permissions to create '" + targetRepoPath + "'. " +
                    "Needs write permissions.", HttpStatus.SC_FORBIDDEN, log);
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
                status.error("Failed to validate target path of pom: " + targetPath, HttpStatus.SC_BAD_REQUEST, e, log);
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
        RepoPath targetRepoPath = targetRrp.getRepoPath();
        StatusEntry lastError = status.getLastError();
        // call interceptors before operation
        beforeOperationOnFile(sourceFile, targetRepoPath);
        if (status.getCancelException(lastError) != null) {
                return;
            }
        overrideTargetFileIfExist(targetRrp, targetRepo);

        // copy or move file
        operationOnFile(sourceFile, targetRrp);
        saveSession();
        LockingHelper.removeLockEntry(sourceFile.getRepoPath());
        status.artifactMoved();
    }

    protected void moveFileMultiTx(VfsFile sourceFile, RepoRepoPath<LocalRepo> targetRrp) {
        assertNotDryRun();
        LockableMover lockableMover = InternalContextHelper.get().beanForType(LockableMover.class);
        lockableMover.moveCopyFile(sourceFile, targetRrp, this, status);
        status.artifactMoved();
    }

    protected void overrideTargetFileIfExist(RepoRepoPath<LocalRepo> targetRrp, LocalRepo targetRepo) {
        MutableVfsItem targetItem = targetRepo.getMutableFsItem(targetRrp.getRepoPath());
        if (targetItem != null) {
            // target repository already contains file or folder with the same name, delete it
            log.debug("File {} already exists in target repository. Overriding.", targetRrp.getRepoPath());
            targetItem.delete();
            saveSession();
        }
    }

    protected MutableVfsFile getMutableVfsTargetFile(RepoRepoPath<LocalRepo> targetRrp, LocalRepo targetRepo) {
        return targetRepo.createOrGetFile(targetRrp.getRepoPath());
    }

    protected void copyVfsFile(VfsFile sourceFile, MutableVfsFile targetFile) {
        // copy the info and the properties only (stats and watches are not required)
        targetFile.tryUsingExistingBinary(sourceFile.getSha1(), sourceFile.getMd5(), sourceFile.length());
        targetFile.fillInfo(sourceFile.getInfo());
        targetFile.setProperties(sourceFile.getProperties());
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

    /**
     * Marks the current folder for pruning, works only if the config set prune to true.
     */
    protected void clearEmptySourceDirs(VfsItem sourceItem) {
        RepoPath sourceRepoPath = getFolderRepoPath(sourceItem);

        // cleanup only in search results or promotion after move
        if (sourceRepoPath == null || !pruneEmptyFolders || copy) {
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
            if (mutableFolder != null) {
                mutableFolder.delete();
                artifactoryContext.beanForType(AddonsManager.class).addonByType(ReplicationAddon.class)
                        .offerLocalReplicationDeleteEvent(folder.getRepoPath());
            }
        }
    }

    protected void saveSession() {
        StorageSessionHolder.getSession().save();
    }

}
