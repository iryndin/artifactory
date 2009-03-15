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
package org.artifactory.common;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;

/**
 * @author freds
 * @date Oct 10, 2008
 */
public enum ConstantsValue {
    config("config.file", null),
    ajaxRefreshMilis("ajaxRefreshMillis", "10000"),
    lockTimeoutSecs("lockTimeoutSecs", "120"),
    failFastLockTimeoutSecs("failFastLockTimeoutSecs", "5"),
    authenticationCacheIdleTimeSecs("authenticationCacheIdleTimeSecs", "300"),
    jcrFixConsistency("jcr.fixConsistency", FALSE.toString()),
    searchMaxResults("search.maxResults", "500"),
    suppressPomConsistencyChecks("maven.suppressPomConsistencyChecks", FALSE.toString()),
    devDebugMode("dev.debug", FALSE.toString()),
    substituteRepoKeys("repo.key.subst.", null),
    jcrConfigPath("jcr.configPath", null),
    springConfigPath("spring.configPath", null),
    artifactoryVersion("version", null),
    artifactoryRevision("revision", null),
    metadataIdleTimeSecs("metadataCacheIdleTimeSecs", "1200"),
    versioningQueryIntervalSecs("versioningQueryIntervalSecs", "43200"),
    gcIntervalMins("gcIntervalMins", "60"),
    gcThreshold("gcThreshold", "100"),
    gcBatchDeleteMaxSize("gcBatchDeleteMaxSize", "30"),
    logsRefreshRateSecs("logs.refreshrate.secs", "10");

    static final String SYS_PROP_PREFIX = "artifactory.";

    private final String propertyName;
    private final String defValue;

    private Long cachedLongValue;
    private Boolean cachedBooleanValue;

    // TODO: Check with time tests if this is useful?
    //private String cachedStringValue;

    ConstantsValue(String propetyName, String defValue) {
        this.propertyName = SYS_PROP_PREFIX + propetyName;
        this.defValue = defValue;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getDefValue() {
        return defValue;
    }

    public String getString() {
        return ArtifactoryProperties.get().getProperty(propertyName, defValue);
    }

    public int getInt() {
        return (int) getLong();
    }

    public long getLong() {
        if (cachedLongValue == null) {
            cachedLongValue = parseLong(getString());
        }
        return cachedLongValue;
    }

    public boolean getBoolean() {
        if (cachedBooleanValue == null) {
            cachedBooleanValue = parseBoolean(getString());
        }
        return cachedBooleanValue;
    }

    static void clearCache() {
        ConstantsValue[] constantsValues = values();
        for (ConstantsValue value : constantsValues) {
            value.cachedBooleanValue = null;
            value.cachedLongValue = null;
        }
    }
}
