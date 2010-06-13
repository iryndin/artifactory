package org.apache.lucene.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This file has been changed for Artifactory by Jfrog Ltd. Copyright 2010, Jfrog Ltd.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Java's builtin ThreadLocal has a serious flaw: it can take an arbitrarily long amount of time to dereference the
 * things you had stored in it, even once the ThreadLocal instance itself is no longer referenced. This is because there
 * is single, master map stored for each thread, which all ThreadLocals share, and that master map only periodically
 * purges "stale" entries.
 * <p/>
 * While not technically a memory leak, because eventually the memory will be reclaimed, it can take a long time and you
 * can easily hit OutOfMemoryError because from the GC's standpoint the stale entries are not reclaimaible.
 * <p/>
 * This class works around that, by only enrolling WeakReference values into the ThreadLocal, and separately holding a
 * hard reference to each stored value.  When you call {@link #close}, these hard references are cleared and then GC is
 * freely able to reclaim space by objects stored in it.
 */

public class CloseableThreadLocal<T> {
    private static boolean activateResourceMonitoring = false;
    private static final AtomicInteger resourcesCounter = new AtomicInteger();

    private static final ThreadLocal<Map<Integer,Object>> CLOSEABLE_THREAD_LOCALS = new ThreadLocal<Map<Integer,Object>>();
    private final Integer id;

    public CloseableThreadLocal() {
        id = hashCode();
    }

    public T get() {
        Map<Integer, Object> map = CLOSEABLE_THREAD_LOCALS.get();
        if (map != null) {
            return (T)map.get(id);
        }
        return null;
    }

    public void set(T object) {
        if (activateResourceMonitoring) {
            resourcesCounter.incrementAndGet();
        }
        Map<Integer, Object> map = CLOSEABLE_THREAD_LOCALS.get();
        if (map == null) {
            map = new HashMap<Integer, Object>();
            CLOSEABLE_THREAD_LOCALS.set(map);
        }
        map.put(id, object);
    }

    public void close() {
        Map<Integer, Object> map = CLOSEABLE_THREAD_LOCALS.get();
        if (activateResourceMonitoring) {
            if (map != null && map.get(id) != null)
                resourcesCounter.decrementAndGet();
        }
        if (map != null) {
            map.remove(id);
        }
    }

    public static void closeAllThreadLocal() {
        if (activateResourceMonitoring) {
            Map<Integer, Object> map = CLOSEABLE_THREAD_LOCALS.get();
            if (map != null) {
                Collection<Object> objects = map.values();
                for (Object object : objects) {
                    if (object != null)
                        resourcesCounter.decrementAndGet();
                }
            }
        }
        CLOSEABLE_THREAD_LOCALS.remove();
    }

    public static void setActivateResourceMonitoring(boolean activateResourceMonitoring) {
        CloseableThreadLocal.activateResourceMonitoring = activateResourceMonitoring;
    }

    public static int getResourceCount() {
        return resourcesCounter.get();
    }
}
