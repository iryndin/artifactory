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

package org.artifactory.rest.resource.system;


import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.ImportRestConstants;
import org.artifactory.api.rest.constant.RepositoriesRestConstants;
import org.artifactory.api.search.SearchService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

/**
 * @author freds
 * @author Tomer Cohen
 * @date Sep 4, 2008
 */
@Path(ImportRestConstants.PATH_ROOT)
@RolesAllowed(AuthorizationService.ROLE_ADMIN)
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ImportResource {
    private static final Logger log = LoggerFactory.getLogger(ImportResource.class);

    @Context
    HttpServletResponse httpResponse;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    SearchService searchService;


    @GET
    @Produces(MediaType.APPLICATION_XML)
    public ImportSettings settingsExample() {
        ImportSettings settings = new ImportSettings(new File("/import/path"));
        settings.setRepositories(repositoryService.getLocalAndCachedRepoDescriptors());
        return settings;
    }

    @POST
    @Path(ImportRestConstants.SYSTEM_PATH)
    @Consumes(MediaType.APPLICATION_XML)
    public Response activateImport(ImportSettings settings) {
        log.debug("Activating import {}", settings);
        StreamStatusHolder holder = new StreamStatusHolder(httpResponse);
        if (!authorizationService.isAdmin()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        settings = new ImportSettings(settings.getBaseDir(), settings, holder);
        try {
            ContextHelper.get().importFrom(settings);
        } catch (Exception e) {
            holder.setError("Received uncaught exception", e, log);
        } finally {
            SearchService searchService = ContextHelper.get().beanForType(SearchService.class);
            searchService.asyncIndexMarkedArchives();
        }
        return Response.ok().build();
    }

    @PUT
    @Path(ImportRestConstants.REPOSITORIES_PATH)
    public void importRepositories(
            //The base path to import from (may contain a single repo or multiple repos with named sub folders
            @QueryParam(RepositoriesRestConstants.PATH) String path,
            //Empty/null repo -> all
            @QueryParam(RepositoriesRestConstants.TARGET_REPO) String targetRepo,
            //Include metadata - default 1
            @QueryParam(RepositoriesRestConstants.INCLUDE_METADATA) String includeMetadata,
            //Verbose - default 0
            @QueryParam(RepositoriesRestConstants.VERBOSE) String verbose) throws IOException {
        StreamStatusHolder statusHolder = new StreamStatusHolder(httpResponse);
        String repoNameToImport = targetRepo;
        if (StringUtils.isBlank(repoNameToImport)) {
            repoNameToImport = "All repositories";
        }
        statusHolder.setStatus("Starting Repositories Import of " + repoNameToImport + " from " + path, log);
        if (!authorizationService.isAdmin()) {
            statusHolder.setError(
                    "User " + authorizationService.currentUsername() + " is not permitted to import repositories",
                    HttpStatus.SC_UNAUTHORIZED, log);
            return;
        }
        if (StringUtils.isEmpty(path)) {
            statusHolder.setError("Source directory path may not be empty.", HttpStatus.SC_BAD_REQUEST, log);
        }
        File baseDir = new File(path);
        if (!baseDir.exists()) {
            statusHolder.setError("Directory " + path + " does not exist.", HttpStatus.SC_BAD_REQUEST, log);
        }
        ImportSettings importSettings = new ImportSettings(baseDir, statusHolder);
        if (StringUtils.isNotBlank(includeMetadata)) {
            importSettings.setIncludeMetadata(Integer.parseInt(includeMetadata) == 1);
        }
        if (StringUtils.isNotBlank(verbose)) {
            importSettings.setVerbose(Integer.parseInt(verbose) == 1);
        }
        try {
            if (StringUtils.isBlank(targetRepo)) {
                repositoryService.importAll(importSettings);
            } else {
                importSettings.setIndexMarkedArchives(true);
                repositoryService.importRepo(targetRepo, importSettings);
            }
        } catch (Exception e) {
            statusHolder.setError("Unable to import repository", e, log);
        } finally {
            if (!importSettings.isIndexMarkedArchives()) {
                searchService.asyncIndexMarkedArchives();
            }
        }
    }

}
