/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.descriptor.security;

import org.artifactory.descriptor.Descriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yossi Shaul
 */
@XmlType(name = "SecurityType", propOrder = {"anonAccessEnabled", "passwordSettings", "ldapSettings"})
public class SecurityDescriptor implements Descriptor {

    @XmlElement(defaultValue = "true")
    private boolean anonAccessEnabled = true;

    @XmlElementWrapper(name = "ldapSettings")
    @XmlElement(name = "ldapSetting", required = false)
    private List<LdapSetting> ldapSettings;

    @XmlElement(name = "passwordSettings", required = false)
    private PasswordSettings passwordSettings = new PasswordSettings();


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
        ldapSettings.add(ldapSetting);
    }

    public LdapSetting removeLdap(String ldapKey) {
        LdapSetting ldapSetting = getLdap(ldapKey);
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

    private LdapSetting getLdap(String ldapKey) {
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
        return getLdap(key) != null;
    }

    public PasswordSettings getPasswordSettings() {
        return passwordSettings;
    }
}