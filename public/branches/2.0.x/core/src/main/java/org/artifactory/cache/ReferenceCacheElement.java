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

    public void set(V value) {
        this.value = createReference(value);
        modified();
    }

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
