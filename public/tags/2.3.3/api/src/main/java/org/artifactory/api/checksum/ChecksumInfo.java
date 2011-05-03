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

package org.artifactory.api.checksum;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.checksum.ChecksumType;

import java.io.Serializable;

/**
 * Holds original and calculated values of a checksum.
 *
 * @author Yossi Shaul
 */
@XStreamAlias("checksum")
public class ChecksumInfo implements Serializable {
    // marks a checksum type with no original checksum to be safe.
    // this marker is used when a file is deployed and we don't have the remote
    // checksum but we have the actual file
    public static final String TRUSTED_FILE_MARKER = "NO_ORIG";

    private final ChecksumType type;
    private final String original;
    private final String actual;

    public ChecksumInfo(ChecksumType type, String original, String actual) {
        this.type = type;
        this.original = original;
        this.actual = actual;
    }

    public ChecksumType getType() {
        return type;
    }

    public String getOriginal() {
        if (isMarkedAsTrusted()) {
            return getActual();
        } else {
            return original;
        }
    }

    public String getActual() {
        return actual;
    }

    public boolean checksumsMatch() {
        return original != null && actual != null && (isMarkedAsTrusted() || actual.equals(original));
    }

    public boolean isMarkedAsTrusted() {
        return TRUSTED_FILE_MARKER.equals(original);
    }

    public boolean isIdentical(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        org.artifactory.checksum.ChecksumInfo info = (org.artifactory.checksum.ChecksumInfo) o;
        if (type != info.getType()) {
            return false;
        }
        if (actual != null ? !actual.equals(info.getActual()) : info.getActual() != null) {
            return false;
        }
        if (original != null ? !original.equals(info.getOriginal()) : info.getOriginal() != null) {
            return false;
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

        org.artifactory.checksum.ChecksumInfo info = (org.artifactory.checksum.ChecksumInfo) o;
        return type == info.getType();
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "ChecksumInfo{" +
                "type=" + type +
                ", original='" + original + '\'' +
                ", actual='" + actual + '\'' +
                '}';
    }
}