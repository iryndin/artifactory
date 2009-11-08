/*
 * This file is part of Artifactory.
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

package org.artifactory.jcr.md;

import org.artifactory.api.common.Info;

import java.lang.reflect.Constructor;

/**
 * @author freds
 * @date Sep 3, 2008
 */
public class MetadataDefinition implements Info {
    /**
     * The key of this definition.
     */
    private final String metadataName;
    /**
     * A Java class that can be marshall/unmarshall with XStream to this metadata XML stream.
     */
    private final Class xstreamClass;
    /**
     * If true the XML stream will be saved under the JCR metadata folder container. If false, this metadata is
     * transient in memory.
     */
    private final boolean persistent;
    /**
     * Valid with xStreamClass not null only. When editing, the cache will activate the copy constructor before
     * editing.
     */
    private final Constructor copyConstructor;
    /**
     * If false, it means this metadata is transparently (store/load) managed without special business logic. The basic
     * folder and file info are set to true here.
     */
    private final boolean internal;

    public MetadataDefinition(String metadataName) {
        this(metadataName, null, true, false);
    }

    public MetadataDefinition(String metadataName, Class xstreamClass, boolean persistent, boolean internal) {
        if (metadataName == null) {
            throw new IllegalArgumentException("Metadata definition name cannot be null.");
        }
        this.xstreamClass = xstreamClass;
        this.metadataName = metadataName;
        this.persistent = persistent;
        this.internal = internal;
        try {
            if (xstreamClass != null) {
                this.copyConstructor = xstreamClass.getDeclaredConstructor(xstreamClass);
            } else {
                this.copyConstructor = null;
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot create a Metadata definition for " + metadataName +
                    " since the class " + xstreamClass + " does not have a copy constructor.", e);
        }
    }

    public String getMetadataName() {
        return metadataName;
    }

    public Class getXstreamClass() {
        return xstreamClass;
    }

    public boolean hasXStream() {
        return xstreamClass != null;
    }

    public Constructor getCopyConstructor() {
        return copyConstructor;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public boolean isInternal() {
        return internal;
    }

    public Object newInstance() {
        if (!hasXStream()) {
            return null;
        }
        try {
            return xstreamClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create new metadata value of class " + xstreamClass, e);
        }
    }

    public Object newInstance(Object metadataValue) {
        if (!hasXStream()) {
            return null;
        }
        try {
            return copyConstructor.newInstance(metadataValue);
        } catch (Exception e) {
            throw new RuntimeException("Cannot copy the metadata value " + metadataValue, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetadataDefinition that = (MetadataDefinition) o;
        return metadataName.equals(that.metadataName);
    }

    @Override
    public int hashCode() {
        return metadataName.hashCode();
    }

    @Override
    public String toString() {
        return "MetadataDefinition{" +
                "metadataName='" + metadataName + '\'' +
                ", xstreamClass=" + xstreamClass +
                '}';
    }
}
