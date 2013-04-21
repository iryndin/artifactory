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
import org.artifactory.api.rest.constant.ArtifactRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.rest.search.result.CreatedInRangeRestSearchResult;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.util.HttpUtils;
import org.artifactory.util.SerializablePair;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static org.artifactory.api.rest.constant.SearchRestConstants.*;

/**
 * @author Eli Givoni
 */
public class CreatedInRangeResource {
    private AuthorizationService authorizationService;
    private SearchService searchService;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public CreatedInRangeResource(AuthorizationService authorizationService, SearchService searchService,
            HttpServletRequest request,
            HttpServletResponse response) {
        this.authorizationService = authorizationService;
        this.searchService = searchService;
        this.request = request;
        this.response = response;
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
     * @param from          The time to start the search. Exclusive (eg, >). If empty will start from 1st Jan 1970
     * @param to            The time to end search. Inclusive (eg, <=), If empty, will not use current time as the
     *                      limit
     * @param reposToSearch Lists of repositories to search within
     * @return CreatedInRangeRestSearchResult Json representation of the modified artifacts whiting the time range
     */
    @GET
    @Produces({SearchRestConstants.MT_CREATED_IN_RANGE_SEARCH_RESULT, MediaType.APPLICATION_JSON})
    public CreatedInRangeRestSearchResult get(@QueryParam(PARAM_IN_RANGE_FROM) Long from,
            @QueryParam(PARAM_IN_RANGE_TO) Long to,
            @QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch) throws IOException {
        if (authorizationService.isAnonymous()) {
            throw new AuthorizationRestException();
        }
        return search(from, to, reposToSearch);
    }

    private CreatedInRangeRestSearchResult search(Long from, Long to, StringList reposToSearch) throws IOException {
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
        List<SerializablePair<RepoPath, Calendar>> results;
        try {
            results = searchService.searchArtifactsCreatedOrModifiedInRange(fromCal, toCal, reposToSearch);
        } catch (RepositoryRuntimeException e) {
            RestUtils.sendNotFoundResponse(response, e.getMessage());
            return null;
        }

        if (!results.isEmpty()) {
            CreatedInRangeRestSearchResult rangeRestSearchResult = new CreatedInRangeRestSearchResult();
            for (SerializablePair<RepoPath, Calendar> result : results) {
                String uri = buildStorageUri(result);
                String time = RestUtils.toIsoDateString(result.getSecond().getTimeInMillis());
                CreatedInRangeRestSearchResult.CreatedEntry entry =
                        new CreatedInRangeRestSearchResult.CreatedEntry(uri, time);
                rangeRestSearchResult.results.add(entry);
            }
            return rangeRestSearchResult;
        }

        RestUtils.sendNotFoundResponse(response, NOT_FOUND);
        return null;
    }

    private String buildStorageUri(SerializablePair<RepoPath, Calendar> result) {
        RepoPath repoPath = result.getFirst();
        String servletContextUrl = HttpUtils.getServletContextUrl(request);
        StringBuilder sb = new StringBuilder(servletContextUrl);
        sb.append("/").append(RestConstants.PATH_API).append("/").append(ArtifactRestConstants.PATH_ROOT);
        sb.append("/").append(repoPath.getRepoKey()).append("/").append(repoPath.getPath());
        return sb.toString();

    }
}
