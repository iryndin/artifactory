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

package org.artifactory.api.repo;

/**
 * Contains the total artifact count
 *
 * @author Noam Tenne
 */
public class ArtifactCount {

    /**
     * The number of results
     */
    long count = 0;

    /**
     * Constructor
     *
     * @param count Number of artifacts in the repositories
     */
    public ArtifactCount(long count) {
        this.count = count;
    }

    /**
     * @return The total count of artifacts in the repositories.
     */
    public long getCount() {
        return count;
    }
}
