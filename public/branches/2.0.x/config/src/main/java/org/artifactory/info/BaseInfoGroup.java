package org.artifactory.info;

/**
 * The base class of all the information groups
 *
 * @author Noam Tenne
 */
public class BaseInfoGroup implements InfoGroup {

    /**
     * Returns a system property via the property name
     *
     * @param propName The property name
     * @return String - The property value
     */
    public static String getSystemProperty(String propName) {
        if ((propName == null) || ("".equals(propName))) {
            throw new IllegalArgumentException("Property name cannot be empty or null");
        }
        String value = System.getProperty(propName);
        if (value == null) {
            throw new IllegalArgumentException("Could not find system property: " + propName);
        }
        return value;
    }

    /**
     * Basic implementation of the getInfo method
     *
     * @return InfoObject[] - An empty info object array
     */
    public InfoObject[] getInfo() {
        return new InfoObject[]{};
    }
}