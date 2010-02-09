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

package org.artifactory.api.md;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A map of stringified keys and values, used for storing arbitrary key-value metdata on repository items.
 *
 * @author Yoav Landman
 */
@XStreamAlias(Properties.ROOT)
public class Properties implements Info {

    public static final String ROOT = "properties";

    private final LinkedHashMultimap<String, String> props;

    public Properties() {
        props = LinkedHashMultimap.create();
    }

    public Properties(Properties m) {
        props = LinkedHashMultimap.create(m.props);
    }

    public int size() {
        return props.size();
    }

    public Set<String> get(@Nullable String key) {
        return props.get(key);
    }

    public boolean putAll(@Nullable String key, Iterable<? extends String> values) {
        return props.putAll(key, values);
    }

    public boolean putAll(Multimap<? extends String, ? extends String> multimap) {
        return props.putAll(multimap);
    }

    public void clear() {
        props.clear();
    }

    public Set<String> removeAll(@Nullable Object key) {
        return props.removeAll(key);
    }

    public boolean put(String key, String value) {
        return props.put(key, value);
    }

    public Collection<String> values() {
        return props.values();
    }

    public Set<Map.Entry<String, String>> entries() {
        return props.entries();
    }

    public Multiset<String> keys() {
        return props.keys();
    }

    public Set<String> keySet() {
        return props.keySet();
    }

    public boolean isEmpty() {
        return props.isEmpty();
    }

    public boolean containsKey(String key) {
        return props.containsKey(key);
    }

    @Override
    public boolean equals(@Nullable Object that) {
        if (that instanceof Properties) {
            Properties otherProps = (Properties) that;
            return this.props.equals(otherProps.props);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return props.hashCode();
    }

    @Override
    public String toString() {
        return props.toString();
    }
}