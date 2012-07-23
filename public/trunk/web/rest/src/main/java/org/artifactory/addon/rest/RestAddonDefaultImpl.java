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

package org.artifactory.addon.rest;

import com.sun.jersey.multipart.FormDataMultiPart;
import org.artifactory.addon.license.LicenseStatus;
import org.artifactory.addon.plugin.ResponseCtx;
import org.artifactory.addon.replication.RemoteReplicationSettings;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.rest.artifact.ItemPermissions;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.rest.artifact.PromotionResult;
import org.artifactory.api.rest.replication.ReplicationRequest;
import org.artifactory.api.rest.search.result.ArtifactVersionsResult;
import org.artifactory.api.rest.search.result.LicensesSearchResult;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.list.KeyValueList;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.resource.artifact.legacy.DownloadResource;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.release.Promotion;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of the rest add-on
 *
 * @author Yoav Landman
 */
@Component
public class RestAddonDefaultImpl implements RestAddon {

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public MoveCopyResult copy(String path, String target, int dryRun, int suppressLayouts, int failFast)
            throws Exception {
        throw new MissingRestAddonException();
    }

    @Override
    public MoveCopyResult move(String path, String target, int dryRun, int suppressLayouts, int failFast)
            throws Exception {
        throw new MissingRestAddonException();
    }

    @Override
    public Response download(String path, DownloadResource.Content content, int mark,
            HttpServletResponse response) throws Exception {
        throw new MissingRestAddonException();
    }

    @Override
    public Set<String> searchArtifactsByPattern(String pattern) {
        throw new MissingRestAddonException();
    }

    @Override
    public MoveCopyResult moveOrCopyBuildItems(boolean move, String buildName, String buildNumber, String started,
            String to, int arts, int deps, StringList scopes, KeyValueList properties, int dry) {
        throw new MissingRestAddonException();
    }

    @Override
    public PromotionResult promoteBuild(String buildName, String buildNumber, Promotion promotion) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response replicate(RemoteReplicationSettings remoteReplicationSettings) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response replicate(RepoPath repoPath, ReplicationRequest replicationRequest) {
        throw new MissingRestAddonException();
    }

    @Override
    public void renameBuilds(String from, String to) {
        throw new MissingRestAddonException();
    }

    @Override
    public void renameBuildsAsync(String from, String to) {
        throw new MissingRestAddonException();
    }

    @Override
    public void deleteBuilds(HttpServletResponse response, String buildName, StringList buildNumbers, int artifacts) {
        throw new MissingRestAddonException();
    }

    @Override
    public void discardOldBuilds(String name, BuildRetention discard, MultiStatusHolder multiStatusHolder) {
        // nop
    }

    @Override
    public ItemInfo getLastModified(String pathToSearch) {
        throw new MissingRestAddonException();
    }

    @Override
    public LicensesSearchResult findLicensesInRepos(LicenseStatus status, StringList repos, String servletContextUrl) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response deleteRepository(String repoKey) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response getRepositoryConfiguration(String repoKey, List<MediaType> mediaTypes) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response createOrReplaceRepository(String repoKey, Map repositoryConfig, List<MediaType> mediaTypes,
            int position) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response updateRepository(String repoKey, Map repositoryConfig, List<MediaType> mediaTypes) {
        throw new MissingRestAddonException();
    }

    @Override
    public Set<RepoPath> searchArtifactsByChecksum(String md5Checksum, String sha1Checksum, StringList reposToSearch) {
        throw new MissingRestAddonException();
    }

    @Override
    @Nonnull
    public Response searchBadChecksumArtifacts(String type, StringList reposToSearch,
            HttpServletRequest request) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response savePathProperties(String path, String recursive, KeyValueList properties) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response deletePathProperties(String path, String recursive, StringList properties) {
        throw new MissingRestAddonException();
    }

    @Override
    public ResponseCtx runPluginExecution(String executionName, Map params, boolean async) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response getStagingStrategy(String strategyName, String buildName, Map params) {
        throw new MissingRestAddonException();
    }

    @Override
    public ItemPermissions getItemPermissions(HttpServletRequest request, String path) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response searchDependencyBuilds(HttpServletRequest request, String sha1) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response calculateYumMetadata(String repoKey, int async) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response getSecurityEntities(HttpServletRequest request, String entityType) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response getSecurityEntity(String entityType, String entityKey) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response deleteSecurityEntity(String entityType, String entityKey) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response createOrReplaceSecurityEntity(String entityType, String entityKey, HttpServletRequest request) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response updateSecurityEntity(String entityType, String entityKey, HttpServletRequest request) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response getReplicationStatus(RepoPath repoPath) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleNuGetTestRequest(@Nonnull String repoKey) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleNuGetMetadataDescriptorRequest(@Nonnull String repoKey) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleNuGetQueryRequest(@Nonnull HttpServletRequest request, @Nonnull String repoKey,
            @Nullable String actionParam) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleNuGetPackagesRequest(@Nonnull HttpServletRequest request, @Nonnull String repoKey) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleFindPackagesByIdRequest(@Nonnull HttpServletRequest request, @Nonnull String repoKey) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleNuGetDownloadRequest(@Nonnull HttpServletResponse response, @Nonnull String repoKey,
            @Nonnull String packageId, @Nonnull String packageVersion) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response runMavenIndexer(List<String> reposToIndex, int force) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleNuGetPublishRequest(@Nonnull String repoKey, @Nonnull FormDataMultiPart content) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response getUserPluginInfo(@Nullable String pluginType) {
        throw new MissingRestAddonException();
    }

    @Override
    public BuildPatternArtifacts getBuildPatternArtifacts(
            @Nonnull BuildPatternArtifactsRequest buildPatternArtifactsRequest, String servletContextUrl) {
        throw new MissingRestAddonException();
    }

    @Override
    public ResponseCtx promote(String promotionName, String buildName, String buildNumber, Map params) {
        throw new MissingRestAddonException();
    }

    @Override
    public ArtifactVersionsResult getArtifactVersions(String groupId, String artifactId, @Nullable String version,
            @Nullable StringList reposToSearch, boolean remote) {
        throw new MissingRestAddonException();
    }

    @Override
    public void writeStreamingFileList(HttpServletResponse response, String requestUrl, String path, int deep,
            int depth, int listFolders, int mdTimestamps, int includeRootPath) {
        throw new MissingRestAddonException();
    }

    @Override
    public Response handleNuGetDeleteRequest(@Nonnull String repoKey, @Nonnull String packageId,
            @Nonnull String packageVersion) {
        throw new MissingRestAddonException();
    }
}
