package org.artifactory.ui.rest.model.admin.security.general;

import org.artifactory.descriptor.security.PasswordSettings;
import org.artifactory.descriptor.security.UserLockPolicy;
import org.artifactory.rest.common.model.BaseModel;

/**
 * @author Chen Keinan
 */
public class SecurityConfig extends BaseModel {

    boolean anonAccessEnabled;
    boolean anonAccessToBuildInfosDisabled;
    boolean hideUnauthorizedResources;
    PasswordSettings passwordSettings;
    UserLockPolicy userLockPolicy;

    public SecurityConfig(){}

    public SecurityConfig(boolean anonAccessEnabled,boolean anonAccessToBuildInfosDisabled,boolean hideUnauthorizedResources,
            PasswordSettings passwordSetting, UserLockPolicy userLockPolicy){
        this.setAnonAccessEnabled(anonAccessEnabled);
        this.setAnonAccessToBuildInfosDisabled(anonAccessToBuildInfosDisabled);
        this.setHideUnauthorizedResources(hideUnauthorizedResources);
        this.setPasswordSettings(passwordSetting);
        this.setUserLockPolicy(userLockPolicy);
    }

    public boolean isAnonAccessEnabled() {
        return anonAccessEnabled;
    }

    public void setAnonAccessEnabled(boolean anonAccessEnabled) {
        this.anonAccessEnabled = anonAccessEnabled;
    }

    public boolean isAnonAccessToBuildInfosDisabled() {
        return anonAccessToBuildInfosDisabled;
    }

    public void setAnonAccessToBuildInfosDisabled(boolean anonAccessToBuildInfosDisabled) {
        this.anonAccessToBuildInfosDisabled = anonAccessToBuildInfosDisabled;
    }

    public boolean isHideUnauthorizedResources() {
        return hideUnauthorizedResources;
    }

    public void setHideUnauthorizedResources(boolean hideUnauthorizedResources) {
        this.hideUnauthorizedResources = hideUnauthorizedResources;
    }

    public PasswordSettings getPasswordSettings() {
        return passwordSettings;
    }

    public void setPasswordSettings(PasswordSettings passwordSettings) {
        this.passwordSettings = passwordSettings;
    }

    public UserLockPolicy getUserLockPolicy() {
        return userLockPolicy;
    }

    public void setUserLockPolicy(UserLockPolicy userLockPolicy) {
        this.userLockPolicy = userLockPolicy;
    }
}
