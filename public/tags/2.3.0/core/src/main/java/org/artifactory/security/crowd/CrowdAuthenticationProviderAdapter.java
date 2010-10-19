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

package org.artifactory.security.crowd;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.sso.ArtifactoryCrowdAuthenticator;
import org.artifactory.api.context.ContextHelper;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * An authentication provider adapter that delegates the calls to the SSO addon.<br>
 * Needed since a provider cannot be added to the spring authentication manager while in an addon library.
 *
 * @author Noam Y. Tenne
 */
public class CrowdAuthenticationProviderAdapter implements AuthenticationProvider {

    private AddonsManager addonsManager;

    public CrowdAuthenticationProviderAdapter() {
        addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
    }

    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return addonsManager.addonByType(ArtifactoryCrowdAuthenticator.class).authenticateCrowd(authentication);
    }

    public boolean supports(Class<? extends Object> authentication) {
        return addonsManager.addonByType(ArtifactoryCrowdAuthenticator.class).isCrowdAuthenticationSupported(
                authentication);
    }
}
