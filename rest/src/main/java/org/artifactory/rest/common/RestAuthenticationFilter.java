/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.rest.common;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.httpclient.HttpStatus;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.api.security.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

/**
 * Authorization filter for all the REST requests.
 *
 * @author Fred Simon
 * @author Yossi Shaul
 * @author Yoav Landman
 */
@Component
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class RestAuthenticationFilter implements ContainerRequestFilter {
    @Context
    HttpServletResponse response;

    @Autowired
    AuthorizationService authorizationService;

    public ContainerRequest filter(ContainerRequest request) {

        boolean authenticated = authorizationService.isAuthenticated();
        boolean anonAccessEnabled = authorizationService.isAnonAccessEnabled();
        if (!authenticated) {
            if (anonAccessEnabled) {
                //If anon access is allowed and we didn't bother authenticating try to perform the action as a user
                request.setSecurityContext(new RoleAuthenticator(UserInfo.ANONYMOUS, AuthorizationService.ROLE_USER));
            } else {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Artifactory API\"");
                try {
                    response.sendError(HttpStatus.SC_UNAUTHORIZED);
                } catch (IOException e) {
                    throw new WebApplicationException(HttpStatus.SC_UNAUTHORIZED);
                }
            }
        } else {
            //Set the authenticated user and role
            String username = authorizationService.currentUsername();
            boolean admin = authorizationService.isAdmin();
            if (admin) {
                request.setSecurityContext(new RoleAuthenticator(username, AuthorizationService.ROLE_ADMIN));
            } else {
                request.setSecurityContext(new RoleAuthenticator(username, AuthorizationService.ROLE_USER));
            }
        }
        return request;
    }

    private class RoleAuthenticator implements SecurityContext {
        private final Principal principal;
        private final String role;

        RoleAuthenticator(final String name, String role) {
            this.role = role;
            this.principal = new Principal() {
                public String getName() {
                    return name;
                }
            };
        }

        public Principal getUserPrincipal() {
            return principal;
        }

        public boolean isUserInRole(String role) {
            return role.equals(this.role);
        }

        public boolean isSecure() {
            return false;
        }

        public String getAuthenticationScheme() {
            return SecurityContext.BASIC_AUTH;
        }
    }
}
