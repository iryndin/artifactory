/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.webapp.servlet.authentication;

import com.google.common.base.Strings;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.plugin.PluginsAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.SecurityService;
import org.artifactory.util.HttpUtils;
import org.artifactory.webapp.servlet.HttpArtifactoryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author freds
 * @date Mar 10, 2009
 */
public class ArtifactoryAuthenticationFilterChain implements ArtifactoryAuthenticationFilter {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryAuthenticationFilterChain.class);

    SecurityService securityService;

    private final List<ArtifactoryAuthenticationFilter> authenticationFilters = new ArrayList<>();
    public List<ArtifactoryAuthenticationFilter> getAuthenticationFilters() {
        return authenticationFilters;
    }

    public void addFilters(Collection<ArtifactoryAuthenticationFilter> filters) {
        ArtifactoryAuthenticationFilter beforeLast = null;
        ArtifactoryAuthenticationFilter last = null;
        for (ArtifactoryAuthenticationFilter filter : filters) {
            if (filter instanceof ArtifactoryBasicAuthenticationFilter) {
                //TODO: [by YS] Not sure the comment below is true. All basic authentications are done by the same filter
                //HACK! ArtifactoryBasicAuthenticationFilter should always be last so it doesn't handle basic auth intended
                //for other sso filters
                last = filter;
            } else if (filter.getClass().getName().endsWith("CasAuthenticationFilter")) {
                // Other Hack! The CAS should be after other SSO filter
                beforeLast = filter;
            } else {
                this.authenticationFilters.add(filter);
            }
        }
        if (beforeLast != null) {
            this.authenticationFilters.add(beforeLast);
        }
        if (last != null) {
            this.authenticationFilters.add(last);
        }
    }

    public void addFilter(ArtifactoryAuthenticationFilter filter) {
        this.authenticationFilters.add(filter);
    }

    public boolean requiresReAuthentication(ServletRequest request, Authentication authentication) {
        for (ArtifactoryAuthenticationFilter filter : authenticationFilters) {
            if (filter.requiresReAuthentication(request, authentication)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptFilter(ServletRequest request) {
        for (ArtifactoryAuthenticationFilter filter : authenticationFilters) {
            if (filter.acceptFilter(request)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptEntry(ServletRequest request) {
        for (ArtifactoryAuthenticationFilter filter : authenticationFilters) {
            if (filter.acceptEntry(request)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getCacheKey(ServletRequest request) {
        String result;
        for (ArtifactoryAuthenticationFilter filter : authenticationFilters) {
            result = filter.getCacheKey(request);
            if (result != null && result.trim().length() > 0) {
                return result;
            }
        }
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        for (ArtifactoryAuthenticationFilter filter : authenticationFilters) {
            filter.init(filterConfig);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, final FilterChain servletChain)
            throws IOException, ServletException {
        FilterChain chainWithAdditive = (request, response) -> {
            try {
                AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
                addonsManager.addonByType(PluginsAddon.class)
                        .executeAdditiveRealmPlugins(new HttpArtifactoryRequest((HttpServletRequest) request));
                servletChain.doFilter(request, response);
            } catch (AuthenticationException e) {
                ContextHelper.get().beanForType(BasicAuthenticationEntryPoint.class).commence(
                        (HttpServletRequest) request, (HttpServletResponse) response, e);
            }
        };

        // First one that accepts
        for (ArtifactoryAuthenticationFilter filter : this.authenticationFilters) {
            if (filter.acceptFilter(servletRequest)) {
                final HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
                final HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;
                long accessTime = HttpUtils.getSessionAccessTime(httpServletRequest);
                String remoteClientAddress = HttpUtils.getRemoteClientAddress((HttpServletRequest) servletRequest);

                try {
                    String loginIdentifier = getLoginIdentifier(servletRequest, filter);

                    if (loginIdentifier == null) {
                        log.debug("Login identifier was not resolved");
                        filter.doFilter(servletRequest, servletResponse, chainWithAdditive);
                    } else {
                        if(Strings.isNullOrEmpty(loginIdentifier)) {
                            // makes sure that session is not locked
                            getSecurityService().ensureSessionIsNotLocked(loginIdentifier);

                            // delay session if applicable
                            getSecurityService().ensureSessionShouldNotBeDelayed(loginIdentifier);
                        } else {
                            // makes sure that user is not locked
                            getSecurityService().ensureUserIsNotLocked(loginIdentifier);

                            // delay login if applicable
                            getSecurityService().ensureLoginShouldNotBeDelayed(loginIdentifier);
                        }

                        // memorise user last access time
                        getSecurityService().updateUserLastAccess(loginIdentifier, remoteClientAddress, accessTime);

                        filter.doFilter(servletRequest, servletResponse, chainWithAdditive);

                        final HttpServletResponse response = (HttpServletResponse) servletResponse;

                        if (response.getStatus()  == 401 || response.getStatus()  == 403) {
                            log.debug("Filter responded with code {}, registering authentication failure!", response.getStatus());
                            // register incorrect login attempt
                            getSecurityService().interceptLoginFailure(loginIdentifier, accessTime);
                        } else if (response.getStatus() < 400 && response.getStatus() >= 200) {
                            log.debug("Filter responded with code {}, registering authentication success!", response.getStatus());
                            // intercept successful login
                            getSecurityService().interceptLoginSuccess(loginIdentifier);
                        } else {
                            log.debug("Filter responded with code {}, skipping result interception", response.getStatus());
                        }
                    }
                } catch (AuthenticationException e) {
                    log.debug("User authentication has failed, {}", e);
                    commence(httpServletRequest, httpServletResponse, e);
                }
                return;
            }
        }
    }

    /**
     * @param servletRequest
     * @param filter
     * @return login identifier
     */
    private String getLoginIdentifier(ServletRequest servletRequest, ArtifactoryAuthenticationFilter filter) {
        String loginIdentifier = filter.getCacheKey(servletRequest);
        try {
            // fetch context LoginIdentifier
            loginIdentifier = filter.getLoginIdentifier(servletRequest);
        } catch (BadCredentialsException e) {
            log.debug("Resolving uses access details has failed, {}", e.getMessage());
            if(loginIdentifier == null)
                loginIdentifier = "";
        }
        return loginIdentifier;
    }

    @Override
    public void destroy() {
        for (ArtifactoryAuthenticationFilter filter : authenticationFilters) {
            filter.destroy();
        }
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        // First one that accepts
        for (ArtifactoryAuthenticationFilter filter : this.authenticationFilters) {
            if (filter.acceptEntry(request)) {
                filter.commence(request, response, authException);
                // TODO: May be check that the response was done
                return;
            }
        }
    }

    /**
     * Searches filter that capable of serve request
     * and returns request LoginIdentifier
     *
     * @param request        The http request
     * @return Login identifier such as user, sessionId, apiKey, etc.
     */
    @Override
    public String getLoginIdentifier(ServletRequest request) {
        for (ArtifactoryAuthenticationFilter filter : this.authenticationFilters) {
            if (filter.acceptEntry(request)) {
                return filter.getLoginIdentifier(request);
            }
        }
        return "";
    }

    private SecurityService getSecurityService() {
        if (securityService == null)
            securityService = ContextHelper.get().beanForType(SecurityService.class);
        return securityService;
    }
}
