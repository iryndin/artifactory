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

package org.artifactory.rest.resource.archive;

import com.sun.jersey.spi.CloseableService;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.addon.rest.RestAddon;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.build.artifacts.BuildArtifactsRequest;
import org.artifactory.api.rest.constant.ArchiveRestConstants;
import org.artifactory.api.rest.constant.BuildRestConstants;
import org.artifactory.api.search.ArchiveIndexer;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.mime.MimeType;
import org.artifactory.mime.NamingUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.rest.util.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Resource class which handles archive operations
 *
 * @author Shay Yaakov
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@Path(ArchiveRestConstants.PATH_ROOT)
@RolesAllowed({AuthorizationService.ROLE_USER, AuthorizationService.ROLE_ADMIN})
public class ArchiveResource {
    private static final Logger log = LoggerFactory.getLogger(ArchiveResource.class);

    @Context
    private HttpServletResponse response;

    @Context
    private CloseableService closeableService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private AddonsManager addonsManager;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ArchiveIndexer archiveIndexer;

    @POST
    @Path(ArchiveRestConstants.PATH_BUILD_ARTIFACTS)
    @Consumes({BuildRestConstants.MT_BUILD_ARTIFACTS_REQUEST, MediaType.APPLICATION_JSON})
    public Response getBuildArtifactsArchive(BuildArtifactsRequest buildArtifactsRequest) throws IOException {
        if (isBlank(buildArtifactsRequest.getBuildName())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot search without build name.");
            return null;
        }
        boolean buildNumberIsBlank = isBlank(buildArtifactsRequest.getBuildNumber());
        boolean buildStatusIsBlank = isBlank(buildArtifactsRequest.getBuildStatus());
        if (buildNumberIsBlank && buildStatusIsBlank) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot search without build number or build status.");
            return null;
        }
        if (!buildNumberIsBlank && !buildStatusIsBlank) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Cannot search with both build number and build status parameters, " +
                            "please omit build number if your are looking for latest build by status " +
                            "or omit build status to search for specific build version.");
            return null;
        }

        if (buildArtifactsRequest.getArchiveType() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Archive type cannot be empty, please provide a type of zip/tar/tar.gz/tgz.");
            return null;
        }

        if (!authorizationService.isAuthenticated()) {
            throw new AuthorizationRestException();
        }

        RestAddon restAddon = addonsManager.addonByType(RestAddon.class);
        try {
            final File buildArtifactsArchive = restAddon.getBuildArtifactsArchive(buildArtifactsRequest);
            if (buildArtifactsArchive == null) {
                RestUtils.sendNotFoundResponse(response,
                        String.format("Could not find any build artifacts for build '%s' number '%s'.",
                                buildArtifactsRequest.getBuildName(),
                                buildArtifactsRequest.getBuildNumber()));
                return null;
            }

            markForDeletionAtResponseEnd(buildArtifactsArchive);

            MimeType mimeType = NamingUtils.getMimeType(buildArtifactsArchive.getName());
            return Response.ok().entity(buildArtifactsArchive).type(mimeType.getType()).build();
        } catch (IOException e) {
            RestUtils.sendNotFoundResponse(response, "Failed to create builds artifacts archive");
            log.error("Failed to create builds artifacts archive: " + e.getMessage(), e);
            return null;
        }
    }

    @POST
    @Path(ArchiveRestConstants.PATH_INDEX)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({AuthorizationService.ROLE_ADMIN})
    public Response index(@QueryParam("path") String path, @QueryParam("recursive") int recursive,
            @QueryParam("indexAllRepos") int indexAllRepos) {

        RepoPath repoPath = null;
        if (indexAllRepos != 1) {
            repoPath = RestUtils.calcRepoPathFromRequestPath(path);
            if (!repositoryService.exists(repoPath)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Could not find repo path " + path).build();
            }
        }

        if (indexAllRepos != 1 && recursive == 0) {
            archiveIndexer.asyncIndex(repoPath);
        } else {
            archiveIndexer.recursiveMarkArchivesForIndexing(repoPath, indexAllRepos == 1);
        }

        String message;
        if (repoPath != null) {
            message = "Archive indexing for path '" + path + "' accepted.";
        } else {
            message = "Archive indexing of all repositories accepted.";
        }
        log.info(message);
        return Response.status(HttpStatus.SC_ACCEPTED).entity(message).build();
    }

    private void markForDeletionAtResponseEnd(final File buildArtifactsArchive) {
        // delete the file after jersey streamed it back to the client
        closeableService.add(new Closeable() {
            @Override
            public void close() throws IOException {
                FileUtils.deleteQuietly(buildArtifactsArchive);
            }
        });
    }
}
