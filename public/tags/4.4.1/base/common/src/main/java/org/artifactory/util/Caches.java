/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Michael Pasternak
 */
public class Caches {

    /**
     * Produces LRU (Least Recently Used) cache
     *
     * @param size initial size
     */
    public static <K, V> Map<K, V> newLRUCache(int size) {
        return LRUCache.newInstance(size);
    }

    /**
     * Produces LRU (Least Recently Used) cache
     *
     * @param size initial size
     * @param loadFactor load factor
     */
    public static <K, V> Map<K, V> newLRUCache(int size, float loadFactor) {
        return LRUCache.newInstance(size, loadFactor);
    }

    /**
     * A Least Recently Used cache implementation
     */
    private static final class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int size;

        /**
         * @param size initial size
         */
        private LRUCache(int size) {
            super(size, 0.75f, true);
            this.size = size;
        }

        /**
         * @param size initial size
         * @param loadFactor load factor
         */
        private LRUCache(int size, float loadFactor) {
            super(size, loadFactor, true);
            this.size = size;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > size;
        }

        /**
         * @param size initial size
         */
        public static <K, V> LRUCache<K, V> newInstance(int size) {
            return new LRUCache<K, V>(size);
        }

        /**
         * @param size initial size
         * @param loadFactor load factor
         */
        public static <K, V> LRUCache<K, V> newInstance(int size, float loadFactor) {
            return new LRUCache<K, V>(size, loadFactor);
        }
    }
}
