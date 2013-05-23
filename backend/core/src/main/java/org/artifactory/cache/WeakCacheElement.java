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
import java.lang.ref.WeakReference;

/**
 * @author freds
 * @date Oct 12, 2008
 */
class WeakCacheElement<V> extends ReferenceCacheElement<V> {

    WeakCacheElement(V value) {
        super(value);
    }

    @Override
    protected Reference<V> createReference(V value) {
        return new WeakReference<V>(value);
    }

}