/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.servlet;

import org.apache.commons.codec.digest.DigestUtils;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.Cache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.UserInfo;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.HttpAuthenticationDetailsSource;
import org.artifactory.webapp.servlet.authentication.ArtifactoryAuthenticationFilter;
import org.artifactory.webapp.servlet.authentication.ArtifactoryAuthenticationFilterChain;
import org.slf4j.Logger;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

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

public class AccessFilter extends DelayedFilterBase {
    private static final Logger log = LoggerFactory.getLogger(AccessFilter.class);

    private ArtifactoryContext context;
    private ArtifactoryAuthenticationFilter authFilter;

    /**
     * holds cached Authentication instances for the non ui requests based on the Authorization header and client ip
     */
    private Cache<AuthCacheKey, Authentication> nonUiAuthCache;

    @Override
    public void initLater(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.context = RequestUtils.getArtifactoryContext(servletContext);
        ArtifactoryAuthenticationFilterChain filterChain = new ArtifactoryAuthenticationFilterChain();
        filterChain.setServeAll(true);
        //Add all the authentication filters
        //TODO: [by yl] Support ordering...
        filterChain.addFilters(context.beansForType(ArtifactoryAuthenticationFilter.class).values());
        authFilter = filterChain;
        initCaches();
        String usePathInfo = filterConfig.getInitParameter("usePathInfo");
        if (usePathInfo != null) {
            RequestUtils.setUsePathInfo(Boolean.parseBoolean(usePathInfo));
        }
        authFilter.init(filterConfig);

    }

    @SuppressWarnings({"unchecked"})
    private void initCaches() {
        CacheService cacheService = context.beanForType(CacheService.class);
        nonUiAuthCache = cacheService.getCache(ArtifactoryCache.authentication);
    }

    public void destroy() {
        //May not be inited yet
        if (authFilter != null) {
            authFilter.destroy();
        }
    }

    public void doFilter(final ServletRequest req, final ServletResponse resp, final FilterChain chain)
            throws IOException, ServletException {
        doFilterInternal((HttpServletRequest) req, ((HttpServletResponse) resp), chain);
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
        //Reuse the authentication if it exists
        Authentication authentication = RequestUtils.getAuthentication(request);
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        boolean reauthenticationRequired = isAuthenticated && authFilter.requiresReauthentication(request,
                authentication);
        if (reauthenticationRequired) {
            /**
             * A re-authentication is required but we might still have data that needs to be invalidated (like the
             * Wicket session)
             */
            Map<String, LogoutHandler> logoutHandlers = ContextHelper.get().beansForType(LogoutHandler.class);
            for (LogoutHandler logoutHandler : logoutHandlers.values()) {
                logoutHandler.logout(request, response, authentication);
            }
        }
        boolean authenticationRequired = !isAuthenticated || reauthenticationRequired;
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (authenticationRequired) {
            if (authFilter.acceptFilter(request)) {
                authenticateAndExecute(request, response, chain, securityContext);
            } else {
                useAnonymousIfPossible(request, response, chain, securityContext);
            }
        } else {
            log.debug("Using authentication {} from Http session.", authentication);
            useAuthentication(request, response, chain, authentication, securityContext);
        }
    }


    private void authenticateAndExecute(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, SecurityContext securityContext) throws IOException, ServletException {
        // Try to see if authentication in cache based on the hashed header and client ip
        AuthCacheKey authCacheKey = new AuthCacheKey(authFilter.getCacheKey(request), request.getRemoteAddr());
        Authentication authentication = nonUiAuthCache.get(authCacheKey);
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        if (isAuthenticated && !authFilter.requiresReauthentication(request, authentication)) {
            log.debug("Header authentication {} found in cache.", authentication);
            useAuthentication(request, response, chain, authentication, securityContext);
            return;
        }
        try {
            authFilter.doFilter(request, response, chain);
        } finally {
            Authentication newAuthentication = securityContext.getAuthentication();
            if (newAuthentication != null && newAuthentication.isAuthenticated()) {
                // Save authentication like in Wicket Session (if session exists)
                if (RequestUtils.setAuthentication(request, newAuthentication, false)) {
                    log.debug("Added authentication {} in Http session.", newAuthentication);
                } else {
                    // If it did not work use the header cache
                    // An authorization cache key with no header can only be used for Anonymous authentication
                    if ((UserInfo.ANONYMOUS.equals(newAuthentication.getName()) && authCacheKey.hasEmptyHeader()) ||
                            (!UserInfo.ANONYMOUS.equals(newAuthentication.getName()) &&
                                    !authCacheKey.hasEmptyHeader())) {
                        nonUiAuthCache.put(authCacheKey, newAuthentication);
                        log.debug("Added authentication {} in cache.", newAuthentication);
                    }
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
                // explicitly ask for the default spring authentication manager by name (we have another one which
                // is only used by the basic authentication filter)
                AuthenticationManager authenticationManager =
                        context.beanForType("authenticationManager", AuthenticationManager.class);
                authentication = authenticationManager.authenticate(authRequest);
                if (authentication != null && authentication.isAuthenticated()) {
                    nonUiAuthCache.put(authCacheKey, authentication);
                    log.debug("Added anonymous authentication {} to cache", authentication);
                }
            } else {
                log.debug("Using cached anonymous authentication");
            }
            useAuthentication(request, response, chain, authentication, securityContext);
        } else {
            if (authFilter.acceptEntry(request)) {
                log.debug("Sending request requiring authentication");
                authFilter.commence(request, response,
                        new InsufficientAuthenticationException("Authentication is required."));
            } else {
                log.debug("No filter or entry just chain");
                chain.doFilter(request, response);
            }
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

    private static class AuthCacheKey {
        private static final String EMPTY_HEADER = DigestUtils.shaHex("");

        private final String hashedHeader;
        private final String ip;

        private AuthCacheKey(String header, String ip) {
            if (header == null) {
                this.hashedHeader = EMPTY_HEADER;
            } else {
                this.hashedHeader = DigestUtils.shaHex(header);
            }
            this.ip = ip;
        }

        public boolean hasEmptyHeader() {
            return this.hashedHeader.equals(EMPTY_HEADER);
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