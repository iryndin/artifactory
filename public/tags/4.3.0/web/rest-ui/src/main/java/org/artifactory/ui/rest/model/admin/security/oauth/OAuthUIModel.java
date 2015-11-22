package org.artifactory.ui.rest.model.admin.security.oauth;

import org.artifactory.api.rest.restmodel.IModel;

import java.util.List;

/**
 * @author Gidi Shabat
 */
public class OAuthUIModel implements IModel {
    private boolean enabled;
    private boolean persistUsers;
    private String defaultNpm;
    private List<OAuthProviderInfo> availableTypes;
    private List<OAuthProviderUIModel> providers;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<OAuthProviderInfo> getAvailableTypes() {
        return availableTypes;
    }

    public void setAvailableTypes(List<OAuthProviderInfo> availableTypes) {
        this.availableTypes = availableTypes;
    }

    public List<OAuthProviderUIModel> getProviders() {
        return providers;
    }

    public void setProviders(List<OAuthProviderUIModel> providers) {
        this.providers = providers;
    }

    public boolean isPersistUsers() {
        return persistUsers;
    }

    public void setPersistUsers(boolean persistUsers) {
        this.persistUsers = persistUsers;
    }

    public String getDefaultNpm() {
        return defaultNpm;
    }

    public void setDefaultNpm(String defaultNpm) {
        this.defaultNpm = defaultNpm;
    }


}
