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

import com.google.common.collect.Sets;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.Checksums;
import org.artifactory.repo.RepoPath;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

/**
 * An already resource who's content is already resolved (no need to get it's handle for its content) that holds the
 * resource content in a string.
 *
 * @author Yossi Shaul
 */
public class ResolvedResource implements RepoResource {

    private final RepoResource wrappedResource;
    private final String content;

    public ResolvedResource(RepoResource wrappedResource, String content) {
        this(wrappedResource, content, true);
    }

    public ResolvedResource(RepoResource wrappedResource, String content, boolean overrideResourceInfoChecksums) {
        this.wrappedResource = wrappedResource;
        this.content = content;
        if (overrideResourceInfoChecksums) {
            overrideChecksums();
        }
    }

    public String getContent() {
        return content;
    }

    public RepoPath getRepoPath() {
        return wrappedResource.getRepoPath();
    }

    public RepoPath getResponseRepoPath() {
        return wrappedResource.getResponseRepoPath();
    }

    public void setResponseRepoPath(RepoPath responsePath) {
        wrappedResource.setResponseRepoPath(responsePath);
    }

    public RepoResourceInfo getInfo() {
        return wrappedResource.getInfo();
    }

    public boolean isFound() {
        return wrappedResource.isFound();
    }

    public boolean isExactQueryMatch() {
        return wrappedResource.isExactQueryMatch();
    }

    public boolean isExpired() {
        return wrappedResource.isExpired();
    }

    public boolean isMetadata() {
        return wrappedResource.isMetadata();
    }

    public long getSize() {
        return wrappedResource.getSize();
    }

    public long getCacheAge() {
        return wrappedResource.getCacheAge();
    }

    public long getLastModified() {
        return wrappedResource.getLastModified();
    }

    public String getMimeType() {
        return wrappedResource.getMimeType();
    }

    private void overrideChecksums() {
        if (getInfo() == null) {
            return;
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(getContent().getBytes("utf-8"));

            Checksum[] checksums = Checksums.calculate(bais, ChecksumType.values());
            Set<ChecksumInfo> checksumInfos = Sets.newHashSetWithExpectedSize(checksums.length);
            for (Checksum checksum : checksums) {
                ChecksumInfo checksumInfo =
                        new ChecksumInfo(checksum.getType(), checksum.getChecksum(), checksum.getChecksum());
                checksumInfos.add(checksumInfo);
            }
            getInfo().setChecksums(checksumInfos);
        } catch (IOException e) {
            // rare since the checksum is calculated on in memory byte array built from a string
            throw new RuntimeException("Failed to calculate content checksum", e);
        }
    }
}
