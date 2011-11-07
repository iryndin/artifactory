/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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
 *
 * Based on: /org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0.jar!
 * /org/apache/jackrabbit/core/data/AbstractDataRecord.class
 */

package org.apache.jackrabbit.core.util.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Yoav Landman
 */
public final class ArtifactoryConnectionHelper extends ConnectionHelper {

    public ArtifactoryConnectionHelper(DataSource dataSrc) {
        super(dataSrc, false);
    }

    public ArtifactoryConnectionHelper(ConnectionHelper conHelper) {
        this(conHelper.dataSource);
    }

    public Connection takeConnection() {
        try {
            return getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Could not get a connection.", e);
        }
    }

    public void putConnection(Connection con) {
        closeResources(con, null, null);
    }

    public ResultSet select(final String sql) throws SQLException {
        return exec(sql, new Object[0], false, 0);
    }

    public ResultSet select(final String sql, final Object... params) throws SQLException {
        return exec(sql, params, false, 0);
    }

    public void txBegin() {
        try {
            startBatch();
            log.trace("Batch tx begin " + getConnection());
        } catch (SQLException e) {
            throw new RuntimeException("Could not start batch.", e);
        }
    }

    public void txEnd(boolean commit) {
        try {
            log.trace("Batch tx end " + getConnection());
            endBatch(commit);
        } catch (Exception e) {
            if (commit) {
                throw new RuntimeException("Could not commit.", e);
            } else {
                if (log.isDebugEnabled()) {
                    log.error("Rollback failed.", e);
                } else {
                    log.warn("Rollback failed: " + e.getMessage());
                }
            }
        }
    }

    public boolean isTxActive() {
        return inBatchMode();
    }
}