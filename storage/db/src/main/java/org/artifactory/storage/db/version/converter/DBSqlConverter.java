package org.artifactory.storage.db.version.converter;

import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.artifactory.util.ResourceUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts database by conversion sql script
 */
public class DBSqlConverter implements DBConverter {
    private static final Logger log = LoggerFactory.getLogger(DBSqlConverter.class);

    @Override
    public void convert(ArtifactoryVersion from, JdbcHelper jdbcHelper, DbType dbType) {
        Connection con = null;
        ResultSet rs = null;
        try {
            // Build resource file name.
            String dbTypeName = dbType.toString();
            String fromVersion = from.name().toLowerCase();
            String resourcePath = "/conversion/" + dbTypeName + "/" + dbTypeName + "_" + fromVersion + ".sql";
            InputStream resource = ResourceUtils.getResource(resourcePath);
            if (resource == null) {
                throw new IOException("Database DDL resource not found at: '" + resourcePath + "'");
            }
            // Execute update
            log.info("Starting schema conversion: " + resourcePath);
            con = jdbcHelper.getDataSource().getConnection();
            DbUtils.executeSqlStream(con, resource);
            log.info("Ending schema conversion: " + resourcePath);
        } catch (SQLException | IOException e) {
            String msg = "Could not convert DB from " + from.toString();
            log.error(msg + " due to " + e.getMessage(), e);
            throw new RuntimeException(msg, e);
        } finally {
            DbUtils.close(con, null, rs);
        }
    }
}
