package org.artifactory.version;

/**
 * Holds all the version data about Artifactory. version name, and revision from the properties file
 * and ArtifactoryVersion that matches those values. 
*
* @author Yossi Shaul
*/
public class CompoundVersionDetails {
    private final ArtifactoryVersion version;
    private final String versionName;
    private final String revision;

    public CompoundVersionDetails(ArtifactoryVersion version, String versionName, String revision) {
        this.version = version;
        this.versionName = versionName;
        this.revision = revision;
    }

    /**
     * @return The closest matched version for the input stream/file
     */
    public ArtifactoryVersion getVersion() {
        return version;
    }

    /**
     * @return The raw version string as read from the input stream/file
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * @return The raw revision string as read from the input stream/file
     */
    public String getRevision() {
        return revision;
    }

    public boolean isCurrent() {
        return version.isCurrent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CompoundVersionDetails details = (CompoundVersionDetails) o;

        if (!revision.equals(details.revision)) {
            return false;
        }
        if (version != details.version) {
            return false;
        }
        if (!versionName.equals(details.versionName)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + versionName.hashCode();
        result = 31 * result + revision.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "version='" + versionName + '\'' +
                ", revision='" + revision + '\'' +
                ", released version=" + version;
    }
}
