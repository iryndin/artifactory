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

package org.artifactory.security.db.apikey;

import org.artifactory.security.RealmAwareAuthenticationProvider;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.security.props.auth.BadPropsAuthException;
import org.artifactory.security.props.auth.PropsAuthNotFoundException;
import org.artifactory.security.props.auth.PropsAuthenticationToken;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.Set;

/**
 * @author Chen Keinan
 */
public class PropsAuthenticationProvider implements RealmAwareAuthenticationProvider {
    private static final Logger log = LoggerFactory.getLogger(PropsAuthenticationProvider.class);

    @Autowired
    private UserGroupStoreService userGroupStore;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(PropsAuthenticationToken.class,
                authentication, "Only Props Authentication Token is supported");
        // Determine props key and value
        String propsKey = (authentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();
        String propsValue = (String) authentication.getCredentials();
        UserInfo user;
        try {
            user = userGroupStore.findUserByProperty(propsKey, propsValue);
        } catch (PropsAuthNotFoundException notFound) {
            log.debug("{} : {} not found", propsKey, propsValue);
            throw new BadPropsAuthException("Bad Props auth Key:" + propsKey);

        }
        if (user == null) {
            log.debug("{} : {} not found", propsKey, propsValue);
            throw new BadPropsAuthException("Bad Props auth Key:" + propsKey);
        }
        return createSuccessAuthentication(authentication, new SimpleUser(user));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return true;
    }

    protected Authentication createSuccessAuthentication(Authentication authentication,
                                                         UserDetails user) {
        PropsAuthenticationToken result = new PropsAuthenticationToken(user,
                user.getPassword(), new NullAuthoritiesMapper().mapAuthorities(user.getAuthorities()));

        result.setDetails(authentication.getDetails());
        result.setAuthenticated(true);
        return result;
    }

    @Override
    public void addExternalGroups(String username, Set<UserGroupInfo> groups) {
        // not require
    }

    @Override
    public boolean userExists(String username) {
        return userGroupStore.userExists(username);
    }

    @Override
    public String getRealm() {
        return null;
    }
}