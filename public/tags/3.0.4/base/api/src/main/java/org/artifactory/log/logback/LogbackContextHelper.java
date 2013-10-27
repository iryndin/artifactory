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

package org.artifactory.log.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.artifactory.logging.version.LoggingVersion;
import org.artifactory.version.CompoundVersionDetails;

import java.io.IOException;

/**
 * @author Yoav Landman
 */
public abstract class LogbackContextHelper {
    private LogbackContextHelper() {
        // utility class
    }

    public static LoggerContext configure(LoggerContext lc, ArtifactoryHome artifactoryHome) {
        return configure(lc, artifactoryHome, "");
    }

    public static LoggerContext configure(LoggerContext lc, ArtifactoryHome artifactoryHome, String contextId) {
        try {
            contextId = StringUtils.trimToEmpty(contextId);
            contextId = "artifactory".equalsIgnoreCase(contextId) ? "" : contextId + " ";
            contextId = StringUtils.isBlank(contextId) ? "" : contextId;

            /**
             * Perform the logback conversion here because if we do it after configuration is loaded, we must wait 'till
             * the changes are detected by the watchdog (possibly missing out on important log messages)
             */
            boolean startedFromDifferentVersion = artifactoryHome.startedFromDifferentVersion();
            if (startedFromDifferentVersion) {

                ArtifactorySystemProperties properties = artifactoryHome.getArtifactoryProperties();
                boolean conversionPerformed =
                        Boolean.valueOf(properties.getProperty(LoggingVersion.LOGGING_CONVERSION_PERFORMED, "false"));

                if (!conversionPerformed) {
                    CompoundVersionDetails source = artifactoryHome.getOriginalVersionDetails();

                    //Might be first run, protect
                    if (source != null) {
                        LoggingVersion.values();
                        LoggingVersion originalVersion =
                                source.getVersion().getSubConfigElementVersion(LoggingVersion.class);
                        originalVersion.convert(artifactoryHome.getEtcDir());
                        properties.setProperty(LoggingVersion.LOGGING_CONVERSION_PERFORMED, "true");
                    }
                }
            }

            JoranConfigurator configurator = new JoranConfigurator();
            lc.stop();
            configurator.setContext(lc);
            // Set the contextId to differentiate AOLs console logger logs
            lc.putProperty("artifactory.contextId", StringUtils.uncapitalize(contextId));
            // Set the artifactory.home so that tokens in the logback config file are extracted
            lc.putProperty(ArtifactoryHome.SYS_PROP, artifactoryHome.getHomeDir().getAbsolutePath());
            configurator.doConfigure(artifactoryHome.getLogbackConfig());
            StatusPrinter.printIfErrorsOccured(lc);
        } catch (JoranException | IOException je) {
            StatusPrinter.print(lc);
        }
        return lc;
    }
}