package org.artifactory.addon;

import java.io.Serializable;
import java.util.Properties;

/**
 * Contains the information of an installed addon
 *
 * @author Noam Y. Tenne
 */
public class AddonInfo implements Serializable {

    private String addonName;
    private String addonDisplayName;
    private String addonPath;
    private AddonState addonState;
    private Properties addonProperties;

    /**
     * Main constructor
     *
     * @param addonName        Name of addon
     * @param addonDisplayName Display ame of addon
     * @param addonPath        Path of addon xml file
     * @param addonState       State of addon
     * @param addonProperties  Addon properties
     */
    public AddonInfo(String addonName, String addonDisplayName, String addonPath, AddonState addonState,
                     Properties addonProperties) {
        this.addonName = addonName;
        this.addonDisplayName = addonDisplayName;
        this.addonPath = addonPath;
        this.addonState = addonState;
        this.addonProperties = addonProperties != null ? addonProperties : new Properties();
    }

    /**
     * Returns the name of the addon
     *
     * @return Addon name
     */
    public String getAddonName() {
        return addonName;
    }

    /**
     * Returns the displayable name of the addon
     *
     * @return Addon display name
     */
    public String getAddonDisplayName() {
        return addonDisplayName;
    }

    /**
     * Returns the path of the addon
     *
     * @return Addon path
     */
    public String getAddonPath() {
        return addonPath;
    }

    /**
     * Returns the state of the addon
     *
     * @return Addon state
     */
    public AddonState getAddonState() {
        return addonState;
    }

    /**
     * Sets the state of the addon
     *
     * @param addonState State to assign to addon
     */
    public void setAddonState(AddonState addonState) {
        this.addonState = addonState;
    }

    /**
     * Returns the addon's properties object
     *
     * @return Addon properties
     */
    public Properties getAddonProperties() {
        return addonProperties;
    }

    /**
     * Returns the addon property that corresponds to the given key
     *
     * @param propertyKey Key of property to retrieve
     * @return Property value if key was found. Null if not
     */
    public String getAddonProperty(String propertyKey) {
        return addonProperties.getProperty(propertyKey);
    }
}