package org.artifactory.ui.rest.model.admin.security.oauth;

import org.artifactory.addon.oauth.OAuthProvidersTypeEnum;

/**
 * @author Gidi Shabat
 */
public enum OAuthUIProvidersTypeEnum {
    github("GitHub", OAuthProvidersTypeEnum.github,new String[]{"apiUrl","authUrl","tokenUrl","basicUrl"},
            new String[]{null,null,null,null}),
    google("Google", OAuthProvidersTypeEnum.google,new String[]{"tokenUrl","apiUrl","authUrl","domain"},
            new String[]{"https://www.googleapis.com/oauth2/v3/token","https://www.googleapis.com/oauth2/v1/userinfo",
                    null,null}),
    openId("OpenID", OAuthProvidersTypeEnum.openId,new String[]{"apiUrl","authUrl","tokenUrl"},
            new String[]{null,null,null});
    private OAuthProvidersTypeEnum providerType;
    private String signature;
    private String[] mandatoryFields;
    private String[] fieldsValues;

    OAuthUIProvidersTypeEnum(String signature, OAuthProvidersTypeEnum providerType, String[] mandatoryFields,
            String[] fieldsValues) {
        this.providerType = providerType;
        this.signature = signature;
        this.mandatoryFields = mandatoryFields;
        this.fieldsValues = fieldsValues;
    }

    public OAuthProviderInfo getProviderInfo(){
        OAuthProviderInfo oAuthProviderInfo = new OAuthProviderInfo();
        oAuthProviderInfo.setFieldsValues(fieldsValues);
        oAuthProviderInfo.setDisplayName(signature);
        oAuthProviderInfo.setType(name());
        oAuthProviderInfo.setMandatoryFields(mandatoryFields);
        return oAuthProviderInfo;
    }


    public OAuthProvidersTypeEnum getProviderType() {
        return providerType;
    }

    public String getSignature() {
        return signature;
    }

    public String[] getMandatoryFields() {
        return mandatoryFields;
    }

    public String[] getFieldsValues() {
        return fieldsValues;
    }

    public static OAuthUIProvidersTypeEnum fromProviderType(OAuthProvidersTypeEnum providerType) {
        for (OAuthUIProvidersTypeEnum oAuthUIProvidersTypeEnum : values()) {
            if(providerType==oAuthUIProvidersTypeEnum.getProviderType()){
                return oAuthUIProvidersTypeEnum;
            }
        }
        return null;
    }
}
