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

package org.artifactory.storage.fs.service;

import org.artifactory.repo.RepoPath;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * A business service to interact with the tasks table.
 *
 * @author Yossi Shaul
 */
public interface TasksService {

    /**
     * Unique name of the archive indexing task type
     */
    String TASK_TYPE_INDEX = "INDEX";

    /**
     * Unique name of the maven metadata calculation task type
     */
    String TASK_TYPE_MAVEN_METADATA = "MAVEN_METADATA_CALC";

    /**
     * @return All the repo paths currently pending for indexing.
     */
    @Nonnull
    Set<RepoPath> getIndexTasks();

    /**
     * @param repoPath The repo path to check
     * @return True if there is a pending index request for this checksum
     */
    boolean hasIndexTask(RepoPath repoPath);

    /**
     * Adds an index task for the given repo path.
     *
     * @param repoPath The repo path to index
     */
    void addIndexTask(RepoPath repoPath);

    /**
     * Removes an index task.
     *
     * @param repoPath The repo path to remove
     * @return True if removed from the database.
     */
    boolean removeIndexTask(RepoPath repoPath);

    /**
     * @return All the repo paths currently pending for maven metadata calculation.
     */
    @Nonnull
    Set<RepoPath> getMavenMetadataCalculationTasks();

    /**
     * @param repoPath The repo path to check
     * @return True if there is a pending index request for this checksum
     */
    //boolean hasMavenMetadataCalculationTask(RepoPath repoPath);

    /**
     * Adds an maven metadata calculation task for the given repo path.
     *
     * @param repoPath The repo path to index
     */
    void addMavenMetadataCalculationTask(RepoPath repoPath);

    /**
     * Removes a maven metadata calculation task.
     *
     * @param repoPath The repo path to remove
     * @return True if removed from the database.
     */
    boolean removeMavenMetadataCalculationTask(RepoPath repoPath);
}
