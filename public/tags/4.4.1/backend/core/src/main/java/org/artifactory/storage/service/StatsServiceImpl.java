package org.artifactory.storage.service;

import org.artifactory.fs.StatsInfo;
import org.artifactory.model.xstream.fs.StatsImpl;
import org.artifactory.repo.RepoPath;
import org.artifactory.storage.db.fs.service.StatsPersistingServiceImpl;
import org.artifactory.storage.fs.service.StatsService;
import org.artifactory.storage.service.constraints.NotSystemUserCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

/**
 * Provides statistics persistence and delegation services
 *
 * @author Michael Pasternak
 */
@Service
public class StatsServiceImpl implements StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsServiceImpl.class);

    @Autowired
    private StatsPersistingServiceImpl statsPersistingService;

    @Autowired
    private StatsDelegatingServiceImpl statsDelegatingService;

    @Autowired
    private NotSystemUserCriteria notSystemUserCriteria;

    /**
     * Collects stats from both DB and queue of events
     *
     * @param repoPath The file repo path to get stats on
     *
     * @return {@link StatsInfo}
     */
    @Nullable
    @Override
    public StatsInfo getStats(RepoPath repoPath) {
        return (StatsImpl) statsPersistingService.getStats(repoPath);
    }

    /**
     * Triggered on local download event
     *
     * Event queued for local stats update and potential delegation
     *
     * @param repoPath       The file repo path to set/update stats
     * @param downloadedBy   User who downloaded the file
     * @param downloadedTime Time the file was downloaded
     * @param fromAnotherArtifactory specifying whether request comes fromAnotherArtifactory
     *                               (happens on first transitive download when artifact not in cache yet)
     */
    @Override
    public void fileDownloaded(RepoPath repoPath, String downloadedBy, long downloadedTime, boolean fromAnotherArtifactory) {
        log.debug("Resource '{}' was downloaded by '{}' at '{}', fromAnotherArtifactory: '{}'",
                repoPath, downloadedBy, downloadedTime, fromAnotherArtifactory);

        if(notSystemUserCriteria.meet(downloadedBy)) {
            statsPersistingService.fileDownloaded(repoPath, downloadedBy, downloadedTime, fromAnotherArtifactory);
            if (!fromAnotherArtifactory) {
                // we'd like to skip trigger comes fromAnotherArtifactory
                // as it anyway will be delegated via remoteDownload event
                statsDelegatingService.fileDownloaded(repoPath, downloadedBy, downloadedTime);
            }
        } else {
            log.debug("User {} is not answering desired criteria, ignoring ...", downloadedBy);
        }
    }

    /**
     * Triggered on remote download event
     *
     * Event queued for local stats update and potential delegation
     *
     * @param origin             The remote host the download was triggered by
     * @param path               The round trip of download request
     * @param repoPath           The file repo path to set/update stats
     * @param downloadedBy       User who downloaded the file
     * @param downloadedTime     Time the file was downloaded
     * @param count              Amount of performed downloads
     */
    @Override
    public void fileDownloadedRemotely(String origin, String path, RepoPath repoPath, String downloadedBy,
            long downloadedTime, long count) {
        log.debug("Resource '{}' was downloaded remotely by '{}', at '{}', from {}, count: '{}'",
                repoPath, downloadedBy, downloadedTime, origin, count);

        if(notSystemUserCriteria.meet(downloadedBy)) {
            statsPersistingService.fileDownloadedRemotely(origin, path, repoPath, downloadedBy, downloadedTime, count);
            statsDelegatingService.fileDownloaded(origin, path, repoPath, downloadedBy, downloadedTime, count);
        } else {
            log.debug("User {} is not answering desired criteria, ignoring ...", downloadedBy);
        }
    }

    @Override
    public int setStats(long nodeId, StatsInfo statsInfo) {
        return statsPersistingService.setStats(nodeId, statsInfo);
    }

    @Override
    public boolean deleteStats(long nodeId) {
        return statsPersistingService.deleteStats(nodeId);
    }

    /**
     * Checks if local stats available
     *
     * @param repoPath The repo path to check
     * @return
     */
    @Override
    public boolean hasStats(RepoPath repoPath) {
        return statsPersistingService.hasStats(repoPath);
    }

    /**
     * Performs all queues flash
     */
    @Override
    public void flushStats() {
        statsPersistingService.flushStats();
        statsDelegatingService.flushStats();
    }
}
