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


import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.rest.constant.SystemRestConstants;
import org.artifactory.api.rest.system.SystemInfo;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * User: freds Date: Aug 12, 2008 Time: 6:11:53 PM
 */
@Path(SystemRestConstants.PATH_ROOT)
@RolesAllowed(AuthorizationService.ROLE_ADMIN)
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SystemResource {

    private static final String[] PROPS =
            {"artifactory.home", "java.vendor", "java.version", "java.vm.name", "user.name"};

    @Context
    HttpServletResponse httpResponse;

    @Context
    private HttpServletRequest httpServletRequest;

    @Autowired
    CentralConfigService centralConfigService;

    @Autowired
    RepositoryService repoService;

    @Autowired
    SecurityService securityService;

    @Autowired
    StorageService storageService;

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public SystemInfo getSystemInfo() throws IOException {
        SystemInfo systemInfo = new SystemInfo();
        systemInfo.artifactoryVersion = centralConfigService.getVersionInfo();
        for (String prop : PROPS) {
            systemInfo.jvm.put(prop, System.getProperty(prop));
        }
        systemInfo.dataDir = ContextHelper.get().getArtifactoryHome().getDataDir().getAbsolutePath();
        return systemInfo;
    }

    @Path(SystemRestConstants.PATH_EXPORT)
    public ExportResource getExportResource() {
        return new ExportResource(httpResponse, repoService);
    }

    @Path(SystemRestConstants.PATH_CONFIGURATION)
    public ConfigResource getConfigResource() {
        return new ConfigResource(centralConfigService);
    }

    @Path(SystemRestConstants.PATH_SECURITY)
    public SecurityResource getSecurityResource() {
        return new SecurityResource(securityService, centralConfigService, httpServletRequest);
    }

    @Path(SystemRestConstants.PATH_STORAGE)
    public StorageResource getStorageResource() {
        return new StorageResource(storageService);
    }

}
