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

package org.artifactory.md;

import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.resource.RepoResourceInfo;

import java.util.Set;

/**
 * @author yoavl
 */
public interface MetadataInfo extends RepoResourceInfo {

    RepoPath getRepoPath();

    ChecksumsInfo getChecksumsInfo();

    String getName();

    void setName(String name);

    long getCreated();

    void setCreated(long created);

    long getLastModified();

    void setLastModified(long lastModified);

    String getLastModifiedBy();

    void setLastModifiedBy(String lastModifiedBy);

    String getSha1();

    String getMd5();

    Set<ChecksumInfo> getChecksums();

    void setChecksums(Set<ChecksumInfo> checksums);

    long getSize();

    void setSize(long size);
}
