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

import org.artifactory.resource.RepoResource;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DefaultRetrievalCache implements RetrievalCache {
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger
            .getLogger(DefaultRetrievalCache.class);

    private final Map<String, CacheElement> cache = new HashMap<String, CacheElement>();
    //Milliseconds
    private long updateInterval;

    public DefaultRetrievalCache(long snapshotUpdateInterval) {
        this.updateInterval = snapshotUpdateInterval;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public long getResourceAge(String path) {
        if (updateInterval <= 0) {
            return -1;
        }
        synchronized (cache) {
            CacheElement cacheElement = cache.get(path);
            if (cacheElement == null) {
                return -1;
            }
            long age = System.currentTimeMillis() - cacheElement.lastUpdated;
            return age;
        }
    }

    public RepoResource getResource(String path) {
        if (updateInterval <= 0) {
            return null;
        }
        synchronized (cache) {
            CacheElement cacheElement = cache.get(path);
            if (cacheElement == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unable to find " + path + " in retrieval cache");
                }
                return null;
            }
            long age = System.currentTimeMillis() - cacheElement.lastUpdated;
            if (age > updateInterval) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Expiring " + cacheElement.getName() + " from retrieval cache ("
                            + age + " > " + updateInterval + ")");
                }
                cache.remove(path);
                return null;
            }
            return cacheElement.res;
        }
    }

    public void setResource(RepoResource res) {
        if (updateInterval <= 0) {
            return;
        }
        String path = res.getPath();
        synchronized (cache) {
            CacheElement cacheElement = cache.get(path);
            if (cacheElement == null) {
                cacheElement = new CacheElement();
                cache.put(path, cacheElement);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Adding " + path + " to retrieval cache");
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Updating " + path + " in retrieval cache");
                }
            }
            cacheElement.res = res;
            cacheElement.lastUpdated = System.currentTimeMillis();
        }
    }

    public RepoResource removeResource(String path) {
        if (updateInterval <= 0) {
            return null;
        }
        synchronized (cache) {
            CacheElement cacheElement = cache.remove(path);
            if (cacheElement != null) {
                return cacheElement.res;
            } else {
                return null;
            }
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private static class CacheElement implements Serializable {
        //Artifact details
        RepoResource res;
        //When was this cache entry last updated
        long lastUpdated;

        String getName() {
            return res != null ? res.getName() : null;
        }
    }

}