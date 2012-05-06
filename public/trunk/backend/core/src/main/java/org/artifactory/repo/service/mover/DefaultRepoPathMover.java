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

import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.StatusEntry;
import org.artifactory.jcr.factory.JcrFsItemFactory;
import org.artifactory.jcr.factory.VfsItemFactory;
import org.artifactory.jcr.md.MetadataAware;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.sapi.fs.VfsFolder;
import org.artifactory.sapi.fs.VfsItem;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * The ordinary repo-path move/copy implementation
 *
 * @author Noam Y. Tenne
 */
class DefaultRepoPathMover extends BaseRepoPathMover {

    private static final Logger log = LoggerFactory.getLogger(DefaultRepoPathMover.class);

    private InternalRepositoryService repositoryService;

    DefaultRepoPathMover(MoveMultiStatusHolder status, MoverConfig moverConfig) {
        super(status, moverConfig);
        repositoryService = ContextHelper.get().beanForType(InternalRepositoryService.class);
    }

    MoveMultiStatusHolder moveOrCopy(VfsItem fsItemToMove, LocalRepo targetLocalRepo, RepoPath targetLocalRepoPath,
            RepoRepoPath<LocalRepo> targetRrp) {

        // if the target is a directory and it exists we move/copy the source UNDER the target directory (ie, we don't
        // replace it). We do it only if the target repo key is null and the target fs item is a directory.
        VfsItem targetFsItem = targetLocalRepo.getLockedJcrFsItem(targetLocalRepoPath);
        if (targetFsItem != null && targetFsItem.isDirectory() && targetLocalRepoKey == null) {
            String adjustedPath = targetLocalRepoPath.getPath() + "/" + fsItemToMove.getName();
            targetRrp = new RepoRepoPath<LocalRepo>(targetLocalRepo,
                    InternalRepoPathFactory.create(targetLocalRepo.getKey(), adjustedPath));

        }

        // ok start moving
        moveRecursive(fsItemToMove, targetRrp);

        // recalculate maven metadata on affected repositories
        if (!dryRun) {
            VfsFolder sourceRootFolder = clearEmptySourceDirs(fsItemToMove);
            calculateMavenMetadata(targetRrp, sourceRootFolder);
        }
        return status;
    }

    private void moveRecursive(VfsItem source, RepoRepoPath<LocalRepo> targetRrp) {

        if (errorsOrWarningsOccurredAndFailFast()) {
            return;
        }

        if (source.isDirectory()) {
            handleDir((VfsFolder) source, targetRrp);
        } else {
            // the source is a file
            handleFile(source, targetRrp);
        }
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    private void handleDir(VfsFolder source, RepoRepoPath<LocalRepo> targetRrp) {
        VfsFolder targetFolder = null;
        RepoPath targetRepoPath = targetRrp.getRepoPath();
        if (canMove(source, targetRrp)) {
            if (!dryRun) {
                StatusEntry lastError = status.getLastError();
                if (copy) {
                    storageInterceptors.beforeCopy(source, targetRepoPath, status, properties);
                } else {
                    storageInterceptors.beforeMove(source, targetRepoPath, status, properties);
                }
                if (status.getCancelException(lastError) != null) {
                    return;
                }
                targetFolder = shallowCopyDirectory(source, targetRrp);
            }
        } else if (!contains(targetRrp) ||
                targetRrp.getRepo().getJcrFsItem(targetRrp.getRepoPath()).isFile()) {
            // target repo doesn't accept this path and it doesn't already contain it OR the target is a file
            // so there is no point to continue to the children
            status.setWarning("Cannot create/override the path '" + targetRrp.getRepoPath() + "'. " +
                    "Skipping this path and all its children.", log);
            return;
        }

        List<VfsItem> children = source.getItems(!copy);
        RepoPath originalRepoPath = targetRrp.getRepoPath();
        for (VfsItem child : children) {
            // update the cached object with the child's repo path.
            targetRrp = new RepoRepoPath<LocalRepo>(targetRrp.getRepo(),
                    InternalRepoPathFactory.create(originalRepoPath, child.getName()));
            // recursive call with the child
            moveRecursive(child, targetRrp);
        }

        if (shouldRemoveSourceFolder(source)) {
            // folder is empty remove it immediately
            // we don't use folder.delete() as it will move to trash and will fire additional events
            storageInterceptors.afterMove(source, targetFolder, status, properties);
            source.bruteForceDelete();
        } else if (!dryRun && copy) {
            storageInterceptors.afterCopy(source, targetFolder, status, properties);
        }

        //If not containing any children and items have been moved (children have actually been moved)
        if (!dryRun && targetFolder != null && !targetFolder.getRepoPath().isRoot() &&
                targetFolder.list().length == 0 && children.size() != 0) {
            // folder is empty remove it immediately
            // we don't use folder.delete() as it will move to trash and will fire additional events
            targetFolder.bruteForceDelete();
        }
    }

    /**
     * This method copies the source folder to the target folder including the folder metadata, excluding children
     *
     * @param sourceFolder src
     * @param targetRrp    dest
     * @return the new folder dest created
     */
    private VfsFolder shallowCopyDirectory(VfsFolder sourceFolder, RepoRepoPath<LocalRepo> targetRrp) {
        assertNotDryRun();
        LocalRepo targetRepo = targetRrp.getRepo();
        RepoPath targetRepoPath = InternalRepoPathFactory.create(targetRepo.getKey(),
                targetRrp.getRepoPath().getPath());
        VfsFolder targetFolder = (VfsFolder) targetRepo.getLockedJcrFsItem(targetRepoPath);
        if (targetFolder == null) {
            // create target folder
            log.debug("Creating target folder {}", targetRepoPath);
            targetFolder = targetRepo.getLockedJcrFolder(targetRepoPath, true);
            targetFolder.mkdirs();
            status.itemMoved();
        } else {
            log.debug("Target folder {} already exists", targetRepoPath);
        }

        // copy all metadata (except maven-metadata.xml) from source to target
        log.debug("Copying folder metadata to {}", targetRepoPath);
        try {
            Set<MetadataDefinition<?, ?>> metadataDefs = VfsItemFactory.getExistingMetadata(sourceFolder, false);
            for (MetadataDefinition definition : metadataDefs) {
                String metadataName = definition.getMetadataName();
                MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
                if (!MavenNaming.MAVEN_METADATA_NAME.equals(metadataName)) {
                    Object metadata = mdph.read((MetadataAware) sourceFolder);
                    mdph.update((MetadataAware) targetFolder, metadata);
                }
            }
        } catch (RepositoryRuntimeException e) {
            status.setError("Unable to retrieve metadata names for " + sourceFolder.getAbsolutePath() +
                    ". Skipping copy of metadata.", e, log);
        }

        return targetFolder;
    }

    // asynchronously call the maven metadata recalculation

    private void calculateMavenMetadata(RepoRepoPath<LocalRepo> targetRrp, VfsFolder rootFolderToMove) {
        assertNotDryRun();

        LocalRepo targetLocalRepo = targetRrp.getRepo();
        VfsItem fsItem = targetLocalRepo.getLockedJcrFsItem(
                InternalRepoPathFactory.create(targetLocalRepo.getKey(), targetRrp.getRepoPath().getPath()));
        //VfsItem fsItem = getLockedTargetFsItem(targetLocalRepo, rootFolderToMove);
        if (fsItem == null) {
            log.debug("Target item doesn't exist. Skipping maven metadata recalculation.");
            return;
        }
        fsItem.getMutableInfo().setModifiedBy(authorizationService.currentUsername());
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
        VfsFolder rootTargetFolderForMetadataCalculation = rootTargetFolder.getLockedParentFolder() != null ?
                rootTargetFolder.getLockedParentFolder() : rootTargetFolder;
        repositoryService.markBaseForMavenMetadataRecalculation(rootTargetFolderForMetadataCalculation.getRepoPath());
        if (executeMavenMetadataCalculation) {
            repositoryService.calculateMavenMetadataAsync(rootTargetFolderForMetadataCalculation.getRepoPath());
        }

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
