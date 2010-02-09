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

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.LdapGroupCoreAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.log.LoggerFactory;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.LdapUtils;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;

/**
 * Custom LDAP authentication provider just for creating local users for newly ldap authenticated users.
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
    public boolean supports(Class<?> authentication) {
        return centralConfig.getDescriptor().getSecurity().isLdapEnabled() && super.supports(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {

        String userName = authentication.getName();
        // If it's an anonymous user, don't bother searching for the user.
        if (UserInfo.ANONYMOUS.equals(userName)) {
            return null;
        }

        log.debug("Trying to authenticate user '{}' via ldap.", userName);
        DirContextOperations dirContextOperations = null;
        try {
            dirContextOperations = authenticator.authenticate(authentication);
            // user authenticated via ldap
            log.debug("'{}' authenticated successfully by ldap server.", userName);

            //Collect internal groups, and if using external groups add them to the user info

            LdapSetting enabledLdapSettings = centralConfig.getDescriptor().getSecurity().getEnabledLdapSettings();
            UserInfo userInfo = userGroupService.findOrCreateExternalAuthUser(
                    userName, !enabledLdapSettings.isAutoCreateUser());

            AddonsManager addonsManager = InternalContextHelper.get().beanForType(AddonsManager.class);
            LdapGroupCoreAddon ldapGroupCoreAddon = addonsManager.addonByType(LdapGroupCoreAddon.class);
            log.debug("Loading LDAP groups");
            ldapGroupCoreAddon.populateGroups(dirContextOperations, userInfo);
            log.debug("Finished Loading LDAP groups");

            SimpleUser user = new SimpleUser(userInfo);
            // create new authentication response containing the user and it's authorities
            UsernamePasswordAuthenticationToken simpleUserAuthentication =
                    new UsernamePasswordAuthenticationToken(user, authentication.getCredentials(),
                            user.getAuthorities());
            return simpleUserAuthentication;
        } catch (AuthenticationException e) {
            String message = String.format("Failed to authenticate user '%s' via LDAP: %s", userName, e.getMessage());
            log.debug(message);
            throw new AuthenticationServiceException(message, e);
        } catch (CommunicationException ce) {
            String message = String.format("Failed to authenticate user '%s' via LDAP: communication error", userName);
            log.warn(message);
            log.debug(message, ce);
            throw new AuthenticationServiceException(message, ce);
        } catch (org.springframework.security.core.AuthenticationException e) {
            String message = String.format("Failed to authenticate user '%s' via LDAP: %s", userName, e.getMessage());
            log.debug(message);
            throw e;
        } catch (Exception e) {
            String message = "Unexpected exception in LDAP authentication:";
            log.error(message, e);
            throw new AuthenticationServiceException(message, e);
        } finally {
            LdapUtils.closeContext(dirContextOperations);
        }
    }
}