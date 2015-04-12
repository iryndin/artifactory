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
 */

package org.artifactory.storage.db.fs.dao;

import org.artifactory.storage.db.fs.entity.Stat;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A data access object for the stats table.
 *
 * @author Yossi Shaul
 */
@Repository
public class StatsDao extends BaseDao {
    private static final Logger log = LoggerFactory.getLogger(StatsDao.class);

    @Autowired
    public StatsDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    @Nullable
    public Stat getStats(long nodeId) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM stats WHERE node_id = ?", nodeId);
            if (resultSet.next()) {
                return statFromResultSet(resultSet);
            }
            return null;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    public int updateStats(Stat stats) throws SQLException {
        log.debug("Updating stats {}", stats);
        return jdbcHelper.executeUpdate("UPDATE stats SET " +
                "download_count = ?, last_downloaded = ?, last_downloaded_by = ? WHERE node_id = ?",
                stats.getDownloadCount(), stats.getLastDownloaded(), stats.getLastDownloadedBy(), stats.getNodeId());
    }

    public int createStats(Stat stats) throws SQLException {
        log.debug("Creating stats {}", stats);
        return jdbcHelper.executeUpdate("INSERT INTO stats VALUES (?, ?, ?, ?)", stats.getNodeId(),
                stats.getDownloadCount(), stats.getLastDownloaded(), stats.getLastDownloadedBy());
    }

    public int deleteStats(long nodeId) throws SQLException {
        log.debug("Deleting stats of node {}", nodeId);
        return jdbcHelper.executeUpdate("DELETE FROM stats WHERE node_id = ?", nodeId);
    }

    public boolean hasStats(long nodeId) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT COUNT(1) FROM stats WHERE node_id = ?", nodeId);
            if (resultSet.next()) {
                int propsCount = resultSet.getInt(1);
                return propsCount > 0;
            }
            return false;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    private Stat statFromResultSet(ResultSet rs) throws SQLException {
        return new Stat(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4));
    }
}
