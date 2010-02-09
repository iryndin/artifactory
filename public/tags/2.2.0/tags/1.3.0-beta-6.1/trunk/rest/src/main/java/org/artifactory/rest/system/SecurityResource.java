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


import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * @author freds
 * @date Sep 4, 2008
 */
public class SecurityResource {
    private static final Logger log = LoggerFactory.getLogger(SecurityResource.class);

    private SecurityService securityService;

    public SecurityResource(SecurityService securityService) {
        this.securityService = securityService;
    }

    @GET
    @Produces("application/xml")
    public SecurityInfo getSecurityData() {
        return securityService.getSecurityData();
    }

    @POST
    @Consumes("application/xml")
    @Produces("text/plain")
    public String importSecurityData(SecurityInfo descriptor) {
        log.debug("Activating import of new security data " + descriptor);
        securityService.removeAllSecurityData();
        securityService.importSecurityData(descriptor);
        return "Import of new Security data succeeded";
    }
}
