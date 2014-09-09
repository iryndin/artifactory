/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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

package org.artifactory.storage.db.fs.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.properties.PropertiesFilter;
import org.artifactory.util.Pair;

import java.util.List;

/**
 * Helper to create properties filter query
 *
 * @author Gidi Shabat
 */
public class PropertiesFilterQueryBuilder {

    private PropertiesFilter propertiesFilter;

    public PropertiesFilterQueryBuilder(PropertiesFilter propertiesFilter) {
        this.propertiesFilter = propertiesFilter;
    }

    public Pair<String, List<String>> createDistinctFirst() {
        List<String> params = Lists.newArrayList();
        String query = "SELECT distinct p1.prop_value FROM nodes n <PROPERTIES_FILTER> <PATH_FILTER> ";
        query = query.replace("<PROPERTIES_FILTER>", nodeIdsFilter1(params));
        query = query.replace("<PATH_FILTER>", createPathFilter(params));
        return new Pair<>(query, params);
    }

    public Pair<String, List<String>> createDistinctVersionQuery() {
        List<String> params = Lists.newArrayList();
        String distinct = propertiesFilter.getDistinct();
        if (StringUtils.isNotEmpty(distinct)) {
            propertiesFilter.addFirst(distinct);
        }
        String query = "SELECT distinct p1.prop_value FROM nodes n <PROPERTIES_FILTER> <PATH_FILTER> ";
        query = query.replace("<PROPERTIES_FILTER>", nodeIdsFilter1(params));
        query = query.replace("<PATH_FILTER>", createPathFilter(params));
        return new Pair<>(query, params);
    }

    public Pair<String, List<String>> createNodeQuery() {
        List<String> params = Lists.newArrayList();
        String query = "SELECT n.* FROM nodes n <PROPERTIES_FILTER> <PATH_FILTER>";
        query = query.replace("<PROPERTIES_FILTER>", nodeIdsFilter1(params));
        query = query.replace("<PATH_FILTER>", createPathFilter(params));
        return new Pair<>(query, params);
    }

    private String createPathFilter(List<String> params) {
        StringBuilder stringBuilder = new StringBuilder();
        if (StringUtils.isNotEmpty(propertiesFilter.getRepo())) {
            stringBuilder.append(" AND repo =? ");
            params.add(propertiesFilter.getRepo());
        }
        if (StringUtils.isNotEmpty(propertiesFilter.getPath())) {
            stringBuilder.append(" AND node_path like ? ");
            params.add(propertiesFilter.getPath() + "%");
        }
        return stringBuilder.toString();
    }

    private String nodeIdsFilter1(List<String> params) {
        StringBuilder total = new StringBuilder();
        StringBuilder join = new StringBuilder();
        StringBuilder where = new StringBuilder();
        propertiesFilter.getProperties().iterator();
        where.append(" WHERE node_type = 1");
        for (int i = 0; i < propertiesFilter.getProperties().size(); i++) {
            Pair<String, String> pair = propertiesFilter.getProperties().get(i);
            String indexValue = "p" + (i + 1);
            join.append("JOIN node_props ");
            join.append(indexValue);
            join.append(" ON n.node_id = ");
            join.append(indexValue);
            join.append(".node_id ");
            where.append(" AND ");
            where.append(indexValue);
            where.append(".prop_key=? ");
            params.add(pair.getFirst());
            String value = pair.getSecond();
            if (value != null) {
                where.append(" AND ");
                where.append(indexValue);
                where.append(".prop_value like ? ");
                params.add(value);
            }
        }

        total.append(join.toString());
        total.append(where.toString());
        return total.toString();
    }
}
