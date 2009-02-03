package org.artifactory.rest.repo;

import org.artifactory.api.search.SearchControls;
import org.artifactory.api.search.SearchResult;
import org.artifactory.api.search.SearchResultGroup;
import org.artifactory.api.search.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * A utility class that can search the repository
 *
 * @author Noam Tenne
 */
public class RestSearchHelper {

    private static final Logger log = LoggerFactory.getLogger(RestSearchHelper.class);
    private SearchService searchService;
    private HttpServletRequest request;
    private String repoToSearch;

    /**
     * Constructor
     *
     * @param request       The request recieved
     * @param searchService An instance of the search service
     * @param repoToSearch  The key of the repo to be searched
     */
    public RestSearchHelper(HttpServletRequest request, SearchService searchService, String repoToSearch) {
        this.request = request;
        this.searchService = searchService;
        this.repoToSearch = repoToSearch;
    }

    /**
     * Searches and returns a plain text result
     *
     * @param sb          StringBuilder to stream result list to
     * @param searchQuery The query to execute
     * @return String - The result of the search
     */
    public String searchPlainText(StringBuilder sb, String searchQuery) {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearch(searchQuery);
        List<SearchResult> resultList = searchService.searchArtifacts(searchControls);
        if (resultList.isEmpty()) {
            return emptyResult(sb, searchQuery);
        }

        sb.append("\nSearch results for query: '").append(searchQuery).append("'");
        sb.append(" in repository: ").append(repoToSearch);
        sb.append("\n====================================\n");
        for (SearchResult searchResult : resultList) {
            sb.append("-").append(searchResult.getRepoKey()).append(":").append(searchResult.getPath()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Searches and returns a SearchResultGroup
     *
     * @param searchQuery The query to execute
     * @return SearchResultGroup - Contains the list of results
     */
    public SearchResultGroup searchXML(String searchQuery) {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearch(searchQuery);
        List<SearchResult> resultList = searchService.searchArtifacts(searchControls);
        return new SearchResultGroup(resultList);
    }

    /**
     * Returns a string with a plain text "no results" message
     *
     * @param sb          StringBuilder to stream text to
     * @param searchQuery The query that was executed
     * @return String - The string to return to the user
     */
    private String emptyResult(StringBuilder sb, String searchQuery) {
        sb.append("No results found for query: '").append(searchQuery).append("'");
        sb.append(" in repository: ").append(repoToSearch);
        return sb.toString();
    }
}