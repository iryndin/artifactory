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
package org.artifactory.update.security;

import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.ConverterUtils;
import org.artifactory.version.VersionComparator;
import org.artifactory.version.converter.XmlConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author freds
 * @date Nov 9, 2008
 */
public enum ArtifactorySecurityVersion {
    v130beta2(ArtifactoryVersion.v125, ArtifactoryVersion.v130beta2,
            new SimpleUserConverter(),
            new RepoPathAclConverter()),
    current(ArtifactoryVersion.v130beta3, ArtifactoryVersion.getCurrent());

    private final VersionComparator comparator;
    private final XmlConverter[] converters;

    ArtifactorySecurityVersion(ArtifactoryVersion from, ArtifactoryVersion until, XmlConverter... converters) {
        this.comparator = new VersionComparator(from, until);
        this.converters = converters;
    }

    public boolean isCurrent() {
        return comparator.isCurrent();
    }

    public boolean supports(ArtifactoryVersion version) {
        return comparator.supports(version);
    }

    public XmlConverter[] getConverters() {
        return converters;
    }

    public String convert(String in) {
        // First create the list of converters to apply
        List<XmlConverter> converters = new ArrayList<XmlConverter>();

        // All converters of versions above me needs to be executed in sequence
        ArtifactorySecurityVersion[] versions = ArtifactorySecurityVersion.values();
        for (ArtifactorySecurityVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.getConverters() != null) {
                converters.addAll(Arrays.asList(version.getConverters()));
            }
        }

        return ConverterUtils.convert(converters, in);
    }

    public static ArtifactorySecurityVersion getSecurityVersion(ArtifactoryVersion version) {
        ArtifactorySecurityVersion[] artifactorySecurityVersions = values();
        for (int i = artifactorySecurityVersions.length - 1; i >= 0; i--) {
            ArtifactorySecurityVersion secVersion = artifactorySecurityVersions[i];
            if (secVersion.supports(version)) {
                return secVersion;
            }
        }
        throw new IllegalStateException(
                "Reading security data from backup of Artifactory version " + version + " is not supported!");
    }
}
