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

package org.artifactory.jcr.factory;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.sapi.common.PathBuilder;
import org.artifactory.sapi.common.PathFactory;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.io.File;

/**
 * This singleton is responsible for providing the path structure in the JCR DB, and for converting RepoPath to absPath
 * (in JCR) and vice-versa.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class JcrPathFactory implements PathFactory {

    private static final Logger log = LoggerFactory.getLogger(JcrPathFactory.class);

    protected static final String REPOS_FOLDER = "repositories";
    protected static final String CONFIGURATION_FOLDER = "configuration";
    protected static final String TRASH_FOLDER = "trash";
    protected static final String LOGS_FOLDER = "logs";
    protected static final String BUILDS_FOLDER = "builds";

    public JcrPathFactory() {
    }

    public PathBuilder createBuilder(String root) {
        return new PathBuilderImpl(root);
    }

    public String escape(String pathElement) {
        return Text.escapeIllegalJcrChars(pathElement);
    }

    public String unEscape(String storagePathElement) {
        return Text.unescapeIllegalJcrChars(storagePathElement);
    }

    class PathBuilderImpl implements PathBuilder {
        private final StringBuilder builder;

        PathBuilderImpl(String start) {
            this.builder = new StringBuilder(start);
        }

        public PathBuilder append(String... pathElements) {
            for (String pathElement : pathElements) {
                if (StringUtils.isNotBlank(pathElement)) {
                    String escapedPath = escape(PathUtils.trimSlashes(pathElement).toString());
                    builder.append("/").append(escapedPath);
                }
            }
            return this;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    public String getAllRepoRootPath() {
        return "/" + REPOS_FOLDER;
    }

    public String getConfigurationRootPath() {
        return "/" + CONFIGURATION_FOLDER;
    }

    public String getTrashRootPath() {
        return "/" + TRASH_FOLDER;
    }

    public String getLogsRootPath() {
        return "/" + LOGS_FOLDER;
    }

    public String getBuildsRootPath() {
        return "/" + BUILDS_FOLDER;
    }

    public String getLogPath(String logKey) {
        return createBuilder("").append(LOGS_FOLDER, logKey).toString();
    }

    public String getRepoRootPath(String repoKey) {
        return createBuilder("").append(REPOS_FOLDER, repoKey).toString();
    }

    public String getConfigPath(String configKey) {
        return createBuilder("").append(CONFIGURATION_FOLDER, configKey).toString();
    }

    public String getBuildsPath(String buildsKey) {
        return createBuilder("").append(BUILDS_FOLDER, buildsKey).toString();
    }

    /**
     * @param exportDir The base export directory
     * @return The base directory under the exportDir to which repositories are exported
     */
    public File getRepositoriesExportDir(File exportDir) {
        return new File(exportDir, REPOS_FOLDER);
    }

    public File getRepoExportDir(File exportDir, String repoKey) {
        return new File(getRepositoriesExportDir(exportDir), repoKey);
    }

    public String getAbsolutePath(RepoPath repoPath) {
        String key = repoPath.getRepoKey();
        String relPath = repoPath.getPath();
        String absPath = getRepoRootPath(key) + (relPath.length() > 0 ? "/" + relPath : "");
        return PathUtils.formatPath(absPath);
    }

    public RepoPath getRepoPath(String absPath) {
        if (absPath == null || absPath.length() == 0) {
            throw new IllegalArgumentException("Absolute path cannot be empty");
        }
        String modifiedAbsPath = absPath.replace('\\', '/');
        String repoJcrRootPath = getAllRepoRootPath();
        String repoKey = repoKeyFromPath(modifiedAbsPath);
        if (repoKey == null) {
            //File moved to trash or repo deleted externally
            return null;
        }
        String relPath = "";
        int repoRootPathLength = repoJcrRootPath.length() + repoKey.length() + 1;
        if (modifiedAbsPath.length() > repoRootPathLength) {
            relPath = PathUtils.formatRelativePath(modifiedAbsPath.substring(repoRootPathLength));
        }
        RepoPath repoPath = InternalRepoPathFactory.create(repoKey, relPath);
        return repoPath;
    }

    public String repoKeyFromPath(String absPath) {
        String repoJcrRootPath = PathFactoryHolder.get().getAllRepoRootPath();
        int idx = absPath.indexOf(repoJcrRootPath);
        if (idx == -1) {
            log.warn("Path '{}' is not a repository path.", absPath);
            return null;
        }
        int repoKeyEnd = absPath.indexOf("/", repoJcrRootPath.length() + 1);
        int repoKeyBegin = repoJcrRootPath.length() + 1;
        String repoKey = repoKeyEnd > 0 ? absPath.substring(repoKeyBegin, repoKeyEnd) :
                absPath.substring(repoKeyBegin);
        return repoKey;
    }
}