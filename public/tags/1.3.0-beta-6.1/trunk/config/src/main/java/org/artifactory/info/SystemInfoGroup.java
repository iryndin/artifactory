package org.artifactory.info;

import java.util.ArrayList;

/**
 * A base class for all information groups that use system properties
 *
 * @author Noam Tenne
 */
public class SystemInfoGroup extends BaseInfoGroup {

    /**
     * Property names
     */
    private String[] properties;

    /**
     * Main constructor
     *
     * @param properties A collection of property names
     */
    public SystemInfoGroup(String... properties) {
        this.properties = properties;
    }

    public SystemInfoGroup() {
    }

    /**
     * Recieves property names and sets them in the global variable
     *
     * @param properties
     */
    public void setProperties(String... properties) {
        this.properties = properties;
    }

    /**
     * Returns all the info objects from the current group
     *
     * @return InfoObject[] - Collection of info objects from current group
     */
    @Override
    public InfoObject[] getInfo() {
        ArrayList<InfoObject> infoList = new ArrayList<InfoObject>();

        for (String prop : properties) {
            InfoObject infoObjcet = new InfoObject(prop, getSystemProperty(prop));
            infoList.add(infoObjcet);
        }

        return infoList.toArray(new InfoObject[infoList.size()]);
    }
}