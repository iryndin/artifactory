/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

/**
 * @author freds
 * @date Oct 19, 2008
 */
abstract class BaseCacheElement<V> implements CacheElement<V> {
    protected long lastModified;
    protected long lastAccess;

    protected BaseCacheElement() {
        long t0 = System.currentTimeMillis();
        lastModified = t0;
        lastAccess = t0;
    }

    public long getLastModified() {
        return lastModified;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    protected void modified() {
        lastModified = System.currentTimeMillis();
    }

    protected void accessed() {
        lastAccess = System.currentTimeMillis();
    }
}
