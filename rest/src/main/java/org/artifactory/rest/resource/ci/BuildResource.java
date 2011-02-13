/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.rest.resource.ci;

import com.google.common.collect.Sets;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.rest.artifact.MoveCopyResult;
import org.artifactory.api.rest.build.BuildInfo;
import org.artifactory.api.rest.build.Builds;
import org.artifactory.api.rest.build.BuildsByName;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.rest.common.list.KeyValueList;
import org.artifactory.rest.common.list.StringList;
import org.artifactory.rest.util.RestUtils;
import org.artifactory.util.DoesNotExistException;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildRetention;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Set;

/**
 * A resource to manage the build actions
 *
 * @author Noam Y. Tenne
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(BuildRestConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_ADMIN, AuthorizationService.ROLE_USER})
public class BuildResource {

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private BuildService buildService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private SearchService searchService;

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    private static final Logger log = LoggerFactory.getLogger(BuildResource.class);

    /**
     * Assemble all, last created, available builds with the last
     *
     * @return Builds json object
     */
    @GET
    @Produces({BuildRestConstants.MT_BUILDS, MediaType.APPLICATION_JSON})
    public Builds getAllBuilds() throws IOException {
        Set<BasicBuildInfo> latestBuildsByName = searchService.getLatestBuildsByName();
        if (!latestBuildsByName.isEmpty()) {
            //Add our builds to the list of build resources
            Builds builds = new Builds();
            builds.slf = getBaseBuildsHref();

            for (BasicBuildInfo buildInfo : latestBuildsByName) {
                String buildHref = getBuildRelativeHref(buildInfo.getName());
                builds.builds.add(new Builds.Build(buildHref, buildInfo.getStarted()));
            }
            return builds;

        }
        String msg = "No builds were found";
        response.sendError(HttpStatus.SC_NOT_FOUND, msg);
        return null;

    }

    /**
     * Get the build name from the request url and assemble all builds under that name.
     *
     * @return BuildsByName json object
     */
    @GET
    @Path("/{name}")
    @Produces({BuildRestConstants.MT_BUILDS_BY_NAME, MediaType.APPLICATION_JSON})
    public BuildsByName getAllSpecificBuilds() throws IOException {
        String buildName = RestUtils.getBuildNameFromRequest(request);
        Set<BasicBuildInfo> buildsByName;
        try {
            buildsByName = buildService.searchBuildsByName(buildName);
        } catch (RepositoryRuntimeException e) {
            buildsByName = Sets.newHashSet();
        }
        if (!buildsByName.isEmpty()) {
            BuildsByName builds = new BuildsByName();
            builds.slf = getBaseBuildsHref() + getBuildRelativeHref(buildName);
            for (BasicBuildInfo basicBuildInfo : buildsByName) {
                String versionHref = getBuildNumberRelativeHref(basicBuildInfo.getNumber());
                builds.buildsNumbers.add(new BuildsByName.Build(versionHref, basicBuildInfo.getStarted()));
            }
            return builds;
        }
        String msg = String.format("No build was found for build name: %s", buildName);
        response.sendError(HttpStatus.SC_NOT_FOUND, msg);
        return null;
    }

    /**
     * Get the build name and number from the request url and send back the exact build for those parameters
     *
     * @return BuildInfo json object
     */
    @GET
    @Path("/{name}/{buildNumber}")
    @Produces({BuildRestConstants.MT_BUILD_INFO, MediaType.APPLICATION_JSON})
    public BuildInfo getBuildInfo() throws IOException {
        String buildName = RestUtils.getBuildNameFromRequest(request);
        String buildNumber = RestUtils.getBuildNumberFromRequest(request);
        Build build = buildService.getLatestBuildByNameAndNumber(buildName, buildNumber);
        if (build != null) {
            BuildInfo buildInfo = new BuildInfo();
            buildInfo.slf = getBuildInfoHref(build.getName(), build.getNumber());
            buildInfo.buildInfo = build;
            return buildInfo;
        } else {
            String msg =
                    String.format("No build was found for build name: %s , build number: %s ", buildName, buildNumber);
            response.sendError(HttpStatus.SC_NOT_FOUND, msg);
            return null;
        }
    }

    /**
     * Adds the given build information to the DB
     *
     * @param build Build to add
     */
    @PUT
    @Consumes({BuildRestConstants.MT_BUILD_INFO, RestConstants.MT_LEGACY_ARTIFACTORY_APP,
            MediaType.APPLICATION_JSON})
    public void addBuild(Build build) throws Exception {
        log.info("Adding build '{} #{}'", build.getName(), build.getNumber());

        if (repositoryService.getDeployableRepoDescriptors().isEmpty()) {
            response.sendError(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        buildService.addBuild(build);
        log.info("Added build '{} #{}'", build.getName(), build.getNumber());
        BuildRetention retention = build.getBuildRetention();
        if (retention != null) {
            RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
            restAddon.discardOldBuilds(build.getName(), retention);
        }
    }

    /**
     * Move or copy the artifacts and\or dependencies of the specified build
     *
     * @param started Build started date. Can be null
     * @param to      Key of target repository to move to
     * @param arts    Zero or negative int if to exclude artifacts from the action take. Positive int to include
     * @param deps    Zero or negative int if to exclude dependencies from the action take. Positive int to include
     * @param scopes  Scopes of dependencies to copy (agnostic if null or empty)
     * @param dry     Zero or negative int if to apply the selected action. Positive int to simulate
     * @return Result of action
     */
    @POST
    @Path("/{action}/{name}/{buildNumber}")
    @Produces({BuildRestConstants.MT_COPY_MOVE_RESULT, MediaType.APPLICATION_JSON})
    public MoveCopyResult moveBuildItems(@QueryParam("started") String started,
            @QueryParam("to") String to,
            @QueryParam("arts") @DefaultValue("1") int arts,
            @QueryParam("deps") int deps,
            @QueryParam("scopes") StringList scopes,
            @QueryParam("properties") KeyValueList properties,
            @QueryParam("dry") int dry) throws IOException {
        return moveOrCopy(started, to, arts, deps, scopes, properties, dry);
    }

    /**
     * Renames structure, content and properties of build info objects
     *
     * @param to Replacement build name
     */
    @POST
    @Path("/rename/{buildName}")
    public String renameBuild(@QueryParam("to") String to) throws IOException {
        String[] pathElements = RestUtils.getBuildRestUrlPathElements(request);
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            String from = URLDecoder.decode(pathElements[1], "UTF-8");
            restAddon.renameBuilds(from, to);

            response.setStatus(HttpStatus.SC_OK);

            return String.format("Build renaming of '%s' to '%s' was successfully started.\n", from, to);
        } catch (AuthorizationException ae) {
            response.sendError(HttpStatus.SC_UNAUTHORIZED, ae.getMessage());
        } catch (IllegalArgumentException iae) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, iae.getMessage());
        } catch (DoesNotExistException dnne) {
            response.sendError(HttpStatus.SC_NOT_FOUND, dnne.getMessage());
        }

        return null;
    }

    /**
     * Removes the build with the given name and number
     *
     * @return Status message
     */
    @DELETE
    @Path("/{name}")
    public void deleteBuilds(@QueryParam("buildNumbers") StringList buildNumbers) throws IOException {
        String[] pathElements = RestUtils.getBuildRestUrlPathElements(request);
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            String buildName = URLDecoder.decode(pathElements[0], "UTF-8");
            restAddon.deleteBuilds(response, buildName, buildNumbers);
        } catch (AuthorizationException ae) {
            response.sendError(HttpStatus.SC_UNAUTHORIZED, ae.getMessage());
        } catch (IllegalArgumentException iae) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, iae.getMessage());
        } catch (DoesNotExistException dnne) {
            response.sendError(HttpStatus.SC_NOT_FOUND, dnne.getMessage());
        }
        response.flushBuffer();
    }

    /**
     * Move or copy the artifacts and\or dependencies of the specified build
     *
     * @param started    Build started date. Can be null
     * @param to         Key of target repository to move to
     * @param arts       Zero or negative int if to exclude artifacts from the action take. Positive int to include
     * @param deps       Zero or negative int if to exclude dependencies from the action take. Positive int to include
     * @param scopes     Scopes of dependencies to copy (agnostic if null or empty)
     * @param properties
     * @param dry        Zero or negative int if to apply the selected action. Positive int to simulate  @return Result
     *                   of action
     */
    private MoveCopyResult moveOrCopy(String started, String to, int arts, int deps, StringList scopes,
            KeyValueList properties, int dry) throws IOException {
        String[] pathElements = RestUtils.getBuildRestUrlPathElements(request);

        boolean move;
        String action = pathElements[0];
        if ("move".equalsIgnoreCase(action)) {
            move = true;
        } else if ("copy".equalsIgnoreCase(action)) {
            move = false;
        } else {
            response.sendError(HttpStatus.SC_BAD_REQUEST, "'" + action +
                    "' is an unsupported operation. Please use 'move' or 'copy'.");
            return null;
        }
        if (properties == null) {
            properties = new KeyValueList("");
        }
        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            return restAddon.moveOrCopyBuildItems(move, URLDecoder.decode(pathElements[1], "UTF-8"),
                    URLDecoder.decode(pathElements[2], "UTF-8"), started, to, arts, deps, scopes, properties, dry);
        } catch (IllegalArgumentException iae) {
            response.sendError(HttpStatus.SC_BAD_REQUEST, iae.getMessage());
        } catch (DoesNotExistException dnee) {
            response.sendError(HttpStatus.SC_NOT_FOUND, dnee.getMessage());
        } catch (ParseException pe) {
            response.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unable to parse given build start date: " +
                    pe.getMessage());
        }
        return null;
    }

    private String getBaseBuildsHref() {
        return RestUtils.getRestApiUrl(request) + "/" + BuildRestConstants.PATH_ROOT;
    }

    private String getBuildRelativeHref(String buildName) throws UnsupportedEncodingException {
        return "/" + URLEncoder.encode(buildName, "utf-8");
    }

    private String getBuildNumberRelativeHref(String buildNumber) throws UnsupportedEncodingException {
        return "/" + URLEncoder.encode(buildNumber, "utf-8");
    }

    private String getBuildInfoHref(String buildName, String buildNumber) throws UnsupportedEncodingException {
        return getBaseBuildsHref() + getBuildRelativeHref(buildName) + getBuildNumberRelativeHref(buildNumber);
    }
}