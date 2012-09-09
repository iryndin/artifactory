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

package org.artifactory.cache;

/**
 * @author freds
 * @date Oct 12, 2008
 */
class HardCacheElement<V> extends BaseCacheElement<V> {
    private V value;

    HardCacheElement(V value) {
        super();
        this.value = value;
    }

    @Override
    public void set(V value) {
        this.value = value;
        modified();
    }

    @Override
    public V get() {
        accessed();
        return value;
    }

    public boolean equals(Object o) {
        if (this == o || value == o) {
            return true;
        }
        if (o == null || value == null) {
            return false;
        }
        if (o.getClass() == getClass()) {
            HardCacheElement element = (HardCacheElement) o;
            return value.equals(element.value);
        } else {
            return value.equals(o);
        }
    }

    public int hashCode() {
        return (value != null ? value.hashCode() : 0);
    }
}
