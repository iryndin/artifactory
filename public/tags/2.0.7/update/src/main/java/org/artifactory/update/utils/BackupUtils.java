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
import org.artifactory.update.security.ArtifactorySecurityVersion;
import org.artifactory.version.ArtifactoryVersion;
import org.artifactory.version.ArtifactoryVersionReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

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
                    "Cannot find Artifactory of null or non existent folder " + backupFolder);
        }

        File propFile = new File(backupFolder, ArtifactoryHome.ARTIFACTORY_PROPERTIES_FILE);
        if (!propFile.exists()) {
            throw new RuntimeException("Backup folder " + backupFolder.getAbsolutePath() +
                    " does not contain file " + propFile.getName());
        }

        return ArtifactoryVersionReader.read(propFile).getVersion();
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
