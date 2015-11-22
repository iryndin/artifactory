package org.artifactory.addon.smartrepo;

import org.artifactory.addon.Addon;
import org.artifactory.fs.StatsInfo;
import org.artifactory.repo.RepoPath;

/**
 * @author Chen Keinan
 */
public interface SmartRepoAddon extends Addon {

    boolean supportRemoteStats();

    /**
     * Triggered on remote download event
     *
     * Event queued for local stats update and potential delegation
     *
     * @param statsInfo The {@link StatsInfo} container
     * @param origin    The remote host the download was triggered by
     * @param repoPath  The file repo path to set/update stats
     */
    void fileDownloadedRemotely(StatsInfo statsInfo, String origin, RepoPath repoPath);

}
