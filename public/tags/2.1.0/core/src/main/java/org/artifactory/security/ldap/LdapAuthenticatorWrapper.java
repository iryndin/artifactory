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
import org.artifactory.config.InternalCentralConfigService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.ldap.SearchPattern;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.util.PathUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationServiceException;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityContextSource;
import org.springframework.security.providers.ldap.authenticator.BindAuthenticator;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Wrapper for the LDAP bind authenticator. Used to authenticate users against ldap and as a factory for the security
 * context and actual authenticator.
 *
 * @author Yossi Shaul
 */
public class LdapAuthenticatorWrapper implements InternalLdapAutenticator {
    private static final Logger log = LoggerFactory.getLogger(LdapAuthenticatorWrapper.class);

    private static final String NO_LDAP_SERVICE_CONFIGURED = "No ldap service configured";

    @Autowired
    private CentralConfigService centralConfig;

    private List<BindAuthenticator> authenticators = new ArrayList<BindAuthenticator>();

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(InternalLdapAutenticator.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalCentralConfigService.class};
    }

    public void init() {
        if (isLdapActive()) {
            authenticators = createBindAuthenticators();
        } else {
            log.debug("Ldap service is disabled");
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        authenticators.clear();
        init();
    }

    public void destroy() {

    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    private List<BindAuthenticator> createBindAuthenticators() {
        ArrayList<BindAuthenticator> authenticators = new ArrayList<BindAuthenticator>();
        List<LdapSetting> ldapSettings = getLdapSettings();
        for (LdapSetting ldapSetting : ldapSettings) {
            if (ldapSetting.isEnabled()) {
                SpringSecurityContextSource contextSource = createSecurityContext(ldapSetting);
                ArtifactoryBindAuthenticator bindAuthenticator =
                        new ArtifactoryBindAuthenticator(contextSource, ldapSetting);
                authenticators.add(bindAuthenticator);
            }
        }
        return authenticators;
    }

    static SpringSecurityContextSource createSecurityContext(LdapSetting ldapSetting) {
        DefaultSpringSecurityContextSource contextSource =
                new DefaultSpringSecurityContextSource(ldapSetting.getLdapUrl());
        // set default connection timeout to 5 seconds
        HashMap env = new HashMap();
        env.put("com.sun.jndi.ldap.connect.timeout", "10000");
        contextSource.setBaseEnvironmentProperties(env);
        SearchPattern searchPattern = ldapSetting.getSearch();
        if (searchPattern != null) {
            if (PathUtils.hasText(searchPattern.getManagerDn())) {
                contextSource.setUserDn(searchPattern.getManagerDn());
                contextSource.setPassword(searchPattern.getManagerPassword());
            } else {
                contextSource.setAnonymousReadOnly(true);
            }
        }

        try {
            contextSource.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return contextSource;
    }

    public DirContextOperations authenticate(Authentication authentication) {
        if (isLdapActive()) {
            BadCredentialsException lastException = null;
            // try all ldap authenticaters, return on first successful authentication
            for (BindAuthenticator authenticator : authenticators) {
                try {
                    DirContextOperations user = authenticator.authenticate(authentication);
                    if (user != null) {
                        return user;
                    }
                } catch (BadCredentialsException e) {
                    lastException = e;
                }
            }
            if (lastException != null) {
                throw lastException;
            } else {
                throw new AuthenticationServiceException(NO_LDAP_SERVICE_CONFIGURED);
            }
        } else {
            throw new AuthenticationServiceException(NO_LDAP_SERVICE_CONFIGURED);
        }
    }

    public boolean isLdapActive() {
        List<LdapSetting> ldaps = getLdapSettings();
        if (ldaps != null) {
            for (LdapSetting ldap : ldaps) {
                if (ldap.isEnabled()) {
                    return true;
                }
            }
        }
        return false;// no ldap configured or all disabled
    }

    public List<LdapSetting> getLdapSettings() {
        CentralConfigDescriptor descriptor = centralConfig.getDescriptor();
        return getLdapSettings(descriptor);
    }

    private List<LdapSetting> getLdapSettings(CentralConfigDescriptor descriptor) {
        SecurityDescriptor security = descriptor.getSecurity();
        if (security != null) {
            return security.getLdapSettings();
        }
        return null;
    }
}
