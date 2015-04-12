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

package org.artifactory.rest.common;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.artifactory.addon.rest.AuthorizationRestException;
import org.artifactory.api.rest.constant.HaRestConstants;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.security.HaSystemAuthenticationToken;
import org.artifactory.security.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
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

    @Override
    public ContainerRequest filter(ContainerRequest request) {

        boolean authenticated = authorizationService.isAuthenticated();
        boolean anonAccessEnabled = authorizationService.isAnonAccessEnabled();
        if (!authenticated) {
            if (anonAccessEnabled) {
                //If anon access is allowed and we didn't bother authenticating try to perform the action as a user
                request.setSecurityContext(new RoleAuthenticator(UserInfo.ANONYMOUS, AuthorizationService.ROLE_USER));
            } else {
                throw new AuthorizationRestException();
            }
        } else {
            //Set the authenticated user and role
            String username = authorizationService.currentUsername();
            boolean admin = authorizationService.isAdmin();

            boolean ha = SecurityContextHolder.getContext().getAuthentication() instanceof HaSystemAuthenticationToken;
            if (ha) {
                request.setSecurityContext(new RoleAuthenticator(username, HaRestConstants.ROLE_HA));
                return request;
            }

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
                @Override
                public String getName() {
                    return name;
                }
            };
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            return role.equals(this.role);
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return SecurityContext.BASIC_AUTH;
        }
    }
}
