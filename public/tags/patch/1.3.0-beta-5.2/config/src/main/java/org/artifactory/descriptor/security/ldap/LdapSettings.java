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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: andhan Date: 2007-nov-14 Time: 19:11:26
 */
@XmlType(name = "LdapSettingsType",
        propOrder = {"ldapUrl", "authenticationPatterns", "managerDn", "managerPassword"})
public class LdapSettings {

    private String ldapUrl;

    @XmlElementWrapper(name = "authenticationPatterns")
    @XmlElement(name = "authenticationPattern", required = true)
    private List<AuthenticationPattern> authenticationPatterns =
            new ArrayList<AuthenticationPattern>();

    private String managerDn;
    private String managerPassword;

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public List<AuthenticationPattern> getAuthenticationPatterns() {
        return authenticationPatterns;
    }

    public void setAuthenticationPatterns(List<AuthenticationPattern> authenticationPatterns) {
        this.authenticationPatterns = authenticationPatterns;
    }

    public String getManagerDn() {
        return managerDn;
    }

    public void setManagerDn(String managerDn) {
        this.managerDn = managerDn;
    }

    public String getManagerPassword() {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }
}
