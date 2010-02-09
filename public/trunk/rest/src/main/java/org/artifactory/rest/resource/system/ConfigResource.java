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


import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * @author freds
 */
public class ConfigResource {
    private static final Logger log = LoggerFactory.getLogger(ConfigResource.class);

    CentralConfigService centralConfigService;

    public ConfigResource(CentralConfigService centralConfigService) {
        this.centralConfigService = centralConfigService;
    }

    @GET
    @Produces("application/xml")
    public CentralConfigDescriptor getConfig() {
        return centralConfigService.getDescriptor();
        /*
        File configFile = ArtifactoryHome.getConfigFile();
        try {
            return org.apache.commons.io.FileUtils.readFileToString(configFile, "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        */
    }

    @POST
    @Consumes("application/xml")
    @Produces("text/plain")
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
}
