package org.artifactory.converters.helpers;

import org.artifactory.storage.db.properties.model.DbProperties;
import org.artifactory.storage.db.properties.service.ArtifactoryCommonDbPropertiesService;
import org.artifactory.version.ArtifactoryVersion;

import java.util.Date;

/**
 * Author: gidis
 */
public class MockDbPropertiesService implements ArtifactoryCommonDbPropertiesService {
    private ArtifactoryVersion version;
    private long release;
    private DbProperties dbProperties;

    public MockDbPropertiesService(ArtifactoryVersion version, long release) {
        this.version = version;
        this.release = release;
    }

    @Override
    public void updateDbProperties(DbProperties dbProperties) {
        this.dbProperties = dbProperties;
    }

    @Override
    public DbProperties getDbProperties() {
        if (version == null) {
            return null;
        } else {
            return new DbProperties(new Date().getTime(), version.getValue(), (int) version.getRevision(), release);
        }
    }

    public boolean isUpdateDbPropertiesHasBeenCalled() {
        return dbProperties != null;
    }
}
