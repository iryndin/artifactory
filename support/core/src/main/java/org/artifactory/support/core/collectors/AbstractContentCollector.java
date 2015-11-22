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

import org.artifactory.io.FileUtils;
import org.artifactory.support.config.CollectConfiguration;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Generic ContentCollector
 *
 * @param <T> configuration type declaration
 *
 *
 * @author Michael Pasternak
 */
public abstract class AbstractContentCollector<T extends CollectConfiguration>
        implements ContentCollector<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractContentCollector.class);
    private final String contentName;

    /**
     * @param contentName a name for specific sub-folder
     */
    protected AbstractContentCollector(String contentName) {
        this.contentName = contentName;
    }

    /**
     * Collects content according to {@link CollectConfiguration}
     *
     * @param configuration {@link CollectConfiguration}
     * @param tmpDir output dir
     *
     * @return boolean
     */
    @Override
    public final boolean collect(T configuration, File tmpDir) {
        try {
            if (configuration.isEnabled()) {
                getLog().debug("Ensuring configuration is correct for '{}'", getContentName());
                doEnsureConfiguration(configuration);

                getLog().info("Starting " + getContentName() + " collection ...");
                File contentSpecificTmpDir = produceTempDirectory(tmpDir);
                ensureTempDir(contentSpecificTmpDir);
                return doCollect(configuration, contentSpecificTmpDir);
            } else {
                getLog().debug("Configuration {} is disabled", configuration);
            }
        } catch (IllegalStateException ise) {
            getLog().error("Executing task " + getContentName() + " has failed ("+ ise.getMessage()+")");
            getLog().debug("Cause: {}", ise);
        }
        return false;
    }

    /**
     * Calculates temp directory from output directory and content-name
     *
     * @param outputDirectory the output directory
     *
     * @return temp directory
     */
    private File produceTempDirectory(File outputDirectory) {
        return new File(
                outputDirectory.getAbsolutePath() + File.separator + getContentName()
        );
    }

    /**
     * Makes sure directory exist
     *
     * @param archiveTmpDir
     */
    private void ensureTempDir(File archiveTmpDir) {
        FileUtils.createDirectory(archiveTmpDir);
    }

    /**
     * @return sub-folder name
     */
    protected String getContentName() {
        return contentName;
    }

    /**
     * Produces content specific output {@link File}
     *
     * @param tmpDir output dir
     * @return output {@link File}
     */
    protected File getOutputFile(File tmpDir) {
        return new File(tmpDir.getPath() + File.separator + getFileName());
    }

    /**
     * @return The filename to be used
     */
    protected String getFileName() {
        return getContentName()+".artifactory";
    }

    /**
     * @return logger
     */
    protected Logger getLog() {
        return log;
    }

    /**
     * Collects content according to {@link CollectConfiguration}
     *
     * @param configuration {@link CollectConfiguration}
     * @param tmpDir output dir
     *
     * @return boolean
     */
    protected abstract boolean doCollect(T configuration, File tmpDir);

    /**
     * Makes sure configuration is valid
     *
     * @param configuration configuration to check
     * @throws BundleConfigurationException if configuration is invalid
     */
    protected abstract void doEnsureConfiguration(T configuration) throws BundleConfigurationException;
}
