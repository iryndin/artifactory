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

import org.artifactory.descriptor.Descriptor;
import org.artifactory.version.converter.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author freds
 * @author Yossi Shaul
 */
public enum ArtifactoryConfigVersion {
    OneZero("http://artifactory.jfrog.org/xsd/1.0.0",
            "http://www.jfrog.org/xsd/artifactory-v1_0_0.xsd",
            ArtifactoryVersion.v122,
            new SnapshotUniqueVersionConverter(),
            new BackupToElementConverter(),
            new RepositoriesKeysConverter()),
    OneOne("http://artifactory.jfrog.org/xsd/1.1.0",
            "http://www.jfrog.org/xsd/artifactory-v1_1_0.xsd",
            ArtifactoryVersion.v125,
            new SnapshotNonUniqueValueConverter()),
    OneTwo("http://artifactory.jfrog.org/xsd/1.2.0",
            "http://www.jfrog.org/xsd/artifactory-v1_2_0.xsd",
            ArtifactoryVersion.v125u1,
            new AnonAccessNameConverter()),
    OneThree("http://artifactory.jfrog.org/xsd/1.3.0",
            "http://www.jfrog.org/xsd/artifactory-v1_3_0.xsd",
            ArtifactoryVersion.v130beta2,
            new BackupListConverter(), new AnnonAccessUnderSecurityConverter(),
            new LdapSettings130Converter()),
    OneThreeOne("http://artifactory.jfrog.org/xsd/1.3.1",
            "http://www.jfrog.org/xsd/artifactory-v1_3_1.xsd",
            ArtifactoryVersion.v130beta3,
            new LdapAuthenticationPatternsConverter()),
    OneThreeTwo("http://artifactory.jfrog.org/xsd/1.3.2",
            "http://www.jfrog.org/xsd/artifactory-v1_3_2.xsd",
            ArtifactoryVersion.v130beta4,
            new BackupKeyConverter(), new LdapListConverter()),
    OneThreeThree("http://artifactory.jfrog.org/xsd/1.3.3",
            "http://www.jfrog.org/xsd/artifactory-v1_3_3.xsd",
            ArtifactoryVersion.v130beta61), // no converters from this to next version
    OneThreeFour("http://artifactory.jfrog.org/xsd/1.3.4",
            "http://www.jfrog.org/xsd/artifactory-v1_3_4.xsd",
            ArtifactoryVersion.v130rc1),
    OneThreeFive("http://artifactory.jfrog.org/xsd/1.3.5",
            "http://www.jfrog.org/xsd/artifactory-v1_3_5.xsd",
            ArtifactoryVersion.getCurrent());

    private final String xsdUri;
    private final String xsdLocation;
    private final ArtifactoryVersion untilArtifactoryVersion;
    private final XmlConverter[] converters;

    public static ArtifactoryConfigVersion getCurrent() {
        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        return versions[versions.length - 1];
    }

    ArtifactoryConfigVersion(String xsdUri, String xsdLocation,
            ArtifactoryVersion untilArtifactoryVersion, XmlConverter... converters) {
        this.xsdUri = xsdUri;
        this.xsdLocation = xsdLocation;
        this.untilArtifactoryVersion = untilArtifactoryVersion;
        this.converters = converters;
    }

    public String convert(String in) {
        // First create the list of converters to apply
        List<XmlConverter> converters = new ArrayList<XmlConverter>();

        // First thing to do is to change the namespace and schema location
        converters.add(new NamespaceConverter());

        // All converters of versions above me needs to be executed in sequence
        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        for (ArtifactoryConfigVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.getConverters() != null) {
                converters.addAll(Arrays.asList(version.getConverters()));
            }
        }

        return ConverterUtils.convert(converters, in);
    }

    public String getXsdUri() {
        return xsdUri;
    }

    public String getXsdLocation() {
        return xsdLocation;
    }

    public XmlConverter[] getConverters() {
        return converters;
    }

    public ArtifactoryVersion getUntilArtifactoryVersion() {
        return untilArtifactoryVersion;
    }

    public static ArtifactoryConfigVersion getConfigVersion(String configXml) {
        ArtifactoryConfigVersion[] configVersions = values();
        // First check sanity of conversion
        // Make sure a conversion is never activated twice
        Set<XmlConverter> allConversions = new HashSet<XmlConverter>();
        ArtifactoryConfigVersion configVersionTest = null;
        for (ArtifactoryConfigVersion configVersion : configVersions) {
            XmlConverter[] versionConversions = configVersion.getConverters();
            for (XmlConverter converter : versionConversions) {
                if (allConversions.contains(converter)) {
                    throw new IllegalStateException(
                            "XML Converter element can only be used once!\n" +
                                    "XML Converter " + converter + " is used in " +
                                    configVersion + " but was already used.");
                }
                allConversions.add(converter);
            }
            configVersionTest = configVersion;
        }
        // The last should be current
        if (configVersionTest != getCurrent()) {
            throw new IllegalStateException("The last config version " + configVersionTest +
                    " is not the current one " + getCurrent());
        }

        // The last should have the same namespace as Descriptor.NS
        if (!Descriptor.NS.equals(getCurrent().getXsdUri())) {
            throw new IllegalStateException("The latest config version" + getCurrent() +
                    " has a different namespace than in the Descriptor interface: " +
                    Descriptor.NS);
        }

        // The last should not have any conversion
        XmlConverter[] currentConversions = getCurrent().getConverters();
        if (currentConversions != null && currentConversions.length > 0) {
            throw new IllegalStateException("The last config version " + configVersionTest +
                    " should have any conversions declared");
        }
        // Find correct version by schema URI
        for (ArtifactoryConfigVersion configVersion : configVersions) {
            if (configXml.contains(configVersion.getXsdUri())) {
                return configVersion;
            }
        }
        return null;
    }
}
