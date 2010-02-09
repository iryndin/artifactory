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

import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple demo of a custom authentication provider.
 *
 * @author Fred Simon
 */
public class ArtifactoryCustomAuthenticationProvider extends ArtifactoryAuthenticationProviderBase {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryCustomAuthenticationProvider.class);

    private static Map<String, String> SIMPLE_USERS = new HashMap<String, String>(2) {{
        put("joe", "cocker");
        put("scott", "tiger");
    }};

    protected String getProviderName() {
        return "SimpleUsers";
    }

    protected Authentication doInternalAuthenticate(Authentication authentication) {
        // All your user authentication needs
        String principal = (String) authentication.getPrincipal();
        String password = (String) authentication.getCredentials();
        if (SIMPLE_USERS.containsKey(principal) && SIMPLE_USERS.get(principal).equals(password)) {
            return new UsernamePasswordAuthenticationToken(principal,
                    password,
                    authentication.getAuthorities());
        }
        throw new BadCredentialsException("Username/Password does not match for " + principal);
    }
}