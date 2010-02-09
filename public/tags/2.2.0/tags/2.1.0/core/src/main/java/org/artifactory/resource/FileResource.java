/*
 * This file is part of Artifactory.
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

package org.artifactory.resource;

import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.repo.RepoPath;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class FileResource implements RepoResource {

    private final FileInfo info;
    private RepoPath repoPath;

    public FileResource(FileInfo fileInfo) {
        this.info = new FileInfoImpl(fileInfo);
    }

    public FileResource(RepoPath repoPath) {
        this.info = new FileInfoImpl(repoPath);
    }

    public RepoPath getRepoPath() {
        return info.getRepoPath();
    }

    public RepoPath getResponseRepoPath() {
        return repoPath != null ? repoPath : getRepoPath();
    }

    public void setResponseRepoPath(RepoPath responsePath) {
        this.repoPath = responsePath;
    }

    public FileInfo getInfo() {
        return info;
    }

    public boolean isFound() {
        return true;
    }

    public boolean isExpired() {
        return false;
    }

    public boolean isMetadata() {
        return false;
    }

    public long getSize() {
        return info.getSize();
    }

    public long getLastModified() {
        return info.getLastModified();
    }

    public String getMimeType() {
        return info.getMimeType();
    }

    public long getCacheAge() {
        long lastUpdated = info.getLastUpdated();
        if (lastUpdated <= 0) {
            return -1;
        }
        return System.currentTimeMillis() - lastUpdated;
    }

    @Override
    public String toString() {
        return info.getRepoPath().toString();
    }
}
