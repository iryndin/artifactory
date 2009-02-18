package org.artifactory.info;

/**
 * An Object to hold system information in form of key and value
 *
 * @author Noam Tenne
 */
public class InfoObject {

    /**
     * Key
     */
    private String propertyName;
    /**
     * Value
     */
    private String propertyValue;

    /**
     * Main constructor
     *
     * @param propertyName  The key
     * @param propertyValue The value
     */
    public InfoObject(String propertyName, String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    /**
     * Returns the key
     *
     * @return String - key
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the value
     *
     * @return String - value
     */
    public String getPropertyValue() {
        return propertyValue;
    }
}
