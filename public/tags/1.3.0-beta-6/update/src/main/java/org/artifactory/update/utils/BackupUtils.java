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
package org.artifactory.update.utils;

import org.apache.commons.io.FileUtils;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.update.security.ArtifactorySecurityVersion;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author freds
 * @date Nov 9, 2008
 */
public class BackupUtils {
    private static final Logger log =
            LoggerFactory.getLogger(BackupUtils.class);

    public static ArtifactoryVersion findVersion(File backupFolder) {
        if (backupFolder == null || !backupFolder.exists()) {
            throw new IllegalArgumentException(
                    "Cannot find Artifactory of null or non existent folder");
        }
        File propFile = new File(backupFolder, ArtifactoryHome.ARTIFACTORY_PROPERTIES_FILE);
        if (!propFile.exists()) {
            throw new RuntimeException(
                    "Backup folder " + backupFolder.getAbsolutePath() + " does not contains file " +
                            propFile.getName());
        }
        Properties props = new Properties();
        try {
            FileInputStream stream = new FileInputStream(propFile);
            props.load(stream);
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read property file " + propFile.getAbsolutePath(), e);
        }
        String artifactoryVersion = props.getProperty(ConstantsValue.artifactoryVersion.getPropertyName());
        String revisionString = props.getProperty(ConstantsValue.artifactoryRevision.getPropertyName());
        // If current version or development version ${project.version}
        if (ArtifactoryVersion.getCurrent().getValue().equals(artifactoryVersion) ||
                artifactoryVersion.equals("${project.version}") ||
                revisionString.equals("${buildNumber}")) {
            // Just return the current version
            return ArtifactoryVersion.getCurrent();
        }
        int artifactoryRevision = Integer.parseInt(revisionString);
        for (ArtifactoryVersion version : ArtifactoryVersion.values()) {
            if (version.getValue().equals(artifactoryVersion)) {
                if (artifactoryRevision != version.getRevision()) {
                    log.warn("Backup version found is " + version + " but the revision " +
                            artifactoryRevision + " is not the one supported!\n" +
                            "Reading the backup folder may work with this version.\n" +
                            "For Information: Using the Command Line Tool is preferable in this case.");
                }
                return version;
            }
        }
        log.warn("Backup version " + artifactoryVersion + " is not part of the realeased version" +
                "The actual version will be determined by the closest revision from " +
                artifactoryRevision + ". Becareful: This action is not the one supported!\n" +
                "Reading the backup folder may or may not work!\n" +
                "For Information: Using the Command Line Tool and providing the version by hand " +
                "is preferable in this case.");
        for (ArtifactoryVersion version : ArtifactoryVersion.values()) {
            if (version.getRevision() >= artifactoryRevision) {
                return version;
            }
        }
        throw new IllegalStateException(
                "No version declared is higher than " + artifactoryRevision);
    }

    public static String convertSecurityFile(File backupFolder, ArtifactoryVersion backupVersion) throws IOException {
        // Find security version that matches. First before or equal valid until
        ArtifactorySecurityVersion securityVersion = ArtifactorySecurityVersion.getSecurityVersion(backupVersion);
        File securityXmlFile = new File(backupFolder, SecurityService.FILE_NAME);
        if (!securityXmlFile.exists()) {
            throw new IOException("Cannot convert non existent security file " +
                    securityXmlFile.getAbsolutePath());
        }
        String secXmlData = FileUtils.readFileToString(securityXmlFile);
        return securityVersion.convert(secXmlData);
    }
}
