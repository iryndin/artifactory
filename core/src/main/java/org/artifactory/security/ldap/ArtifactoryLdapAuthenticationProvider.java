package org.artifactory.security.ldap;

import org.apache.log4j.Logger;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.UserGroupManager;
import org.artifactory.spring.InternalContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationServiceException;
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

    @Override
    public boolean supports(Class authentication) {
        return authenticator.isLdapActive();
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        String userName = authentication.getName();
        LOGGER.debug(String.format("Trying to authenticate user '%s' via ldap.", userName));
        try {
            authentication = super.authenticate(authentication);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            String message = "Unexpected exception in ldap authentication:";
            LOGGER.error(message, e);
            throw new AuthenticationServiceException(message, e);
        }
        if (!authentication.isAuthenticated()) {
            return authentication;
        }

        // user authenticated via ldap
        LOGGER.debug(String.format("'%s' authenticated successfully by ldap server.", userName));

        ArtifactoryLdapAuthenticationProvider transactionalMe =
                InternalContextHelper.get()
                        .beanForType(ArtifactoryLdapAuthenticationProvider.class);
        SimpleUser user = transactionalMe.findOrCreateUser(userName);

        // create new authentication response containing the user and it's authorities
        UsernamePasswordAuthenticationToken simpleUserAuthentication =
                new UsernamePasswordAuthenticationToken(
                        user, authentication.getCredentials(), user.getAuthorities());
        return simpleUserAuthentication;
    }

    /**
     * Find or create a default user and save to the database. This method is called when a user
     * successfully authenticated via LDAP. If the user doesn't exist in the internal user database
     * it will create it.
     *
     * @param userName The user name to find or create
     * @return A new or found SimpleUser (never null)
     */
    // TODO: Should be transactional, but this bean cannot be CGLIBed!
    private SimpleUser findOrCreateUser(String userName) {
        SimpleUser user;
        try {
            user = userGroupManager.loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
            LOGGER.debug(String.format("Creating new ldap user '%s' ...", userName));
            user = new SimpleUser(userName);
            userGroupManager.createUser(user);
        }
        return user;
    }
}
