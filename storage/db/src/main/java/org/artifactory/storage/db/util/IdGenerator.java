/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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
 */

package org.artifactory.storage.db.util;

import org.artifactory.common.ConstantValues;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author mamo
 */
@Service
public class IdGenerator {
    private static final Logger log = LoggerFactory.getLogger(IdGenerator.class);

    @Autowired
    private JdbcHelper jdbcHelper;

    @Autowired
    @Qualifier("uniqueIdsDataSource")
    private DataSource uniqueIdsDataSource;

    public static final String INDEX_TYPE_GENERAL = "general";
    private final long STEP = ConstantValues.dbIdGeneratorFetchAmount.getLong();

    private final Object indexMonitor = new Object();
    private final AtomicLong currentIndex = new AtomicLong(DbService.NO_DB_ID);
    private volatile long maxReservedIndex = DbService.NO_DB_ID;

    @Transactional
    public void initializeIdGenerator() throws SQLException {
        if (STEP <= 0) {
            throw new IllegalArgumentException("IdGenerator STEP must be positive");
        }

        ResultSet rs = null;
        try {
            rs = jdbcHelper.executeSelect("SELECT current_id FROM unique_ids WHERE index_type = ?", INDEX_TYPE_GENERAL);
            if (rs.next()) {
                currentIndex.set(rs.getLong(1));
            } else {
                int firstCurrentIndex = 1;
                jdbcHelper.executeUpdate("INSERT INTO unique_ids VALUES (?, ?)", INDEX_TYPE_GENERAL, firstCurrentIndex);
                log.debug("Created current unique id for the first time");
                currentIndex.set(firstCurrentIndex);
            }
            maxReservedIndex = currentIndex.get();
        } catch (Exception e) {
            throw new SQLException("Could not select current index.", e);
        } finally {
            DbUtils.close(rs);
        }
    }

    public long nextId() {
        final long value = currentIndex.getAndIncrement();
        if (value >= maxReservedIndex) {
            synchronized (indexMonitor) {
                if (value >= maxReservedIndex) {
                    try {
                        long newMax = maxReservedIndex + STEP;
                        maxReservedIndex = updateIndex(INDEX_TYPE_GENERAL, newMax);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not update current index", e);
                    }
                }
            }
        }
        return value;
    }

    private long updateIndex(final String indexType, final long nextMaxCurrentIndex) {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = uniqueIdsDataSource.getConnection();
            stmt = con.prepareStatement("UPDATE unique_ids SET current_id = ? where index_type = ?");
            stmt.setLong(1, nextMaxCurrentIndex);
            stmt.setString(2, indexType);
            stmt.executeUpdate();
            return nextMaxCurrentIndex;
        } catch (SQLException e) {
            throw new StorageException("Failed to update the unique indices table", e);
        } finally {
            DbUtils.close(con, stmt, null, uniqueIdsDataSource);
        }
    }

    @PreDestroy
    private void destroy() {
        DbUtils.closeDataSource(uniqueIdsDataSource);
    }
}
