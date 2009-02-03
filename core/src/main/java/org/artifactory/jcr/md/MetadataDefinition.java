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
package org.artifactory.jcr.md;

import org.artifactory.api.common.Info;

import java.lang.reflect.Constructor;

/**
 * @author freds
 * @date Sep 3, 2008
 */
public class MetadataDefinition implements Info {
    /**
     * The key of this definition
     */
    private final String metadataName;
    /**
     * A Java class that can be marshall/unmarshall with XStream to this metadata XML stream
     */
    private final Class xstreamClass;
    /**
     * If true the XML stream will be saved under the JCR metadata folder container. If false, this
     * metadata is transient in memory
     */
    private final boolean persistent;
    /**
     * Valid with xStreamClass not null only. When editing, the cache will activate the copy
     * constructor before editing.
     */
    private final Constructor copyConstructor;
    /**
     * If true, it means this metadata is transparently (store/load) managed without special
     * business logic. The basic folder and file info are set to false here.
     */
    private final boolean internal;

    public MetadataDefinition(String metadataName) {
        this(metadataName, null, true, false);
    }

    public MetadataDefinition(
            String metadataName, Class xstreamClass, boolean persistent, boolean internal) {
        if (metadataName == null) {
            throw new IllegalArgumentException("Metadata definition name cannot be null");
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
                    " since the class " + xstreamClass + " does not have a copy constructor", e);
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
            throw new RuntimeException("Cannot create new metadata value of class " + xstreamClass,
                    e);
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
