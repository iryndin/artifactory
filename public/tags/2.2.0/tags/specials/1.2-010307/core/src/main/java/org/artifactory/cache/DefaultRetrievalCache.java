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
        String path = res.getRelPath();
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

    public void start() {
        if (updateInterval <= 0) {
            return;
        }
        clear();
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    public void stop() {
        if (updateInterval <= 0) {
            return;
        }
        clear();
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