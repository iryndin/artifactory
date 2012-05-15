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

/**
 * Holds a compound path of a repository id and a path withing that repository separated by ':'
 */
public interface RepoPath extends Info {
    char REPO_PATH_SEP = ':';
    char ARCHIVE_SEP = '!';

    String getRepoKey();

    String getPath();

    String getId();

    /**
     * @return The name of the path as if it was a file (the string after the last '/' or '\')
     */
    String getName();

    RepoPath getParent();

    boolean isRoot();
}
