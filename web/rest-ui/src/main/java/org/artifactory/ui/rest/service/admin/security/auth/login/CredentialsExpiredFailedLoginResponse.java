package org.artifactory.ui.rest.service.admin.security.auth.login;

import org.artifactory.rest.common.model.BaseModel;

/**
 * On failed login due to expired credentials, also signifies if the user is updatable or not
 * so that the UI can decide which error message to show (either lets the user change credentials or
 * instructs user to contact admin)
 *
 * @author Dan Feldman
 */
public class CredentialsExpiredFailedLoginResponse extends BaseModel {
    private static final String CREDENTIALS_EXPIRED_CODE = "CREDENTIALS_EXPIRED";

    private boolean profileUpdatable;
    private String code;

    public CredentialsExpiredFailedLoginResponse() {
    }

    public CredentialsExpiredFailedLoginResponse(boolean profileUpdatable) {
        this.profileUpdatable = profileUpdatable;
        this.code = CREDENTIALS_EXPIRED_CODE;
    }

    public boolean isProfileUpdatable() {
        return profileUpdatable;
    }

    public void setProfileUpdatable(boolean profileUpdatable) {
        this.profileUpdatable = profileUpdatable;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
