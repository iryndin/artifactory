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

package org.artifactory.checksum;

import org.artifactory.common.Info;

import java.util.HashSet;
import java.util.Set;

import static org.artifactory.checksum.ChecksumInfo.TRUSTED_FILE_MARKER;

/**
 * Basic information about the file. Internally not stored as XML but as node properties
 *
 * @author yoavl
 */
public class ChecksumsInfo implements Info {

    private final Set<ChecksumInfo> checksums = new HashSet<ChecksumInfo>(2);

    public ChecksumsInfo() {
        // default empty constructor
    }

    public ChecksumsInfo(ChecksumsInfo other) {
        // create a defensive copy
        if (other.getChecksums() != null) {
            for (ChecksumInfo checksum : other.getChecksums()) {
                addChecksumInfo(checksum);
            }
        }
    }

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
        this.checksums.clear();
        for (ChecksumInfo checksum : checksums) {
            addChecksumInfo(checksum);
        }
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

    /**
     * Adds new checksum info. If checksum of the same type already exists it will be overridden.
     *
     * @param checksumInfo The checksum info to add
     */
    public void addChecksumInfo(ChecksumInfo checksumInfo) {
        if (checksumInfo == null) {
            throw new IllegalArgumentException("Nulls are not allowed");
        }
        checksums.remove(checksumInfo);
        checksums.add(checksumInfo);
    }

    /**
     * Creates and sets this file info checksums. Trusted checksums are ones that doesn't have original checksum but we
     * trust them to be ok.
     */
    public void createTrustedChecksums() {
        ChecksumType[] types = ChecksumType.values();
        for (ChecksumType type : types) {
            addChecksumInfo(new ChecksumInfo(type, TRUSTED_FILE_MARKER, null));
        }
    }

    public boolean isIdentical(ChecksumsInfo info) {
        if (this.checksums == info.checksums) {
            return true;
        }
        if (this.checksums == null || info.checksums == null) {
            return false;
        }
        if (this.checksums.size() != info.checksums.size()) {
            return false;
        }
        for (ChecksumInfo other : checksums) {
            ChecksumInfo mine = info.getChecksumInfo(other.getType());
            if (!other.isIdentical(mine)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChecksumsInfo that = (ChecksumsInfo) o;

        if (checksums != null ? !checksums.equals(that.checksums) : that.checksums != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return checksums != null ? checksums.hashCode() : 0;
    }
}