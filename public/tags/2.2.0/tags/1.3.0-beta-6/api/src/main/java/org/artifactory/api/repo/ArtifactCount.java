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
