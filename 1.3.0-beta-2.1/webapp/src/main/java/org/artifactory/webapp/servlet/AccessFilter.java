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

import org.apache.log4j.Logger;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.webapp.wicket.utils.WebUtils;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.InsufficientAuthenticationException;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ui.AuthenticationDetailsSource;
import org.springframework.security.ui.AuthenticationDetailsSourceImpl;
import org.springframework.security.ui.basicauth.BasicProcessingFilter;
import org.springframework.security.ui.basicauth.BasicProcessingFilterEntryPoint;

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
    private BasicProcessingFilterEntryPoint authenticationEntryPoint;
    private ArtifactorySecurityManager securityManager;

    @SuppressWarnings({"unchecked"})
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        ArtifactoryContext context = getContext();
        authFilter = context.beanForType(BasicProcessingFilter.class);
        authenticationEntryPoint = context.beanForType(BasicProcessingFilterEntryPoint.class);
        securityManager = context.beanForType(ArtifactorySecurityManager.class);
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
        boolean wicketRequest = WebUtils.isWicketRequest(request);
        if (repoRequest && !wicketRequest) {
            //If there is no authentication info on the request, request one
            boolean authExists = WebUtils.isAuthPresent(request);
            if (!authExists) {
                //TODO: [by yl] 
                //Only use anonymous for reads for now. For writes we cannot know if
                //we need to ask for auth unless we check first that for the specific path
                //anonymous does not have write permissions.
                //Unless there a way to know from the request that we already asked for auth and
                //did not get one back?
                String method = request.getMethod();
                boolean read = "get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method) ||
                        "propfind".equalsIgnoreCase(method) || "options".equalsIgnoreCase(method);
                //For read requests check if anonymous read is allowed and do the job as anonynous
                ArtifactoryContext context = getContext();
                boolean anonAccessEnabled = context.getCentralConfig().isAnonAccessEnabled();
                if (read && anonAccessEnabled) {
                    //Create the Anonymous token
                    final UsernamePasswordAuthenticationToken authRequest =
                            new UsernamePasswordAuthenticationToken(
                                    ArtifactorySecurityManager.USER_ANONYMOUS,
                                    "");
                    AuthenticationDetailsSource ads = new AuthenticationDetailsSourceImpl();
                    authRequest.setDetails(ads.buildDetails(request));
                    AuthenticationManager authenticationManager =
                            securityManager.getAuthenticationManager();
                    Authentication auth = authenticationManager.authenticate(authRequest);
                    try {
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        chain.doFilter(req, resp);
                    } finally {
                        SecurityContextHolder.getContext().setAuthentication(null);
                    }
                } else {
                    authenticationEntryPoint.commence(req, resp,
                            new InsufficientAuthenticationException("Authentication is required."));
                }
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
