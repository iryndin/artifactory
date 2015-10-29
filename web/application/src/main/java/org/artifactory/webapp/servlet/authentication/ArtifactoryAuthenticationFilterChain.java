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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

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
    private boolean serveAll = false;
    private final List<ArtifactoryAuthenticationFilter> chain = new ArrayList<>();

    public boolean isServeAll() {
        return serveAll;
    }

    public void setServeAll(boolean serveAll) {
        this.serveAll = serveAll;
    }

    public List<ArtifactoryAuthenticationFilter> getChain() {
        return chain;
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
                this.chain.add(filter);
            }
        }
        if (beforeLast != null) {
            this.chain.add(beforeLast);
        }
        if (last != null) {
            this.chain.add(last);
        }
    }

    public void addFilter(ArtifactoryAuthenticationFilter filter) {
        this.chain.add(filter);
    }

    public boolean requiresReAuthentication(ServletRequest request, Authentication authentication) {
        for (ArtifactoryAuthenticationFilter filter : chain) {
            if (filter.requiresReAuthentication(request, authentication)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptFilter(ServletRequest request) {
        for (ArtifactoryAuthenticationFilter filter : chain) {
            if (filter.acceptFilter(request)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean acceptEntry(ServletRequest request) {
        for (ArtifactoryAuthenticationFilter filter : chain) {
            if (filter.acceptEntry(request)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getCacheKey(ServletRequest request) {
        String result;
        for (ArtifactoryAuthenticationFilter filter : chain) {
            result = filter.getCacheKey(request);
            if (result != null && result.trim().length() > 0) {
                return result;
            }
        }
        return null;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        for (ArtifactoryAuthenticationFilter filter : chain) {
            filter.init(filterConfig);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // First one that accepts
        for (ArtifactoryAuthenticationFilter filter : this.chain) {
            if (filter.acceptFilter(request)) {
                filter.doFilter(request, response, chain);
                // TODO: May be check that the response was done
                return;
            }
        }
        if (serveAll) {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        for (ArtifactoryAuthenticationFilter filter : chain) {
            filter.destroy();
        }
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        // First one that accepts
        for (ArtifactoryAuthenticationFilter filter : this.chain) {
            if (filter.acceptEntry(request)) {
                filter.commence(request, response, authException);
                // TODO: May be check that the response was done
                return;
            }
        }
        if (serveAll) {
            throw new IllegalStateException(
                    "Could not find any authentication entry point valid for request " + request);
        }
    }
}
