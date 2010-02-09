package org.artifactory.version;

/**
 * An object that is used to contain the VersionHolders of the the VersionInfoService
 *
 * @author Noam Tenne
 */
public class ArtifactoryVersioning {
    /**
     * A version holder for the latest version of any kind (beta, rc, release)
     */
    private VersionHolder latest;
    /**
     * A version holder for the latest release version
     */
    private VersionHolder release;

    /**
     * Main constructor
     *
     * @param latest  Version holder with latest version of any kind
     * @param release Version holder with latest release version
     */
    public ArtifactoryVersioning(VersionHolder latest, VersionHolder release) {
        this.latest = latest;
        this.release = release;
    }

    /**
     * Returns the version holder with latest version of any kind
     *
     * @return VersionHolder - Latest version of any kind
     */
    public VersionHolder getLatest() {
        return latest;
    }

    /**
     * Returns the version holder with latest release version
     *
     * @return VersionHolder - Latest release version
     */
    public VersionHolder getRelease() {
        return release;
    }
}