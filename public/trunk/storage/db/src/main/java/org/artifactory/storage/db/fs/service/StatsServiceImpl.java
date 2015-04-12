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
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.HaCommonAddon;
import org.artifactory.addon.ha.semaphore.SemaphoreWrapper;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.ConstantValues;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.MutableStatsInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.model.xstream.fs.StatsImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.fs.dao.StatsDao;
import org.artifactory.storage.db.fs.entity.Stat;
import org.artifactory.storage.fs.VfsException;
import org.artifactory.storage.fs.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A business service to interact with the node stats table.
 *
 * @author Yossi Shaul
 */
@Service
public class StatsServiceImpl implements InternalStatsService {
    private static final Logger log = LoggerFactory.getLogger(StatsServiceImpl.class);
    public static final int DEFAULT_NB_STATS_SAVED_PER_TX = 30;

    @Autowired
    private StatsDao statsDao;

    @Autowired
    private FileService fileService;

    @Autowired
    private DbService dbService;

    /**
     * Stores the statistics events in memory and periodically flush to the storage
     */
    private ConcurrentMap<RepoPath, StatsEvent> statsEvents = Maps.newConcurrentMap();

    private SemaphoreWrapper flushingSemaphore;

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
        StatsEvent statsEvent = statsEvents.get(repoPath);
        if (statsEvent == null) {
            statsEvents.put(repoPath, statsEvent = new StatsEvent(repoPath));
        }
        statsEvent.update(downloadedBy, downloadedTime);
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
        if (statsEvents.isEmpty()) {
            return;
        }
        try {
            if (!getFlushingSemaphore().tryAcquire(ConstantValues.statsFlushTimeoutSecs.getLong(), TimeUnit.SECONDS)) {
                log.debug("Received flush stats request, but another process is already running.");
                return;
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for stats flush", e);
            return;
        }

        try {
            doFlushStats();
        } finally {
            getFlushingSemaphore().release();
        }
    }

    private void doFlushStats() {
        int sizeOnEntry = statsEvents.size();
        log.debug("Flushing {} statistics to storage", sizeOnEntry);
        Iterator<Map.Entry<RepoPath, StatsEvent>> iterator = statsEvents.entrySet().iterator();
        int processed = 0;
        TransactionStatus txStatus = null;
        int savedPerTx = DEFAULT_NB_STATS_SAVED_PER_TX;
        if (dbService.getDatabaseType() == DbType.POSTGRESQL) {
            // PostgreSQL does not support saving more data after a constraint failure (FK, PK, ...)
            savedPerTx = 1;
        }
        try {
            while (iterator.hasNext()) {
                final StatsEvent event = iterator.next().getValue();
                log.trace("Flushing statistics : {}", event);
                if (txStatus == null) {
                    txStatus = startTransaction();
                }
                if (isWriteLocked(event)) {
                    log.debug("Attempting to update stats of write locked node at: {}", event.repoPath);
                    continue;
                }
                iterator.remove(); //remove the object prior to sampling its value to avoid atomicity problems
                processed++;
                StatsSaveResult saveResult = createOrUpdateStats(event);
                switch (saveResult) {
                    case Ignored:
                        log.debug("Attempting to update stats of non-existing or folder node at: {}", event.repoPath);
                        break;
                    case Updated:
                        if (processed % savedPerTx == 0) {
                            log.debug("Flushed {} statistics done, started with {}", processed, sizeOnEntry);
                            try {
                                commitOrRollback(txStatus);
                            } finally {
                                txStatus = null;
                            }
                        }
                        break;
                    case Failed:
                        log.debug("Flushed {} statistics done, started with {}", processed, sizeOnEntry);
                        try {
                            commitOrRollback(txStatus);
                        } finally {
                            txStatus = null;
                        }
                        break;
                }
            }
        } finally {
            commitOrRollback(txStatus);
        }
        log.debug("Successfully flushed {} statistics from total of {}", processed, sizeOnEntry);
    }

    private SemaphoreWrapper getFlushingSemaphore() {
        if (flushingSemaphore == null) {
            AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
            HaCommonAddon haCommonAddon = addonsManager.addonByType(HaCommonAddon.class);
            flushingSemaphore = haCommonAddon.getSemaphore(HaCommonAddon.STATS_SEMAPHORE_NAME);
        }
        return flushingSemaphore;
    }

    private void commitOrRollback(TransactionStatus txStatus) {
        if (txStatus == null) {
            return;
        }
        if (!txStatus.isRollbackOnly()) {
            commitTransaction(txStatus);
        } else {
            rollbackTransaction(txStatus);
        }
    }

    enum StatsSaveResult {Updated, Ignored, Failed}

    /**
     * @param event
     * @return true if element should be consumed, false otherwise
     */
    private StatsSaveResult createOrUpdateStats(StatsEvent event) {
        long nodeId = fileService.getFileNodeId(event.repoPath);
        if (nodeId == DbService.NO_DB_ID) {
            return StatsSaveResult.Ignored;
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
            return StatsSaveResult.Updated;
        } catch (SQLException e) {
            log.warn("Failed to update stats for " + event.repoPath + ": " + e.getMessage() +
                    "\nNode may have been deleted?");
            log.debug("Failed to update stats for " + event.repoPath, e);
            return StatsSaveResult.Failed;
        }
    }

    private boolean isWriteLocked(StatsEvent event) {
        // Repository service not available in storage test until core module
        RepositoryService repositoryService = ContextHelper.get().beanForType(RepositoryService.class);
        if (repositoryService != null) {
            return repositoryService.isWriteLocked(event.repoPath);
        }
        return false;
    }

    private StatsEvent getStatsFromEvents(RepoPath repoPath) {
        return statsEvents.get(repoPath);
    }

    private StatsInfo statToStatsInfo(Stat stat) {
        MutableStatsInfo statsInfo = InfoFactoryHolder.get().createStats();
        statsInfo.setDownloadCount(stat.getDownloadCount());
        statsInfo.setLastDownloaded(stat.getLastDownloaded());
        statsInfo.setLastDownloadedBy(stat.getLastDownloadedBy());
        return statsInfo;
    }

    private Stat statInfoToStat(long nodeId, StatsInfo statsInfo) {
        return new Stat(nodeId, statsInfo.getDownloadCount(), statsInfo.getLastDownloaded(),
                statsInfo.getLastDownloadedBy());
    }

    private TransactionStatus startTransaction() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("StatsTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        AbstractPlatformTransactionManager txManager = getTransactionManager();
        return txManager.getTransaction(def);
    }

    private void commitTransaction(TransactionStatus status) {
        getTransactionManager().commit(status);
    }

    private void rollbackTransaction(TransactionStatus status) {
        getTransactionManager().rollback(status);
    }

    private AbstractPlatformTransactionManager getTransactionManager() {
        return (AbstractPlatformTransactionManager) ContextHelper.get().getBean("artifactoryTransactionManager");
    }

    static class StatsEvent {
        private final RepoPath repoPath;
        private final AtomicInteger eventCount;
        private String downloadedBy;
        private long downloadedTime;

        StatsEvent(RepoPath repoPath) {
            this.repoPath = repoPath;
            this.eventCount = new AtomicInteger();
        }

        void update(String downloadedBy, long downloadedTime) {
            this.downloadedBy = downloadedBy;
            this.downloadedTime = downloadedTime;
            eventCount.incrementAndGet();
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
