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

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.webapp.servlet.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author freds
 */
public class ArtifactoryBasicAuthenticationFilter implements ArtifactoryAuthenticationFilter {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryBasicAuthenticationFilter.class);

    private BasicAuthenticationFilter springBasicAuthenticationFilter;
    private BasicAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        ArtifactoryContext context = RequestUtils.getArtifactoryContext(servletContext);
        springBasicAuthenticationFilter = context.beanForType(BasicAuthenticationFilter.class);
        authenticationEntryPoint = context.beanForType(BasicAuthenticationEntryPoint.class);
        springBasicAuthenticationFilter.init(filterConfig);
    }

    public boolean requiresReAuthentication(ServletRequest request, Authentication authentication) {
        if (acceptFilter(request)) {
            String authUsername = authentication.getPrincipal().toString();
            String username = RequestUtils.extractUsernameFromRequest(request);
            return !username.equals(authUsername);
        }
        return false;
    }

    @Override
    public boolean acceptFilter(ServletRequest request) {
        return RequestUtils.isAuthHeaderPresent((HttpServletRequest) request);
    }

    @Override
    public boolean acceptEntry(ServletRequest request) {
        return !RequestUtils.isUiRequest((HttpServletRequest) request);
    }

    @Override
    public String getCacheKey(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader("Authorization");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        springBasicAuthenticationFilter.doFilter(request, response, chain);
    }

    @Override
    public void destroy() {
        springBasicAuthenticationFilter.destroy();
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        authenticationEntryPoint.commence(request, response, authException);
    }

    /**
     * @param req        The http request
     * @return Login identifier such as user, sessionId, apiKey, etc.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException
     * if the Basic header is not present or is not valid Base64
     */
    @Override
    public String getLoginIdentifier(ServletRequest req) throws BadCredentialsException {
        final HttpServletRequest request = (HttpServletRequest) req;
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Basic ")) {
            // todo: [mp] follow springBasicAuthenticationFilter.doFilter chain resolution
        }
        return extractAndDecodeUser(header);
    }

    /**
     * Decodes the header into a username and password.
     *
     * @throws org.springframework.security.authentication.BadCredentialsException
     * if the Basic header is not present or is not valid Base64
     */
    private String extractAndDecodeUser(String header) throws BadCredentialsException {

        try {
            byte[] base64Token = header.substring(6).getBytes("UTF-8");
            byte[] decoded;
            try {
                decoded = Base64.decode(base64Token);
            } catch (IllegalArgumentException e) {
                throw new BadCredentialsException("Failed to decode basic authentication token");
            }

            String token = new String(decoded, "UTF-8");

            int delim = token.indexOf(":");

            if (delim == -1) {
                throw new BadCredentialsException("Invalid basic authentication token");
            }
            return token.substring(0, delim);
        } catch (UnsupportedEncodingException e) {
            log.debug("Cause: {}", e);
            return "";
        }
    }
}
