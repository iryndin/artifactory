package org.artifactory.storage.db.version;

import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.storage.db.version.converter.DBConverter;
import org.artifactory.storage.db.version.converter.DBSqlConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.VersionComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Artifactory DB version
 */
public enum ArtifactoryDBVersion {
    v100(ArtifactoryVersion.v300, ArtifactoryVersion.v304),
    v101(ArtifactoryVersion.v310, ArtifactoryVersion.getCurrent(),
            new DBSqlConverter());
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryDBVersion.class);


    private final VersionComparator comparator;
    private final DBConverter[] converters;

    ArtifactoryDBVersion(ArtifactoryVersion from, ArtifactoryVersion until, DBConverter... converters) {
        this.comparator = new VersionComparator(from, until);
        this.converters = converters;
    }

    public static ArtifactoryDBVersion getLast() {
        ArtifactoryDBVersion[] versions = ArtifactoryDBVersion.values();
        return versions[versions.length - 1];
    }

    public DBConverter[] getConverters() {
        return converters;
    }

    public static void convert(ArtifactoryVersion from, ArtifactoryVersion target, JdbcHelper jdbcHelper,
            DbType dbType) {
        boolean foundConversion = false;
        // All converters of versions above me needs to be executed in sequence
        ArtifactoryDBVersion[] versions = ArtifactoryDBVersion.values();
        for (ArtifactoryDBVersion version : versions) {
            if (version.comparator.isAfter(from) && !version.comparator.supports(from)) {
                for (DBConverter dbConverter : version.getConverters()) {
                    // Write to log only if conversion has been executed
                    if (!foundConversion) {
                        foundConversion = true;
                        log.info("Starting database conversion from " + from + " to " + target);
                    }
                    dbConverter.convert(version.comparator.getFrom(), jdbcHelper, dbType);
                }

            }
        }
        // Write to log only if conversion has been executed
        if (foundConversion) {
            log.info("Ending database conversion from " + from + " to " + target);
        }
    }

    public VersionComparator getComparator() {
        return comparator;
    }
}
