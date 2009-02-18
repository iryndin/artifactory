package org.artifactory.cli.common;

import org.artifactory.util.PathUtils;

/**
 * Represents a basic parameter of the CLI
 *
 * @author Noam Tenne
 */
public class BaseParam implements Param {
    /**
     * Parameter name
     */
    protected final String name;
    /**
     * Parameter description
     */
    protected final String description;
    /**
     * Indicator of the need of and extra parameter
     */
    protected final boolean needExtraParam;
    /**
     * A description of the extra parameter (if needed)
     */
    protected final String paramDescription;
    /**
     * The value if the extra parameter (if needed)
     */
    private String value = null;

    /**
     * Default constructor
     *
     * @param name             Parameter name
     * @param description      Parameter description
     * @param needExtraParam   Indicates the need of an extra parameter
     * @param paramDescription Describes the extra parameter
     */
    public BaseParam(String name, String description, boolean needExtraParam, String paramDescription) {
        this.paramDescription = paramDescription;
        this.description = description;
        this.name = name;
        this.needExtraParam = needExtraParam;
    }

    /**
     * Returns the parameter name
     *
     * @return String Parameter name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the parameter description
     *
     * @return String Parameter description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the need of an extra parameter
     *
     * @return boolean Does need extra parameter
     */
    public boolean isNeedExtraParam() {
        return needExtraParam;
    }

    /**
     * Returns the parameter description
     *
     * @return String Parameter description
     */
    public String getParamDescription() {
        return paramDescription;
    }

    /**
     * Sets the value of the extra parameter
     *
     * @param value Value of extra parameter
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the value of the extra parameter
     *
     * @return String Value of extra parameter
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns an indication which states if the extra parameter has a valid value
     *
     * @return boolean Is value set
     */
    public boolean isSet() {
        return PathUtils.hasText(value);
    }

    /**
     * Sets default extra parameter value
     */
    public void set() {
        setValue("on");
    }

    @Override
    public String toString() {
        return getName();
    }
}
