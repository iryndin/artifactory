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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Password expiration configuration
 *
 * @author Michael Pasternak
 */

@XmlType(name = "PasswordExpirationPolicyType",
        propOrder = {"enabled", "passwordMaxAge", "notifyByEmail", "currentPasswordValidFor"},
        namespace = Descriptor.NS
)
public class PasswordExpirationPolicy implements Descriptor {

    @XmlElement(defaultValue = "false", required = false)
    private Boolean enabled = false;

    @XmlElement(defaultValue = "60", required = false)
    private Integer passwordMaxAge = 60;

    @XmlElement(defaultValue = "true", required = false)
    private Boolean notifyByEmail = true;

    @XmlElement(required = false)
    private Integer currentPasswordValidFor;

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return number of days for password to get expired (general password live time)
     */
    public Integer getPasswordMaxAge() {
        return passwordMaxAge;
    }

    /**
     * @param passwordMaxAge number of days for password to get expired (general password live time)
     */
    public void setPasswordMaxAge(Integer passwordMaxAge) {
        this.passwordMaxAge = passwordMaxAge;
    }

    public Boolean isNotifyByEmail() {
        return notifyByEmail;
    }

    public void setNotifyByEmail(Boolean notifyByEmail) {
        this.notifyByEmail = notifyByEmail;
    }

    /**
     * @return number of days till password should be changed
     */
    public Integer getCurrentPasswordValidFor() {
        return currentPasswordValidFor;
    }

    /**
     * @param currentPasswordValidFor number of days till password should be changed
     */
    public void setCurrentPasswordValidFor(Integer currentPasswordValidFor) {
        this.currentPasswordValidFor = currentPasswordValidFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PasswordExpirationPolicy that = (PasswordExpirationPolicy) o;

        if (enabled != null ? !enabled.equals(that.enabled) : that.enabled != null) return false;
        if (passwordMaxAge != null ? !passwordMaxAge.equals(that.passwordMaxAge) : that.passwordMaxAge != null)
            return false;
        if (notifyByEmail != null ? !notifyByEmail.equals(that.notifyByEmail) : that.notifyByEmail != null)
            return false;
        return currentPasswordValidFor != null ? currentPasswordValidFor.equals(that.currentPasswordValidFor) : that.currentPasswordValidFor == null;

    }

    @Override
    public int hashCode() {
        int result = enabled != null ? enabled.hashCode() : 0;
        result = 31 * result + (passwordMaxAge != null ? passwordMaxAge.hashCode() : 0);
        result = 31 * result + (notifyByEmail != null ? notifyByEmail.hashCode() : 0);
        result = 31 * result + (currentPasswordValidFor != null ? currentPasswordValidFor.hashCode() : 0);
        return result;
    }
}
