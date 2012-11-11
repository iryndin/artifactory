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

package org.artifactory.rest.common.list;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.artifactory.descriptor.property.Property;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Tomer Cohen
 */
public class KeyValueList extends ArrayList<String> {
    private static final Logger log = LoggerFactory.getLogger(KeyValueList.class);

    public KeyValueList(String s) {
        super();
        for (String v : StringUtils.split(s, '|')) {
            try {
                if (!StringUtils.isWhitespace(v)) {
                    add(v.trim());
                }
            } catch (Exception ex) {
                log.error("Error while parsing list parameter '{}': {}.", s, ex.getMessage());
                throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
            }
        }
    }

    public Map<String, List<String>> toStringMap() {
        Map<String, List<String>> map = Maps.newHashMap();
        for (String keyVal : this) {
            String[] split = StringUtils.split(keyVal, "=");
            if (split.length == 2) {
                String valuesList = split[1];
                String[] valueSplit = StringUtils.split(valuesList, ",");
                List<String> values = Lists.newArrayList(valueSplit);
                map.put(split[0], values);
            }
        }
        return map;
    }

    public Map<Property, List<String>> toPropertyMap() {
        Map<Property, List<String>> map = Maps.newHashMap();
        for (String keyVal : this) {
            String[] split = StringUtils.split(keyVal, "=");
            if (split.length == 2) {
                Property propertyDescriptor = new Property();
                propertyDescriptor.setName(split[0]);
                String value = split[1];
                String replacedValue = StringUtils.replace(value, "\\,", "|");
                String[] valueSplit = StringUtils.split(replacedValue, ",");
                List<String> values = Lists.newArrayList();
                for (String s : valueSplit) {
                    values.add(StringUtils.replace(s, "|", ","));
                }
                map.put(propertyDescriptor, values);
            } else if (split.length == 1) {
                //Empty value
                Property propertyDescriptor = new Property();
                propertyDescriptor.setName(split[0]);
                map.put(propertyDescriptor, Lists.<String>newArrayList(""));
            }
        }
        return map;
    }
}
