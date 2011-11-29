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

package org.artifactory.security.crowd;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.sso.CrowdAddon;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.security.AnonymousAuthenticationToken;
import org.artifactory.security.RealmAwareAuthenticationProvider;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.security.UserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Set;

/**
 * An authentication provider adapter that delegates the calls to the SSO addon.<br> Needed since a provider cannot be
 * added to the spring authentication manager while in an addon library.
 *
 * @author Noam Y. Tenne
 */
public class CrowdAuthenticationProviderAdapter implements RealmAwareAuthenticationProvider {

    private AddonsManager addonsManager;

    public CrowdAuthenticationProviderAdapter() {
        addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName = authentication.getName();
        // If it's an anonymous user, don't bother searching for the user.
        if (UserInfo.ANONYMOUS.equals(userName)) {
            return null;
        }
        return addonsManager.addonByType(CrowdAddon.class).authenticateCrowd(authentication);
    }

    public boolean supports(Class<?> authentication) {
        return addonsManager.addonByType(CrowdAddon.class).isCrowdAuthenticationSupported(authentication);
    }

    public String getRealm() {
        return CrowdAddon.REALM;
    }

    public void addExternalGroups(String username, Set<UserGroupInfo> groups) {
        addonsManager.addonByType(CrowdAddon.class).addExternalGroups(username, groups);
    }

    public boolean userExists(String username) {
        return addonsManager.addonByType(CrowdAddon.class).findUser(username);
    }
}
