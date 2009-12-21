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

import org.artifactory.api.common.StatusHolder;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.BadLdapGrammarException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.InvalidNameException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.ldap.SpringSecurityContextSource;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;

/**
 * This class tests an ldap connection given a ldap settings.
 *
 * @author Yossi Shaul
 */
public class LdapConnectionTester {
    private static final Logger log = LoggerFactory.getLogger(LdapConnectionTester.class);

    /**
     * Tries to connect to an ldap server and returns the result in the status holder. The message in the status holder
     * is meant to be displayed to the user.
     *
     * @param ldapSetting The information for the ldap connection
     * @param username    The username that will be used to test the connection
     * @param password    The password that will be used to test the connection
     * @return StatusHolder with the connection attempt results.
     */
    public StatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
        StatusHolder status = new StatusHolder();
        try {
            SpringSecurityContextSource securityContext =
                    ArtifactoryLdapAuthenticator.createSecurityContext(ldapSetting);
            ArtifactoryBindAuthenticator authenticator = new ArtifactoryBindAuthenticator(
                    securityContext, ldapSetting);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, password);
            authenticator.authenticate(authentication);
            status.setStatus("Successfully connected and authenticated the test user", log);
        } catch (Exception e) {
            handleException(e, status, username);
        }
        return status;
    }

    private void handleException(Exception e, StatusHolder status, String username) {
        log.debug("LDAP connection test failed with exception", e);
        if (e instanceof CommunicationException) {
            status.setError("Failed connecting to the server (probably wrong url or port)", e, log);
        } else if (e instanceof NameNotFoundException) {
            status.setError("Server failed to parse the request: " +
                    ((NameNotFoundException) e).getMostSpecificCause().getMessage(), e, log);
        } else if (e instanceof InvalidNameException) {
            status.setError("Server failed to parse the request: " +
                    ((InvalidNameException) e).getMostSpecificCause().getMessage(), e, log);
        } else if (e instanceof AuthenticationException) {
            status.setError("Authentication failed. Probably a wrong manager dn or manager password", e, log);
        } else if (e instanceof BadCredentialsException) {
            status.setError("Failed to authenticate user " + username, e, log);
        } else if (e instanceof BadLdapGrammarException) {
            status.setError("Failed to parse R\\DN", e, log);
        } else {
            String message = "Error connecting to the LDAP server: ";
            if (StringUtils.hasText(e.getMessage())) {
                message += ": " + e.getMessage();
            }
            status.setError(message, e, log);
        }
    }

}
