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

package org.artifactory.jcr;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.io.File;

/**
 * This singleton is responsible for providing the path structure in the JCR DB, and for converting RepoPath to absPath
 * (in JCR) and vice-versa.
 */
public class JcrPath {

    private static final Logger log = LoggerFactory.getLogger(JcrPath.class);

    protected static final String REPOS_FOLDER = "repositories";
    protected static final String CONFIGURATION_FOLDER = "configuration";
    protected static final String TRASH_FOLDER = "trash";
    protected static final String LOGS_FOLDER = "logs";
    protected static final String BUILDS_FOLDER = "builds";

    /**
     * Strange stuff of a child class overrideable singleton
     */
    private static JcrPath instance = new JcrPath();

    protected JcrPath() {
    }

    public static JcrPath get() {
        return instance;
    }

    protected static void set(JcrPath jcrPath) {
        JcrPath.instance = jcrPath;
    }

    public String getRepoJcrRootPath() {
        return "/" + REPOS_FOLDER;
    }

    public String getConfigJcrRootPath() {
        return "/" + CONFIGURATION_FOLDER;
    }

    public String getTrashJcrRootPath() {
        return "/" + TRASH_FOLDER;
    }

    public String getLogsJcrRootPath() {
        return "/" + LOGS_FOLDER;
    }

    public String getBuildsJcrRootPath() {
        return "/" + BUILDS_FOLDER;
    }

    public String getLogJcrPath(String logKey) {
        return new StringBuilder("/").append(LOGS_FOLDER).append("/").append(logKey).toString();
    }

    public String getRepoJcrPath(String repoKey) {
        return new StringBuilder("/").append(REPOS_FOLDER).append("/").append(repoKey).toString();
    }

    public String getConfigJcrPath(String configKey) {
        return new StringBuilder("/").append(CONFIGURATION_FOLDER).append("/").append(configKey).toString();
    }

    public String getBuildsJcrPath(String buildsKey) {
        return new StringBuilder("/").append(BUILDS_FOLDER).append("/").append(buildsKey).toString();
    }

    /**
     * @param exportDir The base export direcotry
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
        String absPath = getRepoJcrPath(key) + (relPath.length() > 0 ? "/" + relPath : "");
        return absPath;
    }

    public RepoPath getRepoPath(String absPath) {
        if (absPath == null || absPath.length() == 0) {
            throw new IllegalArgumentException("Absolute path cannot be empty");
        }
        String modifiedAbsPath = absPath.replace('\\', '/');
        String repoJcrRootPath = getRepoJcrRootPath();
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
        RepoPath repoPath = new RepoPathImpl(repoKey, relPath);
        return repoPath;
    }

    /**
     * Returns the build name from an absolute path of a build
     *
     * @param absPath Absolute path of a build. '/builds/moo/3/timestamp' For example
     * @return Build name
     */
    public String getBuildNameFromPath(String absPath) {
        String buildName = null;

        if (StringUtils.isNotBlank(absPath)) {
            String modifiedAbsPath = absPath.replace('\\', '/');
            String buildsJcrRootPath = getBuildsJcrRootPath();

            if (modifiedAbsPath.startsWith(buildsJcrRootPath)) {
                //Add 1 to the length, to omitt the '/' that follows the build root
                buildName = modifiedAbsPath.substring(buildsJcrRootPath.length() + 1);

                int indexOfSlash = buildName.indexOf('/');
                if (indexOfSlash != -1) {
                    buildName = buildName.substring(0, indexOfSlash);
                }
            }
        }
        return buildName;
    }

    private String repoKeyFromPath(String absPath) {
        String repoJcrRootPath = JcrPath.get().getRepoJcrRootPath();
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