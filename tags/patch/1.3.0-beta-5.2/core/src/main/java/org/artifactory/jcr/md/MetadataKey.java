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

/**
 * @author freds
 * @date Sep 4, 2008
 */
public class MetadataKey {
    private final String metadataName;
    private final String absPath;

    MetadataKey(MetadataAware ma, String metadataName) {
        this(ma.getAbsolutePath(), metadataName);
    }

    MetadataKey(String absPath, String metadataName) {
        if (absPath == null || metadataName == null) {
            throw new IllegalArgumentException("Path " + absPath +
                    " and Metadata Name " + metadataName + " are mandatory");
        }
        if (absPath.length() == 0 || absPath.charAt(0) != '/') {
            throw new IllegalArgumentException("The absolute path " + absPath +
                    " is not valid and should start with a /.\n" +
                    "Illegal parameters for building key of Metadata Name " + metadataName);
        }
        while (absPath.length() > 2 && absPath.charAt(absPath.length() - 1) == '/') {
            // remove trailing slash
            absPath = absPath.substring(0, absPath.length() - 1);
        }
        this.absPath = absPath;
        this.metadataName = metadataName;
    }

    public String getAbsPath() {
        return absPath;
    }

    public String getMetadataName() {
        return metadataName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetadataKey key = (MetadataKey) o;
        return absPath.equals(key.absPath) && metadataName.equals(key.metadataName);
    }

    @Override
    public int hashCode() {
        int result;
        result = absPath.hashCode();
        result = 31 * result + metadataName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MetadataKey{" +
                "metadataName='" + metadataName + '\'' +
                ", absPath='" + absPath + '\'' +
                '}';
    }
}
