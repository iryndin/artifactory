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
package org.artifactory.descriptor.security.ldap;

import org.artifactory.descriptor.Descriptor;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "LdapSettingType",
        propOrder = {"key", "enabled", "ldapUrl", "userDnPattern", "search"})
public class LdapSetting implements Descriptor {

    @XmlID
    @XmlElement(required = true)
    private String key;

    @XmlElement(defaultValue = "true")
    private boolean enabled = true;

    private String ldapUrl;

    private String userDnPattern;

    private SearchPattern search;

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
