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

package org.artifactory.jcr.version;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.jcr.JcrSession;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Date: 11/13/11
 * Time: 12:32 PM
 *
 * @author Fred Simon
 */
public abstract class MarkerFileConverterBase implements ConfigurationConverter<JcrSession> {
    private static final Logger log = LoggerFactory.getLogger(MarkerFileConverterBase.class);

    public abstract boolean needConversion();

    public abstract void applyConversion();

    protected void createMarkerFile(String markerFileName, String actionDesc) {
        log.info("Marking for {} metadata.", actionDesc);
        File markerFile = new File(ArtifactoryHome.get().getDataDir(), markerFileName);
        try {
            boolean fileWasCreated = markerFile.createNewFile();
            if (!fileWasCreated) {
                String message = String.format("Failed to mark for %s metadata: marker file was not created: '%s'.",
                        actionDesc, markerFile.getAbsolutePath());
                message += markerFile.exists() ? "File already exist" : "File doesn't exist";
                log.warn(message, actionDesc);
            }
        } catch (IOException e) {
            log.error("Error while marking for {} metadata: {}", e.getMessage(), actionDesc);
            log.debug("Error while marking for " + actionDesc + " metadata.", e);
        }
    }
}
