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

package org.artifactory.support.core.bundle;

import org.artifactory.support.config.bundle.BundleConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Defines bundle generator behaviour
 *
 * @author Michael Pasternak
 */
public interface SupportBundleService {
    /**
     * Generates support bundle
     *
     * @param bundleConfiguration config to be used
     *
     * @return compressed archive/s
     */
    List<String> generate(BundleConfiguration bundleConfiguration);

    /**
     * Lists previously created bundles
     *
     * @return archive/s
     */
    List<String> list();

    /**
     * Deletes support bundles
     *
     * @param bundleName name of bundle to delete
     * @param async whether delete should be performed asynchronously
     *
     * @return result
     *
     * @throws FileNotFoundException
     */
    boolean delete(String bundleName, boolean async) throws FileNotFoundException;

    /**
     * Downloads support bundles
     *
     * @param bundleName
     * @return {@link InputStream} to support bundle
     *         (user responsibility is to close stream upon consumption)
     *
     * @throws FileNotFoundException
     */
    InputStream download(String bundleName) throws FileNotFoundException;

    /**
     * @return support bundle output directory
     */
    File getOutputDirectory();
}
