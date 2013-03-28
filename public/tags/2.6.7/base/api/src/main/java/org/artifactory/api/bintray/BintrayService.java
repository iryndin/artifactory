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

package org.artifactory.api.bintray;

import org.artifactory.api.bintray.exception.BintrayException;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.repo.Async;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.jfrog.build.api.Build;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * Provides different Bintray related business methods
 *
 * @author Shay Yaakov
 */
public interface BintrayService {

    String BINTRAY_REPO = "bintray.repo";
    String BINTRAY_PACKAGE = "bintray.package";
    String BINTRAY_VERSION = "bintray.version";
    String BINTRAY_PATH = "bintray.path";

    String PATH_CONTENT = "content";
    String PATH_REPOS = "repos";
    String PATH_PACKAGES = "packages";
    String PATH_USERS = "users";

    String VERSION_SHOW_FILES = "version/show/files";

    /**
     * Pushing synchronously single artifact to Bintray
     *
     * @param itemInfo      The item info to push, in case of a folder all it's contect will get pushed
     * @param bintrayParams The Bintray model which holds the properties where to push
     * @return Multi status holder containing all the logs during the process
     * @throws IOException In case of connection errors with Bintray
     */
    MultiStatusHolder pushArtifact(ItemInfo itemInfo, BintrayParams bintrayParams) throws IOException;

    /**
     * Pushing synchronously all build artifacts to Bintray
     *
     * @param build         The build of which to collect the artifacts to push
     * @param bintrayParams The Bintray model which holds the properties where to push
     * @return Multi status holder containing all the logs during the process
     * @throws IOException In case of connection errors with Bintray
     */
    MultiStatusHolder pushBuild(Build build, BintrayParams bintrayParams) throws IOException;

    /**
     * Pushing asynchronously all build artifacts to Bintray
     *
     * @param build         The build of which to collect the artifacts to push
     * @param bintrayParams The Bintray model which holds the properties where to push
     */
    @Async
    void executeAsyncPushBuild(Build build, BintrayParams bintrayParams);

    /**
     * Generates Bintray properties model from the metadata attached to a certain repo path
     *
     * @param repoPath The repo path to search attached metadata from
     * @return The bintray model constructed from the metadata, empty model in case no metadata exists
     */
    @Nonnull
    BintrayParams createParamsFromProperties(RepoPath repoPath);

    /**
     * Saves the given bintray model parameters as metadata properties on the given repo path
     *
     * @param repoPath      The repo path to attach metadata on
     * @param bintrayParams The bintray model to attach as metadata
     */
    void savePropertiesOnRepoPath(RepoPath repoPath, BintrayParams bintrayParams);

    /**
     * Get available repositories from Bintray
     * The list will contain repositories which the logged in user has permissions to deploy to
     *
     * @throws IOException      In case of connection errors with Bintray
     * @throws BintrayException In case we received any response other than 200 OK
     */
    List<Repo> getReposToDeploy() throws IOException, BintrayException;

    /**
     * Get available packages of specific repository from Bintray
     * The list will contain packages which the logged in user has permissions to deploy to
     *
     * @param repoKey The repository key to search packages under
     * @throws IOException      In case of connection errors with Bintray
     * @throws BintrayException In case we received any response other than 200 OK
     */
    List<String> getPackagesToDeploy(String repoKey) throws IOException, BintrayException;

    /**
     * Get available package versions of specific repository and package from Bintray
     *
     * @param repoKey   The repository key to search packages under
     * @param packageId The package name to search for versions
     * @throws IOException      In case of connection errors with Bintray
     * @throws BintrayException In case we received any response other than 200 OK
     */
    List<String> getVersions(String repoKey, String packageId) throws IOException, BintrayException;

    /**
     * Get the version URL in Bintray of which the user can browse into
     *
     * @param bintrayParams The bintray model to extract the URL from
     */
    String getVersionFilesUrl(BintrayParams bintrayParams);

    /**
     * Get a Bintray user information
     *
     * @param username The username to search
     * @param apiKey   The apiKey which belongs to the given username
     * @throws IOException      In case of connection errors with Bintray
     * @throws BintrayException In case we received any response other than 200 OK
     */
    BintrayUser getBintrayUser(String username, String apiKey) throws IOException, BintrayException;

    /**
     * Validates that the user properly configured his Bintray credentials
     */
    boolean isUserHasBintrayAuth();

    /**
     * Checks if the given url is a Bintray url, if so it needs to be converted from the dl.bintray.com
     * host to the appropriate one (if using credentials than to the Bintray public URL bintray.com/repo/browse/:subject/:repo
     * otherwise using the Bintray get repository REST API URL (bintray.com/repos/:subject/:repo)
     */
    String getBintrayTestRepoUrl(String url);

    /**
     * Get the registation URL for Bintray including Artifactory specific source query param.
     * In case of Artifactory Pro, the license hash is also included with the query param value.
     */
    String getBintrayRegistrationUrl();
}
