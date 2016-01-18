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

import com.google.common.base.Strings;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.addon.ha.semaphore.SemaphoreWrapper;
import org.artifactory.addon.smartrepo.SmartRepoAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.fs.StatsInfo;
import org.artifactory.model.xstream.fs.StatsImpl;
import org.artifactory.repo.RemoteRepoPath;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.fs.entity.Stat;
import org.artifactory.storage.fs.VfsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Map;

/**
 * A business service to interact with the node stats table.
 *
 * @author Yossi Shaul
 */
@Service
public class StatsPersistingServiceImpl extends AbstractStatsService implements StatsPersistingService {

    private static final Logger log = LoggerFactory.getLogger(StatsPersistingServiceImpl.class);
    private SemaphoreWrapper flushingSemaphore;

    /**
     * Collects storage and cache stats
     *
     * @param repoPath
     *
     * @return {@link StatsInfo}
     */
    @Override
    public StatsInfo getStats(RepoPath repoPath) {
        StatsImpl mergedStats = null;
        StatsInfo statsInfo = getStatsFromStorage(repoPath);
        StatsEvent event = getStatsFromEvents(repoPath);

        if (event != null) {
            // return the merged results between the storage and the event
            long localDownloadCount = event.getLocalEventCount().get() +
                    (statsInfo != null ? statsInfo.getDownloadCount() : 0);

            // return the merged results between the storage and the event
            long downloadRemoteCount = event.getRemoteEventCount().get() +
                    (statsInfo != null ? statsInfo.getRemoteDownloadCount() : 0);

            mergedStats = new StatsImpl(statsInfo);
            mergedStats.setDownloadCount(localDownloadCount);
            if(!Strings.isNullOrEmpty(event.getLocalDownloadedBy())) {
                mergedStats.setLastDownloaded(event.getLocalDownloadedTime());
                mergedStats.setLastDownloadedBy(event.getLocalDownloadedBy());
            }

            mergedStats.setRemoteDownloadCount(downloadRemoteCount);
            if(!Strings.isNullOrEmpty(event.getOrigin())) {
                mergedStats.setRemoteLastDownloaded(event.getRemoteDownloadedTime());
                mergedStats.setRemoteLastDownloadedBy(event.getRemoteDownloadedBy());
                mergedStats.setOrigin(event.getOrigin());
            }
            return mergedStats;
        } else if (statsInfo != null) {
            mergedStats = new StatsImpl(statsInfo);
            return injectRemoteEvents(repoPath, mergedStats);
        }
        return statsInfo;
    }

    /**
     * Aggregate and injects remote events counters to mergedStats
     *
     * statistics events may come from different origins for same repoPath,
     * in sake if origin uniqueness, we maintain event per origin, but when
     * we need statistics for specific repoPath, we need to accumulate all
     * remote events for the repoPath
     *
     * @param repoPath {@link RepoPath}
     * @param mergedStats statistics holder to merge to
     *
     * @return mergedStats
     */
    private StatsInfo injectRemoteEvents(RepoPath repoPath, StatsImpl mergedStats) {
        for(Map.Entry<RepoPath, StatsEvent> entry : getStatsEvents().entrySet()) {
            if(!entry.getValue().hasRemoteContent()) continue;
            if(entry.getKey() instanceof RemoteRepoPath) {
            // TODO: do we need this ^ ? previous line makes sure we have RemoteContent
                if(((RemoteRepoPath)entry.getKey()).getActualRepoPath().equals(repoPath)){
                    if(mergedStats == null) {
                        mergedStats = new StatsImpl();
                    }
                    merge(entry.getValue(), mergedStats);
                }
            }
        }

        return mergedStats;
    }

    /**
     * Performs merge of an {@link StatsEvent} into {@link StatsImpl}
     *
     * @param statsEvent
     * @param mergedStats
     */
    private void merge(StatsEvent statsEvent, StatsImpl mergedStats) {
        if (statsEvent != null) {

            long downloadRemoteCount = statsEvent.getRemoteEventCount().get() +
                    (mergedStats != null ? mergedStats.getRemoteDownloadCount() : 0);

            mergedStats.setRemoteDownloadCount(downloadRemoteCount);

            if(statsEvent.getRemoteDownloadedTime() > mergedStats.getRemoteLastDownloaded()) {
                mergedStats.setRemoteLastDownloaded(statsEvent.getRemoteDownloadedTime());
                mergedStats.setRemoteLastDownloadedBy(statsEvent.getRemoteDownloadedBy());
                mergedStats.setOrigin(statsEvent.getOrigin());
            }
        }
    }

    /**
     * Triggered on artifact local download event,
     * queueing event for delegation
     *
     * @param repoPath       The file repo path to set/update stats
     * @param downloadedBy   User who downloaded the file
     * @param downloadedTime Time the file was downloaded
     * @param fromAnotherArtifactory specifying whether request comes fromAnotherArtifactory
     */
    @Override
    public synchronized void fileDownloaded(RepoPath repoPath, String downloadedBy, long downloadedTime, boolean fromAnotherArtifactory) {
        log.debug("Queuing downloaded resource '{}' by '{}' at '{}', fromAnotherArtifactory: '{}'",
                repoPath, downloadedBy, downloadedTime, fromAnotherArtifactory);

        StatsEvent statsEvent = getStatsEvents().get(repoPath);
        if (statsEvent == null) {
            getStatsEvents().put(repoPath, statsEvent = new StatsEvent(repoPath));
        }
        statsEvent.update(downloadedBy, downloadedTime);
    }

    /**
     * Triggered on artifact remote download event
     *
     * @param origin             The remote host the download was triggered by
     * @param path               The round trip of download request
     * @param repoPath           The file repo path to set/update stats
     * @param downloadedBy       User who downloaded the file
     * @param downloadedTime     Time the file was downloaded
     * @param count              Amount of performed downloads
     */
    @Override
    public synchronized void fileDownloadedRemotely(String origin, String path, RepoPath repoPath,
            String downloadedBy, long downloadedTime, long count) {

        log.debug("Queuing resource '{}' downloaded remotely by '{}', at '{}', from {}, count: '{}'",
                repoPath, downloadedBy, downloadedTime, origin, count);

        StatsEvent statsEvent = getStatsEvents().get(repoPath);
        if (statsEvent == null) {
            getStatsEvents().put(repoPath, statsEvent = new StatsEvent(repoPath, origin));
        }
        statsEvent.update(downloadedBy, origin, path, downloadedTime, count);
    }

    @Override
    public int setStats(long nodeId, StatsInfo statsInfo) {
        log.debug("Setting stats '{}' for node '{}'", statsInfo, nodeId);
        try {
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            SmartRepoAddon smartRepoAddon = addonsManager.addonByType(SmartRepoAddon.class);
            deleteStats(nodeId);
            int stats = getStatsDao().createStats(statInfoToStat(nodeId, statsInfo),
                    smartRepoAddon.supportRemoteStats());
            return stats;
        } catch (SQLException e) {
            throw new VfsException("Failed to set stats on node " + nodeId, e);
        }
    }

    @Override
    public boolean deleteStats(long nodeId) {
        log.debug("Deleting stats for node '{}'", nodeId);

        try {
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            SmartRepoAddon smartRepoAddon = addonsManager.addonByType(SmartRepoAddon.class);
            int deletedCount = getStatsDao().deleteStats(nodeId, smartRepoAddon.supportRemoteStats());
            return deletedCount > 0;
        } catch (SQLException e) {
            throw new VfsException("Failed to delete stats from node id " + nodeId, e);
        }
    }

    @Override
    public boolean hasStats(RepoPath repoPath) {
        if (getStatsEvents().containsKey(repoPath)) {
            return true;
        }
        try {
            long nodeId = getFileService().getNodeId(repoPath);
            if (nodeId > 0) {
                return getStatsDao().hasStats(nodeId);
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to check stats existence for " + repoPath);
        }
    }

    /**
     * Persists statistics
     *
     * @param event stats event
     * @param nodeId id representing artifact location
     * @param stats statistics
     *
     * @throws SQLException
     */
    @Override
    protected void processStats(StatsEvent event, long nodeId, Stat stats) throws SQLException {
        log.debug("Processing event: {} for stats {}", event, stats);
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SmartRepoAddon smartRepoAddon = addonsManager.addonByType(SmartRepoAddon.class);
        if (stats != null) {
            stats = new Stat(nodeId,
                    stats.getLocalDownloadCount() + event.getLocalEventCount().get(),
                    event.getLocalDownloadedTime() != 0 ? event.getLocalDownloadedTime() : stats.getLocalLastDownloaded(),
                    !Strings.isNullOrEmpty(event.getLocalDownloadedBy()) ? event.getLocalDownloadedBy() : stats.getLocalLastDownloadedBy(),

                    stats.getRemoteDownloadCount() + event.getRemoteEventCount().get() ,
                    event.getRemoteDownloadedTime() != 0 ? event.getRemoteDownloadedTime() : stats.getRemoteLastDownloaded(),
                    !Strings.isNullOrEmpty(event.getRemoteDownloadedBy()) ? event.getRemoteDownloadedBy() : stats.getRemoteLastDownloadedBy(),
                    !Strings.isNullOrEmpty(event.getOrigin()) ? event.getOrigin() : stats.getOrigin(),
                    !Strings.isNullOrEmpty(event.getPath()) ? event.getPath() : stats.getPath()
            );
            log.debug("Updating stats: {} according for event {}", stats, event);
            getStatsDao().updateStats(stats, smartRepoAddon.supportRemoteStats());
        } else {
            stats = new Stat(nodeId,
                    event.getLocalEventCount().get(), event.getLocalDownloadedTime(), event.getLocalDownloadedBy(),
                    event.getRemoteEventCount().get(), event.getRemoteDownloadedTime(), event.getRemoteDownloadedBy(),
                    event.getOrigin(), event.getPath()
            );

            log.debug("Creating new stats: {} according for event {}", stats, event);
            getStatsDao().createStats(stats, smartRepoAddon.supportRemoteStats());
        }
    }

    @Override
    protected SemaphoreWrapper getFlushingSemaphore() {
        if (flushingSemaphore == null) {
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
            flushingSemaphore = haCommonAddon.getSemaphore(HaCommonAddon.STATS_SEMAPHORE_NAME);
        }
        return flushingSemaphore;
    }

    @Override
    protected void onTraversingStart() {
        log.debug("Starting events processing");
    }

    @Override
    protected void onTraversingEnd() {
        log.debug("Events processing finished");
    }
}
