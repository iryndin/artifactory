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

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.artifactory.support.config.CollectConfiguration;
import org.artifactory.support.core.exceptions.ContentCollectionExceptionException;
import org.artifactory.support.utils.StringBuilderWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Provides content generic services where all
 * items in the context requires same treatment
 *
 * @author Michael Pasternak
 */
public abstract class AbstractGenericContentCollector<T extends CollectConfiguration> extends AbstractContentCollector<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractGenericContentCollector.class);

    /**
     * @param contentName a name for specific sub-folder
     */
    protected AbstractGenericContentCollector(String contentName) {
        super(contentName);
    }

    /**
     * Collects security info
     *
     * @param configuration {@link org.artifactory.support.config.CollectConfiguration}
     * @param tmpDir output dir
     *
     * @return result
     */
    protected final boolean doCollect(T configuration, File tmpDir) {
        if (configuration.isEnabled()) {
            try {
                StringBuilderWrapper content = doProduceContent(configuration);
                Files.write(content, getOutputFile(tmpDir), Charsets.UTF_8);
                getLog().info("Collection of " + getContentName() + " was successfully accomplished");
                return true;
            } catch (IOException | InstantiationException |
                    IllegalAccessException | ContentCollectionExceptionException e) {
                getLog().error("Collecting " + getContentName() + " has failed, - " + e.getMessage());
                getLog().debug("Cause: {}", e);
            }
        } else {
            getLog().debug("Content collection of " + getContentName() +" is disabled");
        }
        return false;
    }

    /**
     * @throws {@link ContentCollectionExceptionException} when no
     * content is available
     */
    protected StringBuilderWrapper failure() {
        throw new ContentCollectionExceptionException(
                "No content was collected for " + getContentName()
        );
    }

    /**
     * @throws {@link ContentCollectionExceptionException} when no
     * content is available
     */
    protected StringBuilderWrapper failure(Exception e) {
        throw new ContentCollectionExceptionException(
                "No content was collected for " + getContentName() +
                " - " + e.getMessage()
        );
    }

    /**
     * Produces content and returns it wrapped with {@link StringBuilderWrapper}
     *
     * @return {@link StringBuilderWrapper}
     *
     * @throws IOException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    abstract protected StringBuilderWrapper doProduceContent(T configuration)
            throws IOException, InstantiationException, IllegalAccessException;
}
