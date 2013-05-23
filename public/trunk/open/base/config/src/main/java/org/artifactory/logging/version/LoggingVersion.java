/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.logging.version;

import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.logging.version.v1.LogbackConfigSwapper;
import org.artifactory.logging.version.v3.LineNumberLayoutLoggerConverter;
import org.artifactory.logging.version.v4.PublicApiPackageChangeLoggerConverter;
import org.artifactory.logging.version.v6.RequestTraceLoggerConverter;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.SubConfigElementVersion;
import org.artifactory.version.VersionComparator;
import org.artifactory.version.XmlConverterUtils;
import org.artifactory.version.converter.XmlConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of the logging configuration versions
 *
 * @author Noam Y. Tenne
 */
public enum LoggingVersion implements SubConfigElementVersion {
    v1(ArtifactoryVersion.v122rc0, ArtifactoryVersion.v208, new LogbackConfigSwapper()),
    v2(ArtifactoryVersion.v210, ArtifactoryVersion.v213, null),
    v3(ArtifactoryVersion.v220, ArtifactoryVersion.v221, new LineNumberLayoutLoggerConverter()),
    v4(ArtifactoryVersion.v222, ArtifactoryVersion.v225, new PublicApiPackageChangeLoggerConverter()),
    v5(ArtifactoryVersion.v230, ArtifactoryVersion.v242, null),
    v6(ArtifactoryVersion.v250, ArtifactoryVersion.v252, new RequestTraceLoggerConverter()),
    v7(ArtifactoryVersion.v260, ArtifactoryVersion.getCurrent(), null);

    public static final String LOGGING_CONVERSION_PERFORMED = "loggingConversionPerformed";

    private static final Logger log = LoggerFactory.getLogger(LoggingVersion.class);

    private final VersionComparator comparator;
    private XmlConverter xmlConverter;

    /**
     * Main constructor
     *
     * @param from         Start version
     * @param until        End version
     * @param xmlConverter XML converter required for the specified range
     */
    LoggingVersion(ArtifactoryVersion from, ArtifactoryVersion until, XmlConverter xmlConverter) {
        this.xmlConverter = xmlConverter;
        this.comparator = new VersionComparator(this, from, until);
    }

    /**
     * Run the needed conversions
     *
     * @param artifactoryHome Artifactory home
     */
    public void convert(ArtifactoryHome artifactoryHome) {
        // First create the list of converters to apply
        List<XmlConverter> converters = new ArrayList<XmlConverter>();

        // All converters of versions above me needs to be executed in sequence
        LoggingVersion[] versions = LoggingVersion.values();
        for (LoggingVersion version : versions) {
            if (version.ordinal() >= ordinal() && version.xmlConverter != null) {
                converters.add(version.xmlConverter);
            }
        }

        if (!converters.isEmpty()) {
            File logbackConfigFile = new File(artifactoryHome.getEtcDir(), ArtifactoryHome.LOGBACK_CONFIG_FILE_NAME);
            try {
                String result =
                        XmlConverterUtils.convert(converters, FileUtils.readFileToString(logbackConfigFile, "utf-8"));
                backupAndSaveLogback(result, artifactoryHome);
            } catch (IOException e) {
                log.error("Error occurred while converting logback config for conversion: {}.", e.getMessage());
                log.debug("Error occurred while converting logback config for conversion", e);
            }
        }
    }

    @Override
    public VersionComparator getComparator() {
        return comparator;
    }

    /**
     * Creates a backup of the existing logback configuration file and proceeds to save post-conversion content
     *
     * @param result          Conversion result
     * @param artifactoryHome Artifactory home
     */
    public void backupAndSaveLogback(String result, ArtifactoryHome artifactoryHome) throws IOException {
        File etcDir = artifactoryHome.getEtcDir();
        File logbackConfigFile = new File(etcDir, ArtifactoryHome.LOGBACK_CONFIG_FILE_NAME);
        if (logbackConfigFile.exists()) {
            File originalBackup = new File(etcDir, "logback.original.xml");
            if (originalBackup.exists()) {
                FileUtils.deleteQuietly(originalBackup);
            }
            FileUtils.moveFile(logbackConfigFile, originalBackup);
        }

        FileUtils.writeStringToFile(logbackConfigFile, result, "utf-8");
    }
}