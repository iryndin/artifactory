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

package org.artifactory.md;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.artifactory.common.Info;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A map of stringified keys and values, used for storing arbitrary key-value metadata on repository items.
 *
 * @author Yoav Landman
 */
public interface Properties extends Info {
    String ROOT = "properties";
    String MATRIX_PARAMS_SEP = ";";
    /**
     * A mandatory property is stored as key+=val
     */
    String MANDATORY_SUFFIX = "+";

    int size();

    @Nullable
    Set<String> get(@Nullable String key);

    boolean putAll(@Nullable String key, Iterable<? extends String> values);

    boolean putAll(Multimap<? extends String, ? extends String> multimap);

    void clear();

    Set<String> removeAll(@Nullable Object key);

    boolean put(String key, String value);

    Collection<String> values();

    Set<Map.Entry<String, String>> entries();

    Multiset<String> keys();

    Set<String> keySet();

    boolean isEmpty();

    boolean containsKey(String key);

    MatchResult matchQuery(Properties queryProperties);

    public enum MatchResult {
        MATCH,
        NO_MATCH,
        CONFLICT
    }
}