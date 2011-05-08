/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.descriptor.repo.RepoLayout;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.util.RepoLayoutUtils;

import java.util.List;

/**
 * The cross-layout repo-path move/copy implementation
 *
 * @author Noam Y. Tenne
 */
class CrossLayoutRepoPathMover extends BaseRepoPathMover {

    private RepoPath lowestCommonTargetParent = null;
    private RepoLayout sourceLayout;
    private RepoLayout targetLayout;
    private boolean isSourceM2;
    private boolean isTargetM2;

    CrossLayoutRepoPathMover(MoveMultiStatusHolder status, MoverConfig moverConfig) {
        super(status, moverConfig);
    }

    void moveOrCopy(LocalRepo sourceRepo, LocalRepo targetRepo, JcrFsItem fsItemToMove) {

        sourceLayout = sourceRepo.getDescriptor().getRepoLayout();
        targetLayout = targetRepo.getDescriptor().getRepoLayout();
        isSourceM2 = RepoLayoutUtils.isDefaultM2(sourceLayout);
        isTargetM2 = RepoLayoutUtils.isDefaultM2(targetLayout);

        moveRecursive(fsItemToMove, targetRepo);

        clearEmptyDirsAndCalcMetadata(new RepoRepoPath<LocalRepo>(targetRepo, lowestCommonTargetParent), fsItemToMove);
    }

    @Override
    protected boolean calcMetadata() {
        return isSourceM2 || isTargetM2;
    }

    @Override
    protected boolean calcMetadataOnSource() {
        return isSourceM2;
    }

    @Override
    protected boolean calcMetadataOnTarget() {
        return isTargetM2 && (lowestCommonTargetParent != null);
    }

    private void moveRecursive(JcrFsItem source, LocalRepo targetRepo) {

        if (errorsOrWarningsOccurredAndFailFast()) {
            return;
        }

        if (source.isDirectory()) {
            JcrFolder sourceFolder = (JcrFolder) source;

            List<JcrFsItem> children = jcrRepoService.getChildren(sourceFolder, true);
            for (JcrFsItem child : children) {

                moveRecursive(child, targetRepo);
            }

            if (shouldRemoveSourceFolder(sourceFolder)) {
                // folder is empty remove it immediately
                // we don't use folder.delete() as it will move to trash and will fire additional events
                sourceFolder.bruteForceDelete();
            }
        } else {
            // the source is a file

            MultiStatusHolder translateStatus = new MultiStatusHolder();
            String translatedTargetPath = ModuleInfoUtils.translateArtifactPath(sourceLayout, targetLayout,
                    source.getRelativePath(), translateStatus);

            status.merge(translateStatus);

            if (errorsOrWarningsOccurredAndFailFast()) {
                return;
            }

            RepoPath targetRepoPath = targetRepo.getRepoPath(translatedTargetPath);

            RepoRepoPath<LocalRepo> targetRrp = new RepoRepoPath<LocalRepo>(targetRepo, targetRepoPath);

            int countBeforeFileHandling = status.getMovedCount();
            handleFile(source, targetRrp);
            int countAfterFileHandling = status.getMovedCount();

            /**
             * Since we move only artifacts and not folders like the default move\copy, we need to find the lowest
             * common parent of all the files we moved
             */
            if (isTargetM2 && !dryRun && !translateStatus.hasWarnings() &&
                    countBeforeFileHandling < countAfterFileHandling) {
                if (lowestCommonTargetParent == null) {
                    lowestCommonTargetParent = targetRrp.getRepoPath();
                } else {

                    while (!lowestCommonTargetParent.isRoot() && !targetRepoPath.isRoot() &&
                            !translatedTargetPath.startsWith(lowestCommonTargetParent.getPath())) {
                        lowestCommonTargetParent = lowestCommonTargetParent.getParent();
                    }
                }
            }
        }
    }
}
