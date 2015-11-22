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

import com.google.common.base.Strings;
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

    /**
     * Fetches all stats for specific nodeId
     *
     * @param nodeId - artifact identifier
     * @param supportRemoteStats
     *
     * @return {@link Stat}
     *
     * @throws SQLException
     */
    @Nullable
    public Stat getStats(long nodeId, boolean supportRemoteStats) throws SQLException {
        Stat stat;

        stat = getLocalStats(nodeId);
        if (supportRemoteStats) {
            stat = updateRemoteDownloadStats(nodeId, stat);
        }

        return stat;
    }

    /**
     * Fetches local or remote stats for specific nodeId,
     * (remote stats fetched by specific origin)
     *
     * @param nodeId - artifact identifier
     * @param origin - host initiated download
     * @param supportRemoteStats
     *
     * @return {@link Stat}
     *
     * @throws SQLException
     */
    @Nullable
    public Stat getStats(long nodeId, String origin, boolean supportRemoteStats) throws SQLException {
        ResultSet resultSet = null;
        Stat stat = null;
        try {
            if(Strings.isNullOrEmpty(origin)) { // get local stats
                return getLocalStats(nodeId);
            } else if (supportRemoteStats) { // get remote stats
                return getRemoteStats(nodeId, origin);
            }
        } finally {
            DbUtils.close(resultSet);
        }
        return stat;
    }

    /**
     * update remote Download stats
     * @param nodeId - node id
     * @param localStats - stats
     * @throws SQLException
     */
    private Stat updateRemoteDownloadStats(long nodeId, Stat localStats) throws SQLException {
        Stat remoteStat = remoteStatsDownload(nodeId);
        Stat mergedStat = merge(localStats, remoteStat);
        if (localStats == null && mergedStat.getRemoteDownloadCount() == 0)
            return localStats;
        return mergedStat;
    }

    /**
     * Merges local and remote stats
     *
     * @param localStats
     * @param remoteStat
     */
    private Stat merge(Stat localStats, Stat remoteStat) {
        if(localStats == null) {
            return remoteStat;
        }
        if (remoteStat != null) {
            localStats.setRemoteDownloadCount(remoteStat.getRemoteDownloadCount());
            localStats.setRemoteLastDownloadedBy(remoteStat.getRemoteLastDownloadedBy());
            localStats.setRemoteLastDownloaded(remoteStat.getRemoteLastDownloaded());
            localStats.setOrigin(remoteStat.getOrigin());
            localStats.setPath(remoteStat.getPath());
        }
        return localStats;
    }

    /**
     * Fetches local stats for the given nodeId
     *
     * @param nodeId - node id

     * @return {@link Stat}
     *
     * @throws SQLException
     */
    private Stat getLocalStats(long nodeId) throws SQLException {
        ResultSet resultSet = null;
        Stat stat = null;

        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM stats WHERE node_id = ?", nodeId);
            if (resultSet.next()) {
                stat = statFromLocalResultSet(resultSet);

            }
        } finally {
            DbUtils.close(resultSet);
        }
        return stat;
    }

    /**
     * Fetches remote stats for the given nodeId and origin
     *
     * @param nodeId - node id
     * @param origin - host initiated download

     * @return {@link Stat}
     *
     * @throws SQLException
     */
    private Stat getRemoteStats(long nodeId, String origin) throws SQLException {
        ResultSet resultSet = null;
        Stat stat = null;

        try {
            resultSet = jdbcHelper.executeSelect("SELECT download_count, last_downloaded, last_downloaded_by " +
                    "FROM stats_remote WHERE node_id = ? and origin = ?", nodeId, origin);
            if (resultSet.next()) {
                stat = statFromRemoteResultSet(resultSet);
            }else {
                return null;
            }
        } finally {
            DbUtils.close(resultSet);
        }
        return stat;
    }

    /**
     * Fetches remote part of the stats
     *
     * @param nodeId - node id
     * @return {@link Stat}
     *
     * @throws SQLException
     */
    private Stat remoteStatsDownload(long nodeId) throws SQLException {
        ResultSet resultSetCount = null;
        ResultSet resultSet = null;
        Stat stat = null;
        try {
            resultSetCount = jdbcHelper.executeSelect("SELECT SUM(download_count), MAX(last_downloaded) " +
                    "FROM stats_remote WHERE node_id = ?", nodeId);
            if (resultSetCount.next()) {
                stat = remoteStatDownloadResultSet(resultSetCount);
                resultSet = jdbcHelper.executeSelect("SELECT last_downloaded_by, origin, path FROM stats_remote WHERE node_id = ?" +
                        " and last_downloaded = ?", nodeId,stat.getRemoteLastDownloaded());
                if (resultSet.next()) {
                    stat.setRemoteLastDownloadedBy(resultSet.getString(1));
                    stat.setOrigin(resultSet.getString(2));
                    stat.setPath(resultSet.getString(3));
                }
            }else {
                return null;
            }
        } finally {
            DbUtils.close(resultSetCount);
            DbUtils.close(resultSet);
        }
        return stat;
    }

    /**
     * check if this node had data from this specific origin (on remote_stats),
     * this check require to determine if an update or create is require
     * @param nodeId - node id
     * @param origin - origin address
     * @return - remote statistic
     * @throws SQLException
     */
    @Nullable
    private boolean isOriginExistForThisNodeId(long nodeId, String origin) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM stats_remote WHERE node_id = ? and origin = ?" , nodeId,origin);
            if (resultSet.next()) {
                return true;
            }else{
                return false;
            }
        } finally {
            DbUtils.close(resultSet);
        }
    }

    /**
     * insert new remote stats if not exist
     * @param stats - stats to insert
     * @return - insert action status
     * @throws SQLException
     */
    private int createRemoteStats(Stat stats) throws SQLException {
        log.debug("Creating stats {}", stats);
        return jdbcHelper.executeUpdate("INSERT INTO stats_remote VALUES (?,?,?,?,?,?)", stats.getNodeId(),stats.getOrigin(),
                stats.getRemoteDownloadCount(), stats.getRemoteLastDownloaded(), stats.getRemoteLastDownloadedBy(), stats.getPath()
        );
    }

    /**
     * delete remote stats
     * @param nodeId - node id
     * @return - delete action status
     * @throws SQLException
     */
    private int deleteRemoteStats(long nodeId) throws SQLException {
        log.debug("Deleting stats of node {}", nodeId);
        return jdbcHelper.executeUpdate("DELETE FROM stats_remote WHERE node_id = ?", nodeId);
    }

    /**
     * update remote stats
     * @param stats - stats to delete
     * @return - update action status
     * @throws SQLException
     */
    private int updateRemoteStats(Stat stats) throws SQLException {
        log.debug("Updating stats {}", stats);
        return jdbcHelper.executeUpdate("UPDATE stats_remote SET " +
                        "download_count = ?, last_downloaded = ?, last_downloaded_by = ?, path = ? " +
                        "WHERE node_id = ? and origin = ?",
                stats.getRemoteDownloadCount(), stats.getRemoteLastDownloaded(), stats.getRemoteLastDownloadedBy(),
                stats.getPath(), stats.getNodeId(),stats.getOrigin()
        );
    }

    /**
     * Updates stats in DB
     *
     * @param stats {@link Stat} to write to DB
     * @param supportRemoteStats is SmartRepo feature on
     *
     * @return -1 : failure, 0 : nothing changed, 1 : success
     * @throws SQLException
     */
    public int updateStats(Stat stats, boolean supportRemoteStats) throws SQLException {
        log.debug("Updating stats {}", stats);
        if (!stats.isRemote()) { // update local stats
            return jdbcHelper.executeUpdate("UPDATE stats SET " +
                            "download_count = ?, last_downloaded = ?, last_downloaded_by = ? WHERE node_id = ?",
                    stats.getLocalDownloadCount(), stats.getLocalLastDownloaded(), stats.getLocalLastDownloadedBy(),
                    stats.getNodeId()
            );
        } else if (supportRemoteStats) { // create or update remote stats data
            return createUpdateRemoteStatData(stats);
        }
        return 0;
    }

    /**
     * create or update remote stats data
     * @param stats - stats
     * @return - update or create status
     * @throws SQLException
     */
    private int createUpdateRemoteStatData(Stat stats) throws SQLException {
        boolean isOriginExistForThisNodeId = isOriginExistForThisNodeId(stats.getNodeId(), stats.getOrigin());
        if (!isOriginExistForThisNodeId) {
            return createRemoteStats(stats);
        }
        else{
            return updateRemoteStats(stats);
        }
    }

    public int createStats(Stat stats, boolean supportRepoStats) throws SQLException {
        log.debug("Creating stats {}", stats);

        if (stats.isRemote()) {
            if (supportRepoStats && !isOriginExistForThisNodeId(stats.getNodeId(), stats.getOrigin())) {
                return createRemoteStats(stats);
            }
        } else {
            return jdbcHelper.executeUpdate("INSERT INTO stats VALUES (?, ?, ?, ?)", stats.getNodeId(),
                    stats.getLocalDownloadCount(), stats.getLocalLastDownloaded(), stats.getLocalLastDownloadedBy()
            );
        }
        return 0; // no change
    }

    public int deleteStats(long nodeId, boolean supportRemoteStats) throws SQLException {
        log.debug("Deleting stats of node {}", nodeId);
        int remoteStatsDeleted = 0;
        int localStatsDeleted = jdbcHelper.executeUpdate("DELETE FROM stats WHERE node_id = ?", nodeId);
        if (supportRemoteStats) {
            remoteStatsDeleted = deleteRemoteStats(nodeId);
        }
        return localStatsDeleted+remoteStatsDeleted;
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

    private Stat statFromLocalResultSet(ResultSet rs) throws SQLException {
        return new Stat(rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getString(4), 0, 0, null
        );
    }

    private Stat remoteStatDownloadResultSet(ResultSet rs) throws SQLException {
        return new Stat(0,0,0,null,rs.getLong(1),rs.getLong(2), null, null, null);
    }

    private Stat statFromRemoteResultSet(ResultSet rs) throws SQLException {
        return new Stat(0,0,0,null,rs.getLong(1),rs.getLong(2), rs.getString(3), null, null);
    }
}
