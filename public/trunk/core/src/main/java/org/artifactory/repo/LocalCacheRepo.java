package org.artifactory.repo;

import org.artifactory.api.repo.RepoPath;

/**
 * Interfarce for the local cache repositories.
 *
 * @author Noam Tenne
 */
public interface LocalCacheRepo extends LocalRepo {
    String PATH_SUFFIX = "-cache";

    RemoteRepo getRemoteRepo();

    void unexpire(String path);

    void zap(RepoPath repoPath);
}
