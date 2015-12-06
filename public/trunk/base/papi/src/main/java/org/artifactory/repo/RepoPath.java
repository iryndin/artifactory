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

package org.artifactory.repo;

import org.artifactory.common.Info;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Holds a compound path of a repository key and a path within that repository, separated by a ':'
 */
public interface RepoPath extends Info {
    char REPO_PATH_SEP = ':';
    char ARCHIVE_SEP = '!';
    String REMOTE_CACHE_SUFFIX = "-cache";

    /**
     * @return The repository key
     */
    @Nonnull
    String getRepoKey();

    /**
     * @return The path inside the repository
     */
    String getPath();

    /**
     * @return The full repository path, like "repoKey:path/to"
     */
    String getId();

    /**
     * A path composed of the repository key and path.
     * <pre>
     * repoKey = "key", path = "path/to" returns "key/path/to"
     * repoKey = "key", name = "" returns "key/"
     * </pre>
     *
     * @return A path composed of the repository key and path
     */
    String toPath();

    /**
     * @return The name of the path as if it were a file (the string after the last '/' or '\')
     */
    String getName();

    /**
     * @return The repo path of the parent folder to this path. Null if this is the root path of the repository.
     */
    @Nullable
    RepoPath getParent();

    /**
     * @return True if this repo path is the root path of the repository (i.e., the path part is empty)
     */
    boolean isRoot();

    /**
     * Whether this repo path is a path to a file, rather than a folder.
     *
     * Note that this function does not query Artifactory for this information,
     * but will usually instead just look at the way the path is formatted: if
     * path ends with a '/' character, it is considered a folder, and if not,
     * it is considered a file.
     *
     * @return True if this repo path represents a file
     */
    boolean isFile();

    /**
     * Whether this repo path is a path to a folder, rather than a file.
     *
     * Note that this function does not query Artifactory for this information,
     * but will usually instead just look at the way the path is formatted: if
     * path ends with a '/' character, it is considered a folder, and if not,
     * it is considered a file.
     *
     * @return True if this repo path represents a folder
     */
    boolean isFolder();
}
