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

package org.artifactory.repo.service;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.Request;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataPersistenceHandler;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.interceptor.RepoInterceptors;
import org.artifactory.repo.jcr.StoringRepo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Set;

/**
 * Moves a folder repo path from one local repository to another non-cache local repository. Only files and folders that
 * are accepted by the target repository will be moved. This class is stateless.
 *
 * @author Yossi Shaul
 */
@Component
public class RepoPathMover {
    private final static Logger log = LoggerFactory.getLogger(RepoPathMover.class);

    @Autowired
    private JcrRepoService jcrRepoService;

    @Autowired
    private JcrService jcrService;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private RepoInterceptors repoInterceptors;

    @Request(aggregateEventsByTimeWindow = true)
    public MoveMultiStatusHolder moveOrCopy(MoverConfig moverConfig) {
        boolean isDryRun = moverConfig.isDryRun();
        RepoPath fromRepoPath = moverConfig.getFromRepoPath();
        boolean isSearchResult = moverConfig.isSearchResult();
        boolean isCopy = moverConfig.isCopy();

        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        // don't output to the logger if executing in dry run
        status.setActivateLogging(!isDryRun);

        LocalRepo sourceRepo = getLocalRepo(fromRepoPath.getRepoKey());
        JcrFsItem fsItemToMove = getRootFsItemToMove(fromRepoPath, sourceRepo);

        RepoPath targetLocalRepoPath = moverConfig.getTargetLocalRepoPath();
        if (targetLocalRepoPath == null) {
            // when using repo key, we move to the target repository to the same hierarchy, in such a case the
            // target repo path is the parent of the source path (think of it as regular file system copy/move)
            String targetRepoKey = moverConfig.getTargetLocalRepoKey();
            targetLocalRepoPath = new RepoPath(targetRepoKey, fromRepoPath.getPath());
        }

        if (fromRepoPath.equals(targetLocalRepoPath)) {
            status.setError(String.format("Cannot move\\copy %s: Destination and source are the same",
                    fromRepoPath), log);
            return status;
        }

        RepoRepoPath<LocalRepo> targetRrp = repositoryService.getRepoRepoPath(targetLocalRepoPath);
        LocalRepo targetLocalRepo = getLocalRepo(targetLocalRepoPath.getRepoKey());
        if (targetLocalRepo.isCache()) {
            throw new IllegalArgumentException(
                    "Target repository " + targetLocalRepoPath.getRepoKey() + " is a cache " +
                            "repository. Moving\\copying to cache repositories is not allowed.");
        }

        // if the target is a directory and it exists we move/copy the source UNDER the target directory (ie, we don't
        // replace it). We do it only if the target repo key is null and the target fs item is a directory. 
        JcrFsItem targetFsItem = targetLocalRepo.getLockedJcrFsItem(targetLocalRepoPath);
        if (targetFsItem != null && targetFsItem.isDirectory() && moverConfig.getTargetLocalRepoKey() == null) {
            String adjustedPath = targetLocalRepoPath.getPath() + "/" + fsItemToMove.getName();
            targetRrp = new RepoRepoPath<LocalRepo>(targetLocalRepo,
                    new RepoPath(targetLocalRepo.getKey(), adjustedPath));

        }

        // ok start moving
        moveRecursive(fsItemToMove, targetRrp, status, isDryRun, isSearchResult, isCopy);

        // recalculate maven metadata on affected repositories
        if (!isDryRun) {
            JcrFolder sourceRootFolder;
            if (fsItemToMove.isDirectory()) {
                //If the item is a directory
                JcrFolder fsFolderToMove = (JcrFolder) fsItemToMove;
                if (isSearchResult && !isCopy) {
                    /**
                     * If search results are being handled, clean up empty folders and return the folder that should be
                     * calculated (parent of last deleted folder)
                     */
                    sourceRootFolder = cleanEmptyFolders(fsFolderToMove, status);
                } else {
                    //If ordinary artifacts are being handled, return the source folder to be calculated
                    sourceRootFolder = fsFolderToMove;
                }
            } else {
                //If the item is a file, just calculate the parent folder
                sourceRootFolder = fsItemToMove.getLockedParentFolder();
            }

            calculateMavenMetadata(targetRrp, sourceRootFolder, isCopy, isDryRun,
                    moverConfig.isExecuteMavenMetadataCalculation());
            updateLastModifiedBy(targetLocalRepoPath);
        }
        return status;
    }

    private void moveRecursive(JcrFsItem source, RepoRepoPath<LocalRepo> targetRrp,
            MoveMultiStatusHolder status, boolean dryRun, boolean isSearchResult, boolean isCopy) {
        if (source.isDirectory()) {
            JcrFolder sourceFolder = (JcrFolder) source;
            JcrFolder targetFolder = null;
            if (canMove(sourceFolder, targetRrp, status, isCopy)) {
                if (!dryRun) {
                    targetFolder = shallowCopyDirectory(sourceFolder, targetRrp, dryRun, status);
                }
            } else if (!contains(targetRrp) ||
                    targetRrp.getRepo().getJcrFsItem(targetRrp.getRepoPath()).isFile()) {
                // target repo doesn't accept this path and it doesn't already contain it OR the target is a file
                // so there is no point to continue to the children
                status.setWarning("Cannot create/override the path '" + targetRrp.getRepoPath() + "'. " +
                        "Skipping this path and all its children.", log);
                return;
            }

            List<JcrFsItem> children = jcrRepoService.getChildren(sourceFolder, true);
            RepoPath originalRepoPath = targetRrp.getRepoPath();
            for (JcrFsItem child : children) {
                // update the cached object with the child's repo path.
                targetRrp = new RepoRepoPath<LocalRepo>(targetRrp.getRepo(),
                        new RepoPath(originalRepoPath, child.getName()));
                // recursive call with the child
                moveRecursive(child, targetRrp, status, dryRun, isSearchResult, isCopy);
            }

            /**
             * - If not handling search results (search result folders are cleaned at a later stage)
             * - If not copying (no source removal when copying)
             * - If not containing any children and items have been moved (children have actually been moved)
             */
            if (!isSearchResult && !isCopy && (sourceFolder.list().length == 0) && (status.getMovedCount() != 0)) {
                // folder is empty remove it immediately
                // we don't use folder.delete() as it will move to trash and will fire additional events
                repoInterceptors.onMove(sourceFolder, targetFolder, status);
                sourceFolder.bruteForceDelete();
            }

            //If not containing any children and items have been moved (children have actually been moved)
            if ((targetFolder != null) && (targetFolder.list().length == 0) && (children.size() != 0)) {
                // folder is empty remove it immediately
                // we don't use folder.delete() as it will move to trash and will fire additional events
                targetFolder.bruteForceDelete();
            }
        } else {
            // the source is a file
            if (canMove(source, targetRrp, status, isCopy)) {
                if (!dryRun) {
                    moveFile(targetRrp, (JcrFile) source, dryRun, status, isCopy);
                } else {
                    status.itemMoved();
                }
            }
        }
    }

    private void updateLastModifiedBy(RepoPath path) {
        if (repositoryService.exists(path)) {
            ItemInfo itemInfo = repositoryService.getItemInfo(path);
            itemInfo.setModifiedBy(authorizationService.currentUsername());
        }
    }

    private void moveFile(RepoRepoPath<LocalRepo> targetRrp, JcrFile sourceFile, boolean dryRun,
            MoveMultiStatusHolder status,
            boolean isCopy) {
        assertNotDryRun(dryRun);
        LocalRepo targetRepo = targetRrp.getRepo();
        if (contains(targetRrp)) {
            // target repository already contains file with the same name, delete it
            log.debug("File {} already exists in target repository. Overriding.", targetRrp.getRepoPath().getPath());
            JcrFsItem existingTargetFile = targetRepo.getLockedJcrFsItem(targetRrp.getRepoPath().getPath());
            existingTargetFile.bruteForceDelete();
        } else {
            // make sure parent directories exist
            RepoPath targetParentRepoPath = new RepoPath(targetRepo.getKey(),
                    targetRrp.getRepoPath().getParent().getPath());
            new JcrFolder(targetParentRepoPath, targetRepo).mkdirs();
        }

        RepoPath targetRepoPath = targetRrp.getRepoPath();
        JcrFile targetJcrFile = new JcrFile(targetRepoPath, targetRepo);
        String sourceAbsPath = JcrPath.get().getAbsolutePath(sourceFile.getRepoPath());
        String targetAbsPath = JcrPath.get().getAbsolutePath(targetRepoPath);

        if (isCopy) {
            //Important - do, otherwise target folders aren't found by the workspace yet
            jcrService.getManagedSession().save();
            log.debug("Copying file {} to {}", sourceAbsPath, targetAbsPath);
            jcrService.copy(sourceAbsPath, targetAbsPath);
            repoInterceptors.onCopy(sourceFile, targetJcrFile, status);
        } else {
            log.debug("Moving file from {} to {}", sourceAbsPath, targetAbsPath);
            jcrService.move(sourceAbsPath, targetAbsPath);
            repoInterceptors.onMove(sourceFile, targetJcrFile, status);
            // mark the moved source file as deleted and remove it from the cache
            sourceFile.setDeleted(true);
            sourceFile.updateCache();
        }
        status.itemMoved();
    }

    // this method will just copy the source folder to the target folder excluding children

    private JcrFolder shallowCopyDirectory(JcrFolder sourceFolder, RepoRepoPath<LocalRepo> targetRrp,
            boolean dryRun, MoveMultiStatusHolder status) {
        assertNotDryRun(dryRun);
        LocalRepo targetRepo = targetRrp.getRepo();
        RepoPath targetRepoPath = new RepoPath(targetRepo.getKey(), targetRrp.getRepoPath().getPath());
        JcrFolder targetFolder = (JcrFolder) targetRepo.getLockedJcrFsItem(targetRepoPath);
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
            Set<MetadataDefinition<?>> metadataDefs = sourceFolder.getExistingMetadata(false);
            for (MetadataDefinition definition : metadataDefs) {
                String metadataName = definition.getMetadataName();
                MetadataPersistenceHandler mdph = definition.getPersistenceHandler();
                if (!MavenNaming.MAVEN_METADATA_NAME.equals(metadataName)) {
                    Object metadata = mdph.read(sourceFolder);
                    mdph.update(targetFolder, metadata);
                }
            }
        } catch (RepositoryException e) {
            status.setError("Unable to retrieve metadata names for " + sourceFolder.getAbsolutePath() +
                    ". Skipping copy of metadata.", e, log);
        }

        return targetFolder;
    }

    // asynchronously call the maven metadata recalculation

    private void calculateMavenMetadata(RepoRepoPath<LocalRepo> targetRrp, JcrFolder rootFolderToMove, boolean copy,
            boolean dryRun, boolean executeMavenMetadataCalculation) {
        assertNotDryRun(dryRun);

        LocalRepo targetLocalRepo = targetRrp.getRepo();
        JcrFsItem fsItem = targetLocalRepo.getJcrFsItem(
                new RepoPath(targetLocalRepo.getKey(), targetRrp.getRepoPath().getPath()));
        //JcrFsItem fsItem = getLockedTargetFsItem(targetLocalRepo, rootFolderToMove);
        if (fsItem == null) {
            log.debug("Target item doesn't exist. Skipping maven metadata recalculation.");
            return;
        }
        JcrFolder rootTargetFolder;
        if (fsItem.isDirectory()) {
            rootTargetFolder = (JcrFolder) fsItem;
        } else {
            rootTargetFolder = fsItem.getParentFolder();
        }
        if (rootTargetFolder == null) {
            //  target repository doesn't exist which means nothing was moved (maybe permissions issue)
            log.debug("Target root folder doesn't exist. Skipping maven metadata recalculation.");
            return;
        }

        // always recalculate the target repository. start with the parent of the moved folder in the target
        JcrFolder rootTargetFolderForMatadataCalculation = rootTargetFolder.getLockedParentFolder() != null ?
                rootTargetFolder.getLockedParentFolder() : rootTargetFolder;
        repositoryService.markBaseForMavenMetadataRecalculation(rootTargetFolderForMatadataCalculation.getRepoPath());
        if (executeMavenMetadataCalculation) {
            repositoryService.calculateMavenMetadataAsync(rootTargetFolderForMatadataCalculation.getRepoPath());
        }

        // recalculate the source repository only if it's not a cache repo and not copy
        StoringRepo sourceRepo = rootFolderToMove.getRepo();
        if (!copy && !sourceRepo.isCache() && rootFolderToMove.getLockedParentFolder() != null) {
            JcrFolder sourceFolderMetadata = rootFolderToMove.getLockedParentFolder();
            repositoryService.markBaseForMavenMetadataRecalculation(sourceFolderMetadata.getRepoPath());
            if (executeMavenMetadataCalculation) {
                repositoryService.calculateMavenMetadataAsync(sourceFolderMetadata.getRepoPath());
            }
        }
    }

    private JcrFsItem getLockedTargetFsItem(LocalRepo targetRepo, JcrFsItem item) {
        RepoPath targetRepoPath = new RepoPath(targetRepo.getKey(), item.getRepoPath().getPath());
        return targetRepo.getLockedJcrFsItem(targetRepoPath);
    }

    private boolean canMove(JcrFsItem source, RepoRepoPath<LocalRepo> targetRrp, MoveMultiStatusHolder status,
            boolean isCopy) {
        RepoPath sourceRepoPath = source.getRepoPath();
        RepoPath targetRepoPath = targetRrp.getRepoPath();
        String targetPath = targetRepoPath.getPath();
        // snapshot/release policy is enforced only on files since it only has a meaning on files
        LocalRepo targetRepo = targetRrp.getRepo();
        if (source.isFile() && !targetRepo.handles(targetPath)) {
            status.setWarning("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath +
                    "' due to its snapshot/release handling policy.", log);
            return false;
        }

        if (!targetRepo.accepts(targetRepoPath)) {
            status.setWarning("The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath +
                    "' due to its include/exclude patterns.", log);
            return false;
        }

        // permission checks
        if (!isCopy && !authorizationService.canDelete(sourceRepoPath)) {
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
                JcrFsItem targetFsItem = targetRepo.getLockedJcrFsItem(targetRepoPath);
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

        // all tests passed
        return true;
    }

    private LocalRepo getLocalRepo(String repoKey) {
        LocalRepo targetLocalRepo = repositoryService.localOrCachedRepositoryByKey(repoKey);
        if (targetLocalRepo == null) {
            throw new IllegalArgumentException("Local repository " + repoKey +
                    " not found (repository is not local or doesn't exist)");
        }
        return targetLocalRepo;
    }

    private JcrFsItem getRootFsItemToMove(RepoPath fromRepoPath, LocalRepo fromRepo) {
        JcrFsItem rootPathToMove = fromRepo.getLockedJcrFsItem(fromRepoPath);
        if (rootPathToMove == null) {
            throw new IllegalArgumentException("Could not find folder at " + fromRepoPath);
        }
        return rootPathToMove;
    }

    private void assertNotDryRun(boolean dryRun) {
        if (dryRun) {
            throw new IllegalStateException("Method call is not allowed in dry run");
        }
    }

    /**
     * Cleans the empty folders of the upper hierarchy, starting from the given folder
     *
     * @param sourceFolder Folder to start clean up at
     * @param status       MoveMultiStatusHolder
     * @return Parent of highest removed folder
     */
    private JcrFolder cleanEmptyFolders(JcrFolder sourceFolder, MoveMultiStatusHolder status) {
        JcrFolder toReturn = null;
        while (toReturn == null) {
            JcrFolder parent = sourceFolder.getLockedParentFolder();
            boolean parentIsRepo = StringUtils.isBlank(parent.getRepoPath().getPath());

            if (!parentIsRepo && hasNoSiblings(parent.getAbsolutePath())) {
                /**
                 * If the current item is a search result, the parent of the current folder isn't the repo node and
                 * the current folder has no siblings, go up
                 */
                sourceFolder = parent;
            } else {
                //Remove current folder, return the parent
                repoInterceptors.onDelete(sourceFolder, status);
                sourceFolder.bruteForceDelete();
                toReturn = parent;
            }
        }

        return toReturn;
    }

    private boolean contains(RepoRepoPath<LocalRepo> rrp) {
        return rrp.getRepo().itemExists(rrp.getRepoPath().getPath());
    }

    /**
     * Indicates if the current source folder has no sibling nodes
     *
     * @param parentAbsPath Absolute path of source folder parent
     * @return True if the current source folder has no siblings, false if not
     */
    private boolean hasNoSiblings(String parentAbsPath) {
        //Important: Make sure to get the child count in a non-locking way
        return (jcrRepoService.getChildrenNames(parentAbsPath).size() == 1);
    }
}