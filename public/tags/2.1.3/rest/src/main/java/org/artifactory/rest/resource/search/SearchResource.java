/*
 * This file is part of Artifactory.
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

package org.artifactory.rest.resource.search;

import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.rest.SearchRestConstants;
import org.artifactory.api.search.SearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.metadata.MetadataSearchControls;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.util.Pair;
import org.artifactory.util.HttpUtils;
import org.codehaus.jackson.JsonGenerator;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Resource class that handles repo actions
 *
 * @author Yoav Landman
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path("/" + SearchRestConstants.SEARCH_PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_USER, AuthorizationService.ROLE_ADMIN})
public class SearchResource {

    @Context
    private HttpServletResponse response;

    @Context
    private HttpServletRequest request;

    @Autowired
    SearchService searchService;

    @GET
    @Path("/" + SearchRestConstants.SEARCH_PATH_METADATA)
    @Produces("application/xml")
    public SearchResults searchAsXml(
            @QueryParam(SearchRestConstants.SEARCH_PARAM_METADATA_METADATA_NAME) String metadataName,
            @QueryParam(SearchRestConstants.SEARCH_PARAM_METADATA_PATH) String path,
            @QueryParam(SearchRestConstants.SEARCH_PARAM_METADATA_VALUE) String query,
            @QueryParam(SearchRestConstants.SEARCH_PARAM_METADATA_EXACT_MATCH) boolean exactMatch
    ) {
        MetadataSearchControls controls = new MetadataSearchControls();
        controls.setMetadataName(metadataName);
        controls.setPath(path);
        controls.setValue(query);
        controls.setExactMatch(exactMatch);
        SearchResults results = searchService.searchMetadata(controls);
        return results;
    }

    /**
     * Returns a json array of modified artifacts within the time range in the format:
     * <pre>
     * [{
     *    "url" : "http://.../libs-releases-local/path_to_artifact",
     *    "modified" : "2009-07-02T11:11:49+03:00"
     *  } ...]
     * </pre>
     * Where the url is the full url of the modified artifact and the modified is the created or modified date of the
     * artifact that is in within the time range.
     *
     * @param from The time to start the search. Exclusive (eg, >). If empty will start from 1st Jan 1970
     * @param to   The time to end search. Inclusive (eg, <=), If empty, will not use current time as the limit
     * @return Json array of the modified artifacts whiting the time range
     */
    @GET
    @Path("/modified")
    @Produces("application/vnd.org.jfrog.artifactory.ModifiedArtifactsList+json")
    public String searchModifiedInRange(
            @QueryParam("from") Long from,
            @QueryParam("to") Long to) throws IOException {

        Calendar fromCal = null;
        if (from != null) {
            fromCal = Calendar.getInstance();
            fromCal.setTimeInMillis(from);
        }

        Calendar toCal = null;
        if (to != null) {
            toCal = Calendar.getInstance();
            toCal.setTimeInMillis(to);
        }

        String contextUrl = HttpUtils.getServletContextUrl(request);

        List<Pair<RepoPath, Calendar>> results = searchService.searchArtifactsCreatedOrModifiedInRange(fromCal, toCal);
        List<Modified> modifiedArtifacts = new ArrayList<Modified>();
        for (Pair<RepoPath, Calendar> result : results) {
            RepoPath repoPath = result.getFirst();
            String url = contextUrl + "/" + repoPath.getRepoKey() + "/" + repoPath.getPath();
            String isoDateTime = ISODateTimeFormat.dateTime().print(result.getSecond().getTimeInMillis());
            modifiedArtifacts.add(new Modified(url, isoDateTime));
        }

        ServletOutputStream stream = response.getOutputStream();
        JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(stream);
        jsonGenerator.writeObject(modifiedArtifacts);

        return "";
    }

    private static class Modified implements Serializable {
        private String url;
        private String modified;

        private Modified(String url, String modified) {
            this.url = url;
            this.modified = modified;
        }

        public String getUrl() {
            return url;
        }

        public String getModified() {
            return modified;
        }
    }

    /**
     * Searches the repository and returns a plain text result
     *
     * @param searchQuery The search query
     * @return String - Plain text response
     */
    /*@GET
    @Produces("text/plain")
    public String searchAsText(@QueryParam(QUERY_PREFIX) String searchQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("Artifactory REST API search\n");
        sb.append("============================\n");
        if ((searchQuery == null) || ("".equals(searchQuery))) {
            sb.append("'query' parameter is either empty or non existant");
            return sb.toString();
        }
        SearchHelper searchHelper = new SearchHelper(searchService, null);
        String searchResult = searchHelper.searchPlainText(sb, searchQuery);
        return searchResult;
    }*/
}
