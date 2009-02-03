package org.artifactory.repo;

import org.artifactory.engine.ResourceStreamHandle;

import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;

public interface RemoteRepo extends Repo {
    long getRetrievalCachePeriodSecs();

    boolean isStoreArtifactsLocally();

    boolean isHardFail();

    LocalCacheRepo getLocalCacheRepo();

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

    long getFailedRetrievalCachePeriodSecs();

    void setFailedRetrievalCachePeriodSecs(long badRretrievalCachePeriodSecs);

    @XmlElement(defaultValue = "43200", required = false)
    long getMissedRetrievalCachePeriodSecs();

    void setMissedRetrievalCachePeriodSecs(long missedRetrievalCachePeriodSecs);

    void clearCaches();

    void removeFromCaches(String path);    
}
