package org.artifactory.storage.db.upgrades.versions;

import org.artifactory.storage.db.itest.DbTestUtils;
import org.artifactory.storage.db.upgrades.common.UpgradeBaseTest;
import org.artifactory.storage.db.version.ArtifactoryDBVersion;
import org.artifactory.version.ArtifactoryVersion;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.artifactory.storage.db.version.ArtifactoryDBVersion.convert;
import static org.artifactory.storage.db.version.ArtifactoryDBVersion.v100;
import static org.artifactory.version.ArtifactoryVersion.getCurrent;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Author: gidis
 */
public class ArtifactoryDBVersionTest extends UpgradeBaseTest {


    public void testConversion100() throws IOException, SQLException {
        rollBackTo300Version();
        // Now the DB is like in 3.0.x, should be missing the new tables of 3.1.x
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            ArtifactoryVersion fromVersion = getFromVersion(v100);
            ArtifactoryVersion currentVersion = getCurrent();
            convert(fromVersion, currentVersion, jdbcHelper, storageProperties.getDbType());
            // No missing tables expected (conversion has been executed)
            assertFalse(DbTestUtils.isTableMissing(connection));
        }
        verifyDbResourcesReleased();
    }

    public void testConversionCurrent() throws IOException, SQLException {
        rollBackTo300Version();
        // Now the DB is like in 3.0.x, should be missing the new tables of 3.1.x
        try (Connection connection = jdbcHelper.getDataSource().getConnection()) {
            ArtifactoryVersion fromVersion = getFromVersion(ArtifactoryDBVersion.getLast());
            ArtifactoryVersion currentVersion = getCurrent();
            convert(fromVersion, currentVersion, jdbcHelper, storageProperties.getDbType());
            // Expected missing tables (conversion hasn't been executed)
            assertTrue(DbTestUtils.isTableMissing(connection));
        }
        verifyDbResourcesReleased();
    }

    private ArtifactoryVersion getFromVersion(ArtifactoryDBVersion version) {
        return version.getComparator().getFrom();
    }
}
