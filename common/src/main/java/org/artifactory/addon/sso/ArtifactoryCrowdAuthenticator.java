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

package org.artifactory.addon.sso;

import org.artifactory.addon.Addon;
import org.springframework.security.core.Authentication;

/**
 * Allows the SSO addon to behave as a proxy for Crowd authentication.<br>
 * These methods cannot appear in the normal addon interface since they need to be used by components in the core
 *
 * @author Noam Y. Tenne
 */
public interface ArtifactoryCrowdAuthenticator extends Addon {

    /**
     * Indicates whether crowd authentication is supported\enabled
     *
     * @param authentication Authentication object for the provider
     * @return True if crowd authentication is supported\enabled
     */
    boolean isCrowdAuthenticationSupported(Class<? extends Object> authentication);

    /**
     * Authenticates the request via Crowd
     *
     * @param authentication Authentication to use
     * @return New token with local user details and credentials
     */
    Authentication authenticateCrowd(Authentication authentication);
}
