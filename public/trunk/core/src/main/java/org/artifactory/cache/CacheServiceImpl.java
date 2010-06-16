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

package org.artifactory.cache;

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.Cache;
import org.artifactory.api.cache.CacheType;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.descriptor.repo.VirtualRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author freds
 * @date Oct 12, 2008
 */
@Service
@Reloadable(beanClass = InternalCacheService.class, initAfter = InternalCentralConfigService.class)
public class CacheServiceImpl implements InternalCacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    /**
     * All global caches for Artifactory
     */
    private final Map<ArtifactoryCache, Cache> caches = new HashMap<ArtifactoryCache, Cache>(4);
    /**
     * All the caches that belongs in a specific repository. So the first key is the repo key, and the second the
     * ArtifactoryCache enum.
     */
    private final Map<String, Map<ArtifactoryCache, Cache>> repoCaches =
            new HashMap<String, Map<ArtifactoryCache, Cache>>(11);

    public void init() {
        log.debug("Creating Artifactory caches");
        log.debug("Creating global caches");
        for (ArtifactoryCache cacheDef : ArtifactoryCache.values()) {
            if (cacheDef.getCacheType() == CacheType.GLOBAL) {
                // on reload the caches are cleaned so only add if not exists
                if (!caches.containsKey(cacheDef)) {
                    caches.put(cacheDef, new BaseCache(cacheDef));
                }
            }
        }
        CentralConfigDescriptor descriptor = InternalContextHelper.get().getCentralConfig().getDescriptor();
        //Local
        Set<String> localRepoKeys = descriptor.getLocalRepositoriesMap().keySet();
        for (String repoKey : localRepoKeys) {
            log.debug("Creating local repo caches for {}", repoKey);
            Map<ArtifactoryCache, Cache> localCaches = newStoringRepoCaches();
            repoCaches.put(repoKey, localCaches);
        }
        //Remote
        Collection<RemoteRepoDescriptor> remoteRepoKeys = descriptor.getRemoteRepositoriesMap().values();
        for (RemoteRepoDescriptor repo : remoteRepoKeys) {
            // First the local cache repo has the same caches as a storing (local) repo
            String repoKey = repo.getKey() + LocalCacheRepo.PATH_SUFFIX;
            log.debug("Creating local repo caches for {}", repoKey);
            Map<ArtifactoryCache, Cache> localCaches = newStoringRepoCaches();
            repoCaches.put(repoKey, localCaches);

            repoKey = repo.getKey();
            log.debug("Creating remote repo caches for {}", repoKey);
            Map<ArtifactoryCache, Cache> remoteCaches = new HashMap<ArtifactoryCache, Cache>();
            for (ArtifactoryCache cacheDef : ArtifactoryCache.values()) {
                if (cacheDef.getCacheType() == CacheType.REMOTE_REPO) {
                    BaseCache cache = new BaseCache(cacheDef, cacheDef.getIdleTime(repo), cacheDef.getMaxSize());
                    remoteCaches.put(cacheDef, cache);
                }
            }
            repoCaches.put(repoKey, remoteCaches);
        }
        //Virtual
        Set<String> virtualRepoKeys = descriptor.getVirtualRepositoriesMap().keySet();
        for (String repoKey : virtualRepoKeys) {
            log.debug("Creating virtual repo caches for {}", repoKey);
            Map<ArtifactoryCache, Cache> virtualCaches = newStoringRepoCaches();
            repoCaches.put(repoKey, virtualCaches);
        }
        //Add the global repo cache
        repoCaches.put(VirtualRepoDescriptor.GLOBAL_VIRTUAL_REPO_KEY, newStoringRepoCaches());
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        destroy();
        init();
    }

    public void destroy() {
        for (Cache map : caches.values()) {
            map.clear();
        }
        for (Map<ArtifactoryCache, Cache> repoCacheMap : repoCaches.values()) {
            for (Cache map : repoCacheMap.values()) {
                map.clear();
            }
        }
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    @SuppressWarnings({"unchecked"})
    public Cache getCache(ArtifactoryCache cache) {
        Cache result = caches.get(cache);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("Cache named " + cache + " does not exists");
    }

    @SuppressWarnings({"unchecked"})
    public Cache getRepositoryCache(String repoKey, ArtifactoryCache cache) {
        Map<ArtifactoryCache, Cache> repoCacheMap = repoCaches.get(repoKey);
        if (repoCacheMap == null) {
            throw new IllegalArgumentException("Repo named '" + repoKey + "' does not have caches.");
        }
        Cache result = repoCacheMap.get(cache);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("Cache named '" + cache + "' for repo '" + repoKey + "' does not exist.");
    }

    public void setRepositoryCache(String repoKey, ArtifactoryCache cacheKey, Cache cache) {
        Map<ArtifactoryCache, Cache> repoCacheMap = repoCaches.get(repoKey);
        if (repoCacheMap == null) {
            throw new IllegalArgumentException("Repo named '" + repoKey + "' does not have caches.");
        }
        repoCacheMap.put(cacheKey, cache);
    }

    private Map<ArtifactoryCache, Cache> newStoringRepoCaches() {
        Map<ArtifactoryCache, Cache> result = new HashMap<ArtifactoryCache, Cache>(2);
        for (ArtifactoryCache cacheDef : ArtifactoryCache.values()) {
            if (cacheDef.getCacheType() == CacheType.STORING_REPO) {
                result.put(cacheDef, new BaseCache(cacheDef));
            }
        }
        return result;
    }
}
