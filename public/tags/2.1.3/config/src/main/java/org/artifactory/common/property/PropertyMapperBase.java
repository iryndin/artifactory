package org.artifactory.common.property;

/**
 * @author Yoav Landman
 */
abstract class PropertyMapperBase implements PropertyMapper {
    private String newPropertyName;

    protected PropertyMapperBase(String newPropertyName) {
        this.newPropertyName = newPropertyName;
    }

    public String getNewPropertyName() {
        return newPropertyName;
    }
}