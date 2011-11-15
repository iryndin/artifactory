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

import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.RepoResource;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;

import java.util.Set;

/**
 * A barebone resource representing minimal file information retrieved from a remote source
 *
 * @author Noam Y. Tenne
 */
public class RemoteRepoResource implements RepoResource {

    private final MutableRepoResourceInfo info;
    private RepoPath responseRepoPath;

    public RemoteRepoResource(RepoPath repoPath, long lastModified, long size, Set<ChecksumInfo> checksums) {
        if (NamingUtils.isMetadata(repoPath.getPath())) {
            info = InfoFactoryHolder.get().createMetadata(repoPath);
        } else {
            info = InfoFactoryHolder.get().createFileInfo(repoPath);
        }
        info.setLastModified(lastModified);
        info.setSize(size);
        info.setChecksums(checksums);
    }

    public RepoPath getRepoPath() {
        return info.getRepoPath();
    }

    public RepoPath getResponseRepoPath() {
        return (responseRepoPath != null) ? responseRepoPath : getRepoPath();
    }

    public void setResponseRepoPath(RepoPath responsePath) {
        responseRepoPath = responsePath;
    }

    public MutableRepoResourceInfo getInfo() {
        return info;
    }

    public boolean isFound() {
        return true;
    }

    public boolean isExactQueryMatch() {
        return true;
    }

    public boolean isExpired() {
        return false;
    }

    public boolean isMetadata() {
        return NamingUtils.isMetadata(getRepoPath().getPath());
    }

    public long getSize() {
        return info.getSize();
    }

    public long getCacheAge() {
        return 0;
    }

    public long getLastModified() {
        return info.getLastModified();
    }

    public String getMimeType() {
        return NamingUtils.getMimeType(getRepoPath().getPath()).getType();
    }
}
