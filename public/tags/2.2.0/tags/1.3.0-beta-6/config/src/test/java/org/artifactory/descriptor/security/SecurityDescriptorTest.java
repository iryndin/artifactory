package org.artifactory.descriptor.security;

import org.artifactory.descriptor.security.ldap.LdapSetting;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * Tests the SecurityDescriptor.
 *
 * @author Yossi Shaul
 */
@Test
public class SecurityDescriptorTest {

    public void defaultConstructor() {
        SecurityDescriptor security = new SecurityDescriptor();

        assertTrue(security.isAnonAccessEnabled(),
                "Annon access should be enabled by default");
        assertNull(security.getLdapSettings(), "Ldap settings list should be null");
    }

    public void addLdap() {
        SecurityDescriptor security = new SecurityDescriptor();
        LdapSetting ldap = new LdapSetting();
        ldap.setKey("ldap1");
        security.addLdap(ldap);

        assertNotNull(security.getLdapSettings());
        assertEquals(security.getLdapSettings().size(), 1);
    }

    public void isLdapExists() {
        SecurityDescriptor security = new SecurityDescriptor();
        LdapSetting ldap = new LdapSetting();
        ldap.setKey("ldap1");
        security.addLdap(ldap);

        assertTrue(security.isLdapExists("ldap1"));
        assertFalse(security.isLdapExists("ldap2"));
    }

    public void removeLdap() {
        SecurityDescriptor security = new SecurityDescriptor();
        LdapSetting ldap1 = new LdapSetting();
        ldap1.setKey("ldap1");
        security.addLdap(ldap1);
        LdapSetting ldap2 = new LdapSetting();
        ldap2.setKey("ldap2");
        security.addLdap(ldap2);

        LdapSetting removedLdap = security.removeLdap("ldap1");
        assertEquals(ldap1, removedLdap);
        assertEquals(security.getLdapSettings().size(), 1);
    }

    public void removeLastLdap() {
        SecurityDescriptor security = new SecurityDescriptor();
        LdapSetting ldap = new LdapSetting();
        ldap.setKey("ldap1");
        security.addLdap(ldap);

        security.removeLdap("ldap1");
        assertNull(security.getLdapSettings(),
                "If no ldap configured the ldap settings list should be null (not just empty)");
    }

}
