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

package org.artifactory.rest.resource.search.types;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.rest.search.result.InfoRestSearchResult;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.gavc.GavcSearchControls;
import org.artifactory.api.search.gavc.GavcSearchResult;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

import static org.artifactory.api.rest.constant.SearchRestConstants.NOT_FOUND;

/**
 * Resource class that handles GAVC search actions
 *
 * @author Eli givoni
 */
public class GavcSearchResource {

    private SearchService searchService;
    private HttpServletRequest request;
    private HttpServletResponse response;

    /**
     * @param searchService Search service instance
     */
    public GavcSearchResource(SearchService searchService, HttpServletRequest request, HttpServletResponse response) {
        this.searchService = searchService;
        this.request = request;
        this.response = response;
    }

    /**
     * Parametrized GAVC search
     *
     * @param groupId       Group ID to search for
     * @param artifactId    Artifact ID to search for
     * @param version       Version to search for
     * @param classifier    Classifier to search for
     * @param reposToSearch Specific repositories to search in
     * @return Rest search results object
     */
    @GET
    @Produces({SearchRestConstants.MT_GAVC_SEARCH_RESULT, MediaType.APPLICATION_JSON})
    public InfoRestSearchResult get(
            @QueryParam(SearchRestConstants.PARAM_GAVC_GROUP_ID) String groupId,
            @QueryParam(SearchRestConstants.PARAM_GAVC_ARTIFACT_ID) String artifactId,
            @QueryParam(SearchRestConstants.PARAM_GAVC_VERSION) String version,
            @QueryParam(SearchRestConstants.PARAM_GAVC_CLASSIFIER) String classifier,
            @QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch)
            throws IOException {
        return search(groupId, artifactId, version, classifier, reposToSearch);
    }

    /**
     * Performs the GAVC search
     *
     * @param groupId       Group ID to search for
     * @param artifactId    Artifact ID to search for
     * @param version       Version to search for
     * @param classifier    Classifier to search for
     * @param reposToSearch Specific repositories to search in
     * @return Rest search results object
     */
    private InfoRestSearchResult search(String groupId, String artifactId, String version, String classifier,
            List<String> reposToSearch) throws IOException {
        if (hasAtLeastOneValidParameter(groupId, artifactId, version, classifier)) {
            GavcSearchControls searchControls = new GavcSearchControls();
            searchControls.setGroupId(groupId);
            searchControls.setArtifactId(artifactId);
            searchControls.setVersion(version);
            searchControls.setClassifier(classifier);
            searchControls.setSelectedRepoForSearch(reposToSearch);

            SearchResults<GavcSearchResult> searchResults = null;
            try {
                searchResults = searchService.searchGavc(searchControls);
            } catch (RepositoryRuntimeException e) {
                RestUtils.sendNotFoundResponse(response, e.getMessage());
                return null;
            }

            if (!searchResults.getResults().isEmpty()) {
                InfoRestSearchResult gavcRestSearchResult = new InfoRestSearchResult();
                for (GavcSearchResult result : searchResults.getResults()) {
                    String uri = RestUtils.buildStorageInfoUri(request, result);
                    gavcRestSearchResult.results.add(new InfoRestSearchResult.SearchEntry(uri));
                }
                return gavcRestSearchResult;
            }
        }
        RestUtils.sendNotFoundResponse(response, NOT_FOUND);
        return null;
    }

    private boolean hasAtLeastOneValidParameter(String groupId, String artifactId, String version, String classifier) {
        return StringUtils.isNotBlank(groupId) || StringUtils.isNotBlank(artifactId) ||
                StringUtils.isNotBlank(version) || StringUtils.isNotBlank(classifier);
    }
}