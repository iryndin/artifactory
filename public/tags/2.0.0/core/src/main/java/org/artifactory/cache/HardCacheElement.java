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

    public void set(V value) {
        this.value = value;
        modified();
    }

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
