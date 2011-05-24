/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.md.Properties;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.release.Promotion;

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
     * In case a dependency contains a {@code null} scope, fill it with an unspecified scope that will be used for
     * filtering.
     */
    public static final String UNSPECIFIED_SCOPE = "unspecified";


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
     * @param buildName         Name of builds to remove
     * @param deleteArtifacts   True if build artifacts should be deleted
     * @param multiStatusHolder Status holder
     */
    void deleteBuild(String buildName, boolean deleteArtifacts, MultiStatusHolder multiStatusHolder);

    /**
     * Removes the build of the given details
     *
     * @param basicBuildInfo    Build info details
     * @param deleteArtifacts   True if build artifacts should be deleted
     * @param multiStatusHolder Status holder
     */
    void deleteBuild(BasicBuildInfo basicBuildInfo, boolean deleteArtifacts, MultiStatusHolder multiStatusHolder);

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

    /**
     * @return True if the build is not Ivy/Gradle/Maven.
     */
    boolean isGenericBuild(Build build);

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
     * @param properties     The properties to tag the copied or move artifacts on their <b>destination</b> path
     * @param dryRun         True if the action should run dry (simulate)
     * @return Result of action
     * @deprecated Use {@link org.artifactory.api.build.BuildService#promoteBuild} instead
     */
    @Deprecated
    @Lock(transactional = true)
    MoveCopyResult moveOrCopyBuildItems(boolean move, BasicBuildInfo basicBuildInfo, String targetRepoKey,
            boolean artifacts, boolean dependencies, List<String> scopes, Properties properties,
            boolean dryRun);

    /**
     * Promotes a build
     *
     * @param buildInfo Basic info of build to promote
     * @param promotion Promotion settings
     * @return Promotion result
     */
    @Lock(transactional = true)
    PromotionResult promoteBuild(BasicBuildInfo buildInfo, Promotion promotion);

    /**
     * Renames the JCR structure and content of build info objects
     *
     * @param from Name to replace
     * @param to   Replacement build name
     */
    void renameBuilds(String from, String to);

    /**
     * Updates the content (jcr data) of the given build. Please note that this method does nothing apart from updating
     * the JSON data. Other properties and data surrounding the build nodes (apart from mandatory) will not change
     *
     * @param build Updated content
     */
    @Lock(transactional = true)
    void updateBuild(Build build);

    /**
     * Returns the build's latest release status
     *
     * @param buildName    Build name
     * @param buildNumber  Build number
     * @param buildStarted Build started
     * @return Last release status if exists. Null if not
     */
    @Lock(transactional = true)
    String getBuildLatestReleaseStatus(String buildName, String buildNumber, String buildStarted);
}
