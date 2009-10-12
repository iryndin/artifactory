package org.artifactory.cli.common;

/**
 * The Parameter class interface
 *
 * @author Noam Tenne
 */
public interface Param {

    /**
     * Returns the parameter description
     *
     * @return String Parameter description
     */
    String getDescription();

    /**
     * Returns an indication of the need for an extra parameter
     *
     * @return boolean Needs extra parameter
     */
    boolean isNeedExtraParam();

    /**
     * Returns the parameter description
     *
     * @return String Parameter description
     */
    String getParamDescription();

    /**
     * Returns the parameter name
     *
     * @return String parameter name
     */
    String getName();

    /**
     * Sets the value of the extra parameter
     *
     * @param value Value of extra parameter
     */
    void setValue(String value);

    /**
     * Returns the value of the extra parameter
     *
     * @return String Extra paramter value
     */
    String getValue();

    /**
     * Returns an indication of the validness of the extra parameter value
     *
     * @return boolean Validness of extra parameter value
     */
    boolean isSet();

    /**
     * Sets default value for the extra parameter
     */
    void set();
}
