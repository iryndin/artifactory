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

package org.artifactory.resource;

import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.InternalFileInfo;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.mime.MimeType;
import org.artifactory.repo.RepoPath;

/**
 * @author Yoav Landman
 */
public class FileResource implements RepoResource {

    private final InternalFileInfo info;
    private RepoPath responseRepoPath;
    private boolean exactQueryMatch;

    public FileResource(InternalFileInfo fileInfo) {
        this(fileInfo, true);
    }

    public FileResource(InternalFileInfo fileInfo, boolean exactQueryMatch) {
        this.info = new FileInfoImpl(fileInfo);
        this.exactQueryMatch = exactQueryMatch;
    }

    public FileResource(RepoPath repoPath) {
        this(new FileInfoImpl(repoPath));
    }

    public RepoPath getRepoPath() {
        return info.getRepoPath();
    }

    public RepoPath getResponseRepoPath() {
        return responseRepoPath != null ? responseRepoPath : getRepoPath();
    }

    public void setResponseRepoPath(RepoPath responsePath) {
        this.responseRepoPath = responsePath;
    }

    public InternalFileInfo getInfo() {
        return info;
    }

    public boolean isFound() {
        return true;
    }

    public boolean isExactQueryMatch() {
        return exactQueryMatch;
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
        MimeType contentType = NamingUtils.getMimeType(info.getRelPath());
        return contentType.getType();
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
