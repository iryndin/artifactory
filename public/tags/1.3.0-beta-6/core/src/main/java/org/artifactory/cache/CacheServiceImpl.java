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
package org.artifactory.cache;

import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheType;
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author freds
 * @date Oct 12, 2008
 */
@Service
public class CacheServiceImpl implements InternalCacheService {
    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    /**
     * All global caches for Artifactory
     */
    private final Map<ArtifactoryCache, Map> caches = new HashMap<ArtifactoryCache, Map>(4);
    /**
     * All the caches that belongs in a specific repository. So the first key is the repo key, and the second the
     * ArtifactoryCache enum.
     */
    private final Map<String, Map<ArtifactoryCache, Map>> repoCaches =
            new HashMap<String, Map<ArtifactoryCache, Map>>(8);

    @PostConstruct
    private void register() {
        InternalContextHelper.get().addReloadableBean(InternalCacheService.class);
    }

    public void init() {
        log.info("Creating Artifactory caches");
        log.debug("Creating global caches");
        for (ArtifactoryCache cacheDef : ArtifactoryCache.values()) {
            if (cacheDef.getCacheType() == CacheType.GLOBAL) {
                caches.put(cacheDef, new BaseCache(cacheDef));
            }
        }
        CentralConfigDescriptor descriptor =
                InternalContextHelper.get().getCentralConfig().getDescriptor();
        Set<String> localRepoKeys = descriptor.getLocalRepositoriesMap().keySet();
        for (String repoKey : localRepoKeys) {
            log.debug("Creating local repo caches for {}", repoKey);
            Map<ArtifactoryCache, Map> localCaches = createRealRepoCaches();
            repoCaches.put(repoKey, localCaches);
        }
        Collection<RemoteRepoDescriptor> remoteRepoKeys =
                descriptor.getRemoteRepositoriesMap().values();
        for (RemoteRepoDescriptor repo : remoteRepoKeys) {
            // First the local cache repo has the same caches than a real repo
            // TODO: Find a better place for this constant
            String repoKey = repo.getKey() + LocalCacheRepo.PATH_SUFFIX;
            log.debug("Creating local repo caches for {}", repoKey);
            Map<ArtifactoryCache, Map> localCaches = createRealRepoCaches();
            repoCaches.put(repoKey, localCaches);

            repoKey = repo.getKey();
            log.debug("Creating remote repo caches for {}", repoKey);
            Map<ArtifactoryCache, Map> remoteCaches = new HashMap<ArtifactoryCache, Map>();
            for (ArtifactoryCache cacheDef : ArtifactoryCache.values()) {
                if (cacheDef.getCacheType() == CacheType.REMOTE_REPO) {
                    BaseCache cache = new BaseCache(cacheDef, cacheDef.getIdleTime(repo),
                            cacheDef.getMaxSize());
                    remoteCaches.put(cacheDef, cache);
                }
            }
            repoCaches.put(repoKey, remoteCaches);
        }
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalCentralConfigService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        destroy();
        init();
    }

    public void destroy() {
        for (Map map : caches.values()) {
            map.clear();
        }
        for (Map<ArtifactoryCache, Map> repoCacheMap : repoCaches.values()) {
            for (Map map : repoCacheMap.values()) {
                map.clear();
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public Map getCache(ArtifactoryCache cache) {
        Map result = caches.get(cache);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("Cache named " + cache + " does not exists");
    }

    @SuppressWarnings({"unchecked"})
    public Map getRepositoryCache(String repoKey, ArtifactoryCache cache) {
        Map<ArtifactoryCache, Map> repoCacheMap = repoCaches.get(repoKey);
        if (repoCacheMap == null) {
            throw new IllegalArgumentException("Repo named " + repoKey + " does not have caches");
        }
        Map result = repoCacheMap.get(cache);
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException(
                "Cache named " + cache + " for repo " + repoKey + " does not exists");
    }

    private Map<ArtifactoryCache, Map> createRealRepoCaches() {
        Map<ArtifactoryCache, Map> result = new HashMap<ArtifactoryCache, Map>(2);
        for (ArtifactoryCache cacheDef : ArtifactoryCache.values()) {
            if (cacheDef.getCacheType() == CacheType.REAL_REPO) {
                result.put(cacheDef, new BaseCache(cacheDef));
            }
        }
        return result;
    }
}
