/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Yossi Shaul
 */
@XmlType(name = "SearchType",
        propOrder = {"searchFilter", "searchBase", "searchSubTree", "managerDn", "managerPassword"},
        namespace = Descriptor.NS)
public class SearchPattern implements Descriptor {

    private String searchFilter;
    private String searchBase;

    @XmlElement(defaultValue = "true")
    private boolean searchSubTree = true;

    private String managerDn;
    private String managerPassword;

    public SearchPattern() {
    }

    public SearchPattern(@Nonnull SearchPattern searchPattern) {
        this.searchFilter = searchPattern.searchFilter;
        this.searchBase = searchPattern.searchBase;
        this.searchSubTree = searchPattern.searchSubTree;
        this.managerDn = searchPattern.managerDn;
        this.managerPassword = searchPattern.managerPassword;
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

    public void duplicate(SearchPattern searchPattern) {
        this.searchFilter = searchPattern.searchFilter;
        this.searchBase = searchPattern.searchBase;
        this.searchSubTree = searchPattern.searchSubTree;
        this.managerDn = searchPattern.managerDn;
        this.managerPassword = searchPattern.managerPassword;
    }
}