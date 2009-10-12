package org.artifactory.repo;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;

/**
 * Interfarce for the local cache repositories.
 *
 * @author Noam Tenne
 */
public interface LocalCacheRepo extends LocalRepo<LocalCacheRepoDescriptor> {
    String PATH_SUFFIX = "-cache";

    RemoteRepo getRemoteRepo();

    void unexpire(String path);

    void zap(RepoPath repoPath);
}
