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
package org.artifactory.webapp.servlet;

import org.acegisecurity.InsufficientAuthenticationException;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.ui.AuthenticationEntryPoint;
import org.acegisecurity.ui.basicauth.BasicProcessingFilter;
import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryContext;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class AccessFilter extends ArtifactoryFilter {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(AccessFilter.class);

    private BasicProcessingFilter authFilter;

    @SuppressWarnings({"unchecked"})
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        ArtifactoryContext context = getContext();
        authFilter = context.beanForType(BasicProcessingFilter.class);
    }

    public void destroy() {
        authFilter.destroy();
        super.destroy();
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void doFilterInternal(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        final String servletPath = request.getServletPath();
        boolean repoRequest = isRepoRequest(servletPath);
        //If we are getting a get request check if anonymous downloads are allowed
        ArtifactoryContext context = getContext();
        boolean anonDownloadsAllowed = context.getCentralConfig().isAnonDownloadsAllowed();
        String method = request.getMethod();
        boolean read = "get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method);
        boolean allowAnonymousRead = read && anonDownloadsAllowed;
        if (repoRequest && !allowAnonymousRead) {
            //If there is no authentication info on the request, ask for it and return
            String header = request.getHeader("Authorization");
            boolean authExists = header != null && header.startsWith("Basic ");
            if (!authExists) {
                AuthenticationEntryPoint point = authFilter.getAuthenticationEntryPoint();
                point.commence(req, resp,
                        new InsufficientAuthenticationException("Authentication is required."));
            } else {
                try {
                    authFilter.doFilter(req, resp, chain);
                } finally {
                    SecurityContextHolder.getContext().setAuthentication(null);
                }
            }
        } else {
            chain.doFilter(req, resp);
        }
    }
}
