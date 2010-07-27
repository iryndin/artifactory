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

package org.artifactory.security.ldap;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.BadLdapGrammarException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.InvalidNameException;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;

import java.util.HashMap;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @author Tomer Cohen
 */
public class AbstractLdapService {
    private static final Logger log = LoggerFactory.getLogger(AbstractLdapService.class);


    protected void handleException(Exception e, StatusHolder status, String username, boolean isSearchAndBindActive) {
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
            if (isSearchAndBindActive) {
                status.setWarning("LDAP authentication failed for " + username +
                        ". Note: you have configured direct user binding " +
                        "and manager-based search, which are usually mutually exclusive. For AD leave the User DN " +
                        "Pattern field empty.", e, log);
            } else {
                status.setError("Authentication failed. Probably a wrong manager dn or manager password", e,
                        log);
            }
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

    /**
     * Create LDAP template to be used for performing LDAP queries.
     *
     * @param settings The LDAP settings with which to create the LDAP template.
     * @return The LDAP template.
     */
    public LdapTemplate createLdapTemplate(LdapSetting settings) {
        return new LdapTemplate(getLdapContext(settings));
    }

    private LdapContextSource getLdapContext(LdapSetting ldapSettings) {
        LdapContextSource ldapContextSource = new LdapContextSource();
        HashMap<String, String> env = new HashMap<String, String>();
        env.put("com.sun.jndi.ldap.connect.timeout", "10000");
        ldapContextSource.setBaseEnvironmentProperties(env);
        ldapContextSource.setUrl(ldapSettings.getLdapUrl());
        SearchPattern searchPattern = ldapSettings.getSearch();
        String managerDn = null;
        String managerPassword = null;
        if (searchPattern != null) {
            managerDn = searchPattern.getManagerDn();
            managerPassword = searchPattern.getManagerPassword();
        }
        if (isBlank(managerDn) || isBlank(managerPassword)) {
            log.debug("Manager credentials not set, trying anonymous query");
            ldapContextSource.setAnonymousReadOnly(true);
        } else {
            ldapContextSource.setUserDn(managerDn);
            ldapContextSource.setPassword(managerPassword);
        }
        try {
            ldapContextSource.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to init ldap context", e);
        }
        return ldapContextSource;
    }
}
