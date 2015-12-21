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

package org.artifactory.api.callhome;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Michael Pasternak
 */
public class FeatureGroup {

    @JsonProperty(value = "name")
    private String name;
    @JsonProperty(value = "attributes")
    private Map<String, Object> attributes;
    @JsonProperty(value = "features")
    private List<FeatureGroup> features;

    public FeatureGroup() {
    }

    public FeatureGroup(String name) {
        this.name = name;
    }

    public void addFeature(FeatureGroup featureGroup) {
        if (features == null)
            features = Lists.newLinkedList();
        features.add(featureGroup);
    }

    public void addFeatureAttribute(String name, Object value) {
        if(attributes == null)
            attributes = Maps.newHashMap();
        attributes.put(name, value);
    }
}
