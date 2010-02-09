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

package org.artifactory.descriptor.security;

import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

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
        assertNull(security.getLdapSettings(), "LDAP settings list should be null");
        assertNotNull(security.getPasswordSettings(), "Password settings list should not be null");
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

    public void addTwoEnabledLdaps() {
        SecurityDescriptor security = new SecurityDescriptor();
        LdapSetting ldap = new LdapSetting();
        ldap.setKey("ldap1");
        security.addLdap(ldap);
        assertEquals(security.getEnabledLdapSettings(), ldap);
        LdapSetting ldap2 = new LdapSetting();
        ldap.setKey("ldap2");
        security.addLdap(ldap2);
        assertEquals(security.getEnabledLdapSettings(), ldap2);
    }

    public void switchEnabledLdap() {
        SecurityDescriptor security = new SecurityDescriptor();
        LdapSetting ldap = new LdapSetting();
        ldap.setKey("ldap1");
        security.addLdap(ldap);
        LdapSetting ldap2 = new LdapSetting();
        ldap2.setKey("ldap2");
        security.addLdap(ldap2);

        assertFalse(ldap.isEnabled(), "ldap1 should be disabled since ldap2 took its place");

        ldap.setEnabled(true);
        security.ldapSettingChanged(ldap);

        assertTrue(ldap.isEnabled(), "ldap1 should be enabled");

        ldap.setEnabled(false);
        security.ldapSettingChanged(ldap);

        assertFalse(ldap.isEnabled(), "ldap1 should be disabled");
        assertFalse(ldap2.isEnabled(), "ldap2 should be disabled");

        ldap2.setEnabled(true);
        security.ldapSettingChanged(ldap2);

        assertTrue(ldap2.isEnabled(), "ldap2 should be enabled");
        assertFalse(ldap.isEnabled(), "ldap1 should be disabled");
    }
}