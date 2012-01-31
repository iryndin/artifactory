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

import java.lang.ref.Reference;

/**
 * @author freds
 * @date Oct 20, 2008
 */
abstract class ReferenceCacheElement<V> extends BaseCacheElement<V> {
    private Reference<V> value;

    ReferenceCacheElement(V value) {
        super();
        this.value = createReference(value);
    }

    @Override
    public void set(V value) {
        this.value = createReference(value);
        modified();
    }

    @Override
    public V get() {
        V result = value.get();
        if (result != null) {
            accessed();
        }
        return result;
    }

    protected abstract Reference<V> createReference(V value);

    public boolean equals(Object o) {
        V refValue = value.get();
        if (this == o || refValue == o) {
            return true;
        }
        if (o == null || refValue == null) {
            return false;
        }
        if (o instanceof ReferenceCacheElement) {
            ReferenceCacheElement element = (ReferenceCacheElement) o;
            return refValue.equals(element.value.get());
        } else {
            return refValue.equals(o);
        }
    }

    public int hashCode() {
        return (value.get() != null ? value.get().hashCode() : 0);
    }
}
