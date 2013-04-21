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

package org.artifactory.addon;

import org.artifactory.addon.plugin.ResponseCtx;
import org.artifactory.api.repo.Async;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.sapi.common.Lock;
import org.artifactory.sapi.fs.VfsFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Core NuGet functionality interface
 *
 * @author Noam Y. Tenne
 */
public interface NuGetAddon extends Addon {

    String API_KEY_HEADER = "X-NuGet-ApiKey";
    String API_KEY_SEPARATOR = ":";
    String REPO_KEY_PARAM = "repoKey";
    String PACKAGE_ID_PARAM = "packageId";
    String PACKAGE_VERSION_PARAM = "packageVersion";

    /**
     * Extracts the spec from the nupkg and save it as a binary property (along with ID, version and digest) for easier
     * availability when searching
     *
     * @param item         NuGet package
     * @param statusHolder Status holder for logging
     */
    @Async(transactional = true, delayUntilAfterCommit = true)
    void extractNuPkgInfo(VfsFile item, MutableStatusHolder statusHolder);

    /**
     * Requests an async nupkg latest update for the given repository and packaged ID
     *
     * @param repoKey   Repository key to act within
     * @param packageId ID of package group to update the latest version state for
     */
    void requestAsyncLatestNuPkgVersionUpdate(String repoKey, String packageId);

    /**
     * Returns the static metadata entity descriptor requested by the NuGet tools
     *
     * @return Metadata entity descriptor string
     */
    String getMetadataEntity();

    /**
     * Handles REST NuGet search requests
     *
     * @param request        Servlet request
     * @param repoKey        Key of repository to search in
     * @param subActionParam An action parameter in addition to the main one
     * @param output         Output stream to write the response to
     */
    void handleSearchRequest(@Nonnull HttpServletRequest request, @Nonnull String repoKey,
            @Nullable String subActionParam, OutputStream output) throws IOException;

    /**
     * Handles REST NuGet download requests
     *
     * @param response       Servlet response
     * @param repoKey        Key of storing repository
     * @param packageId      ID of package to locate
     * @param packageVersion Version of package to locate
     * @return Null if the result was written directly to the original response, a response object otherwise
     */
    @Nullable
    @Lock
    ResponseCtx handleDownloadRequest(@Nonnull HttpServletResponse response, @Nonnull String repoKey,
            @Nonnull String packageId, @Nonnull String packageVersion);

    /**
     * Handles REST NuGet Packages requests
     *
     * @param request Servlet request
     * @param repoKey Key of repository to search in
     * @param output  Output stream to write the response to
     */
    @Lock
    void handlePackagesRequest(@Nonnull HttpServletRequest request, @Nonnull String repoKey,
            @Nonnull OutputStream output) throws IOException;

    /**
     * Handles REST NuGet Package search by ID requests
     *
     * @param request Servlet request
     * @param repoKey Key of repository to search in
     * @param output  Output stream to write the response to
     */
    void handleFindPackagesByIdRequest(@Nonnull HttpServletRequest request, @Nonnull String repoKey,
            @Nonnull OutputStream output) throws IOException;


    /**
     * Handles REST NuGet Package update search requests
     *
     * @param request     Servlet request
     * @param repoKey     Key of repository to search in
     * @param actionParam An action parameter in addition to the main one
     * @param output      Output stream to write the response to
     */
    @Lock
    void handleGetUpdatesRequest(@Nonnull HttpServletRequest request, @Nonnull String repoKey,
            @Nullable String actionParam, @Nonnull OutputStream output) throws IOException;

    /**
     * Prepares a nupkg deployment repository path
     *
     * @param repoKey      Repository key
     * @param packageBytes Bytes of NuPkg
     * @return Deployment target repo path
     */
    @Nonnull
    RepoPath assembleDeploymentRepoPathFromNuPkgSpec(@Nonnull String repoKey, @Nonnull byte[] packageBytes)
            throws IOException;

    /**
     * Finds the repo path of a NuPkg based on it's ID and version
     *
     * @param repoKey        Key of repository to search in
     * @param packageId      ID of NuPkg to search for
     * @param packageVersion Version of NuPkg to search for
     * @return Repo path of NuPkg if found, null if not
     */
    @Nullable
    RepoPath findPackageInLocalOrCacheRepo(@Nonnull String repoKey, @Nonnull String packageId,
            @Nonnull String packageVersion);

    /**
     * Instantiate the remote repository instance
     *
     * @param repoService    Repo service
     * @param repoDescriptor Descriptor of repository to init
     * @param offlineMode    True if Artifactory is in offline mode
     * @param oldRemoteRepo  Old remote repo descriptor if the current one is an update
     * @return Initialized remote repository
     */
    @Nonnull
    RemoteRepo createRemoteRepo(InternalRepositoryService repoService, RemoteRepoDescriptor repoDescriptor,
            boolean offlineMode, RemoteRepo oldRemoteRepo);
}
