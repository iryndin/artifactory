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
import org.artifactory.config.jaxb.JaxbHelper;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import java.io.File;

/**
 * @author freds
 * @date Sep 4, 2008
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
    }

    @POST
    @Consumes("application/xml")
    @Produces("text/plain")
    public String setConfig(CentralConfigDescriptor config) {
        log.debug("Changing configuration with {}", config);
        File configFile = ArtifactoryHome.getConfigFile();
        File tempFile = new File(ArtifactoryHome.getTmpUploadsDir(), "newArtConfig.xml");
        JaxbHelper.writeConfig(config, tempFile);
        FileUtils.switchFiles(configFile, tempFile);
        centralConfigService.reload();
        return "Reload of new configuration succeeded";
    }
}
