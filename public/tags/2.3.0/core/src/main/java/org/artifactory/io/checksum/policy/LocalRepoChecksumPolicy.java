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

package org.artifactory.io.checksum.policy;

import org.artifactory.api.mime.NamingUtils;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.descriptor.repo.LocalRepoChecksumPolicyType;
import org.artifactory.repo.RepoPath;

import java.io.Serializable;
import java.util.Set;

/**
 * This is the checksum policy used by local non-cahce repositories. This class is not supposed to be used by the cache
 * repositories and it doesn't verify the checksums.
 *
 * @author Yossi Shaul
 */
public class LocalRepoChecksumPolicy implements ChecksumPolicy, Serializable {
    private LocalRepoChecksumPolicyType policyType = LocalRepoChecksumPolicyType.CLIENT;

    public boolean verify(Set<ChecksumInfo> checksums) {
        return true;
    }

    public String getChecksum(ChecksumType checksumType, Set<ChecksumInfo> checksums) {
        return getChecksum(checksumType, checksums, null);
    }

    public String getChecksum(ChecksumType checksumType, Set<ChecksumInfo> checksums, RepoPath repoPath) {
        ChecksumInfo checksumInfo = getByType(checksumType, checksums);
        if (checksumInfo == null) {
            return null;
        }

        if (repoPath != null && NamingUtils.isMetadata(repoPath.getPath())) {
            return checksumInfo.getActual();  // metadata checksums are always the "server" checksum            
        }

        if (LocalRepoChecksumPolicyType.CLIENT == policyType) {
            return checksumInfo.getOriginal();  // the "client" checksum
        } else {
            return checksumInfo.getActual();    // the "server" (actual file) checksum
        }
    }

    public void setPolicyType(LocalRepoChecksumPolicyType policyType) {
        if (policyType != null) {
            this.policyType = policyType;
        }
    }

    private ChecksumInfo getByType(ChecksumType checksumType, Set<ChecksumInfo> checksums) {
        for (ChecksumInfo info : checksums) {
            if (checksumType.equals(info.getType())) {
                return info;
            }
        }
        return null;
    }

    public LocalRepoChecksumPolicyType getPolicyType() {
        return policyType;
    }
}
