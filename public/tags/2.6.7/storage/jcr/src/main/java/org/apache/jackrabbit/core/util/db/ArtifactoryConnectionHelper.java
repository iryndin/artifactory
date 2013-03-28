/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Yoav Landman
 */
public final class ArtifactoryConnectionHelper extends ConnectionHelper {

    private static final Logger log = LoggerFactory.getLogger(ArtifactoryConnectionHelper.class);

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
        if (log.isTraceEnabled()) {
            log.trace("select begin: {}", sql);
        }
        ResultSet result = exec(sql, params, false, 0);
        if (log.isTraceEnabled()) {
            log.trace("select ended: {}", sql);
        }
        return result;
    }

    public void txBegin() {
        try {
            startBatch();
            if (log.isTraceEnabled()) {
                log.trace("Batch tx begin " + getBatchConnection());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not start batch.", e);
        }
    }

    public void commit() {
        if (log.isTraceEnabled()) {
            log.trace("Batch tx end " + getBatchConnection());
        }
        try {
            endBatch(true);
        } catch (SQLException e) {
            throw new RuntimeException("Could not commit transaction.", e);
        }
    }

    public void rollback() {
        try {
            if (log.isTraceEnabled()) {
                log.trace("Batch tx end " + getBatchConnection());
            }
            endBatch(false);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error("Rollback failed.", e);
            } else {
                log.warn("Rollback failed: " + e.getMessage());
            }
        }
    }

    public boolean isTxActive() {
        return inBatchMode();
    }

    protected final Connection getBatchConnection() {
        if (inBatchMode()) {
            try {
                return getConnection();
            } catch (SQLException e) {
                log.error("Could not get batch connection.", e);
                return null;
            }
        }
        return null;
    }
}