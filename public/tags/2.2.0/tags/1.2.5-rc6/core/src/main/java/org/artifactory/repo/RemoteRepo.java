/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.repo;

import org.artifactory.engine.ResourceStreamHandle;

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


    long getMissedRetrievalCachePeriodSecs();

    void setMissedRetrievalCachePeriodSecs(long missedRetrievalCachePeriodSecs);

    void clearCaches();

    void removeFromCaches(String path);

    boolean isOffline();

    void setOffline(boolean offline);

    RemoteRepoType getType();

    void setType(RemoteRepoType type);
}
