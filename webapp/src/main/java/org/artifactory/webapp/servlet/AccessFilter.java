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
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.security.UserInfo;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AccessFilter extends ArtifactoryFilter {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(AccessFilter.class);

    private BasicProcessingFilter authFilter;
    private BasicProcessingFilterEntryPoint authenticationEntryPoint;
    private AuthenticationManager authenticationManager;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        ArtifactoryContext context = getContext();
        authFilter = context.beanForType(BasicProcessingFilter.class);
        authenticationEntryPoint = context.beanForType(BasicProcessingFilterEntryPoint.class);
        authenticationManager = context.beanForType(AuthenticationManager.class);
    }

    @Override
    public void destroy() {
        authFilter.destroy();
        super.destroy();
    }

    @Override
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        boolean uiRequest = RequestUtils.isUiRequest(request);
        if (!uiRequest) {
            //If there is no authentication info on the request, request one
            boolean authExists = RequestUtils.isAuthHeaderPresent(request);
            if (!authExists) {
                //Reuse the wicket authentication if it exists
                Authentication authentication = RequestUtils.getAuthentication(request);
                if (authentication == null) {
                    //If anon read is allowed and do the job as anonymous
                    ArtifactoryContext context = getContext();
                    boolean anonAccessEnabled =
                            context.getAuthorizationService().isAnonAccessEnabled();
                    if (anonAccessEnabled) {
                        //Create the Anonymous token
                        final UsernamePasswordAuthenticationToken authRequest =
                                new UsernamePasswordAuthenticationToken(
                                        UserInfo.ANONYMOUS, "");
                        AuthenticationDetailsSource ads = new AuthenticationDetailsSourceImpl();
                        authRequest.setDetails(ads.buildDetails(request));
                        authentication = authenticationManager.authenticate(authRequest);
                    }
                }
                if (authentication != null) {
                    try {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        chain.doFilter(request, response);
                    } finally {
                        SecurityContextHolder.getContext().setAuthentication(null);
                    }
                } else {
                    authenticationEntryPoint.commence(request, response,
                            new InsufficientAuthenticationException("Authentication is required."));
                }
            } else {
                try {
                    authFilter.doFilter(request, response, chain);
                } finally {
                    SecurityContextHolder.getContext().setAuthentication(null);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
