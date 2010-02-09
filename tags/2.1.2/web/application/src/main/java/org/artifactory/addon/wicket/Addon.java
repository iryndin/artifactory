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
    AOL("aol", "Artifactory Online");

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
