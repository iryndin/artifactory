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

package org.artifactory.api.fs;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.util.PathUtils;

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

    private ChecksumType type;
    private String original;
    private String actual;

    public ChecksumInfo(ChecksumType type) {
        this.type = type;
    }

    public ChecksumInfo(ChecksumType type, String original, String actual) {
        this.type = type;
        this.original = original;
        this.actual = actual;
    }

    public ChecksumInfo(ChecksumInfo copy) {
        this.type = copy.type;
        this.original = copy.original;
        this.actual = copy.actual;
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

    public void setOriginal(String original) {
        this.original = original;
    }

    public void setActual(String actual) {
        this.actual = actual;
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

        ChecksumInfo info = (ChecksumInfo) o;
        if (type != info.type) {
            return false;
        }
        if (actual != null ? !actual.equals(info.actual) : info.actual != null) {
            return false;
        }
        if (original != null ? !original.equals(info.original) : info.original != null) {
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

        ChecksumInfo info = (ChecksumInfo) o;
        return type == info.type;
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

    public boolean merge(ChecksumInfo other) {
        if (other == null || other == this || other.isIdentical(this)) {
            return false;
        }
        boolean modified = false;
        if (PathUtils.hasText(other.actual) && !PathUtils.safeStringEquals(other.actual, this.actual)) {
            this.actual = other.actual;
            modified = true;
        }
        if (PathUtils.hasText(other.original) && !PathUtils.safeStringEquals(other.original, this.original)) {
            this.original = other.original;
            modified = true;
        }
        return modified;
    }
}