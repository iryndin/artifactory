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

package org.artifactory.build;

import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.build.ImportableExportableBuild;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.md.Properties;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.spring.ReloadableBean;
import org.jfrog.build.api.Build;

import javax.jcr.Node;
import java.util.Map;
import java.util.Set;

/**
 * The system-internal interface of the build service
 *
 * @author Noam Y. Tenne
 */
public interface InternalBuildService extends ReloadableBean, BuildService {

    /**
     * Returns the build object of the node
     *
     * @param buildNode Build node
     * @return Build object if data was found on the node. Null if not found, or exception occurred
     */
    Build getBuild(Node buildNode);

    /**
     * Returns the best matching file info object from the given results and criteria
     *
     * @param searchResults    File bean search results
     * @param resultProperties Search result property map
     * @param buildName        Build name to search for
     * @param buildNumber      Build number to search for
     * @return The file info of a result that best matches the given criteria
     */
    FileInfo getBestMatchingResult(Set<RepoPath> searchResults, Map<RepoPath, Properties> resultProperties,
            String buildName, long buildNumber);

    /**
     * Locates builds that are named as the given name within a transaction
     *
     * @param buildName Name of builds to locate
     * @return Set of builds with the given name
     */
    @Lock(transactional = true)
    Set<BasicBuildInfo> transactionalSearchBuildsByName(String buildName);

    /**
     * Imports an exportable build info into the database. This is an internal method and should be used to import a
     * single build within a transaction.
     *
     * @param settings Import settings
     * @param build    The build to import
     */
    @Lock(transactional = true)
    void importBuild(ImportSettings settings, ImportableExportableBuild build) throws Exception;

    /**
     * Creates (if needed) and returns the builds root node
     *
     * @return Builds root node
     */
    @Lock(transactional = true)
    Node getOrCreateBuildsRootNode();
}