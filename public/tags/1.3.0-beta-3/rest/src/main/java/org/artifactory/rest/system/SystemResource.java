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
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.SystemActionInfo;
import org.artifactory.api.rest.SystemInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.rest.common.AuthorizationContainerRequestFilter;
import org.artifactory.rest.common.SpringAutowired;
import org.artifactory.utils.PathUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.IOException;

/**
 * User: freds Date: Aug 12, 2008 Time: 6:11:53 PM
 */
@Path("/system")
@ProduceMime("application/xml")
public class SystemResource {
    private static final Logger LOGGER =
            LogManager.getLogger(SystemResource.class);

    private static final String[] PROPS =
            {"java.vendor", "java.version", "java.vm.name", "user.name"};

    @Context
    HttpServletResponse httpResponse;

    @SpringAutowired
    CentralConfigService centralConfigService;

    @SpringAutowired
    RepositoryService repoService;

    @SpringAutowired
    AuthorizationService authorizationService;

    @SpringAutowired
    SecurityService securityService;

    @GET
    public SystemInfo getSystemInfo() throws IOException {
        AuthorizationContainerRequestFilter.checkAuthorization(authorizationService, httpResponse);
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.artifactoryVersion = centralConfigService.getVersionInfo();
        for (String prop : PROPS) {
            systemInfo.jvm.put(prop, System.getProperty(prop));
        }
        systemInfo.configFilePath = ArtifactoryHome.getConfigFile().getAbsolutePath();
        systemInfo.actionExample = new SystemActionInfo();
        systemInfo.actionExample.importFrom = "The local file path to import from";
        systemInfo.actionExample.exportTo = "The local file path to export to";
        systemInfo.actionExample.repositories =
                PathUtils.collectionToDelimitedString(repoService.getAllRepoKeys(), ",");
        return systemInfo;
    }

    @POST
    @ConsumeMime("application/xml")
    public void activate(SystemActionInfo systemActionInfo) {
        AuthorizationContainerRequestFilter.checkAuthorization(authorizationService, httpResponse);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Activated system Action " + systemActionInfo);
        }
        StreamStatusHolder holder = new StreamStatusHolder(httpResponse);
        if (PathUtils.hasText(systemActionInfo.exportTo)) {
            File destDir = new File(systemActionInfo.exportTo);
            ExportSettings exportSettings = new ExportSettings(destDir);
            if (PathUtils.hasText(systemActionInfo.repositories)) {
                // TODO: get list of repos
                ContextHelper.get().exportTo(exportSettings, holder);
            } else {
                ContextHelper.get().exportTo(exportSettings, holder);
            }
        }
        if (PathUtils.hasText(systemActionInfo.importFrom)) {
            File fromDir = new File(systemActionInfo.importFrom);
            ImportSettings importSettings = new ImportSettings(fromDir);
            ContextHelper.get().importFrom(importSettings, holder);
        }
    }

    @Path("import")
    public ImportResource getImportResource() {
        return new ImportResource(httpResponse, authorizationService, repoService);
    }

    @Path("export")
    public ExportResource getExportResource() {
        return new ExportResource(httpResponse, authorizationService, repoService);
    }

    @Path("configuration")
    public ConfigResource getConfigResource() {
        return new ConfigResource(httpResponse, authorizationService, centralConfigService);
    }

    @Path("security")
    public SecurityResource getSecurityResource() {
        return new SecurityResource(httpResponse, authorizationService, securityService);
    }

}
