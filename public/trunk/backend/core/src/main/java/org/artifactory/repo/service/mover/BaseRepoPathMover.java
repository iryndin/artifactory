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
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.StatusEntry;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.PomTargetPathValidator;
import org.artifactory.md.Properties;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.interceptor.StorageInterceptors;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.sapi.fs.VfsFile;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.slf4j.Logger;

import java.io.InputStream;

/**
 * The abstract repo path mover implementation
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseRepoPathMover {
    private static final Logger log = LoggerFactory.getLogger(BaseRepoPathMover.class);

    private JcrService jcrService;
    private InternalRepositoryService repositoryService;

    protected AuthorizationService authorizationService;
    protected JcrRepoService jcrRepoService;
    protected StorageInterceptors storageInterceptors;

    protected final boolean copy;
    protected final boolean dryRun;
    protected final boolean executeMavenMetadataCalculation;
    protected final boolean failFast;
    protected final Properties properties;
    protected final String targetLocalRepoKey;

    private final boolean searchResult;
    protected final MoveMultiStatusHolder status;
    protected ArtifactoryContext artifactoryContext;

    protected BaseRepoPathMover(MoveMultiStatusHolder status, MoverConfig moverConfig) {
        this.status = status;
        artifactoryContext = ContextHelper.get();
        authorizationService = artifactoryContext.getAuthorizationService();
        jcrService = artifactoryContext.beanForType(JcrService.class);
        jcrRepoService = artifactoryContext.beanForType(JcrRepoService.class);
        repositoryService = artifactoryContext.beanForType(InternalRepositoryService.class);
        storageInterceptors = artifactoryContext.beanForType(StorageInterceptors.class);

        copy = moverConfig.isCopy();
        dryRun = moverConfig.isDryRun();
        executeMavenMetadataCalculation = moverConfig.isExecuteMavenMetadataCalculation();
        failFast = moverConfig.isFailFast();
        searchResult = moverConfig.isSearchResult();
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
     * <ul> <li>If not in a dry run.</li> <li>If not handling search results (search result folders are cleaned at a
     * later stage).</li> <li>If not copying (no source removal when copying).</li> <li>If not on the root item (a
     * repo).</li> <li>If not containing any children and items have been moved (children have actually been
     * moved).</li> </ul>
     */
    protected boolean shouldRemoveSourceFolder(VfsFolder sourceFolder) {
        return !dryRun && !searchResult && !copy && !sourceFolder.getRepoPath().isRoot() &&
                sourceFolder.list().length == 0 && status.getMovedCount() != 0;
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
            status.setWarning("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath
                    + "' due to its snapshot/release handling policy.", log);
            return false;
        }

        if (!targetRepo.accepts(targetPath)) {
            status.setWarning("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath
                    + "' due to its include/exclude patterns.", log);
            return false;
        }

        // permission checks
        if (!copy && !authorizationService.canDelete(sourceRepoPath)) {
            status.setWarning("User doesn't have permissions to move '" + sourceRepoPath + "'. " +
                    "Needs delete permissions.", log);
            return false;
        }

        if (contains(targetRrp)) {
            if (!authorizationService.canDelete(targetRepoPath)) {
                status.setWarning("User doesn't have permissions to override '" + targetRepoPath + "'. " +
                        "Needs delete permissions.", log);
                return false;
            }

            // don't allow moving/copying folder to file
            if (source.isDirectory()) {
                VfsItem targetFsItem = targetRepo.getLockedJcrFsItem(targetRepoPath);
                if (targetFsItem != null && targetFsItem.isFile()) {
                    status.setWarning("Can't move folder under file '" + targetRepoPath + "'. ", log);
                    return false;
                }
            }
        } else if (!authorizationService.canDeploy(targetRepoPath)) {
            status.setWarning("User doesn't have permissions to create '" + targetRepoPath + "'. " +
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
                status.setWarning("Failed to validate target path of pom: " + targetPath, e, log);
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
        String sourceAbsPath = PathFactoryHolder.get().getAbsolutePath(sourceFile.getRepoPath());
        if (!jcrService.itemNodeExists(sourceAbsPath)) {
            //RTFACT-4878
            status.setWarning("Cannot move file '" + sourceAbsPath + "' since it no longer exists.", log);
            return;
        }
        if (contains(targetRrp)) {
            // target repository already contains file with the same name, delete it
            log.debug("File {} already exists in target repository. Overriding.", targetRrp.getRepoPath().getPath());
            VfsItem existingTargetFile = targetRepo.getLockedJcrFsItem(targetRrp.getRepoPath().getPath());
            bruteForceDeleteAndReplicateEvent(existingTargetFile);
        } else {
            // make sure parent directories exist
            RepoPath targetParentRepoPath = InternalRepoPathFactory.create(targetRepo.getKey(),
                    targetRrp.getRepoPath().getParent().getPath());
            VfsItemFactory.createVfsFolder(targetParentRepoPath, targetRepo).mkdirs();
        }

        RepoPath targetRepoPath = targetRrp.getRepoPath();
        VfsFile targetJcrFile = VfsItemFactory.createVfsFile(targetRepoPath, targetRepo);
        String targetAbsPath = PathFactoryHolder.get().getAbsolutePath(targetRepoPath);
        if (copy) {
            //Important - do, otherwise target folders aren't found by the workspace yet
            jcrService.getManagedSession().save();
            StatusEntry lastError = status.getLastError();
            storageInterceptors.beforeCopy(sourceFile, targetRepoPath, status, properties);
            if (status.getCancelException(lastError) != null) {
                return;
            }
            log.debug("Copying file {} to {}", sourceAbsPath, targetAbsPath);
            jcrService.copy(sourceAbsPath, targetAbsPath);
            storageInterceptors.afterCopy(sourceFile, targetJcrFile, status, properties);
        } else {
            StatusEntry lastError = status.getLastError();
            storageInterceptors.beforeMove(sourceFile, targetRepoPath, status, properties);
            if (status.getCancelException(lastError) != null) {
                return;
            }
            log.debug("Moving file from {} to {}", sourceAbsPath, targetAbsPath);
            jcrService.move(sourceAbsPath, targetAbsPath);
            storageInterceptors.afterMove(sourceFile, targetJcrFile, status, properties);
            // mark the moved source file as deleted and remove it from the cache
            sourceFile.setDeleted(true);
            sourceFile.updateCache();
        }
        LockingHelper.removeLockEntry(sourceFile.getRepoPath());
        status.itemMoved();
    }

    protected void assertNotDryRun() {
        if (dryRun) {
            throw new IllegalStateException("Method call is not allowed in dry run");
        }
    }

    protected boolean contains(RepoRepoPath<LocalRepo> rrp) {
        return rrp.getRepo().itemExists(rrp.getRepoPath().getPath());
    }

    protected void clearEmptyDirsAndCalcMetadata(RepoRepoPath<LocalRepo> targetRrp, VfsItem fsItemToMove) {
        if (!dryRun) {
            VfsFolder sourceRootFolder = clearEmptySourceDirs(fsItemToMove);

            if (calcMetadata()) {
                calculateMavenMetadata(targetRrp, sourceRootFolder);
            }
        }
    }

    /**
     * Clears any remaining empty source directories (if needed) and returns the highest remaining one
     */
    protected VfsFolder clearEmptySourceDirs(VfsItem fsItemToMove) {
        VfsFolder sourceRootFolder;
        if (fsItemToMove.isDirectory()) {
            //If the item is a directory
            VfsFolder fsFolderToMove = (VfsFolder) fsItemToMove;
            if (searchResult && !copy) {
                /**
                 * If search results are being handled, clean up empty folders and return the folder that should be
                 * calculated (parent of last deleted folder)
                 */
                sourceRootFolder = cleanEmptyFolders(fsFolderToMove);
            } else {
                //If ordinary artifacts are being handled, return the source folder to be calculated
                sourceRootFolder = fsFolderToMove;
            }
        } else {
            //If the item is a file, just calculate the parent folder
            sourceRootFolder = fsItemToMove.getLockedParentFolder();
        }
        return sourceRootFolder;
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

    /**
     * Cleans the empty folders of the upper hierarchy, starting from the given folder. This method is only called after
     * moving search results and only if a folder was the root of the move operation (which means that all the
     * descendants were selected to move).
     *
     * @param sourceFolder Folder to start clean up at
     * @param status       MutableStatusHolder
     * @return Parent of highest removed folder
     */
    private VfsFolder cleanEmptyFolders(VfsFolder sourceFolder) {
        VfsFolder highestRemovedPath = sourceFolder;
        boolean emptyAndNotRoot = true;
        while (emptyAndNotRoot) {
            boolean isRoot = highestRemovedPath.getRepoPath().isRoot();
            emptyAndNotRoot = !isRoot && hasNoSiblings(highestRemovedPath.getAbsolutePath());
            if (emptyAndNotRoot) {
                //Remove current folder, continue to the parent
                storageInterceptors.afterDelete(highestRemovedPath, status);
                bruteForceDeleteAndReplicateEvent(highestRemovedPath);
                highestRemovedPath = highestRemovedPath.getLockedParentFolder();
            }
        }
        return highestRemovedPath;
    }

    /**
     * Indicates if the current source folder has no sibling nodes
     *
     * @param parentAbsPath Absolute path of source folder parent
     * @return True if the current source folder has no siblings, false if not
     */
    private boolean hasNoSiblings(String parentAbsPath) {
        //Important: Make sure to get the child count in a non-locking way
        return jcrRepoService.getChildrenNames(parentAbsPath).isEmpty();
    }

    private void calculateMavenMetadata(RepoRepoPath<LocalRepo> targetRrp, VfsFolder rootFolderToMove) {
        assertNotDryRun();

        if (calcMetadataOnTarget()) {
            LocalRepo targetLocalRepo = targetRrp.getRepo();
            VfsItem fsItem = targetLocalRepo.getJcrFsItem(targetRrp.getRepoPath());

            if (fsItem == null) {
                log.debug("Target item doesn't exist. Skipping maven metadata recalculation.");
                return;
            }
            VfsFolder rootTargetFolder;
            if (fsItem.isDirectory()) {
                rootTargetFolder = (VfsFolder) fsItem;
            } else {
                rootTargetFolder = fsItem.getParentFolder();
            }
            if (rootTargetFolder == null) {
                //  target repository doesn't exist which means nothing was moved (maybe permissions issue)
                log.debug("Target root folder doesn't exist. Skipping maven metadata recalculation.");
                return;
            }
            // always recalculate the target repository. start with the parent of the moved folder in the target
            VfsFolder rootTargetFolderForMatadataCalculation =
                    !rootTargetFolder.getRepoPath().isRoot() && rootTargetFolder.getLockedParentFolder() != null ?
                            rootTargetFolder.getLockedParentFolder() : rootTargetFolder;
            repositoryService.markBaseForMavenMetadataRecalculation(
                    rootTargetFolderForMatadataCalculation.getRepoPath());
            if (executeMavenMetadataCalculation) {
                repositoryService.calculateMavenMetadataAsync(rootTargetFolderForMatadataCalculation.getRepoPath());
            }
        }

        if (calcMetadataOnSource()) {
            // recalculate the source repository only if it's not a cache repo and not copy
            JcrFsItemFactory sourceRepo = VfsItemFactory.getStoringRepo(rootFolderToMove);
            if (!copy && !sourceRepo.isCache() && rootFolderToMove.getLockedParentFolder() != null) {
                VfsFolder sourceFolderMetadata = rootFolderToMove.getLockedParentFolder();
                repositoryService.markBaseForMavenMetadataRecalculation(sourceFolderMetadata.getRepoPath());
                if (executeMavenMetadataCalculation) {
                    repositoryService.calculateMavenMetadataAsync(sourceFolderMetadata.getRepoPath());
                }
            }
        }
    }

    protected boolean errorsOrWarningsOccurredAndFailFast() {
        return (status.hasWarnings() || status.hasErrors()) && failFast;
    }

    protected void bruteForceDeleteAndReplicateEvent(VfsItem item) {
        jcrRepoService.bruteForceDeleteAndReplicateEvent(item);
    }
}
