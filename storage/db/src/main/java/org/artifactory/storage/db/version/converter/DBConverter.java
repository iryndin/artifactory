package org.artifactory.storage.db.version.converter;

import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.version.ArtifactoryVersion;

/**
 *
 */
public interface DBConverter {
    void convert(ArtifactoryVersion from, JdbcHelper jdbcHelper, DbType dbType);
}
