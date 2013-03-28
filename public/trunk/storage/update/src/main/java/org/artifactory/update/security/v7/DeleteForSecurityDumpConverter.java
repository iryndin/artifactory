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

package org.artifactory.update.security.v7;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.log.LoggerFactory;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import javax.jcr.Session;
import java.io.File;
import java.io.IOException;

/**
 * Creates the .deleteForSecurityMarker marker file inside the data directory
 *
 * @author Shay Yaakov
 */
public class DeleteForSecurityDumpConverter implements ConfigurationConverter<Session> {
    private static final Logger log = LoggerFactory.getLogger(DeleteForSecurityDumpConverter.class);

    @Override
    public void convert(Session session) {
        log.info("Creating the dump security marker file...");
        File securityDumpMarkerFile = new File(ArtifactoryHome.get().getDataDir(), ".deleteForSecurityMarker");
        try {
            securityDumpMarkerFile.createNewFile();
        } catch (IOException e) {
            log.debug("Could not create file: '" + securityDumpMarkerFile.getAbsolutePath() + "'.", e);
        }

        log.debug("The dump security marker file has been created.");
    }
}
