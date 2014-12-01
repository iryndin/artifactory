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

package org.artifactory.addon;

/**
 * @author Yoav Aharoni
 */
public enum AddonType {
    //
    //PLEASE MAKE SURE THESE DETAILS ARE CONSISTENT WITH THE ONES IN THE PROPERTY FILES
    //
    AOL("aol", "Artifactory Online", -1),
    BUILD("build", "Build Integration", 100),
    LICENSES("license", "3rd Party License Control", 200),
    REST("rest", "Advanced REST", 300),
    LDAP("ldap", "LDAP Groups", 400),
    REPLICATION("replication", "Replication", 500),
    PROPERTIES("properties", "Properties", 600),
    SEARCH("search", "Smart Searches", 700),
    PLUGINS("plugins", "Plugins", 800),
    YUM("yum", "YUM", 900),
    P2("p2", "P2", 1000),
    NUGET("nuget", "NuGet", 1100),
    LAYOUTS("layouts", "Repository Layouts", 1200),
    FILTERED_RESOURCES("filtered-resources", "Filtered Resources", 1300),
    SSO("sso", "Crowd & SSO Integration", 1400),
    WATCH("watch", "Watches", 1500),
    WEBSTART("webstart", "WebStart & Jar Signing", 1600),
    BLACKDUCK("blackduck", "Black Duck Integration", 250),
    GEMS("gems", "RubyGems", 1100),
    NPM("npm", "NPM", 870),
    DEBIAN("debian", "Debian", 900),
    PYPI("pypi", "PyPI", 970),
    DOCKER("docker", "Docker", 910),
    HA("ha", "High Availability", 2000);


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