package org.artifactory.rest.repo;

import org.artifactory.api.search.SearchResultGroup;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.PermissionTargetInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

/**
 * Resource class that handles repo actions
 *
 * @author Noam Tenne
 */
@Path("/repo")
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class RepoResource {

    @Context
    HttpServletRequest httpRequest;

    @Autowired
    SearchService searchService;

    private static final String QUERY_PREFIX = "query";

    /**
     * Searches the repository and returns a plain text result
     *
     * @param searchQuery The search query
     * @return String - Plain text response
     */
    @GET
    @Produces("text/plain")
    public String searchAsText(@QueryParam(QUERY_PREFIX) String searchQuery) {
        StringBuilder sb = getSearchHeader();
        if ((searchQuery == null) || ("".equals(searchQuery))) {
            sb.append("'query' parameter is either empty or non existant");
            return sb.toString();
        }
        RestSearchHelper restSearchHelper = new RestSearchHelper(httpRequest, searchService,
                PermissionTargetInfo.ANY_REPO);
        String searchResult = restSearchHelper.searchPlainText(sb, searchQuery);
        return searchResult;
    }

    /**
     * Searches the repository and returns an SearchResultGroup result
     *
     * @param searchQuery The search query
     * @return - A SearchResultGroup containing the search results
     */
    @GET
    @Produces("application/xml")
    public SearchResultGroup searchAsXml(@QueryParam(QUERY_PREFIX) String searchQuery) {
        RestSearchHelper restSearchHelper = new RestSearchHelper(httpRequest, searchService,
                PermissionTargetInfo.ANY_REPO);
        SearchResultGroup searchResultGroup = restSearchHelper.searchXML(searchQuery);
        return searchResultGroup;
    }

    /**
     * Returns a plain text search title
     *
     * @return StringBuilder - StringBuilder that contains the title
     */
    private StringBuilder getSearchHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Artifactory REST API search\n");
        sb.append("============================\n");
        return sb;
    }
}
