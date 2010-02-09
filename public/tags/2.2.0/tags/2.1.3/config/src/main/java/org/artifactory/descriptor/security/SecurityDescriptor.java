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

package org.artifactory.descriptor.security;

import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.sso.HttpSsoSettings;
import org.artifactory.util.AlreadyExistsException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yossi Shaul
 */
@XmlType(name = "SecurityType",
        propOrder = {"anonAccessEnabled", "passwordSettings", "ldapSettings", "httpSsoSettings"},
        namespace = Descriptor.NS)
public class SecurityDescriptor implements Descriptor {

    @XmlElement(defaultValue = "true")
    private boolean anonAccessEnabled = true;

    @XmlElementWrapper(name = "ldapSettings")
    @XmlElement(name = "ldapSetting", required = false)
    private List<LdapSetting> ldapSettings;


    @XmlElement(name = "passwordSettings", required = false)
    private PasswordSettings passwordSettings = new PasswordSettings();

    @XmlElement(name = "httpSsoSettings", required = false)
    private HttpSsoSettings httpSsoSettings;

    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public void setAnonAccessEnabled(boolean anonAccessEnabled) {
        this.anonAccessEnabled = anonAccessEnabled;
    }

    public List<LdapSetting> getLdapSettings() {
        return ldapSettings;
    }

    public void setLdapSettings(List<LdapSetting> ldapSettings) {
        this.ldapSettings = ldapSettings;
    }

    public void addLdap(LdapSetting ldapSetting) {
        if (ldapSettings == null) {
            ldapSettings = new ArrayList<LdapSetting>();
        }

        if (ldapSettings.contains(ldapSetting)) {
            throw new AlreadyExistsException("The LDAP configuration " + ldapSetting.getKey() + " already exists");
        }
        ldapSettings.add(ldapSetting);
    }

    public LdapSetting removeLdap(String ldapKey) {
        LdapSetting ldapSetting = getLdapSettings(ldapKey);
        if (ldapSetting == null) {
            return null;
        }

        ldapSettings.remove(ldapSetting);

        // set list to null if empty
        if (ldapSettings.isEmpty()) {
            ldapSettings = null;
        }

        return ldapSetting;
    }

    private LdapSetting getLdapSettings(String ldapKey) {
        if (ldapSettings != null) {
            for (LdapSetting ldap : ldapSettings) {
                if (ldap.getKey().equals(ldapKey)) {
                    return ldap;
                }
            }
        }
        return null;
    }

    public boolean isLdapExists(String key) {
        return getLdapSettings(key) != null;
    }

    public LdapSetting getEnabledLdapSettings() {
        if (ldapSettings != null) {
            for (LdapSetting ldap : ldapSettings) {
                if (ldap.isEnabled()) {
                    return ldap;
                }
            }
        }
        return null;
    }

    public boolean isLdapEnabled() {
        return getEnabledLdapSettings() != null;
    }

    public PasswordSettings getPasswordSettings() {
        return passwordSettings;
    }

    public HttpSsoSettings getHttpSsoSettings() {
        return httpSsoSettings;
    }

    public void setHttpSsoSettings(HttpSsoSettings httpSsoSettings) {
        this.httpSsoSettings = httpSsoSettings;
    }
}