/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2014 JFrog Ltd.
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
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.search.common.RestResultFieldName;
import org.artifactory.api.rest.search.result.DynamicItemSearchResult;
import org.artifactory.api.search.ItemSearchResults;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.search.artifact.ArtifactSearchResult;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.fs.ItemInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Calendar;

import static org.artifactory.api.rest.constant.SearchRestConstants.*;

/**
 * @author Eli Givoni
 */
public class AnyDateInRangeResource extends GenericSearchResource {
    private static final Logger log = LoggerFactory.getLogger(AnyDateInRangeResource.class);

    private AuthorizationService authorizationService;
    private RepositoryService repositoryService;

    public AnyDateInRangeResource(AuthorizationService authorizationService, SearchService searchService,
            RepositoryService repositoryService) {
        super(searchService);
        this.authorizationService = authorizationService;
        this.repositoryService = repositoryService;
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
    @Produces({MT_ARTIFACT_RESULT, MediaType.APPLICATION_JSON})
    public Response dynamicGet(@QueryParam(PARAM_IN_RANGE_FROM) String from,
            @QueryParam(PARAM_IN_RANGE_TO) String to,
            @QueryParam(PARAM_REPO_TO_SEARCH) StringList reposToSearch,
            @QueryParam(PARAM_DATE_FIELDS) StringList dateFields,
            @QueryParam(PARAM_RESULT_FIELDS) StringList resultFields
    ) throws IOException {
        if (authorizationService.isAnonymous()) {
            throw new AuthorizationRestException();
        }
        return search(RestUtils.extractLongEpoch(from),
                RestUtils.extractLongEpoch(to),
                reposToSearch, dateFields, resultFields);
    }

    protected Response createResponse(ItemSearchResults<ArtifactSearchResult> results, Calendar from,
            Calendar to, StringList resultFieldNames) {
        RestResultFieldName[] resultFields;
        if (resultFieldNames != null && !resultFieldNames.isEmpty()) {
            resultFields = new RestResultFieldName[resultFieldNames.size()];
            int i = 0;
            for (String fieldName : resultFieldNames) {
                RestResultFieldName byFieldName = RestResultFieldName.byFieldName(fieldName);
                if (byFieldName == null) {
                    String msg = "Result field name " + fieldName + " unknown!";
                    log.debug(msg);
                    return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
                }
                resultFields[i++] = byFieldName;
            }
        } else {
            resultFields = new RestResultFieldName[]{RestResultFieldName.LAST_MODIFIED, RestResultFieldName.CREATED};
        }
        DynamicItemSearchResult restResults = new DynamicItemSearchResult();
        for (ArtifactSearchResult result : results.getResults()) {
            DynamicItemSearchResult.SearchEntry entry = new DynamicItemSearchResult.SearchEntry();
            ItemInfo itemInfo = result.getItemInfo();
            entry.folder = itemInfo.isFolder();
            entry.repo = itemInfo.getRepoKey();
            entry.path = itemInfo.getRelPath();
            for (RestResultFieldName resultField : resultFields) {
                switch (resultField) {
                    case CREATED:
                        entry.created = RestUtils.toIsoDateString(itemInfo.getCreated());
                        break;
                    case LAST_MODIFIED:
                        entry.lastModified = RestUtils.toIsoDateString(itemInfo.getLastModified());
                        break;
                    case LAST_DOWNLOADED:
                        StatsInfo stats = repositoryService.getStatsInfo(itemInfo.getRepoPath());
                        if (stats != null) {
                            entry.lastDownloaded = RestUtils.toIsoDateString(stats.getLastDownloaded());
                            entry.downloadCount = "" + stats.getDownloadCount();
                        } else {
                            entry.lastDownloaded = "";
                            entry.downloadCount = "0";
                        }
                        break;
                }
            }
            restResults.results.add(entry);
        }

        return Response.ok(restResults).type(MT_ARTIFACT_RESULT).build();
    }
}
