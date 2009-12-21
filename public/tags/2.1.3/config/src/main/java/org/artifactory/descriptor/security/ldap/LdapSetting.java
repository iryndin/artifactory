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

package org.artifactory.descriptor.security.ldap;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LdapSettingType",
        propOrder = {"key", "enabled", "ldapUrl", "userDnPattern", "search", "autoCreateUser"},
        namespace = Descriptor.NS)
public class LdapSetting implements Descriptor {

    @XmlID
    @XmlElement(required = true)
    private String key;

    @XmlElement(defaultValue = "true")
    private boolean enabled = true;

    private String ldapUrl;

    private String userDnPattern;

    private SearchPattern search;
    @XmlElement(defaultValue = "false")
    private boolean autoCreateUser = false;


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = transformUrlProtocol(ldapUrl);
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
    }

    public SearchPattern getSearch() {
        return search;
    }

    public void setSearch(SearchPattern search) {
        this.search = search;
    }

    public boolean isAutoCreateUser() {
        return autoCreateUser;
    }

    public void setAutoCreateUser(boolean autoCreateUser) {
        this.autoCreateUser = autoCreateUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LdapSetting)) {
            return false;
        }

        LdapSetting setting = (LdapSetting) o;

        if (key == null || !key.equals(setting.key)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (key == null) {
            return super.hashCode();
        }
        return key.hashCode();
    }

    /**
     * Validates the LDAP URL by assuring that the protocol specification is in lower-case letters
     * (http://issues.jfrog.org/jira/browse/RTFACT-2036)
     *
     * @param url URL to validate
     * @return Validated URL
     */
    private String transformUrlProtocol(String url) {
        if ((url == null) || (url.length() == 0) || (!url.contains(":"))) {
            return url;
        }
        StringBuilder builder = new StringBuilder();

        String[] splitUrl = url.split(":");
        splitUrl[0] = splitUrl[0].toLowerCase();

        for (int i = 0; i < splitUrl.length; i++) {
            builder.append(splitUrl[i]);
            if (i < (splitUrl.length - 1)) {
                builder.append(":");
            }
        }

        return builder.toString();
    }
}
