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
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.SecurityService;
import org.artifactory.rest.common.AuthorizationContainerRequestFilter;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.ConsumeMime;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.ProduceMime;

/**
 * @author freds
 * @date Sep 4, 2008
 */
public class SecurityResource {
    private static final Logger LOGGER =
            LogManager.getLogger(SecurityResource.class);

    private HttpServletResponse httpResponse;
    private AuthorizationService authService;
    private SecurityService securityService;

    public SecurityResource(HttpServletResponse httpResponse, AuthorizationService authService,
            SecurityService securityService) {
        this.httpResponse = httpResponse;
        this.authService = authService;
        this.securityService = securityService;
    }

    @GET
    @ProduceMime("application/xml")
    public SecurityInfo getSecurityData() {
        AuthorizationContainerRequestFilter.checkAuthorization(authService, httpResponse);
        return securityService.getSecurityData();
    }

    @POST
    @ConsumeMime("application/xml")
    @ProduceMime("text/plain")
    public String importSecurityData(SecurityInfo descriptor) {
        AuthorizationContainerRequestFilter.checkAuthorization(authService, httpResponse);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Activating import of new security data " + descriptor);
        }
        securityService.removeAllSecurityData();
        securityService.importSecurityData(descriptor);
        return "Import of new Security data succeeded";
    }
}
