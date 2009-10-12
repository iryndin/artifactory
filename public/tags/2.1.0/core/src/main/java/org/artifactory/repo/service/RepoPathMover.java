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

package org.artifactory.repo.service;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MoveMultiStatusHolder;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public MoveMultiStatusHolder moveOrCopy(MoverConfig moverConfig) {
        boolean isDryRun = moverConfig.isDryRun();
        RepoPath fromRepoPath = moverConfig.getFromRepoPath();
        String targetLocalRepoKey = moverConfig.getTargetLocalRepoKey();
        boolean isSearchResult = moverConfig.isSearchResult();

        MoveMultiStatusHolder status = new MoveMultiStatusHolder();
        // don't output to the logger if executing in dry run
        status.setActivateLogging(!isDryRun);

        if (fromRepoPath.getRepoKey().equals(targetLocalRepoKey)) {
            status.setError(String.format("Cannot move %s: Destination and source repositories are the same",
                    fromRepoPath), log);
            return status;
        }

        LocalRepo targetLocalRepo = getLocalRepo(targetLocalRepoKey);
        if (targetLocalRepo.isCache()) {
            throw new IllegalArgumentException("Target repository " + targetLocalRepoKey + " is a cache repository. " +
                    "Moving to cache repositoris is not allowed.");
        }

        LocalRepo sourceRepo = getLocalRepo(fromRepoPath.getRepoKey());
        JcrFsItem fsItemToMove = getRootFsItemToMove(fromRepoPath, sourceRepo);

        // ok start moving
        moveRecursive(targetLocalRepo, fsItemToMove, status, isDryRun, isSearchResult);

        // recalculate maven metadata on affected repositories
        if (!isDryRun) {

            JcrFolder sourceRootFolder;

            if (fsItemToMove.isDirectory()) {
                //If the item is a directory
                JcrFolder fsFolderToMove = (JcrFolder) fsItemToMove;
                if (isSearchResult) {
                    /**
                     * If search results are being handeled, clean up empty folders and return the folder that should be
                     * calculated (parent of last deleted folder)
                     */
                    sourceRootFolder = cleanEmptyFolders(fsFolderToMove);
                } else {
                    //If ordinary artifacts are being handeled, return the source folder to be calculated 
                    sourceRootFolder = fsFolderToMove;
                }
            } else {
                //If the item is a file, just calculate the parent folder
                sourceRootFolder = fsItemToMove.getLockedParentFolder();
            }

            calculateMavenMetadata(targetLocalRepoKey, sourceRootFolder, moverConfig.isCopy(), isDryRun,
                    moverConfig.isExecuteMavenMetadataCalculation());
        }
        return status;
    }

    private void moveRecursive(LocalRepo targetRepo, JcrFsItem item, MoveMultiStatusHolder status, boolean dryRun,
            boolean isSearchResult) {
        if (item.isDirectory()) {
            JcrFolder folder = (JcrFolder) item;
            JcrFolder targetFolder = null;
            if (canMove(targetRepo, folder, status)) {
                if (!dryRun) {
                    targetFolder = shallowCopyDirectory(targetRepo, folder, dryRun, status);
                }
            } else if (!contains(targetRepo, folder)) {
                // target repo doesn't accept this path and it doesn't already contain it
                // so there is no point to continue to the children
                status.setWarning("Cannot create/override the path '" + folder.getRelativePath() + "'. " +
                        "Skipping this path and all its children.", log);
                return;
            }

            List<JcrFsItem> children = jcrRepoService.getChildren(folder, true);
            for (JcrFsItem child : children) {
                // recursive call with the child
                moveRecursive(targetRepo, child, status, dryRun, isSearchResult);
            }

            //If all files were moved from the source directory (release vs. snapshot, etc') remove folder
            if (!isSearchResult && (folder != null) && (folder.list().length == 0) && (status.getMovedCount() != 0)) {
                // folder is empty remove it immediately
                // we don't use folder.delete() as it will move to trash and will fire additional events
                folder.bruteForceDelete();
            }

            //If no files were moved to the target directory (release vs. snapshot, etc') remove newly created folder
            if ((targetFolder != null) && (targetFolder.list().length == 0)) {
                // folder is empty remove it immediately
                // we don't use folder.delete() as it will move to trash and will fire additional events
                targetFolder.bruteForceDelete();
            }
        } else {
            if (canMove(targetRepo, item, status)) {
                if (!dryRun) {
                    moveFile(targetRepo, (JcrFile) item, dryRun, status);
                } else {
                    status.itemMoved();
                }
            }
        }
    }

    private void moveFile(LocalRepo targetRepo, JcrFile sourceFile, boolean dryRun, MoveMultiStatusHolder status) {
        assertNotDryRun(dryRun);
        if (contains(targetRepo, sourceFile)) {
            // target repository already contains file with the same name, delete it
            log.debug("File {} already exists in target repository. Overriding.", sourceFile.getRelativePath());
            JcrFsItem existingTargetFile = getLockedTargetFsItem(targetRepo, sourceFile);
            existingTargetFile.bruteForceDelete();
        } else {
            // make sure parent directories exist
            RepoPath targetParentRepoPath = new RepoPath(targetRepo.getKey(), sourceFile.getParentRepoPath().getPath());
            new JcrFolder(targetParentRepoPath, targetRepo).mkdirs();
        }

        RepoPath targetRepoPath = new RepoPath(targetRepo.getKey(), sourceFile.getRepoPath().getPath());
        String sourceAbsPath = JcrPath.get().getAbsolutePath(sourceFile.getRepoPath());
        String targetAbsPath = JcrPath.get().getAbsolutePath(targetRepoPath);
        log.debug("Moving file from {} to {}", sourceAbsPath, targetAbsPath);
        jcrService.move(sourceAbsPath, targetAbsPath);
        status.itemMoved();

        // mark the moved source file as deleted and remove it from the cache
        sourceFile.setDeleted(true);
        sourceFile.updateCache();
    }

    // this method will just copy the source folder to the target folder excluding children
    private JcrFolder shallowCopyDirectory(LocalRepo targetRepo, JcrFolder sourceFolder, boolean dryRun,
            MoveMultiStatusHolder status) {
        assertNotDryRun(dryRun);
        RepoPath targetRepoPath = new RepoPath(targetRepo.getKey(), sourceFolder.getRepoPath().getPath());
        JcrFolder targetFolder = (JcrFolder) getLockedTargetFsItem(targetRepo, sourceFolder);
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
        List<String> metadataNames = sourceFolder.getXmlMetadataNames();
        for (String metadataName : metadataNames) {
            if (!MavenNaming.MAVEN_METADATA_NAME.equals(metadataName)) {
                //sourceFolder.getMetadataInfo()
                String metadata = sourceFolder.getXmlMetdata(metadataName);
                targetFolder.setXmlMetadata(metadataName, metadata);
            }
        }

        return targetFolder;
    }

    // asynchronously call the maven metadata recalculation
    private void calculateMavenMetadata(String targetLocalRepoKey, JcrFolder rootFolderToMove, boolean copy,
            boolean dryRun, boolean executeMaveneMetadataCalculation) {
        assertNotDryRun(dryRun);

        LocalRepo targetLocalRepo = getLocalRepo(targetLocalRepoKey);
        JcrFolder rootTargetFolder = (JcrFolder) getLockedTargetFsItem(targetLocalRepo, rootFolderToMove);
        if (rootTargetFolder == null) {
            //  target repository doesn't exist which means nothing was moved (maybe permissions issue)
            log.debug("Target root folder doesn't exist. Skipping maven metadata recalculation.");
            return;
        }

        // always recalculate the target repository. start with the parent of the moved folder in the target
        JcrFolder rootTargetFolderForMatadataCalculation = rootTargetFolder.getLockedParentFolder() != null ?
                rootTargetFolder.getLockedParentFolder() : rootTargetFolder;
        repositoryService.markBaseForMavenMetadataRecalculation(rootTargetFolderForMatadataCalculation.getRepoPath());
        if (executeMaveneMetadataCalculation) {
            repositoryService.calculateMavenMetadata(rootTargetFolderForMatadataCalculation.getRepoPath());
        }

        // recalculate the source repository only if it's not a cache repo and not copy
        StoringRepo sourceRepo = rootFolderToMove.getRepo();
        if (!copy && !sourceRepo.isCache() && rootFolderToMove.getLockedParentFolder() != null) {
            JcrFolder sourceFolderMetadata = rootFolderToMove.getLockedParentFolder();
            repositoryService.markBaseForMavenMetadataRecalculation(sourceFolderMetadata.getRepoPath());
            if (executeMaveneMetadataCalculation) {
                repositoryService.calculateMavenMetadata(sourceFolderMetadata.getRepoPath());
            }
        }
    }

    private boolean contains(LocalRepo targetRepo, JcrFsItem item) {
        return targetRepo.itemExists(item.getRepoPath().getPath());
    }

    private JcrFsItem getLockedTargetFsItem(LocalRepo targetRepo, JcrFsItem item) {
        RepoPath targetRepoPath = new RepoPath(targetRepo.getKey(), item.getRepoPath().getPath());
        return targetRepo.getLockedJcrFsItem(targetRepoPath);
    }

    private boolean canMove(LocalRepo targetRepo, JcrFsItem item, MoveMultiStatusHolder status) {
        RepoPath sourceRepoPath = item.getRepoPath();
        RepoPath targetRepoPath = new RepoPath(targetRepo.getKey(), item.getRelativePath());
        String targetPath = targetRepoPath.getPath();
        // snapshot/release policy is enfored only on files since it only has a meaning on files
        if (item.isFile() && !targetRepo.handles(targetPath)) {
            status.setWarning(
                    "The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath +
                            "' due to its snapshot/release handling policy.", log);
            return false;
        }

        if (!targetRepo.accepts(targetPath)) {
            status.setWarning(
                    "The repository '" + targetRepo.getKey() + "' rejected the path '" + targetPath +
                            "' due to its include/exclude patterns.", log);
            return false;
        }

        // permission checks
        if (!authorizationService.canDelete(sourceRepoPath)) {
            status.setWarning("User doesn't have permissions to move '" + sourceRepoPath + "'. " +
                    "Needs delete permissions.", log);
            return false;
        }

        if (contains(targetRepo, item)) {
            if (!authorizationService.canDelete(targetRepoPath)) {
                status.setWarning("User doesn't have permissions to override '" + targetRepoPath + "'. " +
                        "Needs delete permissions.", log);
                return false;
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
     * @return Parent of highest removed folder
     */
    private JcrFolder cleanEmptyFolders(JcrFolder sourceFolder) {
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
                sourceFolder.bruteForceDelete();
                toReturn = parent;
            }
        }

        return toReturn;
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