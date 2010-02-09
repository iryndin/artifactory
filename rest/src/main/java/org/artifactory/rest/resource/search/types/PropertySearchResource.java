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

import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.rest.search.result.InfoRestSearchResult;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
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
import java.util.Map;

import static org.artifactory.api.rest.constant.SearchRestConstants.NOT_FOUND;

/**
 * Resource class that handles Property search actions
 *
 * @author Eli Givoni
 */
public class PropertySearchResource {

    private SearchService searchService;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public PropertySearchResource(SearchService searchService, HttpServletRequest request,
            HttpServletResponse response) {
        this.searchService = searchService;
        this.request = request;
        this.response = response;
    }

    @GET
    @Produces({SearchRestConstants.MT_PROPERTY_SEARCH_RESULT, MediaType.APPLICATION_JSON})
    public InfoRestSearchResult get(@QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch)
            throws IOException {
        return search(reposToSearch);
    }

    private InfoRestSearchResult search(List<String> reposToSearch) throws IOException {
        Map<String, String[]> parametersMap = request.getParameterMap();
        if (parametersMap.isEmpty()) {
            RestUtils.sendNotFoundResponse(response, NOT_FOUND);
            return null;
        }

        // build the search controls using the query parameters
        PropertySearchControls searchControls = new PropertySearchControls();
        searchControls.setSelectedRepoForSearch(reposToSearch);
        for (String parameterName : parametersMap.keySet()) {
            String[] values = parametersMap.get(parameterName);
            // don't use the repos parameter as a property name parameter
            if (!SearchRestConstants.PARAM_REPO_TO_SEARCH.equals(parameterName)) {
                for (String value : values) {
                    // all searches are "open" ones
                    searchControls.put(parameterName, value, true);
                }
            }
        }

        SearchResults<PropertySearchResult> searchResults = null;
        try {
            searchResults = searchService.searchProperty(searchControls);
        } catch (RepositoryRuntimeException e) {
            RestUtils.sendNotFoundResponse(response, e.getMessage());
            return null;
        }

        List<PropertySearchResult> results = searchResults.getResults();
        if (!results.isEmpty()) {
            InfoRestSearchResult infoSearchResult = new InfoRestSearchResult();
            for (PropertySearchResult result : results) {
                String uri = RestUtils.buildStorageInfoUri(request, result);
                InfoRestSearchResult.SearchEntry entry = new InfoRestSearchResult.SearchEntry(uri);
                infoSearchResult.results.add(entry);
            }
            return infoSearchResult;
        } else {
            RestUtils.sendNotFoundResponse(response, SearchRestConstants.NOT_FOUND);
            return null;
        }
    }

}
