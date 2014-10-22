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

package org.artifactory.build;

import org.artifactory.api.build.BuildService;
import org.artifactory.api.build.ImportableExportableBuild;
import org.artifactory.fs.FileInfo;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.Lock;
import org.artifactory.spring.ReloadableBean;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildFileBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * The system-internal interface of the build service
 *
 * @author Noam Y. Tenne
 */
public interface InternalBuildService extends ReloadableBean, BuildService {

    /**
     * Name of folder to temporarily store previous build backups during an incremental
     */
    String BACKUP_BUILDS_FOLDER = "builds.previous";

    /**
     * Returns a map of build artifact/dependency and it's matching FileInfo
     *
     * @param build          The searched build (searching within it's artifacts/dependencies)
     * @param strictMatching When false, fallback to searching by checksums in case the bean wasn't found by build.name and build.number properties
     * @param artifacts      Whether to search for artifacts or dependencies
     */
    Map<BuildFileBean, FileInfo> getBuildBeansInfo(Build build, boolean strictMatching, boolean artifacts);

    /**
     * Imports an exportable build info into the database. This is an internal method and should be used to import a
     * single build within a transaction.
     *
     * @param settings Import settings
     * @param build    The build to import
     */
    @Lock
    void importBuild(ImportSettings settings, ImportableExportableBuild build) throws Exception;

    /**
     * Renames the JSON content within a build
     *
     * @param buildRun Build to rename
     * @param to       Replacement build name
     */
    @Lock
    void renameBuild(BuildRun buildRun, String to);

    /**
     * Returns latest build by name and status (which can be {@link BuildService.LATEST_BUILD} or a status value (e.g: "Released")
     *
     * @param buildName   the name of the build
     * @param buildStatus the desired status (which can be {@link BuildService.LATEST_BUILD} or a status value (e.g: "Released")
     * @return the build (if found)
     */
    @Nullable
    Build getLatestBuildByNameAndStatus(String buildName, String buildStatus);

    /**
     * Adds the given build configuration to Artifactory
     *
     * @param detailedBuildRun Build to add
     */
    void addBuild(@Nonnull DetailedBuildRun detailedBuildRun);

    /**
     * Persists the changes made to the given existing build configuration
     *
     * @param detailedBuildRun Existing build configuration
     */
    void updateBuild(@Nonnull DetailedBuildRun detailedBuildRun);
}