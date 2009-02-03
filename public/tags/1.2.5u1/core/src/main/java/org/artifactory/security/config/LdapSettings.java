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
package org.artifactory.security.config;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by IntelliJ IDEA. User: andhan Date: 2007-nov-14 Time: 19:11:26
 */
@XmlType(name = "LdapSettingsType",
        propOrder = {"authenticationMethod", "ldapUrl", "userDnPattern", "managerDn",
                "managerPassword", "searchAuthPasswordAttributeName"})
public class LdapSettings {
    private LdapAuthenticationMethod authenticationMethod;
    private String ldapUrl;
    private String userDnPattern;
    private String managerDn;
    private String managerPassword;
    private String searchAuthPasswordAttributeName;

    @XmlElement
    public LdapAuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(LdapAuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    @XmlElement
    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    @XmlElement
    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
    }

    @XmlElement
    public String getManagerDn() {
        return managerDn;
    }

    public void setManagerDn(String managerDn) {
        this.managerDn = managerDn;
    }

    @XmlElement
    public String getManagerPassword() {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }


    @XmlElement
    public String getSearchAuthPasswordAttributeName() {
        return searchAuthPasswordAttributeName;
    }

    public void setSearchAuthPasswordAttributeName(String searchAuthPasswordAttributeName) {
        this.searchAuthPasswordAttributeName = searchAuthPasswordAttributeName;
    }
}
