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

import org.artifactory.common.ResourceStreamHandle;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoType;
import org.artifactory.resource.RepoResource;

import java.io.IOException;

public interface RemoteRepo<T extends RemoteRepoDescriptor> extends RealRepo<T> {
    long getRetrievalCachePeriodSecs();

    boolean isStoreArtifactsLocally();

    boolean isHardFail();

    LocalCacheRepo getLocalCacheRepo();

    /**
     * Retrieves a resource from the remote repository
     *
     * @return A handle for the remote resource
     */
    ResourceStreamHandle retrieveResource(String relPath) throws IOException;

    /**
     * Retrieves a resource remotely if the remote resource was found and is newer
     */
    ResourceStreamHandle conditionalRetrieveResource(String relPath) throws IOException;

    long getFailedRetrievalCachePeriodSecs();

    long getMissedRetrievalCachePeriodSecs();

    void clearCaches();

    /**
     * Removes a path from the repository caches (missed and failed)
     *
     * @param path The path to remove from the cache. The path is relative path from the repository root.
     * @param removeSubPaths    If true will also remove any sub paths from the caches.
     */
    void removeFromCaches(String path, boolean removeSubPaths);

    boolean isOffline();

    RemoteRepoType getType();

    ResourceStreamHandle downloadAndSave(RepoResource res, RepoResource targetResource) throws IOException;
}
