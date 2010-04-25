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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author freds
 * @date Oct 19, 2008
 */
public class BaseCache<K, V> implements Cache<K, V> {
    private final ArtifactoryCache definition;
    private final ConcurrentHashMap<K, CacheElement<V>> cacheData;
    private final CacheReference refType;
    private final boolean idleOnAccess;
    private final boolean usePutIfAbsent;
    private final long idleTime;
    private final int maxSize;

    public BaseCache(ArtifactoryCache definition) {
        this(definition, definition.getIdleTime(), definition.getMaxSize());
    }

    public BaseCache(ArtifactoryCache definition, long idleTime, int maxSize) {
        this.definition = definition;
        switch (definition.getRefType()) {
            case HARD:
                refType = CacheReference.HARD;
                break;
            case SOFT:
                refType = CacheReference.SOFT;
                break;
            case WEAK:
                refType = CacheReference.WEAK;
                break;
            default:
                throw new IllegalStateException(
                        "Impossible enum issue on " + definition.getRefType());
        }
        this.idleTime = idleTime;
        this.maxSize = maxSize;
        this.idleOnAccess = definition.isResetIdleOnRead();
        this.usePutIfAbsent = definition.isSinglePut();
        this.cacheData =
                new ConcurrentHashMap<K, CacheElement<V>>(definition.getInitialSize(), 0.75f, 10);
    }

    public ArtifactoryCache getDefinition() {
        return definition;
    }

    public Set<K> keySet() {
        return cacheData.keySet();
    }

    /**
     * @return An unmodifiable copy collection of the map values.
     */
    public Collection<V> values() {
        Map<K, V> result = retrieveSimpleMap();
        return result.values();
    }

    /**
     * @return An unmodifiable copy set of the map entries.
     */
    public Set<Entry<K, V>> entrySet() {
        Map<K, V> result = retrieveSimpleMap();
        return result.entrySet();
    }

    private Map<K, V> retrieveSimpleMap() {
        Set<Entry<K, CacheElement<V>>> entries = cacheData.entrySet();
        if (maxSize > 0 && entries.size() > maxSize) {
            // Needs removal of elements
            // TODO: LRU removal
        }
        Set<K> removeAll = null;
        Map<K, V> result = new HashMap<K, V>();
        for (Entry<K, CacheElement<V>> entry : entries) {
            V refData = getRefData(entry.getValue());
            if (refData == null) {
                // Delete the entry
                if (removeAll == null) {
                    removeAll = new HashSet<K>();
                }
                removeAll.add(entry.getKey());
            } else {
                result.put(entry.getKey(), refData);
            }
        }
        if (removeAll != null) {
            for (K k : removeAll) {
                cacheData.remove(k);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public void clear() {
        cacheData.clear();
    }

    public V replace(K key, V value) {
        return getRefData(cacheData.replace(key, createNewElement(value)));
    }

    public V remove(Object key) {
        CacheElement<V> element = cacheData.remove(key);
        return element == null ? null : element.get();
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public V put(K key, V value) {
        CacheElement<V> element;
        CacheElement<V> newElement = createNewElement(value);
        if (usePutIfAbsent) {
            element = cacheData.putIfAbsent(key, newElement);
            if (element == null) {
                // this is a new non-existing element return the new element
                //TODO: [by YS] this breaks the behaviour of putIfAbsent which returns null is it it doesn't exist
                return newElement.get();
            } else {
                V v = element.get();
                // TODO: We have a synchronize issue here
                if (v == null) {
                    element.set(value);
                }
                return element.get();
            }
        } else {
            element = cacheData.put(key, newElement);
            if (element == null) {
                return null;
            } else {
                return element.get();
            }
        }
    }

    private CacheElement<V> createNewElement(V value) {
        return refType.createCacheElement(value);
    }

    public boolean containsValue(Object value) {
        return cacheData.containsValue(value);
    }

    public boolean containsKey(Object key) {
        return cacheData.containsKey(key);
    }

    public V get(Object key) {
        CacheElement<V> element = cacheData.get(key);
        return getRefData(element);
    }

    private V getRefData(CacheElement<V> element) {
        if (element == null) {
            return null;
        }
        V result = element.get();
        if (idleTime <= 0) {
            return result;
        }
        if (result == null || getAge(element) >= idleTime) {
            return null;
        }
        return result;
    }

    private long getAge(CacheElement<V> element) {
        if (idleOnAccess) {
            return System.currentTimeMillis() - element.getLastAccess();
        } else {
            return System.currentTimeMillis() - element.getLastModified();
        }
    }

    public int size() {
        return cacheData.size();
    }

    public boolean isEmpty() {
        return cacheData.isEmpty();
    }
}
