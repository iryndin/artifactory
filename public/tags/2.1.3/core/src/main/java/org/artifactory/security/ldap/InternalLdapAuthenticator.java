package org.artifactory.security.ldap;

import org.artifactory.spring.ReloadableBean;
import org.springframework.security.providers.ldap.LdapAuthenticator;

/**
 * @author Tomer Cohen
 */
public interface InternalLdapAuthenticator extends LdapAuthenticator, ReloadableBean {
}