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

package org.artifactory.rest.resource.system;


import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.SearchService;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import java.io.File;

/**
 * @author freds
 * @date Sep 4, 2008
 */
public class ImportResource {
    private static final Logger log = LoggerFactory.getLogger(ImportResource.class);

    HttpServletResponse httpResponse;
    RepositoryService repoService;

    public ImportResource(HttpServletResponse httpResponse, RepositoryService repoService) {
        this.httpResponse = httpResponse;
        this.repoService = repoService;
    }

    @GET
    @Produces("application/xml")
    public ImportSettings settingsExample() {
        ImportSettings settings = new ImportSettings(new File("/import/path"));
        settings.setRepositories(repoService.getLocalAndCachedRepoDescriptors());
        return settings;
    }

    @POST
    @Consumes("application/xml")
    public void activateImport(ImportSettings settings) {
        log.debug("Activating import {}", settings);
        StreamStatusHolder holder = new StreamStatusHolder(httpResponse);
        settings = new ImportSettings(settings.getBaseDir(), settings, holder);
        try {
            ContextHelper.get().importFrom(settings);
        } catch (Exception e) {
            holder.setError("Received uncaught exception", e, log);
        } finally {
            SearchService searchService = ContextHelper.get().beanForType(SearchService.class);
            searchService.indexMarkedArchives();
        }
    }
}
