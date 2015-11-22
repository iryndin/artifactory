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

package org.artifactory.support.core.collectors;

import org.artifactory.support.config.CollectConfiguration;
import org.artifactory.support.core.exceptions.TempDirAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Provides content specific services where every
 * item in the context requires unique treatment
 *
 * @author Michael Pasternak
 */
public abstract class AbstractSpecificContentCollector<T extends CollectConfiguration>
        extends AbstractContentCollector<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractSpecificContentCollector.class);

    /**
     * @param contentName a name for specific sub-folder
     */
    protected AbstractSpecificContentCollector(String contentName) {
        super(contentName);
    }

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * Performs copy to destination directory
     *
     * @param file source
     * @param tmpDir target
     */
    protected void copyToTempDir(Path file, File tmpDir) {
        log.debug("Initiating copy of file '{}'", file.getFileName());
        try {
            Files.copy(
                    file,
                    Paths.get(
                            tmpDir.getPath() + File.separator + file.getFileName()
                    ),
                    StandardCopyOption.COPY_ATTRIBUTES
            );
        } catch (IOException e) {
            throw new TempDirAccessException("Cannot copy file '"+file.getFileName()
                    +"' to '" + tmpDir.getPath() + "' because " + e.getMessage(), e
            );
        }
    }

    /**
     * Prints failure cause by exception
     *
     * @param e the cause
     */
    protected boolean failure(Exception e) {
        getLog().warn("Collecting content has failed - '" + e.getMessage() + "'");
        getLog().debug("Cause: {}", e);
        getLog().info("Collection of " + getContentName() +
                " was not properly accomplished, see logs for more details");
        return false;
    }
}
