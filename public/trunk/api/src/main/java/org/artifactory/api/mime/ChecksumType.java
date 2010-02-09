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

package org.artifactory.api.mime;

import org.artifactory.jcr.JcrTypes;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public enum ChecksumType {
    sha1("SHA-1", ".sha1", 40, JcrTypes.PROP_ARTIFACTORY_SHA1_ORIGINAL, JcrTypes.PROP_ARTIFACTORY_SHA1_ACTUAL),
    md5("MD5", ".md5", 32, JcrTypes.PROP_ARTIFACTORY_MD5_ORIGINAL, JcrTypes.PROP_ARTIFACTORY_MD5_ACTUAL);

    private final String alg;
    private final String ext;
    private final int length;    // length of the hexadecimal string representation of the checksum
    private final String actualPropName;
    private final String originalPropName;

    ChecksumType(String alg, String ext, int length, String actualPropName, String originalPropName) {
        this.alg = alg;
        this.ext = ext;
        this.length = length;
        this.originalPropName = originalPropName;
        this.actualPropName = actualPropName;
    }

    public String alg() {
        return alg;
    }

    public String ext() {
        return ext;
    }

    public String getActualPropName() {
        return actualPropName;
    }

    public String getOriginalPropName() {
        return originalPropName;
    }

    /**
     * @param candidate Checksum candidate
     * @return True if this string is a checksum value for this type
     */
    @SuppressWarnings({"SimplifiableIfStatement"})
    public boolean isValid(String candidate) {
        if (candidate == null || candidate.length() != length) {
            return false;
        }
        return candidate.matches("[a-fA-F0-9]{" + length + "}");
    }

    /**
     * @param ext The checksum filename extension assumed to start with '.' for example '.sha1'.
     * @return Checksum type for the given extension. Null if not found.
     */
    public static ChecksumType forExtension(String ext) {
        if (sha1.ext.equals(ext)) {
            return sha1;
        } else if (md5.ext.equals(ext)) {
            return md5;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return alg;
    }
}
