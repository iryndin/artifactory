/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.converters;

import org.artifactory.common.property.FatalConversionException;
import org.artifactory.logging.version.LoggingVersion;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Author: gidis
 */
public class LoggingConverter implements ArtifactoryConverterAdapter {
    private static final Logger log = LoggerFactory.getLogger(LoggingConverter.class);
    private File from;
    private File to;

    public LoggingConverter(File from, File to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        try {
            // Perform the logback conversion here because if we do it after configuration is loaded, we must wait 'till
            // the changes are detected by the watchdog (possibly missing out on important log messages)
            //Might be first run, protect
            if (from.exists()) {
                LoggingVersion.convert(source.getVersion(), target.getVersion(), from, to);
            }
        } catch (FatalConversionException e) {
            //When a fatal conversion happens fail the context loading
            log.error(
                    "Conversion failed with fatal status.\n" +
                            "You should analyze the error and retry launching " +
                            "Artifactory. Error is: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            //When conversion fails - report and continue - don't fail
            log.error("Failed to execute logging conversion.", e);
        }
    }

    @Override
    public boolean isInterested(CompoundVersionDetails source, CompoundVersionDetails target) {
        return source != null && !source.isCurrent();
    }

    @Override
    public void conversionEnded() {
    }
}

