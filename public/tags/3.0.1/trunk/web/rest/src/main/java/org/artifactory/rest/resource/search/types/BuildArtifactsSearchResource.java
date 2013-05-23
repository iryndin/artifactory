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

import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.rest.build.artifacts.BuildArtifactsRequest;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.rest.search.result.DownloadRestSearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.fs.FileInfo;
import org.artifactory.rest.util.RestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Resource for retrieving build artifacts according to input regexp patterns.
 *
 * @author Shay Yaakov
 * @see BuildArtifactsRequest
 */
public class BuildArtifactsSearchResource {

    private final RestAddon restAddon;
    private AuthorizationService authorizationService;
    private HttpServletRequest request;
    private final HttpServletResponse response;

    public BuildArtifactsSearchResource(RestAddon restAddon, AuthorizationService authorizationService,
            HttpServletRequest request, HttpServletResponse response) {
        this.restAddon = restAddon;
        this.authorizationService = authorizationService;
        this.request = request;
        this.response = response;
    }

    @POST
    @Consumes({BuildRestConstants.MT_BUILD_ARTIFACTS_REQUEST, MediaType.APPLICATION_JSON})
    @Produces({SearchRestConstants.MT_BUILD_ARTIFACTS_SEARCH_RESULT, MediaType.APPLICATION_JSON})
    public DownloadRestSearchResult get(BuildArtifactsRequest buildArtifactsRequest) throws IOException {
        if (isBlank(buildArtifactsRequest.getBuildName())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot search without build name.");
            return null;
        }
        boolean buildNumberIsBlank = isBlank(buildArtifactsRequest.getBuildNumber());
        boolean buildStatusIsBlank = isBlank(buildArtifactsRequest.getBuildStatus());
        if (buildNumberIsBlank && buildStatusIsBlank) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot search without build number or build status.");
            return null;
        }
        if (!buildNumberIsBlank && !buildStatusIsBlank) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot search with both build number and build status parameters, " +
                            "please omit build number if your are looking for latest build by status " +
                            "or omit build status to search for specific build version.");
            return null;
        }

        if (!authorizationService.isAuthenticated()) {
            throw new AuthorizationRestException();
        }

        Map<FileInfo, String> buildArtifacts;
        try {
            buildArtifacts = restAddon.getBuildArtifacts(buildArtifactsRequest);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getLocalizedMessage());
            return null;
        }
        if (buildArtifacts == null || buildArtifacts.isEmpty()) {
            RestUtils.sendNotFoundResponse(response,
                    String.format("Could not find any build artifacts for build '%s' number '%s'.",
                            buildArtifactsRequest.getBuildName(),
                            buildArtifactsRequest.getBuildNumber()));
            return null;
        }

        DownloadRestSearchResult downloadRestSearchResult = new DownloadRestSearchResult();
        for (FileInfo fileInfo : buildArtifacts.keySet()) {
            String downloadUri = RestUtils.buildDownloadUri(request, fileInfo.getRepoKey(), fileInfo.getRelPath());
            downloadRestSearchResult.results.add(new DownloadRestSearchResult.SearchEntry(downloadUri));
        }

        return downloadRestSearchResult;
    }
}
