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

package org.artifactory.api.mime;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public enum ChecksumType {
    sha1("SHA-1", ".sha1"), md5("MD5", ".md5");

    private final String alg;
    private final String ext;

    ChecksumType(String alg, String ext) {
        this.alg = alg;
        this.ext = ext;
    }

    public String alg() {
        return alg;
    }

    public String ext() {
        return ext;
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
