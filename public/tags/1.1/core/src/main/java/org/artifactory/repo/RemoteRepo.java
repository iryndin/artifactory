package org.artifactory.repo;

import org.artifactory.engine.ResourceStreamHandle;

import java.io.IOException;

public interface RemoteRepo extends Repo {
    long getRetrievalCachePeriodSecs();

    boolean isStoreArtifactsLocally();

    boolean isHardFail();

    LocalCacheRepo getLocalCacheRepo();

    boolean isCacheRetrievalFailures();

    void setCacheRetrievalFailures(boolean cacheRetrievalFailures);

    boolean isCacheRetrievalMisses();

    void setCacheRetrievalMisses(boolean cacheRetrievalMisses);

    void setHardFail(boolean hardFail);

    void setRetrievalCachePeriodSecs(long snapshotCachePeriod);

    void setStoreArtifactsLocally(boolean cacheArtifactsLocally);

    /**
     * Retrieves a resource from the remote repository
     *
     * @param relPath
     * @return A handle for the remote resource
     * @throws IOException
     */
    ResourceStreamHandle retrieveResource(String relPath) throws IOException;
}
