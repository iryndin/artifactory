package org.artifactory.security.ldap;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.config.CentralConfigServiceImpl;
import org.artifactory.descriptor.security.Security;
import org.artifactory.descriptor.security.ldap.LdapSettings;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationServiceException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.providers.ldap.LdapAuthenticator;
import org.springframework.security.providers.ldap.authenticator.BindAuthenticator;

import javax.annotation.PostConstruct;

/**
 * Wrapper for the LDAP bind authenticator. Used to authenticate users against ldap and as a factory
 * for the security context and actual authenticator.
 *
 * @author Yossi Shaul
 */
public class LdapAuthenticatorWrapper
        implements LdapAuthenticator, PostInitializingBean {
    private static final String NO_LDAP_SERVICE_CONFIGURED = "No ldap service configured";

    @Autowired
    private CentralConfigService centralConfig;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addPostInit(LdapAuthenticatorWrapper.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[]{CentralConfigServiceImpl.class};
    }

    public void init() {
        // nothing to init here but it is needed to the create methods
    }

    public DefaultSpringSecurityContextSource createSecurityContext() {
        if (isLdapActive()) {
            LdapSettings ldapSettings = getLdapSettings();
            DefaultSpringSecurityContextSource contextSource =
                    new DefaultSpringSecurityContextSource(
                            ldapSettings.getLdapUrl());
            contextSource.setUserDn(ldapSettings.getManagerDn());
            contextSource.setPassword(ldapSettings.getManagerPassword());
            return contextSource;
        }
        throw new IllegalStateException(
                "Cannot create the LDAP Security Context without LDAP settings");
    }

    public BindAuthenticator createBindAuthenticator() {
        if (isLdapActive()) {
            LdapSettings ldapSettings = getLdapSettings();
            DefaultSpringSecurityContextSource contextSource =
                    InternalContextHelper.get().beanForType(
                            DefaultSpringSecurityContextSource.class);
            BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
            bindAuthenticator.setUserDnPatterns(new String[]{ldapSettings.getUserDnPattern()});
            return bindAuthenticator;
        }
        throw new IllegalStateException(
                "Cannot create the LDAP Bind Authenticator without LDAP settings");
    }

    public DirContextOperations authenticate(Authentication authentication) {
        if (isLdapActive()) {
            try {
                BindAuthenticator authenticator =
                        InternalContextHelper.get().beanForType(BindAuthenticator.class);
                return authenticator.authenticate(authentication);
            } catch (BeanNotOfRequiredTypeException e) {
                throw new AuthenticationServiceException(NO_LDAP_SERVICE_CONFIGURED);

            }
        } else {
            throw new AuthenticationServiceException(NO_LDAP_SERVICE_CONFIGURED);
        }
    }

    public boolean isLdapActive() {
        return getLdapSettings() != null;
    }

    public LdapSettings getLdapSettings() {
        Security security = centralConfig.getDescriptor().getSecurity();
        if (security != null) {
            return security.getLdapSettings();
        }
        return null;
    }
}
