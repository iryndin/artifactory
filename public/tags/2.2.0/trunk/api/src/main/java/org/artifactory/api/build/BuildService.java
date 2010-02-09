/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.api.build;

import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.repo.Lock;
import org.artifactory.build.api.Build;

import java.util.List;

/**
 * Build service main interface
 *
 * @author Noam Y. Tenne
 */
public interface BuildService extends ImportableExportable {

    public static final String BUILD_CHECKSUM_PREFIX_MD5 = "{MD5}";
    public static final String BUILD_CHECKSUM_PREFIX_SHA1 = "{SHA1}";

    /**
     * Adds the given build to the DB
     *
     * @param build Build to add
     */
    @Lock(transactional = true)
    void addBuild(Build build);

    /**
     * Returns the JSON string of the given build object
     *
     * @param build Build to parse as JSON
     * @return Build JSON if parsing succeeded. Empty string if not
     */
    String getBuildAsJson(Build build);

    /**
     * Removes the given build
     *
     * @param build Build to remove
     */
    void deleteBuild(Build build);

    /**
     * Returns the latest build for the given name and number
     *
     * @param buildName   Name of build to locate
     * @param buildNumber Number of build to locate
     * @return Latest build if found. Null if not
     */
    @Lock(transactional = true)
    Build getLatestBuildByNameAndNumber(String buildName, long buildNumber);

    /**
     * Locates builds that are named as the given name
     *
     * @param buildName Name of builds to locate
     * @return List of builds with the given name
     */
    List<Build> searchBuildsByName(String buildName);

    @Lock(transactional = true)
    void exportTo(ExportSettings settings);

    void importFrom(ImportSettings settings);
}