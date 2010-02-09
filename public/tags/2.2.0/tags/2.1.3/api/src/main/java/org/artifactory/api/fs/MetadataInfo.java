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

package org.artifactory.api.fs;

import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.util.Set;

/**
 * @author yoavl
 */
public class MetadataInfo implements RepoResourceInfo {
    private static final Logger log = LoggerFactory.getLogger(MetadataInfo.class);

    private String name;
    private final RepoPath repoPath;
    private long created;
    private long lastModified;
    private String lastModifiedBy;
    private long size;
    private ChecksumsInfo checksumsInfo;

    public MetadataInfo(RepoPath parentRepoPath, String metadataName) {
        this.repoPath = getMetadataRepoPath(parentRepoPath, metadataName);
        this.name = metadataName;
        this.checksumsInfo = new ChecksumsInfo();
    }

    public MetadataInfo(RepoPath repoPath) {
        this.repoPath = repoPath;
        this.name = NamingUtils.getMetadataName(repoPath.getPath());
        this.checksumsInfo = new ChecksumsInfo();
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public ChecksumsInfo getChecksumsInfo() {
        return checksumsInfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getSha1() {
        return checksumsInfo.getSha1();
    }

    public String getMd5() {
        return checksumsInfo.getMd5();
    }

    public Set<ChecksumInfo> getChecksums() {
        return checksumsInfo.getChecksums();
    }

    public void setChecksums(Set<ChecksumInfo> checksums) {
        checksumsInfo.setChecksums(checksums);
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "MetadataInfo{repoPath=" + repoPath + '}';
    }

    private RepoPath getMetadataRepoPath(RepoPath parentRepoPath, String metadataName) {
        String path = parentRepoPath.getPath();
        boolean alreadyMetadataPath = NamingUtils.isMetadata(path);
        if (alreadyMetadataPath) {
            log.warn("Path {} is already a metadata path.", path);
        }
        //TODO: [by yl] Evaluate the impact of normalizing the path to use the standard metadata format (a.jar#mdname)

        return new RepoPath(parentRepoPath.getRepoKey(),
                alreadyMetadataPath ? path : NamingUtils.getMetadataPath(path, metadataName));
    }
}
