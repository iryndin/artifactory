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
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import java.io.File;
import java.io.IOException;

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
    public String getConfig() {
        File configFile = ArtifactoryHome.getConfigFile();
        try {
            return org.apache.commons.io.FileUtils.readFileToString(configFile, "utf-8");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @POST
    @Consumes("application/xml")
    @Produces("text/plain")
    public String setConfig(String xmlContent) {
        log.debug("Recieved new configuration data.");
        File configFile = ArtifactoryHome.getConfigFile();
        File tempFile = new File(ArtifactoryHome.getTmpUploadsDir(), "newArtConfig.xml");
        try {
            org.apache.commons.io.FileUtils.writeStringToFile(tempFile, xmlContent, "utf-8");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        FileUtils.switchFiles(configFile, tempFile);
        centralConfigService.reload();
        CentralConfigDescriptor descriptor = centralConfigService.getDescriptor();
        int x = descriptor.getLocalRepositoriesMap().size();
        int y = descriptor.getRemoteRepositoriesMap().size();
        int z = descriptor.getVirtualRepositoriesMap().size();
        return "Reload of new configuration (" + x + " local repos, " + y + " remote repos, " + z +
                " virtual repos) succeeded";
    }
}
