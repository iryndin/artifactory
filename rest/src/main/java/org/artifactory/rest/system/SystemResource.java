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


import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.SystemInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.common.ArtifactoryHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.io.IOException;

/**
 * User: freds Date: Aug 12, 2008 Time: 6:11:53 PM
 */
@Path("/system")
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SystemResource {
    private static final Logger log = LoggerFactory.getLogger(SystemResource.class);

    private static final String[] PROPS =
            {"java.vendor", "java.version", "java.vm.name", "user.name"};

    @Context
    HttpServletResponse httpResponse;

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    RepositoryService repoService;

    @Autowired
    AuthorizationService authorizationService;

    @Autowired
    SecurityService securityService;

    @GET
    @Produces("application/xml")
    public SystemInfo getSystemInfo() throws IOException {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.artifactoryVersion = centralConfigService.getVersionInfo();
        for (String prop : PROPS) {
            systemInfo.jvm.put(prop, System.getProperty(prop));
        }
        systemInfo.configFilePath = ArtifactoryHome.getConfigFile().getAbsolutePath();
        return systemInfo;
    }

    @Path("import")
    public ImportResource getImportResource() {
        return new ImportResource(httpResponse, repoService);
    }

    @Path("export")
    public ExportResource getExportResource() {
        return new ExportResource(httpResponse, repoService);
    }

    @Path("configuration")
    public ConfigResource getConfigResource() {
        return new ConfigResource(centralConfigService);
    }

    @Path("security")
    public SecurityResource getSecurityResource() {
        return new SecurityResource(securityService);
    }

}
