/*
 * This file is part of Artifactory.
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

import org.apache.commons.codec.binary.Base64;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.servlet.RequestUtils;
import org.slf4j.Logger;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.ui.basicauth.BasicProcessingFilter;
import org.springframework.security.ui.basicauth.BasicProcessingFilterEntryPoint;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * @author freds
 * @date Mar 10, 2009
 */
public class BasicArtifactoryAuthenticationFilter implements ArtifactoryAuthenticationFilter {
    private ArtifactoryContext context;
    private BasicProcessingFilter authFilter;
    private BasicProcessingFilterEntryPoint authenticationEntryPoint;
    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final Logger log = LoggerFactory.getLogger(BasicArtifactoryAuthenticationFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        this.context = RequestUtils.getArtifactoryContext(servletContext);
        authFilter = context.beanForType(BasicProcessingFilter.class);
        authenticationEntryPoint = context.beanForType(BasicProcessingFilterEntryPoint.class);
        authFilter.init(filterConfig);
    }

    public boolean validAuthentication(ServletRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (acceptFilter(request)) {
            String authUsername = authentication.getPrincipal().toString();
            String header = ((HttpServletRequest) request).getHeader("Authorization");
            String token = null;
            if ((header != null) && header.startsWith("Basic ")) {
                byte[] base64Token;
                try {
                    base64Token = header.substring(6).getBytes(DEFAULT_ENCODING);
                    token = new String(Base64.decodeBase64(base64Token), DEFAULT_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    log.info("the encoding is not supported");
                }
                String username = "";
                int delim = token.indexOf(":");
                if (delim != -1) {
                    username = token.substring(0, delim);
                }
                return username.equals(authUsername);
            }
        }
        return true;
    }

    public boolean acceptFilter(ServletRequest request) {
        return RequestUtils.isAuthHeaderPresent((HttpServletRequest) request);
    }

    public boolean acceptEntry(ServletRequest request) {
        return !RequestUtils.isUiRequest((HttpServletRequest) request);
    }

    public String getCacheKey(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader("Authorization");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        authFilter.doFilter(request, response, chain);
    }

    public void destroy() {
        authFilter.destroy();
    }

    public void commence(ServletRequest request, ServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        authenticationEntryPoint.commence(request, response, authException);
    }
}
