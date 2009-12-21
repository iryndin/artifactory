/*
 * This file is part of Artifactory.
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

package org.artifactory.version.converter.v136;

import org.apache.commons.io.FileUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * The logback configuration converter
 *
 * @author Noam Tenne
 */
public class LogbackConfigConverter {

    private static final Logger log = LoggerFactory.getLogger(LogbackConfigConverter.class);

    /**
     * Renames the old logback config file to "logback.xml.old", and copies the updated config file from
     * META-INF/default to "logback.xml".
     *
     * @param artifactoryHome Instance of ArtifactoryHome
     */
    public void convert(ArtifactoryHome artifactoryHome) {
        File etcDir = artifactoryHome.getEtcDir();
        File oldConfigFile = new File(etcDir, ArtifactoryHome.LOGBACK_CONFIG_FILE_NAME);

        //Make sure old config file exists
        if ((oldConfigFile.exists()) && (oldConfigFile.isFile())) {
            boolean renamingSucceeded = oldConfigFile.renameTo(new File(etcDir, "logback.original.xml"));

            //Make sure the old config has been renamed
            if (renamingSucceeded) {
                log.info("Old logback configuration file has been successfully backed up.");
            } else {
                log.warn("Old logback configuration file has could not be backed up.");
            }
        } else {
            //Log to debug if a config file isn't found
            log.debug("Old logback configuration file was not found.");
        }

        //Get the updated config
        URL newConfigFile = getClass().getResource("/META-INF/default/" + ArtifactoryHome.LOGBACK_CONFIG_FILE_NAME);
        if (newConfigFile == null) {
            log.error("Replacement logback configuration file was not found in '/META-INF/default/'.");
            return;
        }

        //Copy the updated config to the original location ($ARTIFACTORY_HOME/etc/logback.xml)
        try {
            FileUtils.copyURLToFile(newConfigFile, oldConfigFile);
        } catch (IOException e) {
            log.error("An error has occurred while converting the logback configuration file.", e);
        }

        log.info("Logback configuration file has been successfully converted.");
    }
}
