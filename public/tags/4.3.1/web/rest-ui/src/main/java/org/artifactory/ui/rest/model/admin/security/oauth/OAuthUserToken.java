package org.artifactory.ui.rest.model.admin.security.oauth;

import org.artifactory.api.rest.restmodel.IModel;

/**
 * @author Gidi Shabat
 */
public class OAuthUserToken implements IModel {
    private String userName;
    private String providerName;

    public OAuthUserToken(String userName, String providerName) {
        this.userName = userName;
        this.providerName = providerName;
    }

    public String getUserName() {
        return userName;
    }

    public String getProviderName() {
        return providerName;
    }
}
