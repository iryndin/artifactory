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

package org.artifactory.addon;

import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.Async;
import org.artifactory.api.rest.artifact.FileList;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.resource.artifact.DownloadResource;
import org.artifactory.rest.resource.artifact.SyncResource;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * An interface that holds all the REST related operations that are available only as part of Artifactory's Add-ons.
 *
 * @author Tomer Cohen
 */
public interface RestAddon extends AddonFactory {
    /**
     * Copy an artifact from one path to another.
     *
     * @param path   The source path of the artifact.
     * @param target The target repository where to copy/move the Artifact to.
     * @param dryRun A flag to indicate whether to perform a dry run first before performing the actual action.
     * @return A JSON object of all the messages and errors that occurred during the action.
     * @throws Exception If an error occurred during the dry run or the actual action an exception is thrown.
     */
    MoveCopyResult copy(String path, String target, int dryRun) throws Exception;

    /**
     * Move an artifact from one path to another.
     *
     * @param path   The source path of the artifact.
     * @param target The target repository where to copy/move the Artifact to.
     * @param dryRun A flag to indicate whether to perform a dry run first before performing the actual action.
     * @return A JSON object of all the messages and errors that occurred during the action.
     * @throws Exception If an error occurred during the dry run or the actual action an exception is thrown.
     */
    MoveCopyResult move(String path, String target, int dryRun) throws Exception;

    Response download(String path, DownloadResource.Content content, int mark,
            HttpServletResponse response) throws Exception;

    /**
     * Search for artifacts within a repository matching a given pattern.<br>
     * The pattern should be like repo-key:this/is/a/pattern
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
     * @param dry         Zero if to apply the selected action. One to simulate
     * @return Result of action
     */
    MoveCopyResult moveOrCopyBuildItems(boolean move, String buildName, String buildNumber, String started,
            String to, int arts, int deps, StringList scopes, int dry) throws ParseException;

    /**
     * Returns a list of files under the given folder path
     *
     * @param uri  Request URI as sent by the user
     * @param path Path to search under
     * @param deep Zero if the scanning should be shallow. One for deep
     * @return File list object
     */
    FileList getFileList(String uri, String path, int deep);

    /**
     * Locally replicates the given remote path
     *
     * @param path           The path of the remote folder to replicate
     * @param progress       One to show transfer progress, Zero or other to show normal transfer completion message
     * @param mark           Every how many bytes to print a progress mark (when using progress tracking policy)
     * @param deleteExisting One to delete existing files which do not exist in the remote source, Zero to keep
     * @param overwrite      Null for default artifactory behavior, never for never replacing an existing file and force
     *                       for replacing existing files (only if the local file is older than the target)
     * @param httpResponse   Response to return once complete
     * @return Response
     */
    Response replicate(String path, int progress, int mark, int deleteExisting, SyncResource.Overwrite overwrite,
            HttpServletResponse httpResponse) throws IOException;

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
     * Returns the latest modified item of the given file or folder (recursively)
     *
     * @param pathToSearch Repo path to search in
     * @return Latest modified item
     */
    ItemInfo getLastModified(String pathToSearch);
}