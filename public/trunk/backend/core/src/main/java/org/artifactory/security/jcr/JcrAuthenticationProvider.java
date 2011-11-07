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

package org.artifactory.security.jcr;

import org.artifactory.log.LoggerFactory;
import org.artifactory.security.InternalRealmAwareAuthentication;
import org.artifactory.security.RealmAwareAuthenticationProvider;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.security.UserGroupManager;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Set;

/**
 * @author Yoav Landman
 */
public class JcrAuthenticationProvider extends DaoAuthenticationProvider implements RealmAwareAuthenticationProvider {
    private static final Logger log = LoggerFactory.getLogger(JcrAuthenticationProvider.class);

    public static final String INTERNAL_REALM = "internal";

    @Autowired
    private UserGroupManager userGroupManager;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication authenticate = super.authenticate(authentication);
        return new InternalRealmAwareAuthentication(authenticate.getPrincipal(), authenticate.getCredentials(),
                authenticate.getAuthorities());
    }

    public String getRealm() {
        return INTERNAL_REALM;
    }

    public void addExternalGroups(String username, Set<UserGroupInfo> groups) {
        log.debug("User '{}' is an internal user that belongs to the following groups '{}'", username, groups);
        // nop
    }

    public JcrAuthenticationProvider() {
        setPasswordEncoder(new Md5PasswordEncoder());
    }

    public boolean userExists(String username) {
        return userGroupManager.userExists(username);
    }
}