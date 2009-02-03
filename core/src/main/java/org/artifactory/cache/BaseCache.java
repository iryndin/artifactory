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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author freds
 * @date Oct 19, 2008
 */
class BaseCache<K, V> implements Map<K, V> {
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
        this.idleOnAccess = definition.isIdleOnAccess();
        this.usePutIfAbsent = definition.isUsePutIfAbsent();
        this.cacheData =
                new ConcurrentHashMap<K, CacheElement<V>>(definition.getInitialSize(), 0.75f, 10);
    }

    public ArtifactoryCache getDefinition() {
        return definition;
    }

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
        return result;
    }

    public Collection<V> values() {
        Map<K, V> result = retrieveSimpleMap();
        return result.values();
    }

    public Set<K> keySet() {
        return cacheData.keySet();
    }

    public void clear() {
        cacheData.clear();
    }

    public V replace(K key, V value) {
        return getRefData(cacheData.replace(key, createNewElement(value)));
    }

    public boolean replace(K key, V oldValue, V newValue) {
        return cacheData.replace(key, createNewElement(oldValue), createNewElement(newValue));
    }

    public boolean remove(Object key, Object value) {
        return cacheData.remove(key, value);
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

    public V putIfAbsent(K key, V value) {
        CacheElement<V> element = cacheData.putIfAbsent(key, createNewElement(value));
        return element == null ? null : element.get();
    }

    public V put(K key, V value) {
        CacheElement<V> element;
        CacheElement<V> newElement = createNewElement(value);
        if (usePutIfAbsent) {
            element = cacheData.putIfAbsent(key, newElement);
            if (element == null) {
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
