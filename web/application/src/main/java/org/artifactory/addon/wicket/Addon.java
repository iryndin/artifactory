/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.addon.wicket;

/**
 * @author Yoav Aharoni
 */
public enum Addon {
    WEBSTART("webstart", "WebStart and Jar Signing"),
    SEARCH("search", "Smart Searches"),
    PROPERTIES("properties", "Properties"),
    WATCH("watch", "Watches"),
    SSO("sso", "Single Sign-on"),
    LDAP("ldapgroup", "LDAP Group Synchronization"),
    AOL("aol", "Artifactory Online"),
    BUILD("build", "Build Integration");

    private String addonName;
    private String addonDisplayName;

    Addon(String addonName, String addonDisplayName) {
        this.addonName = addonName;
        this.addonDisplayName = addonDisplayName;
    }

    public String getAddonDisplayName() {
        return addonDisplayName;
    }

    public String getAddonName() {
        return addonName;
    }
}