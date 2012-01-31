package org.artifactory.rest.resource.search.types;

import org.artifactory.addon.rest.MissingRestAddonException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.rest.constant.SearchRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Resource for retrieving artifacts with bad checksums.
 *
 * @author Tomer Cohen
 */
public class BadChecksumSearchResource {
    private static final Logger log = LoggerFactory.getLogger(BadChecksumSearchResource.class);

    private final RestAddon restAddon;
    private final AuthorizationService authorizationService;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public BadChecksumSearchResource(AuthorizationService authorizationService, RestAddon restAddon,
            HttpServletRequest request, HttpServletResponse response) {
        this.restAddon = restAddon;
        this.authorizationService = authorizationService;
        this.request = request;
        this.response = response;
    }

    /**
     * Searches for artifacts by checksum
     *
     * @param type          The {@link org.artifactory.checksum.ChecksumType} to search for
     * @param reposToSearch Specific repositories to search within
     * @return Search results object
     */
    @GET
    @Produces({SearchRestConstants.MT_BAD_CHECKSUM_SEARCH_RESULT, MediaType.APPLICATION_JSON})
    public Object get(@QueryParam("type") String type,
            @QueryParam(SearchRestConstants.PARAM_REPO_TO_SEARCH) StringList reposToSearch) throws IOException {
        if (!authorizationService.isAuthenticated()) {
            RestUtils.sendUnauthorizedResponse(response,
                    "This search resource is available to authenticated users only.");
            return null;
        }
        log.debug("Finding bad '{}' checksum artifacts in {} ", type, reposToSearch);
        try {
            return restAddon.searchBadChecksumArtifacts(type, reposToSearch, request);
        } catch (MissingRestAddonException mrae) {
            throw mrae;
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}
