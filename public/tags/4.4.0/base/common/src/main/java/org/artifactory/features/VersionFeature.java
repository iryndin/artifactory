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

package org.artifactory.features;

/**
 * Created by michaelp on 10/1/15.
 */

import org.artifactory.api.config.VersionInfo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;

/**
 * Single VersionFeature
 */
@XmlType(name = "VersionFeature")
public class VersionFeature implements Serializable {

    @XmlElement(required = true)
    private String name;
    @XmlElement(required = true)
    private VersionInfo availableFrom;

    /**
     * serialization .ctr
     */
    public VersionFeature() {
    }

    /**
     * @param name feature name
     * @param availableFrom version this feature is available from (inclusive)
     */
    public VersionFeature(String name, VersionInfo availableFrom) {
        this.name = name;
        this.availableFrom = availableFrom;
    }

    /**
     * @return {@link VersionInfo} that this {@link VersionFeature}
     *         is available from
     */
    public VersionInfo getAvailableFrom() {
        return availableFrom;
    }

    /**
     * @return feature name
     */
    public String getName() {
        return name;
    }

    /**
     * Defines equality of {@link VersionFeature}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VersionFeature)) return false;

        VersionFeature that = (VersionFeature)other;
        return this.getName().equals(that.getName()) &&
                this.getAvailableFrom().compareTo(that.getAvailableFrom()) == 0;
    }
}

