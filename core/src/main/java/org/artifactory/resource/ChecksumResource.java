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

package org.artifactory.resource;

import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.RepoPath;

/**
 * A checksum resource is used as a response for checksum request and it wraps the actual file resource for which the
 * checksum is requested. Currently it is built only for existing checksum (meaning isFound is always true).
 *
 * @author Yossi Shaul
 */
public class ChecksumResource extends FileResource {
    private final ChecksumType type;
    private final String checksum;

    public ChecksumResource(FileResource resource, ChecksumType type, String checksum) {
        super(resource.getInfo());
        this.type = type;
        this.checksum = checksum;
    }

    @Override
    public RepoPath getRepoPath() {
        RepoPath fileRepoPath = super.getRepoPath();
        return new RepoPath(fileRepoPath.getRepoKey(), fileRepoPath.getPath() + type.ext());
    }

    @Override
    public RepoPath getResponseRepoPath() {
        RepoPath repoPath = super.getResponseRepoPath();
        // super might already call this class getRepoPath, so fix the path only if required 
        if (!repoPath.getPath().endsWith(type.ext())) {
            repoPath = new RepoPath(repoPath.getRepoKey(), repoPath.getPath() + type.ext());
        }
        return repoPath;
    }

    @Override
    public long getSize() {
        return checksum != null ? checksum.length() : type.length();
    }

    @Override
    public String getMimeType() {
        return ContentType.cheksum.getMimeType();
    }
}
