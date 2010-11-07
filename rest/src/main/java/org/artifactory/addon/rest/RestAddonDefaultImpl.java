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

package org.artifactory.addon.rest;

import org.artifactory.addon.license.LicenseStatus;
import org.artifactory.addon.plugin.ResponseCtx;
import org.artifactory.api.rest.artifact.FileList;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.rest.search.result.LicensesSearchResult;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.list.KeyValueList;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.resource.artifact.DownloadResource;
import org.artifactory.rest.resource.artifact.SyncResource;
import org.springframework.stereotype.Component;

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

    public boolean isDefault() {
        return true;
    }

    public MoveCopyResult copy(String path, String target, int dryRun) throws Exception {
        throw new MissingRestAddonException();
    }

    public MoveCopyResult move(String path, String target, int dryRun) throws Exception {
        throw new MissingRestAddonException();
    }

    public Response download(String path, DownloadResource.Content content, int mark,
            HttpServletResponse response) throws Exception {
        throw new MissingRestAddonException();
    }

    public Set<String> searchArtifactsByPattern(String pattern) {
        throw new MissingRestAddonException();
    }

    public MoveCopyResult moveOrCopyBuildItems(boolean move, String buildName, String buildNumber, String started,
            String to, int arts, int deps, StringList scopes, int dry) {
        throw new MissingRestAddonException();
    }

    public FileList getFileList(String uri, String path, int deep) {
        throw new MissingRestAddonException();
    }

    public Response replicate(String path, int progress, int mark, int deleteExisting, SyncResource.Overwrite overwrite,
            HttpServletResponse httpResponse) {
        throw new MissingRestAddonException();
    }

    public void renameBuilds(String from, String to) {
        throw new MissingRestAddonException();
    }

    public void renameBuildsAsync(String from, String to) {
        throw new MissingRestAddonException();
    }

    public void deleteBuilds(HttpServletResponse response, String buildName, StringList buildNumbers) {
        throw new MissingRestAddonException();
    }

    public ItemInfo getLastModified(String pathToSearch) {
        throw new MissingRestAddonException();
    }

    public LicensesSearchResult findLicensesInRepos(LicenseStatus status, StringList repos, String servletContextUrl) {
        throw new MissingRestAddonException();
    }

    public Response deleteRepository(String repoKey) {
        throw new MissingRestAddonException();
    }

    public Response getRepositoryConfiguration(String repoKey, List<MediaType> mediaTypes) {
        throw new MissingRestAddonException();
    }

    public Response createOrReplaceRepository(String repoKey, Map repositoryConfig, List<MediaType> mediaTypes,
            int position) {
        throw new MissingRestAddonException();
    }

    public Response updateRepository(String repoKey, Map repositoryConfig, List<MediaType> mediaTypes) {
        throw new MissingRestAddonException();
    }

    public Set<RepoPath> searchArtifactsByChecksum(String md5Checksum, String sha1Checksum, StringList reposToSearch) {
        throw new MissingRestAddonException();
    }

    public Response savePropertiesOnPath(String path, String recursive, KeyValueList properties) {
        throw new MissingRestAddonException();
    }

    public ResponseCtx runPluginExecution(String executionName, Map params, boolean async) {
        throw new MissingRestAddonException();
    }
}
