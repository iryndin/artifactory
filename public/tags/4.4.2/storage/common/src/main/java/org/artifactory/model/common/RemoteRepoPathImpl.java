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

import com.google.common.base.Strings;
import org.artifactory.repo.RemoteRepoPath;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents RemoteRepoPath implementation which obligates to RepoPath
 * contract and adding remote Origin in to the context
 */
public class RemoteRepoPathImpl implements RemoteRepoPath {

    private final String origin;
    private final RepoPath repoPath;

    /**
     * @param origin origin host
     * @param repoPath {@link RepoPath}
     */
    public RemoteRepoPathImpl(String origin, RepoPath repoPath) {
        assert !Strings.isNullOrEmpty(origin) : "Origin cannot be empty";
        assert repoPath != null : "RepoPath cannot be empty";

        this.origin = origin;
        this.repoPath = repoPath;
    }

    public static RemoteRepoPathImpl newInstance(String origin, String repoPath) {
        RepoPath path = RepoPathFactory.create(repoPath);
        return new RemoteRepoPathImpl(origin, path);
    }

    public static RemoteRepoPathImpl newInstance(String origin, RepoPath path) {
        return new RemoteRepoPathImpl(origin, path);
    }

    @Nonnull
    @Override
    public String getRepoKey() {
        return repoPath.getRepoKey();
    }

    @Override
    public String getPath() {
        return repoPath.getPath();
    }

    @Override
    public String getId() {
        return repoPath.getId();
    }

    @Override
    public String toPath() {
        return  repoPath.toPath();
    }

    @Override
    public String getName() {
        return repoPath.getName();
    }

    @Nullable
    @Override
    public RepoPath getParent() {
        return repoPath.getParent();
    }

    @Override
    public boolean isRoot() {
        return repoPath.isRoot();
    }

    @Override
    public boolean isFile() {
        return repoPath.isFile();
    }

    @Override
    public boolean isFolder() {
        return repoPath.isFolder();
    }

    /**
     * @return remote origin
     */
    @Override
    public String getOrigin() {
        return origin;
    }

    /**
     * @return {@link RepoPath} on remote host
     */
    @Override
    public RepoPath getActualRepoPath() {
        return repoPath;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }
        return repoPath.equals(other) &&
                this.getOrigin().equals(((RemoteRepoPath) other).getOrigin());
    }

    @Override
    public int hashCode() {
        int result;
        result = getActualRepoPath().hashCode();
        result = 31 * result + getOrigin().hashCode();
        return result;
    }
}
