package org.artifactory.ui.rest.model.admin.security.oauth;

import org.artifactory.api.rest.restmodel.IModel;

/**
 * @author Gidi Shabat
 */
public class OAuthProviderInfo implements IModel {
    private String displayName;
    private String type;
    private String[] mandatoryFields;
    private String[] fieldsValues;


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getMandatoryFields() {
        return mandatoryFields;
    }

    public void setMandatoryFields(String[] mandatoryFields) {
        this.mandatoryFields = mandatoryFields;
    }

    public String[] getFieldsValues() {
        return fieldsValues;
    }

    public void setFieldsValues(String[] fieldsValues) {
        this.fieldsValues = fieldsValues;
    }
}

