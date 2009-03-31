package org.artifactory.version;

/**
 * Holds the various Artifactory database versions.
 *
 * @author Yossi Shaul
 */
public enum ArtifactoryDbVersion {
    one(ArtifactoryVersion.v122rc0, ArtifactoryVersion.v130beta2),
    two(ArtifactoryVersion.v130beta3, ArtifactoryVersion.v130beta5),
    three(ArtifactoryVersion.v130beta6, ArtifactoryVersion.v130beta61),
    four(ArtifactoryVersion.v130rc1, ArtifactoryVersion.getCurrent());

    private final VersionComparator comparator;

    ArtifactoryDbVersion(ArtifactoryVersion from, ArtifactoryVersion until) {
        this.comparator = new VersionComparator(from, until);
    }

    public boolean isCurrent() {
        return comparator.isCurrent();
    }

    public boolean supports(ArtifactoryVersion version) {
        return comparator.supports(version);
    }

    public VersionComparator getComparator() {
        return comparator;
    }

    public static ArtifactoryDbVersion findVersion(ArtifactoryVersion version) {
        for (ArtifactoryDbVersion dbVersion : values()) {
            if (dbVersion.supports(version)) {
                return dbVersion;
            }
        }
        throw new IllegalArgumentException("No DB version found for Artifactory version" + version);
    }
}
