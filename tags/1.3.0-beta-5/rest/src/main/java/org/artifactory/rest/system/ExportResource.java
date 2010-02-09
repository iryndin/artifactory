/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.rest.system;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.rest.common.AuthorizationContainerRequestFilter;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.ProduceMime;
import java.io.File;

/**
 * @author freds
 * @date Sep 4, 2008
 */
public class ExportResource {
    private static final Logger LOGGER =
            LogManager.getLogger(ExportResource.class);

    HttpServletResponse httpResponse;
    AuthorizationService authorizationService;
    RepositoryService repoService;

    public ExportResource(HttpServletResponse httpResponse,
            AuthorizationService authorizationService,
            RepositoryService repoService) {
        this.httpResponse = httpResponse;
        this.authorizationService = authorizationService;
        this.repoService = repoService;
    }

    @GET
    @ProduceMime("application/xml")
    public ExportSettings settingsExample() {
        ExportSettings settings = new ExportSettings(new File("/import/path"));
        settings.setReposToExport(repoService.getLocalAndCachedRepoDescriptors());
        return settings;
    }

    @POST
    @ConsumeMime("application/xml")
    public void activateExport(ExportSettings settings) {
        AuthorizationContainerRequestFilter.checkAuthorization(authorizationService, httpResponse);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Activating export " + settings);
        }
        StreamStatusHolder holder = new StreamStatusHolder(httpResponse);
        ContextHelper.get().exportTo(settings, holder);
    }
}
