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

package org.artifactory.model.common;

import org.apache.commons.lang.StringUtils;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.PathUtils;

import javax.annotation.Nullable;

/**
 * An object identity that represents a repository and a path inside the repository
 * <p/>
 *
 * @author Yoav Landman
 */
public final class RepoPathImpl implements RepoPath {

    private final String repoKey;
    private final String path;

    /**
     * @param repoKey The key of any repo
     * @param path    The relative path inside the repo
     */
    public RepoPathImpl(String repoKey, String path) {
        this.repoKey = repoKey;
        this.path = PathUtils.formatRelativePath(path);
    }

    /**
     * Create a repo path representing the child of parent
     *
     * @param parent the repo path of the parent folder
     * @param child  the child name
     */
    public RepoPathImpl(RepoPath parent, String child) {
        this.repoKey = parent.getRepoKey();
        this.path = PathUtils.formatRelativePath(parent.getPath() + "/" + child);
    }

    public RepoPathImpl(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException(
                    "RepoAndPathIdIdentity cannot have a null id");
        }
        int idx = id.indexOf(REPO_PATH_SEP);
        if (idx <= 0) {
            throw new IllegalArgumentException(
                    "Could not determine both repository key and path from '" +
                            id + "'.");
        }
        this.repoKey = id.substring(0, idx);
        this.path = PathUtils.formatRelativePath(id.substring(idx + 1));
    }

    @Override
    public String getRepoKey() {
        return repoKey;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getId() {
        return repoKey + REPO_PATH_SEP + path;
    }

    @Override
    public String getName() {
        return NamingUtils.stripMetadataFromPath(PathUtils.getFileName(getPath()));
    }

    @Override
    public String toPath() {
        return repoKey + (StringUtils.isNotBlank(path) ? "/" + path : "");
    }

    /**
     * @return Parent of this repo path. Null if has no parent
     */
    @Override
    @Nullable
    public RepoPath getParent() {
        if (isRoot()) {
            return null;
        } else {
            return new RepoPathImpl(repoKey, PathUtils.getParent(path));
        }
    }

    /**
     * @return True if this path is the root (ie, getPath() is empty string)
     */
    @Override
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
        return path.equals(repoPath.getPath()) && repoKey.equals(repoPath.getRepoKey());
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
}