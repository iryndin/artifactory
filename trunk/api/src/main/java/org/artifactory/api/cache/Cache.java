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

import java.util.Map;

/**
 * The main interface for the Artifactory cache
 *
 * @author Noam Y. Tenne
 */
public interface Cache<K, V> extends Map<K, V> {

    /**
     * Returns the definition of the cache
     *
     * @return Cache definition
     */
    ArtifactoryCache getDefinition();

    /**
     * Replaces the value of the key
     *
     * @param key   Key to replace the value of
     * @param value New value to set for the key
     * @return The old value if exists, Null if not
     */
    V replace(K key, V value);
}