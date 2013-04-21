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

package org.artifactory.storage.db.fs.service;

import com.google.common.collect.Maps;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.factory.xstream.XStreamInfoFactory;
import org.artifactory.fs.MutableStatsInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.model.xstream.fs.StatsImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.fs.dao.StatsDao;
import org.artifactory.storage.db.fs.entity.Stat;
import org.artifactory.storage.fs.VfsException;
import org.artifactory.storage.fs.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A business service to interact with the node stats table.
 *
 * @author Yossi Shaul
 */
@Service
public class StatsServiceImpl implements InternalStatsService {
    private static final Logger log = LoggerFactory.getLogger(StatsServiceImpl.class);

    @Autowired
    private StatsDao statsDao;

    @Autowired
    private FileService fileService;

    /**
     * Stores the statistics events in memory and periodically flush to the storage
     */
    private ConcurrentMap<RepoPath, StatsEvent> statsEvents = Maps.newConcurrentMap();

    @Override
    public StatsInfo getStats(RepoPath repoPath) {
        StatsInfo statsInfo = getStatsFromStorage(repoPath);
        StatsEvent event = getStatsFromEvents(repoPath);
        if (event == null) {
            return statsInfo;
        }

        // return the merged results between the storage and the event
        long downloadCount = event.eventCount.get() + (statsInfo != null ? statsInfo.getDownloadCount() : 0);
        StatsImpl mergedStats = new StatsImpl();
        mergedStats.setLastDownloaded(event.downloadedTime);
        mergedStats.setLastDownloadedBy(event.downloadedBy);
        mergedStats.setDownloadCount(downloadCount);
        return mergedStats;
    }

    private StatsInfo getStatsFromStorage(RepoPath repoPath) {
        long nodeId = fileService.getNodeId(repoPath);
        if (nodeId > DbService.NO_DB_ID) {
            return loadStats(nodeId);
        } else {
            return null;
        }
    }

    private StatsInfo loadStats(long nodeId) {
        try {
            Stat stats = statsDao.getStats(nodeId);
            if (stats != null) {
                return statToStatsInfo(stats);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new VfsException("Failed to load stats for " + nodeId, e);
        }
    }

    @Override
    public synchronized void fileDownloaded(RepoPath repoPath, String downloadedBy, long downloadedTime) {
        StatsEvent newEvent = new StatsEvent(repoPath, downloadedBy, downloadedTime);
        StatsEvent oldEvent = statsEvents.put(repoPath, newEvent);
        if (oldEvent != null) {
            // if we found old event for the same repo path, add the old count to the new event
            newEvent.eventCount.addAndGet(oldEvent.eventCount.get());
        }
    }

    @Override
    public int setStats(long nodeId, StatsInfo statsInfo) {
        try {
            deleteStats(nodeId);
            return statsDao.createStats(statInfoToStat(nodeId, statsInfo));
        } catch (SQLException e) {
            throw new VfsException("Failed to set stats on node " + nodeId, e);
        }
    }

    @Override
    public boolean deleteStats(long nodeId) {
        try {
            int deletedCount = statsDao.deleteStats(nodeId);
            return deletedCount > 0;
        } catch (SQLException e) {
            throw new VfsException("Failed to delete stats from node id " + nodeId, e);
        }
    }

    @Override
    public boolean hasStats(RepoPath repoPath) {
        if (statsEvents.containsKey(repoPath)) {
            return true;
        }
        try {
            long nodeId = fileService.getNodeId(repoPath);
            if (nodeId > 0) {
                return statsDao.hasStats(nodeId);
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to check stats existence for " + repoPath);
        }
    }

    @Override
    public void flushStats() {
        if (!statsEvents.isEmpty()) {
            InternalStatsService txStatsService = ContextHelper.get().beanForType(InternalStatsService.class);
            txStatsService.doFlushStats();
        }
    }

    @Override
    public synchronized void doFlushStats() {
        log.trace("Flushing statistics to storage");
        Iterator<Map.Entry<RepoPath, StatsEvent>> iterator = statsEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<RepoPath, StatsEvent> statsEventEntry = iterator.next();
            iterator.remove();
            log.debug("Flushing statistics : {}", statsEventEntry.getValue());
            createOrUpdateStats(statsEventEntry.getValue());
        }
        log.trace("Flushing statistics done");
    }

    private void createOrUpdateStats(StatsEvent event) {
        long nodeId = fileService.getNodeId(event.repoPath);
        if (nodeId == DbService.NO_DB_ID) {
            log.debug("Attempting to update stats of non-existing node at: {}", event.repoPath);
            return;
        }
        try {
            Stat stats = statsDao.getStats(nodeId);
            if (stats != null) {
                stats = new Stat(nodeId, stats.getDownloadCount() + event.eventCount.get(),
                        event.downloadedTime, event.downloadedBy);
                statsDao.updateStats(stats);
            } else {
                stats = new Stat(nodeId, event.eventCount.get(), event.downloadedTime, event.downloadedBy);
                statsDao.createStats(stats);
            }
        } catch (SQLException e) {
            log.error("Failed to update stats for " + event.repoPath + ": " + e.getMessage());
            log.debug("Failed to update stats for " + event.repoPath, e.getMessage());
        }
    }

    private StatsEvent getStatsFromEvents(RepoPath repoPath) {
        return statsEvents.get(repoPath);
    }

    private StatsInfo statToStatsInfo(Stat stat) {
        MutableStatsInfo statsInfo = new XStreamInfoFactory().createStats();
        statsInfo.setDownloadCount(stat.getDownloadCount());
        statsInfo.setLastDownloaded(stat.getLastDownloaded());
        statsInfo.setLastDownloadedBy(stat.getLastDownloadedBy());
        return statsInfo;
    }

    private Stat statInfoToStat(long nodeId, StatsInfo statsInfo) {
        return new Stat(nodeId, statsInfo.getDownloadCount(), statsInfo.getLastDownloaded(),
                statsInfo.getLastDownloadedBy());
    }

    private class StatsEvent {
        private final RepoPath repoPath;
        private final String downloadedBy;
        private final long downloadedTime;
        private final AtomicInteger eventCount;

        public StatsEvent(RepoPath repoPath, String downloadedBy, long downloadedTime) {
            this.repoPath = repoPath;
            this.downloadedBy = downloadedBy;
            this.downloadedTime = downloadedTime;
            this.eventCount = new AtomicInteger(1);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(repoPath).append("|").append(eventCount).append("|").append(downloadedBy)
                    .append("|").append(downloadedTime);
            return sb.toString();
        }
    }
}
