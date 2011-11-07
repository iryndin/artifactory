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
import org.artifactory.api.config.ExportSettingsImpl;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.ImportRestConstants;
import org.artifactory.api.rest.constant.SystemRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Sep 4, 2008
 */
@Path(SystemRestConstants.PATH_EXPORT)
@RolesAllowed(AuthorizationService.ROLE_ADMIN)
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExportResource {
    private static final Logger log = LoggerFactory.getLogger(ExportResource.class);

    @Context
    HttpServletResponse httpResponse;

    @Autowired
    RepositoryService repoService;

    @GET
    @Path(ImportRestConstants.SYSTEM_PATH)
    @Produces({SystemRestConstants.MT_EXPORT_SETTINGS, MediaType.APPLICATION_JSON})
    public ExportSettingsConfigurationImpl settingsExample() {
        ExportSettingsConfigurationImpl settings = new ExportSettingsConfigurationImpl();
        settings.exportPath = "/export/path";
        return settings;
    }

    @POST
    @Path(ImportRestConstants.SYSTEM_PATH)
    @Consumes({SystemRestConstants.MT_EXPORT_SETTINGS, MediaType.APPLICATION_JSON})
    public Response activateExport(ExportSettingsConfigurationImpl settings) {
        StreamStatusHolder holder = new StreamStatusHolder(httpResponse);
        ExportSettingsImpl exportSettings = new ExportSettingsImpl(new File(settings.exportPath), holder);
        exportSettings.setIncludeMetadata(settings.includeMetadata);
        exportSettings.setCreateArchive(settings.createArchive);
        exportSettings.setIgnoreRepositoryFilteringRulesOn(settings.bypassFiltering);
        exportSettings.setVerbose(settings.verbose);
        exportSettings.setFailFast(settings.failOnError);
        exportSettings.setFailIfEmpty(settings.failIfEmpty);
        exportSettings.setM2Compatible(settings.m2);
        exportSettings.setIncremental(settings.incremental);
        exportSettings.setExcludeContent(settings.excludeContent);

        if (!settings.excludeContent) {
            exportSettings.setRepositories(getAllLocalRepoKeys());
        }
        log.debug("Activating export {}", settings);
        try {
            ContextHelper.get().exportTo(exportSettings);
            if (!httpResponse.isCommitted() && holder.isError()) {
                return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(
                        "Export finished with errors. Check " +
                                "Artifactory logs for more details.").build();
            }
        } catch (Exception e) {
            if (!httpResponse.isCommitted()) {
                return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
            }
        }
        return Response.ok().build();
    }

    private List<String> getAllLocalRepoKeys() {
        List<String> repoKeys = new ArrayList<String>();
        for (LocalRepoDescriptor localRepoDescriptor : repoService.getLocalAndCachedRepoDescriptors()) {
            repoKeys.add(localRepoDescriptor.getKey());
        }
        return repoKeys;
    }
}
