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

import org.artifactory.api.util.SerializablePair;

/**
 * The implementation of a checksum pair which was not found
 *
 * @author Noam Y. Tenne
 */
public class UnfoundChecksumPair extends SerializablePair<String, String> implements ChecksumPair {

    /**
     * Default constructor, sets the pair with null values
     */
    public UnfoundChecksumPair() {
        super(null, null);
    }

    public String getSha1() {
        return null;
    }

    public String getMd5() {
        return null;
    }

    public boolean checksumsFound() {
        return false;
    }
}