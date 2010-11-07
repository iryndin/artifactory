/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.version;

import org.artifactory.version.converter.NamespaceConverter;
import org.artifactory.version.converter.SnapshotUniqueVersionConverter;
import org.artifactory.version.converter.XmlConverter;
import org.artifactory.version.converter.v100.BackupToElementConverter;
import org.artifactory.version.converter.v100.RepositoriesKeysConverter;
import org.artifactory.version.converter.v110.SnapshotNonUniqueValueConverter;
import org.artifactory.version.converter.v120.AnonAccessNameConverter;
import org.artifactory.version.converter.v130.AnnonAccessUnderSecurityConverter;
import org.artifactory.version.converter.v130.BackupListConverter;
import org.artifactory.version.converter.v130.LdapSettings130Converter;
import org.artifactory.version.converter.v131.LdapAuthenticationPatternsConverter;
import org.artifactory.version.converter.v132.BackupKeyConverter;
import org.artifactory.version.converter.v132.LdapListConverter;
import org.artifactory.version.converter.v134.BackupExcludedVirtualRepoConverter;
import org.artifactory.version.converter.v135.ProxyNTHostConverter;
import org.artifactory.version.converter.v136.IndexerCronRemoverConverter;
import org.artifactory.version.converter.v136.RepositoryTypeConverter;
import org.artifactory.version.converter.v141.ProxyDefaultConverter;
import org.artifactory.version.converter.v142.RepoIncludeExcludePatternsConverter;
import org.artifactory.version.converter.v143.RemoteChecksumPolicyConverter;
import org.artifactory.version.converter.v144.MultiLdapXmlConverter;
import org.artifactory.version.converter.v144.ServerIdXmlConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author freds
 * @author Yossi Shaul
 */
public enum ArtifactoryConfigVersion implements SubConfigElementVersion {
    v100("http://artifactory.jfrog.org/xsd/1.0.0",
            "http://www.jfrog.org/xsd/artifactory-v1_0_0.xsd",
            ArtifactoryVersion.v122rc0,
            ArtifactoryVersion.v125rc6,
            new SnapshotUniqueVersionConverter(),
            new BackupToElementConverter(),
            new RepositoriesKeysConverter()),
    v110("http://artifactory.jfrog.org/xsd/1.1.0",
            "http://www.jfrog.org/xsd/artifactory-v1_1_0.xsd",
            ArtifactoryVersion.v125,
            ArtifactoryVersion.v125,
            new SnapshotNonUniqueValueConverter()),
    v120("http://artifactory.jfrog.org/xsd/1.2.0",
            "http://www.jfrog.org/xsd/artifactory-v1_2_0.xsd",
            ArtifactoryVersion.v125u1,
            ArtifactoryVersion.v125u1,
            new AnonAccessNameConverter()),
    v130("http://artifactory.jfrog.org/xsd/1.3.0",
            "http://www.jfrog.org/xsd/artifactory-v1_3_0.xsd",
            ArtifactoryVersion.v130beta1,
            ArtifactoryVersion.v130beta2,
            new BackupListConverter(), new AnnonAccessUnderSecurityConverter(),
            new LdapSettings130Converter()),
    v131("http://artifactory.jfrog.org/xsd/1.3.1",
            "http://www.jfrog.org/xsd/artifactory-v1_3_1.xsd",
            ArtifactoryVersion.v130beta3,
            ArtifactoryVersion.v130beta3,
            new LdapAuthenticationPatternsConverter()),
    v132("http://artifactory.jfrog.org/xsd/1.3.2",
            "http://www.jfrog.org/xsd/artifactory-v1_3_2.xsd",
            ArtifactoryVersion.v130beta4,
            ArtifactoryVersion.v130beta4,
            new BackupKeyConverter(), new LdapListConverter()),
    v133("http://artifactory.jfrog.org/xsd/1.3.3",
            "http://www.jfrog.org/xsd/artifactory-v1_3_3.xsd",
            ArtifactoryVersion.v130beta5,
            ArtifactoryVersion.v130beta61),
    v134("http://artifactory.jfrog.org/xsd/1.3.4",
            "http://www.jfrog.org/xsd/artifactory-v1_3_4.xsd",
            ArtifactoryVersion.v130rc1,
            ArtifactoryVersion.v130rc1,
            new BackupExcludedVirtualRepoConverter()),
    v135("http://artifactory.jfrog.org/xsd/1.3.5",
            "http://www.jfrog.org/xsd/artifactory-v1_3_5.xsd",
            ArtifactoryVersion.v130rc2,
            ArtifactoryVersion.v205,
            new ProxyNTHostConverter()),
    v136("http://artifactory.jfrog.org/xsd/1.3.6",
            "http://www.jfrog.org/xsd/artifactory-v1_3_6.xsd",
            ArtifactoryVersion.v206,
            ArtifactoryVersion.v208,
            new IndexerCronRemoverConverter(), new RepositoryTypeConverter()),
    v140("http://artifactory.jfrog.org/xsd/1.4.0",
            "http://www.jfrog.org/xsd/artifactory-v1_4_0.xsd",
            ArtifactoryVersion.v210,
            ArtifactoryVersion.v210, new ProxyDefaultConverter()),
    v141("http://artifactory.jfrog.org/xsd/1.4.1",
            "http://www.jfrog.org/xsd/artifactory-v1_4_1.xsd",
            ArtifactoryVersion.v211,
            ArtifactoryVersion.v212,
            new RepoIncludeExcludePatternsConverter()),
    v142("http://artifactory.jfrog.org/xsd/1.4.2",
            "http://www.jfrog.org/xsd/artifactory-v1_4_2.xsd",
            ArtifactoryVersion.v213,
            ArtifactoryVersion.v221,
            new RemoteChecksumPolicyConverter()),
    v143("http://artifactory.jfrog.org/xsd/1.4.3",
            "http://www.jfrog.org/xsd/artifactory-v1_4_3.xsd",
            ArtifactoryVersion.v222,
            ArtifactoryVersion.v223,
            new MultiLdapXmlConverter(), new ServerIdXmlConverter()),
    v144("http://artifactory.jfrog.org/xsd/1.4.4",
            "http://www.jfrog.org/xsd/artifactory-v1_4_4.xsd",
            ArtifactoryVersion.v224,
            ArtifactoryVersion.v225),
    v145("http://artifactory.jfrog.org/xsd/1.4.5",
            "http://www.jfrog.org/xsd/artifactory-v1_4_5.xsd",
            ArtifactoryVersion.v230,
            ArtifactoryVersion.v230),
    v146("http://artifactory.jfrog.org/xsd/1.4.6",
            "http://www.jfrog.org/xsd/artifactory-v1_4_6.xsd",
            ArtifactoryVersion.v231,
            ArtifactoryVersion.getCurrent());

    private final String xsdUri;
    private final String xsdLocation;
    private final VersionComparator comparator;
    private final XmlConverter[] converters;

    /**
     * @param from       The artifactory version this config version was first used
     * @param until      The artifactory version this config was last used in (inclusive)
     * @param converters A list of converters to use to move from <b>this</b> config version to the <b>next</b> config
     *                   version
     */
    ArtifactoryConfigVersion(String xsdUri, String xsdLocation, ArtifactoryVersion from, ArtifactoryVersion until,
            XmlConverter... converters) {
        this.comparator = new VersionComparator(this, from, until);
        this.xsdUri = xsdUri;
        this.xsdLocation = xsdLocation;
        this.converters = converters;
    }

    public static ArtifactoryConfigVersion getCurrent() {
        ArtifactoryConfigVersion[] versions = ArtifactoryConfigVersion.values();
        return versions[versions.length - 1];
    }

    public boolean isCurrent() {
        return comparator.isCurrent();
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

        return XmlConverterUtils.convert(converters, in);
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

    public VersionComparator getComparator() {
        return comparator;
    }

    public static ArtifactoryConfigVersion getConfigVersion(String configXml) {
        // Find correct version by schema URI
        ArtifactoryConfigVersion[] configVersions = values();
        for (ArtifactoryConfigVersion configVersion : configVersions) {
            if (configXml.contains(configVersion.getXsdUri())) {
                return configVersion;
            }
        }
        return null;
    }
}
