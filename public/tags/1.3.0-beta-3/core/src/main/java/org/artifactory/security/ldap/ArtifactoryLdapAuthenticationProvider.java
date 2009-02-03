package org.artifactory.security.ldap;

import org.apache.log4j.Logger;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.UserGroupManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.ldap.LdapAuthenticationProvider;
import org.springframework.security.userdetails.UsernameNotFoundException;

/**
 * Custom Ldap authentication provider just for creating local users for newly ldap authenticated
 * users.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryLdapAuthenticationProvider extends LdapAuthenticationProvider {
    private final static Logger LOGGER =
            Logger.getLogger(ArtifactoryLdapAuthenticationProvider.class);

    @Autowired
    private UserGroupManager userGroupManager;

    private LdapAuthenticatorWrapper authenticator;

    public ArtifactoryLdapAuthenticationProvider(LdapAuthenticatorWrapper authenticator) {
        super(authenticator);
        this.authenticator = authenticator;
    }

    public boolean supports(Class authentication) {
        return authenticator.isLdapActive();
    }

    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        String userName = authentication.getName();
        LOGGER.debug(String.format("Trying to authenticate user '%s' via ldap.", userName));
        authentication = super.authenticate(authentication);
        if (!authentication.isAuthenticated()) {
            return authentication;
        }

        // user authenticated via ldap
        LOGGER.debug(String.format("'%s' authenticated successfully by ldap server.", userName));

        SimpleUser user;
        try {
            user = userGroupManager.loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
            user = createUser(userName);
        }

        // create new authentication response containing the user and it's authorities
        UsernamePasswordAuthenticationToken simpleUserAuthentication =
                new UsernamePasswordAuthenticationToken(
                        user, authentication.getCredentials(), user.getAuthorities());
        return simpleUserAuthentication;
    }

    /**
     * Create a default user and save to the database. This method is called when a user
     * successfully authenticated via ldap but doesn't exist in the internal user database.
     *
     * @param userName The user name for the new user.
     * @return A new SimpleUser
     */
    private SimpleUser createUser(String userName) {
        LOGGER.debug(String.format("Creating new ldap user '%s' ...", userName));
        SimpleUser user = new SimpleUser(userName);
        userGroupManager.createUser(user);
        return user;
    }
}
