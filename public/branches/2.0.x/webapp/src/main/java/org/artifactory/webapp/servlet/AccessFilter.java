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

import org.apache.commons.codec.digest.DigestUtils;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.api.security.UserInfo;
import org.artifactory.security.HttpAuthenticationDetailsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.InsufficientAuthenticationException;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ui.AuthenticationDetailsSource;
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
import java.util.Map;

public class AccessFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AccessFilter.class);

    private ArtifactoryContext context;
    private BasicProcessingFilter authFilter;
    private BasicProcessingFilterEntryPoint authenticationEntryPoint;

    /**
     * holds cached Authentication instances for the non ui requests based on the Authorization header and client ip
     */
    private Map<AuthCacheKey, Authentication> nonUiAuthCache;

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.context = (ArtifactoryContext) servletContext
                .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        authFilter = context.beanForType(BasicProcessingFilter.class);
        authenticationEntryPoint = context.beanForType(BasicProcessingFilterEntryPoint.class);
        initCaches();
        String usePathInfo = filterConfig.getInitParameter("usePathInfo");
        if (usePathInfo != null) {
            RequestUtils.setUsePathInfo(Boolean.parseBoolean(usePathInfo));
        }
    }

    @SuppressWarnings({"unchecked"})
    private void initCaches() {
        CacheService cacheService = context.beanForType(CacheService.class);
        nonUiAuthCache = cacheService.getCache(ArtifactoryCache.authentication);
    }

    public void destroy() {
        authFilter.destroy();
    }

    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
            throws IOException, ServletException {
        ArtifactoryContextThreadBinder.bind(context);
        try {
            doFilterInternal((HttpServletRequest) req, ((HttpServletResponse) resp), chain);
        } finally {
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    private void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String servletPath = RequestUtils.getServletPathFromRequest(request);
        String method = request.getMethod();
        if ((servletPath == null || "/".equals(servletPath) || servletPath.length() == 0) &&
                "get".equalsIgnoreCase(method)) {
            //We were called with an empty path - redirect to the app main page
            response.sendRedirect("./" + RequestUtils.WEBAPP_URL_PATH_PREFIX);
            return;
        }
        boolean uiRequest = RequestUtils.isUiRequest(request);
        if (!uiRequest) {
            //Reuse the wicket authentication if it exists
            Authentication authentication = RequestUtils.getAuthentication(request);
            SecurityContext securityContext = SecurityContextHolder.getContext();
            if (authentication != null) {
                log.debug("Using authentication {} from Http session in non UI request.", authentication);
                useAuthentication(request, response, chain, authentication, securityContext);
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
            FilterChain chain, SecurityContext securityContext) throws IOException, ServletException {
        // Try to see if authentication in cache based on the hashed header and client ip
        AuthCacheKey authCacheKey = new AuthCacheKey(request);
        Authentication authentication = nonUiAuthCache.get(authCacheKey);
        if (authentication != null) {
            log.debug("Header authentication {} found in cache.", authentication);
            useAuthentication(request, response, chain, authentication, securityContext);
            return;
        }
        try {
            authFilter.doFilter(request, response, chain);
        } finally {
            Authentication auth = securityContext.getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // Save authentication like in Wicket Session (if session exists
                if (!RequestUtils.setAuthentication(request, auth, false)) {
                    // Use the header cache
                    nonUiAuthCache.put(authCacheKey, auth);
                    log.debug("Added authentication {} in cache.", auth);
                } else {
                    log.debug("Added authentication {} in Http session.", auth);
                }
            }
            securityContext.setAuthentication(null);
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void useAnonymousIfPossible(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, SecurityContext securityContext) throws IOException, ServletException {
        boolean anonAccessEnabled = context.getAuthorizationService().isAnonAccessEnabled();
        if (anonAccessEnabled) {
            log.debug("Using anonymous");
            AuthCacheKey authCacheKey = new AuthCacheKey("", request.getRemoteAddr());
            Authentication authentication = nonUiAuthCache.get(authCacheKey);
            if (authentication == null) {
                log.debug("Creating the Anonymous token");
                final UsernamePasswordAuthenticationToken authRequest =
                        new UsernamePasswordAuthenticationToken(UserInfo.ANONYMOUS, "");
                AuthenticationDetailsSource ads = new HttpAuthenticationDetailsSource();
                authRequest.setDetails(ads.buildDetails(request));
                authentication = context.beanForType(AuthenticationManager.class).authenticate(authRequest);
                if (authentication != null && authentication.isAuthenticated()) {
                    nonUiAuthCache.put(authCacheKey, authentication);
                    log.debug("Added anonymous authentication {} to cache", authentication);
                }
            } else {
                log.debug("Using cached anonymous authentication");
            }
            useAuthentication(request, response, chain, authentication, securityContext);
        } else {
            log.debug("Sending header requiring authentication");
            authenticationEntryPoint.commence(request, response,
                    new InsufficientAuthenticationException("Authentication is required."));
        }
    }

    private void useAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authentication, SecurityContext securityContext) throws IOException, ServletException {
        try {
            securityContext.setAuthentication(authentication);
            chain.doFilter(request, response);
        } finally {
            securityContext.setAuthentication(null);
        }
    }

    private class AuthCacheKey {
        private String hashedHeader;
        private String ip;

        AuthCacheKey(HttpServletRequest request) {
            this(request.getHeader("Authorization"), request.getRemoteAddr());
        }

        private AuthCacheKey(String header, String ip) {
            if (header == null) {
                header = "";
            }
            this.hashedHeader = DigestUtils.shaHex(header);
            this.ip = ip;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AuthCacheKey key = (AuthCacheKey) o;
            return hashedHeader.equals(key.hashedHeader) && ip.equals(key.ip);
        }

        @Override
        public int hashCode() {
            int result = hashedHeader.hashCode();
            result = 31 * result + ip.hashCode();
            return result;
        }
    }

}
