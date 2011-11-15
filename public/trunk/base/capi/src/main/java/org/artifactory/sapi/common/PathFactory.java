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

package org.artifactory.sapi.common;

import org.artifactory.repo.RepoPath;

import java.io.File;

/**
 * Date: 8/4/11
 * Time: 11:38 AM
 *
 * @author Fred Simon
 */
public interface PathFactory {
    PathBuilder createBuilder(String root);

    String escape(String pathElement);

    String unEscape(String storagePathElement);

    String getAllRepoRootPath();

    String getConfigurationRootPath();

    String getTrashRootPath();

    String getLogsRootPath();

    String getBuildsRootPath();

    String getLogPath(String logKey);

    String getRepoRootPath(String repoKey);

    String repoKeyFromPath(String absPath);

    String getConfigPath(String configKey);

    String getBuildsPath(String buildsKey);

    File getRepositoriesExportDir(File exportDir);

    File getRepoExportDir(File exportDir, String repoKey);

    String getAbsolutePath(RepoPath repoPath);

    RepoPath getRepoPath(String absPath);
}
