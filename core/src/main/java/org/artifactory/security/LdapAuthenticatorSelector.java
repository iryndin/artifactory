package org.artifactory.security;

import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.providers.ldap.LdapAuthenticator;
import org.acegisecurity.userdetails.ldap.LdapUserDetails;
import org.artifactory.config.CentralConfig;
import org.artifactory.security.config.LdapAuthenticationMethod;
import org.artifactory.security.config.LdapSettings;
import org.artifactory.security.config.Security;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: andhan Date: 2007-nov-06 Time: 11:55:23
 */
public class LdapAuthenticatorSelector
        implements LdapAuthenticator, BeanFactoryAware, InitializingBean {
    private static final String NO_LDAP_SERVICE_CONFIGURED = "No ldap service configured";

    private Map<String, String> authenticators;
    private String activeAuthenticatorName;
    private BeanFactory beanFactory;
    private CentralConfig centralConfig;


    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void afterPropertiesSet() throws Exception {
        if (centralConfig.getSecurity() != null &&
                centralConfig.getSecurity().getLdapSettings() != null) {
            Security security = centralConfig.getSecurity();
            LdapSettings ldapSettings = security.getLdapSettings();
            LdapAuthenticationMethod authenticationMethod = ldapSettings.getAuthenticationMethod();
            activeAuthenticatorName = authenticationMethod.getName();
        }
    }

    public CentralConfig getCentralConfig() {
        return centralConfig;
    }

    public void setCentralConfig(CentralConfig centralConfig) {
        this.centralConfig = centralConfig;
    }

    public Map<String, String> getAuthenticators() {
        return authenticators;
    }

    public void setAuthenticators(Map<String, String> authenticators) {
        this.authenticators = authenticators;
    }

    public String getActiveAuthenticatorName() {
        return activeAuthenticatorName;
    }

    public void setActiveAuthenticatorName(String activeAuthenticatorName) {
        this.activeAuthenticatorName = activeAuthenticatorName;
    }

    public LdapUserDetails authenticate(String username, String password) {
        String authenticatorBeanName = authenticators.get(getActiveAuthenticatorName());

        if (authenticatorBeanName != null && beanFactory.containsBean(authenticatorBeanName)) {
            try {
                LdapAuthenticator authenticator = (LdapAuthenticator) beanFactory
                        .getBean(authenticatorBeanName, LdapAuthenticator.class);
                return authenticator.authenticate(username, password);

            } catch (BeanNotOfRequiredTypeException e) {
                throw new AuthenticationServiceException(NO_LDAP_SERVICE_CONFIGURED);

            }
        } else {
            throw new AuthenticationServiceException(NO_LDAP_SERVICE_CONFIGURED);
        }
    }

}
