package org.artifactory.security.ldap;

import org.artifactory.api.common.StatusHolder;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.AuthenticationException;
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
    private final static Logger log = LoggerFactory.getLogger(LdapConnectionTester.class);

    /**
     * Tries to connect to an ldap server and returns the result in the status holder.
     * The message in the status holder is meant to be displayed to the user.
     * @param ldapSetting   The information for the ldap connection
     * @param username      The username that will be used to test the connection
     * @param password      The password that will be used to test the connection
     * @return StatusHolder with the connection attempt results. 
     */
    public StatusHolder testLdapConnection(LdapSetting ldapSetting, String username,
            String password) {
        StatusHolder status = new StatusHolder();
        try {
            SpringSecurityContextSource securityContext =
                    LdapAuthenticatorWrapper.createSecurityContext(ldapSetting);
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
            status.setError("Servel failed to parse the request: " +
                    ((NameNotFoundException) e).getMostSpecificCause().getMessage(), e, log);
        } else if (e instanceof InvalidNameException) {
            status.setError("Servel failed to parse the request: " +
                    ((InvalidNameException) e).getMostSpecificCause().getMessage(), e, log);
        } else if (e instanceof AuthenticationException) {
            status.setError("Authentication failed. " +
                    "Probably a wrong manager dn or manager password", e, log);
        } else if (e instanceof BadCredentialsException) {
            status.setError("Failed to authenticate user " + username, e, log);
        } else {
            String message = "Error connecting to the LDAP server";
            if (StringUtils.hasText(e.getMessage())) {
                message += ": " + e.getMessage();
            }
            status.setError(message, e, log);
        }
    }

}
