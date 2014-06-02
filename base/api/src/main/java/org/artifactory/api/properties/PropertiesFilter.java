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

package org.artifactory.api.properties;

import org.artifactory.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Transfer object to pass properties information to the service
 *
 * @author Gidi Shabat
 */
public class PropertiesFilter {
    private List<Pair<String, String>> properties = new ArrayList<>();
    private String path;
    private String repo;

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void addProperty(String key, String... values) {
        if (values != null) {
            for (String value : values) {
                properties.add(new Pair<>(key, value));
            }
        } else {
            properties.add(new Pair<>(key, (String) null));
        }
    }

    public String getPath() {
        return path;
    }

    public String getRepo() {
        return repo;
    }

    public void addProperty(String key) {
        properties.add(new Pair<>(key, (String) null));
    }

    public void addFirst(String key) {
        properties.add(0, new Pair<>(key, (String) null));
    }

    public List<Pair<String, String>> getProperties() {
        return properties;
    }

    public void removeProperty(String key) {
        Iterator<Pair<String, String>> iterator = properties.iterator();
        while (iterator.hasNext()) {
            Pair<String, String> pair = iterator.next();
            if (pair.getFirst().equals(key)) {
                iterator.remove();
            }
        }
    }
}
