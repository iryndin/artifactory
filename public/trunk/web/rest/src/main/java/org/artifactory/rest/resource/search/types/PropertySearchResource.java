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

import org.artifactory.api.repo.RepositoryBrowsingService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.rest.search.result.InfoRestSearchResult;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.property.PropertySearchControls;
import org.artifactory.api.search.property.PropertySearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.fs.ItemInfo;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;
import org.artifactory.rest.util.StorageInfoHelper;
import org.artifactory.sapi.common.RepositoryRuntimeException;

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

    private AuthorizationService authorizationService;
    private SearchService searchService;
    private RepositoryService repositoryService;
    private RepositoryBrowsingService repoBrowsingService;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public PropertySearchResource(AuthorizationService authorizationService, SearchService searchService,
            RepositoryService repositoryService, RepositoryBrowsingService repoBrowsingService,
            HttpServletRequest request, HttpServletResponse response) {
        this.authorizationService = authorizationService;
        this.searchService = searchService;
        this.repositoryService = repositoryService;
        this.repoBrowsingService = repoBrowsingService;
        this.request = request;
        this.response = response;
    }

    @GET
    @Produces({SearchRestConstants.MT_PROPERTY_SEARCH_RESULT, MediaType.APPLICATION_JSON})
    public InfoRestSearchResult get(
            @QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch) throws IOException {
        return search(reposToSearch);
    }

    @SuppressWarnings({"unchecked"})
    private InfoRestSearchResult search(List<String> reposToSearch) throws IOException {
        Map<String, String[]> parametersMap = request.getParameterMap();
        if (parametersMap.isEmpty()) {
            RestUtils.sendNotFoundResponse(response, NOT_FOUND);
            return null;
        }

        // build the search controls using the query parameters
        PropertySearchControls searchControls = new PropertySearchControls();
        searchControls.setLimitSearchResults(authorizationService.isAnonymous());
        searchControls.setSelectedRepoForSearch(reposToSearch);
        for (Map.Entry<String, String[]> parameterEntry : parametersMap.entrySet()) {
            String parameterName = parameterEntry.getKey();
            // don't use the repos parameter and the properties parameter as a property name parameter
            if (!SearchRestConstants.PARAM_REPO_TO_SEARCH.equals(parameterName) &&
                    !SearchRestConstants.PARAM_PROPERTIES.equals(parameterName)) {
                String[] values = parameterEntry.getValue();
                for (String value : values) {
                    // all searches are "open" ones
                    searchControls.put(parameterName, value, true);
                }
            }
        }

        if (searchControls.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The search term cannot be empty");
            return null;
        }
        if (searchControls.isWildcardsOnly()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Search term containing only wildcards is not permitted");
            return null;
        }

        ItemSearchResults<PropertySearchResult> searchResults;
        try {
            searchResults = searchService.searchProperty(searchControls);
        } catch (RepositoryRuntimeException e) {
            RestUtils.sendNotFoundResponse(response, e.getMessage());
            return null;
        }

        List<PropertySearchResult> results = searchResults.getResults();
        InfoRestSearchResult infoSearchResult = new InfoRestSearchResult();
        for (PropertySearchResult result : results) {
            ItemInfo itemInfo = result.getItemInfo();
            StorageInfoHelper storageInfoHelper = new StorageInfoHelper(request, repositoryService, repoBrowsingService,
                    itemInfo);
            infoSearchResult.results.add(storageInfoHelper.createStorageInfo());
        }
        return infoSearchResult;
    }
}
