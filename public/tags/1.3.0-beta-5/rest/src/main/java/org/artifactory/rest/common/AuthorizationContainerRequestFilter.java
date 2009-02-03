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
package org.artifactory.rest.common;

import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.security.AuthorizationService;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

/**
 * User: freds Date: Aug 13, 2008 Time: 2:25:34 PM
 */
public class AuthorizationContainerRequestFilter
        /* works only in 0.9 implements ContainerRequestFilter*/ {

    public static void checkAuthorization(AuthorizationService service,
            HttpServletResponse response) {
        if (!service.isAdmin()) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Artifactory API\"");
            throw new WebApplicationException(HttpStatus.SC_UNAUTHORIZED);
        }
    }
}
