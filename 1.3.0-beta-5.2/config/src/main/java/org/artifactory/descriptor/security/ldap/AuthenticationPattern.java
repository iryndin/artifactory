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
import javax.xml.bind.annotation.XmlType;

/**
 * @author freds
 * @date Sep 12, 2008
 */
@XmlType(name = "AuthenticationPattern",
        propOrder = {"userDnPattern", "searchFilter", "searchBase", "searchSubTree"})
public class AuthenticationPattern {
    private String userDnPattern;
    private String searchFilter;
    private String searchBase;
    @XmlElement(defaultValue = "true")
    private boolean searchSubTree = true;

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public boolean isSearchSubTree() {
        return searchSubTree;
    }

    public void setSearchSubTree(boolean searchSubTree) {
        this.searchSubTree = searchSubTree;
    }
}
