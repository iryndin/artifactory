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

package org.artifactory.addon.rest;

import org.artifactory.addon.Addon;
import org.artifactory.addon.license.LicenseStatus;
import org.artifactory.addon.plugin.ResponseCtx;
import org.artifactory.addon.replication.RemoteReplicationSettings;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.repo.Async;
import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.exception.BlackedOutException;
import org.artifactory.api.rest.artifact.FileList;
import org.artifactory.api.rest.artifact.ItemPermissions;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.api.rest.search.result.LicensesSearchResult;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.list.KeyValueList;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.resource.artifact.DownloadResource;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.release.Promotion;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * An interface that holds all the REST related operations that are available only as part of Artifactory's Add-ons.
 *
 * @author Tomer Cohen
 */
public interface RestAddon extends Addon {
    /**
     * Copy an artifact from one path to another.
     *
     * @param path            The source path of the artifact.
     * @param target          The target repository where to copy/move the Artifact to.
     * @param dryRun          A flag to indicate whether to perform a dry run first before performing the actual
     *                        action.
     * @param suppressLayouts Indicates whether path translation across different layouts should be suppressed.
     * @param failFast        Indicates whether the operation should fail upon encountering an error.
     * @return A JSON object of all the messages and errors that occurred during the action.
     * @throws Exception If an error occurred during the dry run or the actual action an exception is thrown.
     */
    MoveCopyResult copy(String path, String target, int dryRun, int suppressLayouts, int failFast) throws Exception;

    /**
     * Move an artifact from one path to another.
     *
     * @param path            The source path of the artifact.
     * @param target          The target repository where to copy/move the Artifact to.
     * @param dryRun          A flag to indicate whether to perform a dry run first before performing the actual
     *                        action.
     * @param suppressLayouts Indicates whether path translation across different layouts should be suppressed.
     * @param failFast        Indicates whether the operation should fail upon encountering an error.
     * @return A JSON object of all the messages and errors that occurred during the action.
     * @throws Exception If an error occurred during the dry run or the actual action an exception is thrown.
     */
    MoveCopyResult move(String path, String target, int dryRun, int suppressLayouts, int failFast) throws Exception;

    Response download(String path, DownloadResource.Content content, int mark,
            HttpServletResponse response) throws Exception;

    /**
     * Search for artifacts within a repository matching a given pattern.<br> The pattern should be like
     * repo-key:this/is/a/pattern
     *
     * @param pattern Pattern to search for
     * @return Set of matching artifact paths relative to the repo
     */
    Set<String> searchArtifactsByPattern(String pattern) throws ExecutionException, TimeoutException,
            InterruptedException;

    /**
     * Moves or copies build artifacts and\or dependencies
     *
     * @param move        True if the items should be moved. False if they should be copied
     * @param buildName   Name of build to target
     * @param buildNumber Number of build to target
     * @param started     Start date of build to target (can be null)
     * @param to          Key of target repository to move to
     * @param arts        Zero if to exclude artifacts from the action take. One to include
     * @param deps        Zero if to exclude dependencies from the action take. One int to include
     * @param scopes      Scopes of dependencies to copy (agnostic if null or empty)
     * @param properties  Properties to tag the moved or copied artifacts.
     * @param dry         Zero if to apply the selected action. One to simulate  @return Result of action
     * @deprecated Use {@link org.artifactory.addon.rest.RestAddon#promoteBuild} instead
     */
    @Deprecated
    MoveCopyResult moveOrCopyBuildItems(boolean move, String buildName, String buildNumber, String started,
            String to, int arts, int deps, StringList scopes, KeyValueList properties, int dry) throws ParseException;

    /**
     * Promotes a build
     *
     * @param buildName   Name of build to promote
     * @param buildNumber Number of build to promote
     * @param promotion   Promotion settings
     * @return Promotion result
     */
    PromotionResult promoteBuild(String buildName, String buildNumber, Promotion promotion) throws ParseException;

    /**
     * Returns a list of files under the given folder path
     *
     * @param uri          Request URI as sent by the user
     * @param path         Path to search under
     * @param deep         Zero if the scanning should be shallow. One for deep
     * @param depth        Number of hierarchy levels to iterate down
     * @param listFolders  Zero if folders should be excluded. One if they should be included
     * @param mdTimestamps Zero if metadata last modified timestamps should not be included in the list. One if they
     *                     should
     */
    @Lock(transactional = true)
    FileList getFileList(String uri, String path, int deep, int depth, int listFolders, int mdTimestamps)
            throws BlackedOutException;

    /**
     * Locally replicates the given remote path
     *
     * @param remoteReplicationSettings Settings
     * @return Response
     */
    Response replicate(RemoteReplicationSettings remoteReplicationSettings) throws IOException;

    /**
     * Renames structure, content and properties of build info objects. The actual rename is done asynchronously.
     *
     * @param from Name to replace
     * @param to   Replacement build name
     */
    void renameBuilds(String from, String to);

    /**
     * Renames structure, content and properties of build info objects in an asynchronous manner.
     *
     * @param from Name to replace
     * @param to   Replacement build name
     */
    @Async
    void renameBuildsAsync(String from, String to);

    /**
     * Deletes the build with given name and number
     *
     * @param response
     * @param buildName    Name of build to delete
     * @param buildNumbers Numbers of builds to delete
     * @param artifacts    1 if build artifacts should be deleted
     */
    void deleteBuilds(HttpServletResponse response, String buildName, StringList buildNumbers, int artifacts)
            throws IOException;

    /**
     * Discard old builds as according to count or date.
     *
     * @param name              Build name
     * @param discard           The discard object that holds a count or date.
     * @param multiStatusHolder Status holder
     */
    void discardOldBuilds(String name, BuildRetention discard, MultiStatusHolder multiStatusHolder);

    /**
     * Returns the latest modified item of the given file or folder (recursively)
     *
     * @param pathToSearch Repo path to search in
     * @return Latest modified item
     */
    org.artifactory.fs.ItemInfo getLastModified(String pathToSearch);

    /**
     * Find licenses in repositories, if empty, a scan of all repositories will take place.
     *
     * @param status            A container to hold the different license statuses.
     * @param repos             The repositories to scan, if empty, all repositories will be scanned.
     * @param servletContextUrl The contextUrl of the server.
     * @return The search results.
     */
    LicensesSearchResult findLicensesInRepos(LicenseStatus status, StringList repos, String servletContextUrl);

    /**
     * Delete a repository via REST.
     *
     * @param repoKey The repokey that is associated to the repository that is wanted for deletion.
     */
    Response deleteRepository(String repoKey);

    /**
     * Get Repository configuration according to the repository key in conjunction with the media type to enforce a
     * certain type of repository configuration.
     *
     * @param repoKey    The repokey of the repository.
     * @param mediaTypes The acceptable media types for this request
     * @return The response with the configuration embedded in it.
     */
    Response getRepositoryConfiguration(String repoKey, List<MediaType> mediaTypes);

    /**
     * Create or replace an existing repository via REST.
     *
     * @param repoKey
     * @param repositoryConfig Map of attributes.
     * @param mediaTypes       The mediatypes of which are applicable. {@link org.artifactory.api.rest.constant.RepositoriesRestConstants}
     * @param position         The position in the map that the newly created repository will be placed
     */
    Response createOrReplaceRepository(String repoKey, Map repositoryConfig, List<MediaType> mediaTypes, int position);

    /**
     * Update an existing repository via REST.
     *
     * @param repoKey          The repokey of the repository to be updated.
     * @param repositoryConfig The repository config of what is to be updated.
     * @param mediaTypes       The acceptable media types for this REST command.
     * @return The response for this command.
     */
    Response updateRepository(String repoKey, Map repositoryConfig, List<MediaType> mediaTypes);

    /**
     * Search for artifacts by their checksums
     *
     * @param md5Checksum   MD5 checksum value
     * @param sha1Checksum  SHA1 checksum value
     * @param reposToSearch Specific repositories to search in
     * @return Set of repo paths matching the given checksum
     */
    Set<RepoPath> searchArtifactsByChecksum(String md5Checksum, String sha1Checksum, StringList reposToSearch);

    /**
     * Search the repository(ies) for artifacts which have a mismatch between their server generated checksums and their
     * client generated checksums, this can result from an inequality or if one is missing.
     *
     * @param type          the type of checksum to search for (md5, sha1).
     * @param reposToSearch The list of repositories to search for the corrupt artifacts, if empty all repositories will
     *                      be searched
     * @param request       The request
     * @return The response object with the result as its entity.
     */
    @Nonnull
    Response searchBadChecksumArtifacts(String type, StringList reposToSearch,
            HttpServletRequest request);

    /**
     * Save properties on a certain path (which must be a valid {@link org.artifactory.repo.RepoPath})
     *
     * @param path       The path on which to set the properties
     * @param recursive  Whether the property attachment should be recursive.
     * @param properties The properties to attach as a list.
     * @return The response of the operation
     */
    Response savePathProperties(String path, String recursive, KeyValueList properties);

    Response deletePathProperties(String path, String recursive, StringList properties);

    ResponseCtx runPluginExecution(String executionName, Map params, boolean async);

    ItemPermissions getItemPermissions(HttpServletRequest request, String path);

    Response searchDependencyBuilds(HttpServletRequest request, String sha1) throws UnsupportedEncodingException;
}
