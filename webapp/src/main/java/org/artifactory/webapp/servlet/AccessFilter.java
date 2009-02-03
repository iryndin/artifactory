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
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.security.UserInfo;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.InsufficientAuthenticationException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ui.AuthenticationDetailsSource;
import org.springframework.security.ui.AuthenticationDetailsSourceImpl;
import org.springframework.security.ui.basicauth.BasicProcessingFilter;
import org.springframework.security.ui.basicauth.BasicProcessingFilterEntryPoint;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccessFilter implements Filter {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger LOGGER = Logger.getLogger(AccessFilter.class);

    private ArtifactoryContext context;
    private BasicProcessingFilter authFilter;
    private BasicProcessingFilterEntryPoint authenticationEntryPoint;

    // TODO: Change this to ehcache, google collection or other
    // http://tech.puredanger.com/2008/04/17/concurrentreferencehashmap-in-java-7/
    private final Map<String, AuthenticationCacheEntry> authCache =
            new ConcurrentHashMap<String, AuthenticationCacheEntry>(
                    100, 0.75f, 20);

    static class AuthenticationCacheEntry {
        private long timeSinceLastAccess = System.currentTimeMillis();
        private WeakReference<Authentication> auth;

        AuthenticationCacheEntry(Authentication authentication) {
            auth = new WeakReference<Authentication>(authentication);
        }

        boolean isValid() {
            return auth.get() != null &&
                    System.currentTimeMillis() - timeSinceLastAccess < 300000L;// 5 mins default
        }

        Authentication getAuthentication() {
            timeSinceLastAccess = System.currentTimeMillis();
            return auth.get();
        }

        void setAuthentication(Authentication authentication) {
            timeSinceLastAccess = System.currentTimeMillis();
            auth = new WeakReference<Authentication>(authentication);
        }
    }

    @Override

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.context =
                (ArtifactoryContext) servletContext
                        .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        authFilter = context.beanForType(BasicProcessingFilter.class);
        authenticationEntryPoint = context.beanForType(BasicProcessingFilterEntryPoint.class);
    }

    public void destroy() {
        authFilter.destroy();
    }

    public void doFilter(final ServletRequest req, final ServletResponse resp,
            final FilterChain chain)
            throws IOException, ServletException {
        ArtifactoryContextThreadBinder.bind(context);
        try {
            doFilterInternal((HttpServletRequest) req, ((HttpServletResponse) resp), chain);
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    private void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        boolean uiRequest = RequestUtils.isUiRequest(request);
        if (!uiRequest) {
            //Reuse the wicket authentication if it exists
            Authentication authentication = RequestUtils.getAuthentication(request);
            SecurityContext securityContext = SecurityContextHolder.getContext();
            if (authentication != null) {
                LOGGER.debug("Using authentication " + authentication +
                        " from Http session in non UI request.");
                useAuthentication(request, response, chain, authentication,
                        securityContext);
            } else {
                //If there is no authentication info on the request, use anonymus or request one
                boolean authExists = RequestUtils.isAuthHeaderPresent(request);
                if (!authExists) {
                    useAnonymousIfPossible(request, response, chain, securityContext);
                } else {
                    authenticateAndExecute(request, response, chain, securityContext);
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void authenticateAndExecute(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, SecurityContext securityContext)
            throws IOException, ServletException {
        // Try to see if header in cache
        String header = request.getHeader("Authorization");
        LOGGER.debug("Using basic authentication header " + header);
        AuthenticationCacheEntry ce = authCache.get(header);
        if (ce != null) {
            if (ce.isValid()) {
                Authentication authentication = ce.getAuthentication();
                LOGGER.debug("Header authentication " + authentication + " in cache.");
                useAuthentication(request, response, chain, authentication,
                        securityContext);
                return;
            }
        }
        try {
            authFilter.doFilter(request, response, chain);
        } finally {
            Authentication auth =
                    securityContext.getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // Save authentication like in Wicket Session (if session exists
                if (!RequestUtils.setAuthentication(request, auth, false)) {
                    // Use the header cache
                    if (ce != null) {
                        ce.setAuthentication(auth);
                    } else {
                        authCache.put(header, new AuthenticationCacheEntry(auth));
                    }
                    LOGGER.debug("Added authentication " + auth + " in cache.");
                } else {
                    LOGGER.debug("Added authentication " + auth + " in Http session.");
                }
            }
            securityContext.setAuthentication(null);
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void useAnonymousIfPossible(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, SecurityContext securityContext)
            throws IOException, ServletException {
        boolean anonAccessEnabled =
                context.getAuthorizationService().isAnonAccessEnabled();
        if (anonAccessEnabled) {
            LOGGER.debug("Using anonymous");
            // TODO: Verify that Authentication is thread safe and can be reused
            AuthenticationCacheEntry ce = authCache.get(UserInfo.ANONYMOUS);
            Authentication authentication;
            if (ce != null && ce.isValid()) {
                authentication = ce.getAuthentication();
            } else {
                LOGGER.debug("Creating the Anonymous token");
                final UsernamePasswordAuthenticationToken authRequest =
                        new UsernamePasswordAuthenticationToken(
                                UserInfo.ANONYMOUS, "");
                AuthenticationDetailsSource ads = new AuthenticationDetailsSourceImpl();
                authRequest.setDetails(ads.buildDetails(request));
                authentication =
                        context.beanForType(AuthenticationManager.class).authenticate(authRequest);
                if (authentication != null && authentication.isAuthenticated()) {
                    authCache.put(UserInfo.ANONYMOUS, new AuthenticationCacheEntry(authentication));
                    LOGGER.debug("Adding anonymous authentication " + authentication + " to cache");
                }
            }
            useAuthentication(request, response, chain, authentication, securityContext);
        } else {
            LOGGER.debug("Sending header requiring authentication");
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("Authentication is required."));
        }
    }

    private void useAuthentication(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain, Authentication authentication,
            SecurityContext securityContext) throws IOException, ServletException {
        try {
            securityContext.setAuthentication(authentication);
            chain.doFilter(request, response);
        } finally {
            securityContext.setAuthentication(null);
        }
    }
}
