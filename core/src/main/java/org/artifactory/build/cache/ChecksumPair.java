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

package org.artifactory.build.cache;

/**
 * The main interface for the pair-extending checksum info class
 *
 * @author Noam Y. Tenne
 */
public interface ChecksumPair {

    /**
     * Returns the value of the SHA1 checksum
     *
     * @return SHA1 checksum value if found, null if not
     */
    String getSha1();

    /**
     * Returns the value of the MD5 checksum
     *
     * @return MD5 checksum value if found, null if not
     */
    String getMd5();

    /**
     * Indicates if the checksums were found or not
     *
     * @return True if the checksums were found
     */
    boolean checksumsFound();
}