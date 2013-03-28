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

package org.artifactory.rest.resource.search.types;

import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.rest.search.result.ArtifactVersionsResult;
import org.artifactory.api.rest.search.result.VersionEntry;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

/**
 * Resource class that handles artifact latest version search action
 *
 * @author Shay Yaakov
 */
public class ArtifactLatestVersionSearchResource {
    private RestAddon restAddon;
    private HttpServletResponse response;

    public ArtifactLatestVersionSearchResource(RestAddon restAddon, HttpServletResponse response) {
        this.restAddon = restAddon;
        this.response = response;
    }

    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public String get(
            @QueryParam(SearchRestConstants.PARAM_GAVC_GROUP_ID) String groupId,
            @QueryParam(SearchRestConstants.PARAM_GAVC_ARTIFACT_ID) String artifactId,
            @QueryParam(SearchRestConstants.PARAM_GAVC_VERSION) String version,
            @QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch,
            @QueryParam(SearchRestConstants.PARAM_FETCH_FROM_REMOTE) int remote)
            throws IOException {
        try {
            boolean useRemote = remote == 1;
            ArtifactVersionsResult artifactVersions = restAddon.getArtifactVersions(
                    groupId, artifactId, version, reposToSearch, useRemote);
            List<VersionEntry> results = artifactVersions.getResults();

            if (results.isEmpty()) {
                RestUtils.sendNotFoundResponse(response);
                return null;
            }

            boolean searchForReleaseVersion = StringUtils.isBlank(version);
            VersionEntry latest;
            String notFoundMessage = null;
            if (searchForReleaseVersion) {
                latest = getLatestReleaseVersion(results);
                if (latest == null) {
                    notFoundMessage = "Latest release version not found";
                }
            } else {
                latest = getLatestIntegrationVersion(results);
                if (latest == null) {
                    notFoundMessage = "Latest integration version not found";
                }
            }

            if (latest == null) {
                RestUtils.sendNotFoundResponse(response, notFoundMessage);
                return null;
            } else {
                return latest.getVersion();
            }

        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
        return null;
    }

    private VersionEntry getLatestReleaseVersion(List<VersionEntry> results) {
        for (VersionEntry result : results) {
            if (!result.isIntegration()) {
                return result;
            }
        }
        return null;
    }

    private VersionEntry getLatestIntegrationVersion(List<VersionEntry> results) {
        for (VersionEntry result : results) {
            if (result.isIntegration()) {
                return result;
            }
        }
        return null;
    }
}
