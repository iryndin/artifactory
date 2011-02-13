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
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.api.repo.Request;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoRepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.util.RepoLayoutUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private InternalRepositoryService repositoryService;

    @Request(aggregateEventsByTimeWindow = true)
    public MoveMultiStatusHolder moveOrCopy(MoverConfig moverConfig) {
        boolean isDryRun = moverConfig.isDryRun();
        RepoPath fromRepoPath = moverConfig.getFromRepoPath();

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
            targetLocalRepoPath = new RepoPathImpl(targetRepoKey, fromRepoPath.getPath());
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

        boolean canCrossLayouts = !moverConfig.isSuppressLayouts() &&
                RepoLayoutUtils.canCrossLayouts(sourceRepo.getDescriptor().getRepoLayout(),
                        targetLocalRepo.getDescriptor().getRepoLayout());

        MoveMultiStatusHolder resultStatus;
        if (canCrossLayouts) {
            resultStatus = new CrossLayoutRepoPathMover(moverConfig).moveOrCopy(sourceRepo, targetLocalRepo,
                    fsItemToMove);
        } else {
            resultStatus = new DefaultRepoPathMover(moverConfig).moveOrCopy(fsItemToMove, targetLocalRepo,
                    targetLocalRepoPath, targetRrp);
        }

        status.merge(resultStatus);
        return status;
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
}
