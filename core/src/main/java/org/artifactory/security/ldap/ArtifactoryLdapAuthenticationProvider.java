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

package org.artifactory.security.ldap;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.SimpleUser;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationServiceException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.ldap.LdapAuthenticationProvider;
import org.springframework.security.providers.ldap.LdapAuthenticator;

/**
 * Custom Ldap authentication provider just for creating local users for newly ldap authenticated users.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryLdapAuthenticationProvider extends LdapAuthenticationProvider {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryLdapAuthenticationProvider.class);

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private CentralConfigService centralConfig;

    private LdapAuthenticator authenticator;


    @Autowired
    public ArtifactoryLdapAuthenticationProvider(LdapAuthenticator authenticator) {
        super(authenticator);
        this.authenticator = authenticator;
    }

    @Override
    public boolean supports(Class authentication) {
        return centralConfig.getDescriptor().getSecurity().getEnabledLdapSettings() != null &&
                super.supports(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName = authentication.getName();
        log.debug("Trying to authenticate user '{}' via ldap.", userName);
        try {
            authentication = super.authenticate(authentication);
        } catch (AuthenticationException e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to authenticate user {} via ldap: {}", userName, e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            String message = "Unexpected exception in ldap authentication:";
            log.error(message, e);
            throw new AuthenticationServiceException(message, e);
        }
        if (!authentication.isAuthenticated()) {
            return authentication;
        }

        // user authenticated via ldap
        log.debug("'{}' authenticated successfully by ldap server.", userName);

        SimpleUser user = new SimpleUser(userGroupService.findOrCreateExternalAuthUser(userName));
        // create new authentication response containing the user and it's authorities
        UsernamePasswordAuthenticationToken simpleUserAuthentication =
                new UsernamePasswordAuthenticationToken(user, authentication.getCredentials(), user.getAuthorities());
        return simpleUserAuthentication;
    }
}