/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
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

import javax.xml.bind.annotation.XmlType;

import javax.xml.bind.annotation.XmlElement;

/**
 * User lock configuration
 *
 * @author Michael Pasternak
 */
@XmlType(name = "UserLockPolicyType", namespace = Descriptor.NS)
public class UserLockPolicy implements Descriptor {

    @XmlElement(defaultValue = "false", required = false)
    private boolean enabled = false;

    @XmlElement(defaultValue = "5", required = false)
    private int loginAttempts = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public int getLoginAttempts() {
        return loginAttempts;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLoginAttempts(int loginAttempts) {
        assert loginAttempts <= 100 : "loginAttempts cannot be greater than 100";
        this.loginAttempts = loginAttempts;
    }

    @Override
    public boolean equals(Object other) {

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        if (this == other) {
            return true;
        }

        UserLockPolicy that = (UserLockPolicy) other;

        if (this.isEnabled() != that.isEnabled()) {
            return false;
        }

        if (this.getLoginAttempts() != that.getLoginAttempts()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 17 * result + loginAttempts;
        return result;
    }
}
