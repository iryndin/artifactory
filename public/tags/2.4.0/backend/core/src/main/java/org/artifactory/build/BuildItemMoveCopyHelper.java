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

package org.artifactory.build;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.common.StatusEntry;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.jfrog.build.api.Build;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * A helper class for moving or copying build artifacts and dependencies
 *
 * @author Noam Y. Tenne
 * @deprecated Use {@link org.artifactory.build.BuildPromotionHelper} instead
 */
@Deprecated
public class BuildItemMoveCopyHelper extends BaseBuildPromoter {
    private static final Logger log = LoggerFactory.getLogger(BuildItemMoveCopyHelper.class);

    /**
     * Move or copy build artifacts and\or dependencies
     *
     * @param move          True if the items should be moved. False if they should be copied
     * @param buildRun      Basic info of the selected build
     * @param targetRepoKey Key of target repository to move to
     * @param artifacts     True if the build artifacts should be moved\copied
     * @param dependencies  True if the build dependencies should be moved\copied
     * @param scopes        Scopes of dependencies to copy (agnostic if null or empty)
     * @param properties    The properties to tag the copied or move artifacts on their <b>destination</b> path
     * @param dryRun        True if the action should run dry (simulate)
     * @return Result of action
     */
    public MoveCopyResult moveOrCopy(boolean move, BuildRun buildRun, String targetRepoKey,
            boolean artifacts, boolean dependencies, List<String> scopes, Properties properties, boolean dryRun) {
        Build build = getBuild(buildRun);

        assertRepoExists(targetRepoKey);

        MultiStatusHolder statusHolder = new MultiStatusHolder();
        Set<RepoPath> itemsToMove = collectItems(build, artifacts, dependencies, scopes, false, false, statusHolder);

        if (!itemsToMove.isEmpty()) {
            if (move) {
                try {
                    statusHolder.merge(move(itemsToMove, targetRepoKey, dryRun, false));
                } catch (Exception e) {
                    statusHolder.setError("Error occurred while moving: " + e.getMessage(), e, log);
                }
            } else {
                try {
                    statusHolder.merge(copy(itemsToMove, targetRepoKey, dryRun, false));
                } catch (Exception e) {
                    statusHolder.setError("Error occurred while copying: " + e.getMessage(), e, log);
                }
            }

            if ((properties != null) && (!properties.isEmpty())) {
                //Rescan after action might have been taken
                Set<RepoPath> itemsToTag = collectItems(build, artifacts, dependencies, scopes, false, false,
                        statusHolder);

                if (!itemsToTag.isEmpty()) {
                    tagBuildItemsWithProperties(itemsToTag, properties, false, dryRun, statusHolder);
                }
            }
        }


        MoveCopyResult result = new MoveCopyResult();
        appendMessages(result, statusHolder);
        return result;
    }

    /**
     * Append the status holder messages to the move copy result
     *
     * @param result            Result to append to
     * @param multiStatusHolder Status holder to copy from
     */
    private void appendMessages(MoveCopyResult result, MultiStatusHolder multiStatusHolder) {
        for (StatusEntry statusEntry : multiStatusHolder.getAllEntries()) {
            result.messages.add(new MoveCopyResult.MoveCopyMessages(statusEntry));
        }
    }
}