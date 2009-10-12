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

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.security.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

/**
 * Authorization filter for all the REST requests.
 *
 * @author Fred Simon
 * @author Yossi Shaul
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class RestAuthorizationFilter implements ContainerRequestFilter {
    @Context
    HttpServletResponse response;

    @Autowired
    AuthorizationService authorizationService;

    public ContainerRequest filter(ContainerRequest request) {
        if (!authorizationService.isAdmin()) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Artifactory API\"");
            try {
                response.sendError(HttpStatus.SC_UNAUTHORIZED);
            } catch (IOException e) {
                throw new WebApplicationException(HttpStatus.SC_UNAUTHORIZED);
            }
        }
        return request;
    }
}
