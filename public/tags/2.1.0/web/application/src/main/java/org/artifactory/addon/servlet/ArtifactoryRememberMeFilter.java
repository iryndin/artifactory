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

package org.artifactory.addon.servlet;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.webapp.servlet.RequestUtils;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import static org.springframework.security.ui.rememberme.AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY;
import org.springframework.security.ui.rememberme.RememberMeProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Eli Givoni
 */
public class ArtifactoryRememberMeFilter implements ArtifactoryAuthenticationFilter {

    private RememberMeProcessingFilter rememberMeDelegateFilter;

    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        ArtifactoryContext context = RequestUtils.getArtifactoryContext(servletContext);
        rememberMeDelegateFilter = context.beanForType(RememberMeProcessingFilter.class);
        rememberMeDelegateFilter.init(filterConfig);
    }

    public boolean validAuthentication(ServletRequest request, Authentication authentication) {
        return !(authentication == null || !authentication.isAuthenticated());
    }

    public boolean acceptFilter(ServletRequest request) {
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();

        if ((cookies == null) || (cookies.length == 0)) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if (SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean acceptEntry(ServletRequest request) {
        return false;
    }

    public String getCacheKey(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader("Authorization");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        rememberMeDelegateFilter.doFilter(request, response, chain);
    }

    public void destroy() {
        rememberMeDelegateFilter.destroy();
    }

    public void commence(ServletRequest request, ServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        throw new UnsupportedOperationException("This filter doesn't support commencing");
    }
}
