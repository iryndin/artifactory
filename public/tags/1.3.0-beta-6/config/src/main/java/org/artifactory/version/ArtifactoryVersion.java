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
package org.artifactory.version;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * User: freds Date: May 29, 2008 Time: 10:15:59 AM
 */
public enum ArtifactoryVersion {
    v122rc0("1.2.2-rc0", 804),
    v122rc1("1.2.2-rc1", 819),
    v122rc2("1.2.2-rc2", 826),
    v122("1.2.2", 836),
    v125rc0("1.2.5-rc0", 970),
    v125rc1("1.2.5-rc1", 1015),
    v125rc2("1.2.5-rc2", 1082),
    v125rc3("1.2.5-rc3", 1087),
    v125rc4("1.2.5-rc4", 1104),
    v125rc5("1.2.5-rc5", 1115),
    v125rc6("1.2.5-rc6", 1136),
    v125("1.2.5", 1154),
    v125u1("1.2.5u1", 1174),
    v130beta1("1.3.0-beta-1", 1501),
    v130beta2("1.3.0-beta-2", 1509),
    v130beta3("1.3.0-beta-3", 1992),
    v130beta4("1.3.0-beta-4", 2065),
    v130beta5("1.3.0-beta-5", 2282),
    v130beta6("1.3.0-beta-6", Integer.MAX_VALUE);

    public static ArtifactoryVersion getCurrent() {
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        return versions[versions.length - 1];
    }

    private final String value;
    private final int revision;
    private final List<URL> resources = new ArrayList<URL>();

    ArtifactoryVersion(String value, int revision) {
        this.value = value;
        this.revision = revision;
    }

    public String getValue() {
        return value;
    }

    public int getRevision() {
        return revision;
    }

    public URL findResource(String resourceName) {
        // Look into my resources
        for (URL url : resources) {
            if (url.toString().endsWith(resourceName)) {
                return url;
            }
        }
        // Try to load it from classpath with my version
        URL result = getClass().getResource("/" + name() + "/" + resourceName);
        if (result != null) {
            resources.add(result);
            return result;
        }

        // If last version, no luck
        if (ordinal() == 0) {
            return null;
        }
        // Look in previous versions
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        return versions[ordinal() - 1].findResource(resourceName);
    }

    public boolean isCurrent() {
        return this == getCurrent();
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is before the other version
     */
    public boolean before(ArtifactoryVersion otherVersion) {
        return this.compareTo(otherVersion) < 0;
    }

    /**
     * @param otherVersion The other ArtifactoryVersion
     * @return returns true if this version is before or equal to the other version
     */
    public boolean beforeOrEqual(ArtifactoryVersion otherVersion) {
        return this == otherVersion || this.compareTo(otherVersion) < 0;
    }
}
