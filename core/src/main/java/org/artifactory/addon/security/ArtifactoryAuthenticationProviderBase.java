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

package org.artifactory.addon.security;

import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.UserGroupManager;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

/**
 * Base class for custom authentication provider just for creating local users for newly authenticated users.
 *
 * @author Fred Simon
 */
public abstract class ArtifactoryAuthenticationProviderBase implements AuthenticationProvider {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryAuthenticationProviderBase.class);

    @Autowired
    private UserGroupManager userGroupManager;

    @Autowired
    private UserGroupService userGroupservice;

    public boolean supports(Class authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    protected abstract String getProviderName();

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName = authentication.getName();
        log.debug("Trying to authenticate user '{}' via {}", userName, getProviderName());
        try {
            authentication = doInternalAuthenticate(authentication);
        } catch (AuthenticationException e) {
            log.debug("Failed to authenticate user {} via {}: {}",
                    new Object[]{userName, getProviderName(), e.getMessage()});
            throw e;
        } catch (Exception e) {
            String message = "Unexpected exception in " + getProviderName() + " authentication:";
            log.error(message, e);
            throw new AuthenticationServiceException(message, e);
        }
        if (!authentication.isAuthenticated()) {
            return authentication;
        }

        // user authenticated via ldap
        log.debug("'{}' authenticated successfully by {}.", userName, getProviderName());

        SimpleUser user = findOrCreateUser(userName);

        // create new authentication response containing the user and it's authorities
        UsernamePasswordAuthenticationToken simpleUserAuthentication =
                new UsernamePasswordAuthenticationToken(user, authentication.getCredentials(), user.getAuthorities());
        return simpleUserAuthentication;
    }

    protected abstract Authentication doInternalAuthenticate(Authentication authentication);

    /**
     * Find or create a default user and save to the database. This method is called when a user successfully
     * authenticated via LDAP. If the user doesn't exist in the internal user database it will create it.
     *
     * @param userName The user name to find or create
     * @return A new or found SimpleUser (never null)
     */
    protected SimpleUser findOrCreateUser(String userName) {
        SimpleUser user;
        try {
            user = userGroupManager.loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
            log.debug(String.format("Creating new user '%s' for %s...", userName, getProviderName()));
            // Creates a new user with invalid password, and user permissions.
            // The created user cannot update its profile.
            UserInfoBuilder builder = new UserInfoBuilder(userName);
            Set<String> defaultGroups = userGroupservice.getNewUserDefaultGroupsNames();
            UserInfo userInfo = builder.internalGroups(defaultGroups).build();
            user = new SimpleUser(userInfo);
            userGroupManager.createUser(user);
        }
        return user;
    }
}