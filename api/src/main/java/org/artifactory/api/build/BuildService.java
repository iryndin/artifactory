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
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.jcr.JcrTypes;
import org.jfrog.build.api.Build;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Build service main interface
 *
 * @author Noam Y. Tenne
 */
public interface BuildService extends ImportableExportable {

    public static final String BUILD_CHECKSUM_PREFIX_MD5 = "{MD5}";
    public static final String BUILD_CHECKSUM_PREFIX_SHA1 = "{SHA1}";
    public static final String PROP_BUILD_LATEST_NUMBER = JcrTypes.PROP_BUILD_LATEST_NUMBER;
    public static final String PROP_BUILD_LATEST_START_TIME = JcrTypes.PROP_BUILD_LATEST_START_TIME;

    /**
     * Adds the given build to the DB
     *
     * @param build Build to add
     */
    @Lock(transactional = true)
    void addBuild(Build build);

    /**
     * Returns the JSON string of the given build details
     *
     * @param buildName    Build name
     * @param buildNumber  Build number
     * @param buildStarted Build started
     * @return Build JSON if parsing succeeded. Empty string if not
     */
    String getBuildAsJson(String buildName, String buildNumber, String buildStarted);

    /**
     * Removes all the builds of the given name
     *
     * @param buildName Name of builds to remove
     */
    void deleteBuild(String buildName);

    /**
     * Removes the build of the given details
     *
     * @param basicBuildInfo
     */
    void deleteBuild(BasicBuildInfo basicBuildInfo);

    /**
     * Returns the build of the given details
     *
     * @param buildName    Build name
     * @param buildNumber  Build number
     * @param buildStarted Build started
     * @return Build if found. Null if not
     */
    @Lock(transactional = true)
    Build getBuild(String buildName, String buildNumber, String buildStarted);

    /**
     * Returns the latest build for the given name and number
     *
     * @param buildName   Name of build to locate
     * @param buildNumber Number of build to locate
     * @return Latest build if found. Null if not
     */
    @Lock(transactional = true)
    Build getLatestBuildByNameAndNumber(String buildName, String buildNumber);

    /**
     * Locates builds that are named as the given name
     *
     * @param buildName Name of builds to locate
     * @return Set of builds with the given name
     */
    Set<BasicBuildInfo> searchBuildsByName(String buildName);

    /**
     * Locates builds that are named and numbered as the given name and number
     *
     * @param buildName   Name of builds to locate
     * @param buildNumber Number of builds to locate
     * @return Set of builds with the given name
     */
    Set<BasicBuildInfo> searchBuildsByNameAndNumber(String buildName, String buildNumber);

    @Lock(transactional = true)
    void exportTo(ExportSettings settings);

    Set<String> findScopes(Build build);

    void importFrom(ImportSettings settings);

    /**
     * Returns the CI server URL of the given build
     *
     * @param basicBuildInfo Basic build info to extract URL of
     * @return URL if exists, form of blank string if not
     */
    String getBuildCiServerUrl(BasicBuildInfo basicBuildInfo) throws IOException;

    /**
     * Moves or copies build artifacts and\or dependencies
     *
     * @param move           True if the items should be moved. False if they should be copied
     * @param basicBuildInfo Basic info of the selected build
     * @param targetRepoKey  Key of target repository to move to
     * @param artifacts      True if the build artifacts should be moved\copied
     * @param dependencies   True if the build dependencies should be moved\copied
     * @param scopes         Scopes of dependencies to copy (agnostic if null or empty)
     * @param dryRun         True if the action should run dry (simulate)
     * @return Result of action
     */
    @Lock(transactional = true)
    MoveCopyResult moveOrCopyBuildItems(boolean move, BasicBuildInfo basicBuildInfo, String targetRepoKey,
            boolean artifacts, boolean dependencies, List<String> scopes, boolean dryRun);

    /**
     * Renames the JCR structure and content of build info objects
     *
     * @param from Name to replace
     * @param to   Replacement build name
     */
    void renameBuilds(String from, String to);
}