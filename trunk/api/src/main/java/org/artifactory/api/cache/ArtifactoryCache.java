/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.api.cache;

import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;

/**
 * @author freds
 * @date Oct 19, 2008
 */
public enum ArtifactoryCache {
    authentication(
            CacheType.GLOBAL, ElementReferenceType.WEAK, false, true,
            ConstantValues.securityAuthenticationCacheIdleTimeSecs, null, 100),
    acl(
            CacheType.GLOBAL, ElementReferenceType.HARD, false, true,
            null, null, 30),
    fsItemCache(
            CacheType.STORING_REPO, ElementReferenceType.SOFT, false, false,
            ConstantValues.fsItemCacheIdleTimeSecs, null, 5000),
    locks(
            CacheType.STORING_REPO, ElementReferenceType.SOFT, true, true,
            ConstantValues.fsItemCacheIdleTimeSecs, null, 2000),
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
    versioning(
            CacheType.GLOBAL, ElementReferenceType.HARD, false, false,
            ConstantValues.versioningQueryIntervalSecs, null, 3),
    buildItemMissingMd5(
            CacheType.GLOBAL, ElementReferenceType.SOFT, true, true,
            ConstantValues.missingBuildChecksumCacheIdeTimeSecs, null, 100),
    buildItemMissingSha1(
            CacheType.GLOBAL, ElementReferenceType.SOFT, true, true,
            ConstantValues.missingBuildChecksumCacheIdeTimeSecs, null, 100),
    artifactoryUpdates(
            CacheType.GLOBAL, ElementReferenceType.HARD, false, false,
            ConstantValues.artifactoryUpdatesRefreshIntervalSecs, null, 1);

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
    private final boolean singlePut;
    /**
     * Check the idle time out from the last access time if true. If false (default) will check from last modified
     * time.
     */
    private final boolean resetIdleOnRead;
    /**
     * The getLong() on this ConstantsValue will provide the timeout in seconds. When timeout is reached the value is
     * declared invalid and will be set to null.
     */
    private final ConstantValues idleTime;
    /**
     * The getLong() on this ConstantsValue will provide the maximum number of elements in the cache.
     */
    private final ConstantValues maxSize;
    /**
     * The initial Map size when cache is created. The default is the initial size of Map.
     */
    private final int initialSize;

    ArtifactoryCache(CacheType cacheType, ElementReferenceType refType, boolean singlePut,
            boolean resetIdleOnRead, ConstantValues idleTime, ConstantValues maxSize,
            int initialSize) {
        this.cacheType = cacheType;
        this.refType = refType;
        this.singlePut = singlePut;
        this.resetIdleOnRead = resetIdleOnRead;
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

    public boolean isSinglePut() {
        return singlePut;
    }

    public boolean isResetIdleOnRead() {
        return resetIdleOnRead;
    }

    /**
     * @return Idle time in milliseconds
     */
    public long getIdleTime() {
        if (idleTime != null) {
            return idleTime.getLong() * 1000L;
        } else {
            return -1;
        }
    }

    /**
     * @return Idle time in milliseconds
     */
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