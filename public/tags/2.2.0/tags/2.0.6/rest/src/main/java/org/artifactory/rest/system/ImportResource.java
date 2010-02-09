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


import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try {
            ContextHelper.get().importFrom(settings, holder);
        } catch (Exception e) {
            holder.setError("Received uncaugth exception", e, log);
        }
    }
}
