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
import org.artifactory.api.search.SearchResultBase;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.xml.metadata.MetadataSearchControls;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.util.List;

import static org.artifactory.api.rest.constant.SearchRestConstants.NOT_FOUND;

/**
 * Resource class that handles metadata search actions
 *
 * @author Noam Y. Tenne
 */
public class XpathSearchResource {

    private AuthorizationService authorizationService;
    private SearchService searchService;
    private HttpServletResponse response;
    private HttpServletRequest request;

    /**
     * @param authorizationService
     * @param searchService        Search service instance
     */
    public XpathSearchResource(AuthorizationService authorizationService, SearchService searchService,
            HttpServletRequest request,
            HttpServletResponse response) {
        this.authorizationService = authorizationService;
        this.searchService = searchService;
        this.request = request;
        this.response = response;
    }

    /**
     * Parametrized metadata search
     *
     * @param metadataName  True if the search should be performed as an XML search. False if as a metadata search
     * @param searchType    1 for metadata search, 0 for xml search
     * @param path          The path to search for within the metadata
     * @param value         The value to search for within the metadata
     * @param reposToSearch Specific repositories to search in
     * @return Rest search results object
     */
    @GET
    @Produces({SearchRestConstants.MT_XPATH_SEARCH_RESULT})
    public InfoRestSearchResult get(
            @QueryParam(SearchRestConstants.PARAM_METADATA_NAME_SEARCH) String metadataName,
            @QueryParam(SearchRestConstants.PARAM_METADATA_SEARCH_TYPE) String searchType,
            @QueryParam(SearchRestConstants.PARAM_METADATA_PATH) String path,
            @QueryParam(SearchRestConstants.PARAM_METADATA_VALUE) String value,
            @QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch) throws IOException {
        return search(metadataName, searchType, path, value, reposToSearch);
    }


    /**
     * Performs the metadata search
     *
     * @param metadataName  True if the search should be performed as an XML search. False if as a metadata search
     * @param searchType    1 for metadata search, 0 for xml search
     * @param path          The path to search for within the metadata
     * @param value         The value to search for within the metadata
     * @param reposToSearch Specific repositories to search in
     * @return Rest search results object
     */
    private InfoRestSearchResult search(String metadataName, String searchType, String path, String value,
            List<String> reposToSearch) throws IOException {
        String dataToSearch = StringUtils.isNotBlank(metadataName) ? metadataName : "*";
        MetadataSearchControls controls = new MetadataSearchControls();
        controls.setMetadataName(dataToSearch);
        controls.setPath(path);
        controls.setValue(value);
        controls.setLimitSearchResults(authorizationService.isAnonymous());
        controls.setSelectedRepoForSearch(reposToSearch);

        SearchResults searchResults = null;
        //any value of typeSearch except 1 is xmlSearch
        boolean isXmlSearch = searchType == null || !searchType.equals(String.valueOf(1));
        try {
            if (isXmlSearch) {
                searchResults = searchService.searchXmlContent(controls);
            } else {
                searchResults = searchService.searchMetadata(controls);
            }
        } catch (RepositoryRuntimeException e) {
            RestUtils.sendNotFoundResponse(response, e.getMessage());
            return null;
        }

        List results = searchResults.getResults();
        if (!results.isEmpty()) {
            InfoRestSearchResult infoRestSearchResult = new InfoRestSearchResult();
            for (Object result : results) {
                String uri = RestUtils.buildStorageInfoUri(request, (SearchResultBase) result);
                infoRestSearchResult.results.add(new InfoRestSearchResult.SearchEntry(uri));
            }
            return infoRestSearchResult;
        }

        RestUtils.sendNotFoundResponse(response, NOT_FOUND);
        return null;
    }

}