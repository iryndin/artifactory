/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.api.repo;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.Info;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.util.PathUtils;

/**
 * An object identity that represents a repository and a groupId
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public final class RepoPath implements Info {

    public static final char REPO_PATH_SEP = ':';

    private final String repoKey;
    private final String path;

    /**
     * @param repoKey The key of any repo
     * @param path    The relativ path inside the repo
     */
    public RepoPath(String repoKey, String path) {
        this.repoKey = repoKey;
        this.path = PathUtils.formatRelativePath(path);
    }

    /**
     * Create a repo path representing the child of parent
     *
     * @param parent the repo path of the parent folder
     * @param child  the child name
     */
    public RepoPath(RepoPath parent, String child) {
        this.repoKey = parent.repoKey;
        this.path = PathUtils.formatRelativePath(parent.path + "/" + child);
    }

    public RepoPath(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException(
                    "RepoAndGroupIdIdentity cannot have a null id");
        }
        int idx = id.indexOf(REPO_PATH_SEP);
        if (idx <= 0) {
            throw new IllegalArgumentException(
                    "Could not determine both repository key and groupId from '" +
                            id + "'.");
        }
        this.repoKey = id.substring(0, idx);
        this.path = PathUtils.formatRelativePath(id.substring(idx + 1));
    }

    public String getRepoKey() {
        return repoKey;
    }

    public String getPath() {
        return path;
    }

    public String getId() {
        return repoKey + REPO_PATH_SEP + path;
    }

    public String getName() {
        return NamingUtils.stripMetadataFromPath(PathUtils.getName(getPath()));
    }

    /**
     * @return Parent of this repo path. Null if has no parent
     */
    public RepoPath getParent() {
        if (isRoot()) {
            return null;
        } else {
            return new RepoPath(repoKey, PathUtils.getParent(path));
        }
    }

    /**
     * @return True if this path is the root (ie, getPath() is empty string)
     */
    public boolean isRoot() {
        return StringUtils.isBlank(getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepoPath repoPath = (RepoPath) o;
        return path.equals(repoPath.path) && repoKey.equals(repoPath.repoKey);
    }

    @Override
    public int hashCode() {
        int result;
        result = repoKey.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return getId();
    }

    public static RepoPath secureRepoPathForRepo(String repoKey) {
        return new RepoPath(repoKey, PermissionTargetInfo.ANY_PATH);
    }

    public static RepoPath getLockingTargetRepoPath(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (NamingUtils.isMetadata(path)) {
            String fsItemPath = NamingUtils.getMetadataParentPath(path);
            return new RepoPath(repoPath.getRepoKey(), fsItemPath);
        } else {
            return repoPath;
        }
    }

    public static RepoPath childRepoPath(RepoPath repoPath, String childRelPath) {
        if (!childRelPath.startsWith("/")) {
            childRelPath = "/" + childRelPath;
        }
        return new RepoPath(repoPath.getRepoKey(), repoPath.getPath() + childRelPath);
    }

    /**
     * Static factory method to create repo path for the root repository path
     *
     * @param repoKey The repository key
     * @return RepoPath of the root
     */
    public static RepoPath repoRootPath(String repoKey) {
        return new RepoPath(repoKey, StringUtils.EMPTY);
    }
}