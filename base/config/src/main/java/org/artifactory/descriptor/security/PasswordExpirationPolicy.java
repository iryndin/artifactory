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
        propOrder = {"enabled", "expiresIn", "notifyByEmail", "currentPasswordValidFor"},
        namespace = Descriptor.NS
)
public class PasswordExpirationPolicy implements Descriptor {

    @XmlElement(defaultValue = "false", required = false)
    private Boolean enabled = false;

    @XmlElement(defaultValue = "60", required = false)
    private Integer expiresIn = 60;

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

    @Override
    public boolean equals(Object other) {

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        if (this == other) {
            return true;
        }

        PasswordExpirationPolicy that = (PasswordExpirationPolicy) other;

        if (this.isEnabled() != that.isEnabled()) {
            return false;
        }

        if (this.getExpiresIn() != that.getExpiresIn()) {
            return false;
        }

        if (this.isNotifyByEmail() != that.isNotifyByEmail()) {
            return false;
        }

        return true;
    }

    /**
     * @return number of days for password to get expired (general password live time)
     */
    public Integer getExpiresIn() {
        return expiresIn;
    }

    /**
     * @param expiresIn number of days for password to get expired (general password live time)
     */
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
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
    public int hashCode() {
        int result = (enabled ? 1 : 0);
        result = 17 * expiresIn + result;
        result = 31 * result + (notifyByEmail ? 1 : 0);
        result = 37 * (currentPasswordValidFor != null ? currentPasswordValidFor.intValue() : 0) + result;
        return result;
    }
}
