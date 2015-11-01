package org.artifactory.repo.service;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.Lock;

/**
 * @author Gidi Shabat
 */
public interface LockableUndeploy {
    @Lock
    BasicStatusHolder undeployInternal(RepoPath repoPath, boolean calcMavenMetadata,BasicStatusHolder statusHolder);
}
