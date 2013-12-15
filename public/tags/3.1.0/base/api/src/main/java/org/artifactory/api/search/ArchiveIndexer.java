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

package org.artifactory.api.search;

import org.artifactory.api.repo.Async;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.Lock;

import javax.annotation.Nullable;

/**
 * The archive indexer manages the indexes task queue and performs archives indexing.
 *
 * @author Yossi Shaul
 */
public interface ArchiveIndexer {

    @Async(delayUntilAfterCommit = true)
    void asyncIndex(RepoPath archiveRepoPath);

    /**
     * Indexes all the archives that were marked
     */
    @Async(delayUntilAfterCommit = true)
    void asyncIndexMarkedArchives();

    /**
     * Adds the given repo path to the archive indexing queue.
     */
    @Lock
    void markArchiveForIndexing(RepoPath searchPath);

    /**
     * Recursively adds the given repo path to the archive indexing queue.
     *
     * @param Base          repository path to start indexing from
     * @param indexAllRepos If true ignores the base repo path and index all the local/cache repositories
     */
    @Async(delayUntilAfterCommit = true)
    void recursiveMarkArchivesForIndexing(@Nullable RepoPath baseRepoPath, boolean indexAllRepos);

    /**
     * @param repoPath The repo path to check
     * @return True if the binary file on this repo path is already indexed. False if doesn't exist of not indexed
     */
    boolean isIndexed(RepoPath repoPath);

    /**
     * @param sha1 The sha1 of the binary to check
     * @return True if the binary file for this checksum is already indexed. False if doesn't exist of not indexed
     */
    boolean isIndexed(String sha1);
}
