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

package org.artifactory.addon;

/**
 * @author Yoav Aharoni
 */
public enum AddonType {
    //
    //PLEASE MAKE SURE THESE DETAILS ARE CONSISTENT WITH THE ONES IN THE PROPERTY FILES
    //
    WEBSTART("webstart", "WebStart & Jar Signing", 1300),
    SEARCH("search", "Smart Searches", 700),
    PROPERTIES("properties", "Properties", 600),
    WATCH("watch", "Watches", 1200),
    SSO("sso", "Crowd & SSO Integration", 1100),
    LDAP("ldap", "LDAP Group", 400),
    AOL("aol", "Artifactory Online", -1),
    BUILD("build", "Build Integration", 100),
    LICENSES("license", "3rd Party License Control", 200),
    PLUGINS("plugins", "Plugins", 900),
    LAYOUTS("layouts", "Repository Layouts", 1000),
    FILTERED_RESOURCES("filtered-resources", "Filtered Resources", 800),
    REPLICATION("replication", "Replication", 500),
    REST("rest", "Advanced REST", 300),
    P2("p2", "P2", 950),
    YUM("yum", "YUM", 1400);

    private String addonName;
    private String addonDisplayName;
    private int displayOrdinal;

    AddonType(String addonName, String addonDisplayName, int displayOrdinal) {
        this.addonName = addonName;
        this.addonDisplayName = addonDisplayName;
        this.displayOrdinal = displayOrdinal;
    }

    public String getAddonDisplayName() {
        return addonDisplayName;
    }

    public String getAddonName() {
        return addonName;
    }

    public int getDisplayOrdinal() {
        return displayOrdinal;
    }
}