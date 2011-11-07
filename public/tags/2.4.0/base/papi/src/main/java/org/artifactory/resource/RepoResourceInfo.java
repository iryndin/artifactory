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
import org.artifactory.checksum.ChecksumsInfo;
import org.artifactory.common.Info;
import org.artifactory.repo.RepoPath;

import java.util.Set;

/**
 * Date: 8/1/11
 * Time: 7:09 PM
 *
 * @author Fred Simon
 */
public interface RepoResourceInfo extends Info {
    RepoPath getRepoPath();

    String getName();

    long getLastModified();

    long getSize();

    String getSha1();

    String getMd5();

    ChecksumsInfo getChecksumsInfo();

    /**
     * Should use the container getter
     * @return
     */
    @Deprecated
    Set<ChecksumInfo> getChecksums();
}
