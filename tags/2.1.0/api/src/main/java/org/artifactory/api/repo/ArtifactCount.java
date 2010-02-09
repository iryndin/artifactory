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

package org.artifactory.api.repo;

/**
 * Contains the total artifact (*.pom and *.xml) count
 *
 * @author Noam Tenne
 */
public class ArtifactCount {

    /**
     * The number of jars in the repositories
     */
    int numberOfJars = 0;

    /**
     * The number of poms in the repositories
     */
    int numberOfPoms = 0;

    /**
     * Constructor
     *
     * @param numberOfJars Number of jars in the repositories
     * @param numberOfPoms Number of poms in the repositories
     */
    public ArtifactCount(int numberOfJars, int numberOfPoms) {
        this.numberOfJars = numberOfJars;
        this.numberOfPoms = numberOfPoms;
    }

    /**
     * Returns the number of jars in the repositories
     *
     * @return int Number of jars
     */
    public int getNumberOfJars() {
        return numberOfJars;
    }

    /**
     * Returns the number of poms in the repositories
     *
     * @return int Number of poms
     */
    public int getNumberOfPoms() {
        return numberOfPoms;
    }

    /**
     * @return The total count of artifacts in the repositories.
     */
    public int getTotalCount() {
        return getNumberOfJars() + getNumberOfPoms();
    }
}
