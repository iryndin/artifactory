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

import org.artifactory.api.common.Info;
import org.artifactory.api.mime.ChecksumType;

import java.util.HashSet;
import java.util.Set;

/**
 * Basic information about the file. Internally not stored as XML but as node properties
 *
 * @author yoavl
 */
public class ChecksumsInfo implements Info {

    private Set<ChecksumInfo> checksums = new HashSet<ChecksumInfo>(2);

    public String getSha1() {
        ChecksumInfo sha1 = getChecksumInfo(ChecksumType.sha1);
        return sha1 == null ? null : sha1.getActual();
    }

    public String getMd5() {
        ChecksumInfo md5 = getChecksumInfo(ChecksumType.md5);
        return md5 == null ? null : md5.getActual();
    }

    public void setChecksums(Set<ChecksumInfo> checksums) {
        if (checksums == null) {
            throw new IllegalArgumentException("Checksums cannot be null.");
        }
        this.checksums = checksums;
    }

    public Set<ChecksumInfo> getChecksums() {
        return checksums;
    }

    public ChecksumInfo getChecksumInfo(ChecksumType type) {
        if (checksums != null) {
            for (ChecksumInfo checksum : checksums) {
                if (type.equals(checksum.getType())) {
                    return checksum;
                }
            }
        }
        return null;
    }

    public void addChecksumInfo(ChecksumInfo checksumInfo) {
        if (checksumInfo == null) {
            throw new IllegalArgumentException("Nulls are not allowed");
        }
        checksums.add(checksumInfo);
    }

    /**
     * Creates and sets this file info checksums. Trusted checksums are ones that doesn't have original checksum but we
     * trust them to be ok.
     */
    public void createTrustedChecksums() {
        ChecksumType[] types = ChecksumType.values();
        Set<ChecksumInfo> checksumInfos = new HashSet<ChecksumInfo>(types.length);
        for (ChecksumType type : types) {
            ChecksumInfo checksumInfo = new ChecksumInfo(type);
            checksumInfo.setOriginal(ChecksumInfo.TRUSTED_FILE_MARKER);
            checksumInfos.add(checksumInfo);
        }
        setChecksums(checksumInfos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChecksumsInfo)) {
            return false;
        }

        ChecksumsInfo info = (ChecksumsInfo) o;

        if (!checksums.equals(info.checksums)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return checksums.hashCode();
    }
}