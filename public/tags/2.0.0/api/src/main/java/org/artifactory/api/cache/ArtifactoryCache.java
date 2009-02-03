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
package org.artifactory.api.cache;

import org.artifactory.common.ConstantsValue;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;

/**
 * @author freds
 * @date Oct 19, 2008
 */
public enum ArtifactoryCache {
    authentication(
            CacheType.GLOBAL, ElementReferenceType.WEAK, false, true,
            ConstantsValue.authenticationCacheIdleTimeSecs, null, 100),
    fsItemCache(
            CacheType.REAL_REPO, ElementReferenceType.SOFT, false, false,
            ConstantsValue.metadataIdleTimeSecs, null, 500),
    locks(
            CacheType.REAL_REPO, ElementReferenceType.SOFT, true, true,
            ConstantsValue.metadataIdleTimeSecs, null, 200),
    missed(
            CacheType.REMOTE_REPO, ElementReferenceType.HARD, false, false, null, null, 100) {
        @Override
        public long getIdleTime(RemoteRepoDescriptor descriptor) {
            return descriptor.getMissedRetrievalCachePeriodSecs() * 1000L;
        }},
    failed(
            CacheType.REMOTE_REPO, ElementReferenceType.HARD, false, false, null, null, 100) {
        @Override
        public long getIdleTime(RemoteRepoDescriptor descriptor) {
            return descriptor.getFailedRetrievalCachePeriodSecs() * 1000L;
        }},
    mergedMetadataChecksums(
            CacheType.GLOBAL, ElementReferenceType.SOFT, false, false,
            ConstantsValue.metadataIdleTimeSecs, null, 100),
    versioning(
            CacheType.GLOBAL, ElementReferenceType.HARD, false, true,
            ConstantsValue.versioningQueryIntervalSecs, null, 3);

    /**
     * The type of cache: Global, or per repo (indexed by repoKey)
     */
    private final CacheType cacheType;
    /**
     * The type of cache element entry
     */
    private final ElementReferenceType refType;
    /**
     * The first call to put(key,value) will insert the value, then all subsequent put(key,newValue) will return the
     * first value, ignoring the newValue.
     */
    private final boolean usePutIfAbsent;
    /**
     * Check the idle time out from the last access time if true. If false (default) will check from last modified
     * time.
     */
    private final boolean idleOnAccess;
    /**
     * The getLong() on this ConstantsValue will provide the timeout in seconds. When timeout is reached the value is
     * declared invalid and will be set to null.
     */
    private final ConstantsValue idleTime;
    /**
     * The getLong() on this ConstantsValue will provide the maximum number of elements in the cache.
     */
    private final ConstantsValue maxSize;
    /**
     * The initial Map size when cache is created. The default is the initial size of Map.
     */
    private final int initialSize;

    ArtifactoryCache(CacheType cacheType, ElementReferenceType refType, boolean usePutIfAbsent,
            boolean idleOnAccess, ConstantsValue idleTime, ConstantsValue maxSize,
            int initialSize) {
        this.cacheType = cacheType;
        this.refType = refType;
        this.usePutIfAbsent = usePutIfAbsent;
        this.idleOnAccess = idleOnAccess;
        this.idleTime = idleTime;
        this.maxSize = maxSize;
        this.initialSize = initialSize;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public ElementReferenceType getRefType() {
        return refType;
    }

    public boolean isUsePutIfAbsent() {
        return usePutIfAbsent;
    }

    public boolean isIdleOnAccess() {
        return idleOnAccess;
    }

    public long getIdleTime() {
        if (idleTime != null) {
            return idleTime.getLong() * 1000L;
        } else {
            return -1;
        }
    }

    public long getIdleTime(RemoteRepoDescriptor descriptor) {
        return -1;
    }

    public int getMaxSize() {
        if (maxSize == null) {
            return -1;
        }
        return maxSize.getInt();
    }

    public int getInitialSize() {
        return initialSize;
    }
}
