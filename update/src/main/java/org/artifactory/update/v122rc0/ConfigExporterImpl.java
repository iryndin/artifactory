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
package org.artifactory.update.v122rc0;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.update.VersionsHolder;
import org.artifactory.update.config.ArtifactoryConfigUpdate;
import org.artifactory.update.config.ConfigExporter;
import org.artifactory.version.ArtifactoryVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Converts the artifactory.config.xml to the latest version. Currently can convert all the supported versions.
 *
 * @author freds
 * @date Aug 15, 2008
 */
public class ConfigExporterImpl implements ConfigExporter {
    private CentralConfigDescriptor descriptor;

    public void init() {
        try {
            File oldConfigFile = ArtifactoryHome.getConfigFile();
            File newConfigFile = ArtifactoryConfigUpdate.convertConfigFile(oldConfigFile);
            ArtifactoryHome.setConfigFile(newConfigFile);

            File configFile = ArtifactoryHome.getConfigFile();
            descriptor = JaxbHelper.readConfig(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CentralConfigDescriptor getDescriptor() {
        if (descriptor == null) {
            init();
        }
        return descriptor;
    }

    public void setDescriptor(CentralConfigDescriptor descriptor) {
        // TODO
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        File destFile = new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_CONFIG_FILE);
        File originalConfigFile = ArtifactoryHome.getConfigFile();
        try {
            FileUtils.copyFile(originalConfigFile, destFile);
            // Create the artifactory.properties and artifactory.system.properties files under dest
            Properties versionProps = new Properties();
            // Always latest version for dump command
            ArtifactoryVersion currentVersion = ArtifactoryVersion.getCurrent();
            versionProps.setProperty(ConstantsValue.artifactoryVersion.getPropertyName(), currentVersion.getValue());
            versionProps.setProperty(ConstantsValue.artifactoryRevision.getPropertyName(),
                    "" + currentVersion.getRevision());
            FileOutputStream os = new FileOutputStream(
                    new File(settings.getBaseDir(), ArtifactoryHome.ARTIFACTORY_PROPERTIES_FILE));
            try {
                versionProps.store(os,
                        "Exported properties from DB of " + VersionsHolder.getOriginalVersion());
            } finally {
                IOUtils.closeQuietly(os);
            }
            // TODO: the system properties
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to copy " + originalConfigFile + " to " + destFile, e);
        }
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
    }
}
