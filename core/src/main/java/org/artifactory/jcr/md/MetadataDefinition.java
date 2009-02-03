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
    private final String metadataName;
    private final Class xstreamClass;
    private final Constructor copyConstructor;

    public MetadataDefinition(String metadataName) {
        this(metadataName, null);
    }

    public MetadataDefinition(String metadataName, Class xstreamClass) {
        if (metadataName == null) {
            throw new IllegalArgumentException("Metadata definition name cannot be null");
        }
        this.xstreamClass = xstreamClass;
        this.metadataName = metadataName;
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

    public Constructor getCopyConstructor() {
        return copyConstructor;
    }

    public Object newInstance() {
        if (xstreamClass == null) {
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
        if (xstreamClass == null) {
            return null;
        }
        try {
            return copyConstructor.newInstance(metadataValue);
        } catch (Exception e) {
            throw new RuntimeException("Cannot copy the metadata value " + metadataValue, e);
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetadataDefinition that = (MetadataDefinition) o;

        if (!metadataName.equals(that.metadataName)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return metadataName.hashCode();
    }

    public String toString() {
        return "MetadataDefinition{" +
                "metadataName='" + metadataName + '\'' +
                ", xstreamClass=" + xstreamClass +
                '}';
    }
}
