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

package org.artifactory.support.core.collectors.analysis;

import org.artifactory.mbean.ThreadDumper;
import org.artifactory.support.config.analysis.ThreadDumpConfiguration;
import org.artifactory.support.core.collectors.AbstractSpecificContentCollector;
import org.artifactory.support.core.exceptions.BundleConfigurationException;
import org.artifactory.support.utils.StringBuilderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.google.common.io.Files;
import com.google.common.base.Charsets;

/**
 * ThreadDump collector
 *
 * @author Michael Pasternak
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class ThreadDumpCollector extends AbstractSpecificContentCollector<ThreadDumpConfiguration> {

    private static final Logger log = LoggerFactory.getLogger(ThreadDumpCollector.class);
    private static final String FILE_EXTENSION = ".tdump";
    private final ThreadDumper threadDumper;

    public ThreadDumpCollector() {
        super("thread-dump");
        threadDumper = new ThreadDumper();
    }

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * Collects thread dump/s according to {@link org.artifactory.support.config.CollectConfiguration}
     *
     * @param configuration {@link org.artifactory.support.config.analysis.ThreadDumpConfiguration}
     * @param tmpDir output dir
     *
     * @return result
     */
    @Override
    protected boolean doCollect(ThreadDumpConfiguration configuration, File tmpDir) {
        boolean result = true;
        if (configuration.getCount() > 1) {
            for(short i=0;i<configuration.getCount();i++) {
                result &= createDump(tmpDir, Optional.of(i));
                result &= sleep(configuration.getInterval());
            }
        } else {
            result = createDump(tmpDir, Optional.empty());
        }

        if (result)
            getLog().info("Collection of " + getContentName() + " was successfully accomplished");
        else
            getLog().info("Collection of " + getContentName() + " has not accomplished successfully");

        return result;
    }

    /**
     * Executes interval between collected dumps
     *
     * @param interval amount of time to sleep (millis)
     *
     * @return result
     */
    private boolean sleep(long interval) {
        try {
            getLog().debug("Sleeping for {} millis", interval);
            Thread.sleep(interval);
        } catch (InterruptedException e) {
            getLog().error("Error while executing interval on thread dump collection");
            getLog().debug("Cause: {}", e);
            return false;
        }
        return true;
    }

    /**
     * Produces thread dump and saves it to the corresponding file
     *
     * @param id an ordered id of the collected dump
     *
     * @return {@link StringBuilderWrapper}
     */
    private boolean createDump(File tmpDir, Optional<Short> id) {
        try {
            getLog().debug("Producing thread dump {}", id.isPresent() ? id.get() : 1);
            StringBuilderWrapper td = new StringBuilderWrapper(threadDumper.dumpThreads());
            Files.write(td, getOutputFile(tmpDir, id), Charsets.UTF_8);
        } catch (IOException e) {
            getLog().error("Creating thread dump has failed: " + e.getMessage());
            getLog().debug("Cause: {}", e);
            return false;
        }
        return true;
    }

    /**
     * @return The filename to be used
     */
    @Override
    protected String getFileName() {
        return getContentName() + ".tdump";
    }

    /**
     * @param id the ordered id of artifact
     *
     * @return The filename to be used
     */
    protected String getFileName(Optional<Short> id) {
        return !id.isPresent() ? getFileName() : getContextFileName(id);
    }

    /**
     * @param id the ordered id of artifact
     *
     * @return file name
     */
    protected String getContextFileName(Optional<Short> id) {
        return !id.isPresent() ?
                getFileName()
                :
                super.getContentName() + "-" + Integer.toString(id.get() + 1) +
                        FILE_EXTENSION;
    }

    /**
     * Produces content specific output {@link File}
     *
     * @param tmpDir output dir
     * @param id the ordered id of artifact
     *
     * @return output {@link File}
     */
    protected File getOutputFile(File tmpDir, Optional<Short> id) {
        return new File(tmpDir.getPath() + File.separator + getFileName(id));
    }

    /**
     * Makes sure configuration is valid
     *
     * @param configuration configuration to check
     * @throws BundleConfigurationException if configuration is invalid
     */
    @Override
    protected void doEnsureConfiguration(ThreadDumpConfiguration configuration)
            throws BundleConfigurationException {
        ensureThreadDumpConfig(
                configuration.getCount(), configuration.getInterval()
        );
    }

    /**
     * Makes sure ThreadDumpConfig is valid
     *
     * @param count dumps count
     * @param interval interval between dumps (millis)
     *
     * @throws BundleConfigurationException thrown when illegal configuration is found
     */
    private void ensureThreadDumpConfig(int count, long interval)
            throws BundleConfigurationException {
        if (count < 0)
            throw new BundleConfigurationException(
                    "ThreadDump configuration is illegal, " +
                            "amount of dumps (count) cannot be negative number"
            );
        if (interval < 0)
            throw new BundleConfigurationException(
                    "ThreadDump configuration is illegal, " +
                            "dumps interval cannot be negative number"
            );
    }
}
