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
import com.google.common.collect.Maps;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ha.semaphore.SemaphoreWrapper;
import org.artifactory.addon.smartrepo.SmartRepoAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.common.ConstantValues;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.MutableStatsInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.fs.dao.StatsDao;
import org.artifactory.storage.db.fs.entity.Stat;
import org.artifactory.storage.fs.VfsException;
import org.artifactory.storage.fs.service.FileService;
import org.artifactory.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Michael Pasternak on 8/25/15.
 */
public abstract class AbstractStatsService {

    private static final Logger log = LoggerFactory.getLogger(AbstractStatsService.class);
    public static final int DEFAULT_NB_STATS_SAVED_PER_TX = 30;
    private ConcurrentMap<RepoPath, StatsEvent> statsEvents = Maps.newConcurrentMap();

    @Autowired
    private StatsDao statsDao;

    @Autowired
    private FileService fileService;

    @Autowired
    private DbService dbService;

    public StatsDao getStatsDao() {
        return statsDao;
    }

    public FileService getFileService() {
        return fileService;
    }

    public DbService getDbService() {
        return dbService;
    }

    public ConcurrentMap<RepoPath, StatsEvent> getStatsEvents() {
        return statsEvents;
    }

    public void flushStats() {
        if (getStatsEvents().isEmpty()) {
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
        int sizeOnEntry = getStatsEvents().size();
        log.debug("Flushing {} statistics to storage", sizeOnEntry);
        Iterator<Map.Entry<RepoPath, StatsEvent>> iterator = getStatsEvents().entrySet().iterator();
        int processed = 0;
        TransactionStatus txStatus = null;
        int savedPerTx = DEFAULT_NB_STATS_SAVED_PER_TX;
        if (getDbService().getDatabaseType() == DbType.POSTGRESQL) {
            // PostgreSQL does not support saving more data after a constraint failure (FK, PK, ...)
            savedPerTx = 1;
        }
        try {
            onTraversingStart();
            while (iterator.hasNext()) {
                final StatsEvent event = iterator.next().getValue();
                log.trace("Flushing statistics : {}", event);
                if (txStatus == null) {
                    txStatus = startTransaction();
                }
                if (isWriteLocked(event)) {
                    log.debug("Attempting to update stats of write locked node at: {}", event.getRepoPath());
                    continue;
                }
                iterator.remove(); //remove the object prior to sampling its value to avoid atomicity problems
                processed++;
                StatsSaveResult saveResult = createOrUpdateStats(event);
                switch (saveResult) {
                    case Ignored:
                        log.debug("Attempting to update stats of non-existing or folder node at: {}",
                                event.getRepoPath());
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
            onTraversingEnd();
        }
        log.debug("Successfully flushed {} statistics from total of {}", processed, sizeOnEntry);
    }

    protected enum StatsSaveResult {
        Updated, Ignored, Failed;
    }

    public static class StatsEvent {
        public static final String PATH_DELIMITER = "->";
        private final RepoPath repoPath;

        private final AtomicLong localEventCount;
        private String localDownloadedBy;
        private long localDownloadedTime;

        private final AtomicLong remoteEventCount;
        private String remoteDownloadedBy;
        private long remoteDownloadedTime;
        private String origin;
        private StringBuilder path; // a path from the download triggering host (an origin)
                                    // to the actual node containing the artifact

        public StatsEvent(RepoPath repoPath) {
            this.repoPath = repoPath;
            this.localEventCount = new AtomicLong();
            this.remoteEventCount = new AtomicLong();
            this.path = new StringBuilder();
        }

        public StatsEvent(RepoPath repoPath, String origin) {
            this.repoPath = repoPath;
            this.localEventCount = new AtomicLong();
            this.remoteEventCount = new AtomicLong();
            this.origin = origin;
            this.path = new StringBuilder();
        }

        /**
         * Updates local statistics
         *
         * @param downloadedBy an local user was logged in
         *                     at the time of update
         * @param downloadedTime a time stamp of download request
         */
        public void update(String downloadedBy, long downloadedTime) {
            this.localDownloadedBy = downloadedBy;
            this.localDownloadedTime = downloadedTime;
            localEventCount.incrementAndGet();
        }

        /**
         * Updates remote statistics with foreign counter
         *
         * @param downloadedBy The remote user was logged in
         *                     at the time of update
         * @param origin The remote host the download was triggered by
         * @param path   The round trip of download request
         * @param downloadedTime a time stamp of download request
         * @param delta  The download counter to add
         */
        public void update(String downloadedBy, String origin, String path, long downloadedTime, long delta) {
            this.remoteDownloadedBy = downloadedBy;
            this.remoteDownloadedTime = downloadedTime;
            if(Strings.isNullOrEmpty(origin))
                this.origin = origin;
            this.remoteEventCount.addAndGet(delta);
            updatePath(origin, path);
        }

        /**
         * Appends intermediateOrigin to the path
         *
         * @param origin The remote host participating in
         *               download request trip (e.g a->c->d->...)
         * @param path   The round trip of download request
         */
        private void updatePath(String origin, String path) {
            if (!Strings.isNullOrEmpty(origin) ) {
                if (!Strings.isNullOrEmpty(path)) {
                    this.path.append(path).append(PATH_DELIMITER);
                }
                this.path.append(origin).append(PATH_DELIMITER);
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
                     sb.append(repoPath)

                    .append("|").append(localEventCount)
                    .append("|").append(localDownloadedBy)
                    .append("|").append(localDownloadedTime)

                    .append("|").append(remoteEventCount)
                    .append("|").append(remoteDownloadedBy)
                    .append("|").append(remoteDownloadedTime)
                    .append("|").append(origin)
                    .append("|").append(getPath());

            return sb.toString();
        }

        public RepoPath getRepoPath() {
            return repoPath;
        }


        public AtomicLong getRemoteEventCount() {
            return remoteEventCount;
        }

        public String getRemoteDownloadedBy() {
            return remoteDownloadedBy;
        }

        public long getRemoteDownloadedTime() {
            return remoteDownloadedTime;
        }

        /**
         * @return remote host the download was triggered by
         */
        public String getOrigin() {
            return origin;
        }

        /**
         * @return a path representing artifact download trip
         */
        public String getPath() {
            String pathString;
            if (path.length() != 0 &&
                    (pathString = path.toString()).endsWith(PATH_DELIMITER)) {
                    return StringUtils.replaceLast(pathString, PATH_DELIMITER, "");
            }
            return path.toString();
        }

        /**
         * @return determination on whether this {@link StatsEvent}
         *         has remote content
         */
        public boolean hasRemoteContent() {
            return !Strings.isNullOrEmpty(origin);
        }

        public AtomicLong getLocalEventCount() {
            return localEventCount;
        }

        public String getLocalDownloadedBy() {
            return localDownloadedBy;
        }

        public long getLocalDownloadedTime() {
            return localDownloadedTime;
        }
    }

    protected abstract SemaphoreWrapper getFlushingSemaphore();

    protected void commitOrRollback(TransactionStatus txStatus) {
        if (txStatus == null) {
            return;
        }
        if (!txStatus.isRollbackOnly()) {
            commitTransaction(txStatus);
        } else {
            rollbackTransaction(txStatus);
        }
    }

    protected boolean isWriteLocked(StatsEvent event) {
        // Repository service not available in storage test until core module
        RepositoryService repositoryService = ContextHelper.get().beanForType(RepositoryService.class);
        if (repositoryService != null) {
            return repositoryService.isWriteLocked(event.getRepoPath());
        }
        return false;
    }

    protected void commitTransaction(TransactionStatus status) {
        getTransactionManager().commit(status);
    }

    protected void rollbackTransaction(TransactionStatus status) {
        getTransactionManager().rollback(status);
    }

    protected AbstractPlatformTransactionManager getTransactionManager() {
        return (AbstractPlatformTransactionManager) ContextHelper.get().getBean("artifactoryTransactionManager");
    }

    protected TransactionStatus startTransaction() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("StatsTransaction");
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        AbstractPlatformTransactionManager txManager = getTransactionManager();
        return txManager.getTransaction(def);
    }

    /**
     * @param event
     * @return true if element should be consumed, false otherwise
     */
    protected StatsSaveResult createOrUpdateStats(StatsEvent event) {
        long nodeId = getFileService().getFileNodeId(event.getRepoPath());
        if (nodeId == DbService.NO_DB_ID) {
            return StatsSaveResult.Ignored;
        }

        try {
            Stat stats = getStatistics(nodeId, event.getOrigin());
            processStats(event, nodeId, stats);
            return StatsSaveResult.Updated;
        } catch (SQLException e) {
            log.warn("Failed to update stats for " + event.getRepoPath() + ": " + e.getMessage() +
                    "\nNode may have been deleted?");
            log.debug("Failed to update stats for " + event.getRepoPath(), e);
            return StatsSaveResult.Failed;
        }
    }

    /**
     * Load stats from DB
     *
     * @param repoPath identifier
     *
     * @return {@link StatsInfo}
     */
    protected StatsInfo getStatsFromStorage(RepoPath repoPath) {
        long nodeId = getFileService().getNodeId(repoPath);
        if (nodeId > DbService.NO_DB_ID) {
            return loadStats(nodeId);
        } else {
            return null;
        }
    }

    private StatsInfo loadStats(long nodeId) {
        try {
            Stat stats = getStatistics(nodeId);
            if (stats != null) {
                return statToStatsInfo(stats);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new VfsException("Failed to load stats for " + nodeId, e);
        }
    }

    /**
     * Fetches local and remote statistics for specific origin
     *
     * @param nodeId - node id
     * @param origin - origin initiated download
     *
     * @return {@link Stat}
     *
     * @throws SQLException
     */
    private Stat getStatistics(long nodeId, String origin) throws SQLException {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SmartRepoAddon smartRepoAddon = addonsManager.addonByType(SmartRepoAddon.class);
        return getStatsDao().getStats(nodeId, origin, smartRepoAddon.supportRemoteStats());
    }

    /**
     * get local and remote statistics
     *
     * @param nodeId - node id
     * @return statistics
     * @throws SQLException
     */
    private Stat getStatistics(long nodeId) throws SQLException {
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SmartRepoAddon smartRepoAddon = addonsManager.addonByType(SmartRepoAddon.class);
        return getStatsDao().getStats(nodeId, smartRepoAddon.supportRemoteStats());
    }

    /**
     * Fetches StatsEvent by RepoPath
     *
     * @param repoPath
     *
     * @return {@link StatsEvent}
     */
    protected StatsEvent getStatsFromEvents(RepoPath repoPath) {
        return getStatsEvents().get(repoPath);
    }

    /**
     * Converts Stat to StatsInfo
     *
     * @param stat
     *
     * @return {@link StatsInfo}
     */
    protected StatsInfo statToStatsInfo(Stat stat) {
        MutableStatsInfo statsInfo = InfoFactoryHolder.get().createStats();
        statsInfo.setDownloadCount(stat.getLocalDownloadCount());
        statsInfo.setLastDownloaded(stat.getLocalLastDownloaded());
        statsInfo.setLastDownloadedBy(stat.getLocalLastDownloadedBy());

        statsInfo.setRemoteDownloadCount(stat.getRemoteDownloadCount());
        statsInfo.setRemoteLastDownloaded(stat.getRemoteLastDownloaded());
        statsInfo.setRemoteLastDownloadedBy(stat.getRemoteLastDownloadedBy());
        statsInfo.setOrigin(stat.getOrigin());

        return statsInfo;
    }

    /**
     * Converts StatsInfo to Stat
     *
     * @param nodeId
     * @param statsInfo
     *
     * @return {@link Stat}
     */
    protected Stat statInfoToStat(long nodeId, StatsInfo statsInfo) {
        return new Stat(
                nodeId,
                statsInfo.getDownloadCount(), statsInfo.getLastDownloaded(), statsInfo.getLastDownloadedBy(),
                statsInfo.getRemoteDownloadCount(), statsInfo.getRemoteLastDownloaded(), statsInfo.getRemoteLastDownloadedBy()
        );
    }

    /**
     * Performs stats processing
     *
     * @param event
     * @param nodeId
     * @param stats
     * 
     * @throws SQLException
     */
    protected abstract void processStats(StatsEvent event, long nodeId, Stat stats) throws SQLException;

    /**
     * Occurs before traversing queued events
     */
    protected abstract void onTraversingStart();

    /**
     * Occurs after traversing queued events
     */
    protected abstract void onTraversingEnd();
}
