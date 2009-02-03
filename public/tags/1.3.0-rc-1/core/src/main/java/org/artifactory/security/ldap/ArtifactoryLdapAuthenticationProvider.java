package org.artifactory.security.ldap;

import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.UserGroupManager;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationException;
import org.springframework.security.AuthenticationServiceException;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.providers.ldap.LdapAuthenticationProvider;
import org.springframework.security.userdetails.UsernameNotFoundException;

import java.util.Set;

/**
 * Custom Ldap authentication provider just for creating local users for newly ldap authenticated
 * users.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryLdapAuthenticationProvider extends LdapAuthenticationProvider {
    private static final Logger log =
            LoggerFactory.getLogger(ArtifactoryLdapAuthenticationProvider.class);

    @Autowired
    private UserGroupManager userGroupManager;

    @Autowired
    private UserGroupService userGroupservice;

    private InternalLdapAutenticator authenticator;

    public ArtifactoryLdapAuthenticationProvider(InternalLdapAutenticator authenticator) {
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

        ArtifactoryLdapAuthenticationProvider transactionalMe =
                InternalContextHelper.get().beanForType(ArtifactoryLdapAuthenticationProvider.class);
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
            log.debug(String.format("Creating new ldap user '%s' ...", userName));
            // Creates a new user with invalid password, and user permissions.
            // The created user cannot update its profile.
            UserInfo userInfo = new UserInfo(userName, SimpleUser.INVALID_PASSWORD, "",
                    false, true, false, true, true, true);
            Set<String> defaultGroups = userGroupservice.getNewUserDefaultGroupsNames();
            userInfo.setGroups(defaultGroups);
            user = new SimpleUser(userInfo);
            userGroupManager.createUser(user);
        }
        return user;
    }
}
