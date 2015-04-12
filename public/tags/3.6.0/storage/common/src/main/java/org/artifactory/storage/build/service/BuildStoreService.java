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

package org.artifactory.storage.build.service;

import org.artifactory.api.build.ImportableExportableBuild;
import org.artifactory.build.BuildRun;
import org.artifactory.checksum.ChecksumType;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.release.PromotionStatus;

import java.util.List;
import java.util.Set;

/**
 * Date: 11/14/12
 * Time: 12:40 PM
 *
 * @author freds
 */
public interface BuildStoreService {
    void addBuild(String buildJson);

    void addBuild(Build build);

    /**
     * Locates and fills in missing checksums of all the modules of a build
     *
     * @param build the build with modules to fill checksums
     */
    void populateMissingChecksums(Build build);

    BuildRun getBuildRun(String buildName, String buildNumber, String buildStarted);

    String getBuildAsJson(BuildRun buildRun);

    Build getLatestBuild(String buildName, String buildNumber);

    Set<BuildRun> findBuildsByName(String buildName);

    Set<BuildRun> findBuildsByNameAndNumber(String buildName, String buildNumber);

    List<String> getAllBuildNames();

    ImportableExportableBuild getExportableBuild(BuildRun buildRun);

    void deleteAllBuilds(String buildName);

    void deleteBuild(BuildRun buildRun);

    void deleteBuild(String buildName, String buildNumber, String buildStarted);

    void deleteAllBuilds();

    Set<BuildRun> getLatestBuildsByName();

    Build getBuildJson(BuildRun buildRun);

    void renameBuild(BuildRun originalBuildRun, Build renamedBuild, String currentUser);

    void addPromotionStatus(Build build, PromotionStatus promotion, String currentUser);

    Set<BuildRun> findBuildsForChecksum(BuildSearchCriteria criteria, ChecksumType type, String checksum);
}
