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

package org.artifactory.rest.resource.system;


import org.apache.commons.lang.StringUtils;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;

/**
 * @author freds
 */
public class ConfigResource {
    private static final Logger log = LoggerFactory.getLogger(ConfigResource.class);

    CentralConfigService centralConfigService;
    private HttpServletRequest request;

    public ConfigResource(CentralConfigService centralConfigService, HttpServletRequest httpServletRequest) {
        this.centralConfigService = centralConfigService;
        this.request = httpServletRequest;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public CentralConfigDescriptor getConfig() {
        return centralConfigService.getDescriptor();
        /*
        File configFile = ArtifactoryHome.getConfigFile();
        try {
            return org.apache.commons.io.Files.readFileToString(configFile, "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        */
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public String setConfig(String xmlContent) {
        log.debug("Received new configuration data.");
        centralConfigService.setConfigXml(xmlContent);
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        int x = descriptor.getLocalRepositoriesMap().size();
        int y = descriptor.getRemoteRepositoriesMap().size();
        int z = descriptor.getVirtualRepositoriesMap().size();
        return "Reload of new configuration (" + x + " local repos, " + y + " remote repos, " + z +
                " virtual repos) succeeded";
    }

    @PUT
    @Consumes("application/xml")
    @Path("remoteRepositories")
    public void useRemoteRepositories(String xmlContent) {
        //TODO: [by tc] do
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("logoUrl")
    public String logoUrl() {
        String descriptorLogo = centralConfigService.getDescriptor().getLogo();
        if (StringUtils.isNotBlank(descriptorLogo)) {
            return descriptorLogo;
        }

        File logoFile = new File(ContextHelper.get().getArtifactoryHome().getLogoDir(), "logo");
        if (logoFile.exists()) {
            return HttpUtils.getServletContextUrl(request) + "/webapp/logo?" + logoFile.lastModified();
        }

        return null;
    }
}
