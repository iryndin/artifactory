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

package org.artifactory.mime.version;

import com.google.common.collect.Lists;
import org.artifactory.mime.version.converter.v1.XmlIndexedConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;
import org.artifactory.version.XmlConverterUtils;
import org.artifactory.version.converter.XmlConverter;

import java.util.Arrays;
import java.util.List;

/**
 * A mimetypes.xml version.
 *
 * @author Yossi Shaul
 */
public enum MimeTypesVersion implements SubConfigElementVersion {
    v1(ArtifactoryVersion.v223, ArtifactoryVersion.v225, new XmlIndexedConverter()),
    v2(ArtifactoryVersion.v230, ArtifactoryVersion.getCurrent(), null);

    private final XmlConverter[] converters;

    /**
     * @param start      First Artifactory version that this version was supported.
     * @param end        Last Artifactory version that this version was support.
     * @param converters List of converters to apply for conversion to the next config version.
     */
    MimeTypesVersion(ArtifactoryVersion start, ArtifactoryVersion end, XmlConverter... converters) {
        this.converters = converters;
    }

    public String convert(String mimeTypesXmlAsString) {
        // First create the list of converters to apply
        List<XmlConverter> converters = Lists.newArrayList();

        // All converters of versions above me needs to be executed in sequence
        MimeTypesVersion[] versions = MimeTypesVersion.values();
        for (MimeTypesVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.converters != null) {
                converters.addAll(Arrays.asList(version.converters));
            }
        }

        return XmlConverterUtils.convert(converters, mimeTypesXmlAsString);
    }

    public VersionComparator getComparator() {
        throw new UnsupportedOperationException("stop being lazy and implement me");
    }

    /**
     * @param mimeTypesXmlAsString The string representation of the mimetypes.xml file
     * @return The {@link MimeTypesVersion} matching the xml content.
     */
    public static MimeTypesVersion findVersion(String mimeTypesXmlAsString) {
        final String VERSION_ATT = "<mimetypes version=\"";
        int versionIdx = mimeTypesXmlAsString.indexOf(VERSION_ATT);
        if (versionIdx < 0) {
            throw new IllegalArgumentException("Unidentified mimetypes configuration");
        }

        int versionStartIndex = versionIdx + VERSION_ATT.length();
        int version = Integer.parseInt(mimeTypesXmlAsString.substring(versionStartIndex, versionStartIndex + 1));
        if (MimeTypesVersion.values().length < version) {
            throw new IllegalArgumentException("Version " + version + " no found.");
        }
        return MimeTypesVersion.values()[version - 1];
    }

    public static MimeTypesVersion getCurrent() {
        MimeTypesVersion[] versions = MimeTypesVersion.values();
        return versions[versions.length - 1];
    }

    public boolean isCurrent() {
        return this == getCurrent();
    }
}
