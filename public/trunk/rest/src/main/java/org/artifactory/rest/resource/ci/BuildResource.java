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

package org.artifactory.rest.resource.ci;

import com.google.common.collect.Lists;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.rest.build.BuildInfo;
import org.artifactory.api.rest.build.Builds;
import org.artifactory.api.rest.build.BuildsByName;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.rest.constant.RestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.api.Build;
import org.artifactory.log.LoggerFactory;
import org.artifactory.rest.util.RestUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;

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
    private AuthorizationService authorizationService;

    @Autowired
    private BuildService buildService;

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
    @Produces({BuildRestConstants.MT_BUILDS,
            MediaType.APPLICATION_JSON})
    public Builds getAllBuilds() throws IOException {
        List<Build> allBuilds = searchService.getLatestBuildsByName();
        if (!allBuilds.isEmpty()) {
            //Add our builds to the list of build resources
            Builds builds = new Builds();
            builds.slf = getBaseBuildsHref();
            for (Build build : allBuilds) {
                String buildHref = getBuildRelativeHref(build);
                builds.builds.add(new Builds.Build(buildHref, build.getStarted()));
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
    @Produces({BuildRestConstants.MT_BUILDS_BY_NAME,
            MediaType.APPLICATION_JSON})
    public BuildsByName getAllSpecificBuilds() throws IOException {
        String buildName = RestUtils.getBuildNameFromRequest(request);
        List<Build> buildList;
        try {
            buildList = buildService.searchBuildsByName(buildName);
        } catch (RepositoryRuntimeException e) {
            buildList = Lists.newArrayList();
        }
        if (!buildList.isEmpty()) {
            String uri = getBaseBuildsHref() + "/" + buildName;
            BuildsByName builds = new BuildsByName();
            builds.slf = uri;
            for (Build build : buildList) {
                String versionHref = getBuildNumberRelativeHref(build);
                builds.buildsNumbers.add(new BuildsByName.Build(versionHref, build.getStarted()));
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
    @Produces({BuildRestConstants.MT_BUILD_INFO,
            MediaType.APPLICATION_JSON})
    public BuildInfo getBuildInfo() throws IOException {
        String buildName = RestUtils.getBuildNameFromRequest(request);
        long buildNumber = RestUtils.getBuildNumberFromRequest(request);
        Build build = buildService.getLatestBuildByNameAndNumber(buildName, buildNumber);
        if (build != null) {
            BuildInfo buildInfo = new BuildInfo();
            buildInfo.slf = getBuildInfoHref(build);
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

        if (!authorizationService.hasPermission(ArtifactoryPermission.DEPLOY)) {
            response.sendError(HttpStatus.SC_UNAUTHORIZED);
            return;
        }

        buildService.addBuild(build);
        log.info("Added build '{} #{}'", build.getName(), build.getNumber());
    }

    private String getBaseBuildsHref() {
        return RestUtils.getRestApiUrl(request) + "/" + BuildRestConstants.PATH_ROOT;
    }

    private String getBuildRelativeHref(Build build) {
        return "/" + build.getName();
    }

    private String getBuildNumberRelativeHref(Build build) {
        return "/" + build.getNumber();
    }

    private String getBuildInfoHref(Build build) {
        return getBaseBuildsHref() + getBuildRelativeHref(build) + getBuildNumberRelativeHref(build);
    }
}