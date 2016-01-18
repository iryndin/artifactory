package org.artifactory.ui.rest.model.artifacts.search.propertysearch;

import org.artifactory.descriptor.property.PredefinedValue;
import org.artifactory.descriptor.property.Property;
import org.artifactory.ui.rest.model.artifacts.search.BaseSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chen Keinan
 */
public class PropertySearch extends BaseSearch {

    private List<PropertyKeyValues> propertyKeyValues;

    public List<PropertyKeyValues> getPropertyKeyValues() {
        return propertyKeyValues;
    }

    public void setPropertyKeyValues(
            List<PropertyKeyValues> propertyKeyValues) {
        this.propertyKeyValues = propertyKeyValues;
    }

    public void updatePropertySearchData(String key, String value){
        Property property = new Property(key);
        List<PredefinedValue> predefinedValues = new ArrayList<>();
        predefinedValues.add(new PredefinedValue(value,true));
        property.setPredefinedValues(predefinedValues);
        PropertyKeyValues propertyKeyValues = new PropertyKeyValues(null,property);
        List<PropertyKeyValues> propertyKeyValuesList = new ArrayList<>();
        propertyKeyValuesList.add(propertyKeyValues);
        this.setPropertyKeyValues(propertyKeyValuesList);
    }
}
