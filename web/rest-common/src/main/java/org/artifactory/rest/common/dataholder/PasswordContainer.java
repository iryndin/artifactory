package org.artifactory.rest.common.dataholder;

import org.artifactory.rest.common.model.RestModel;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;
import org.springframework.security.core.CredentialsContainer;

/**
 * Object used to hold password change data
 *
 * Created by Michael Pasternak on 1/5/16.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY
)
public class PasswordContainer implements RestModel, CredentialsContainer {

    private String userName;
    private char[] oldPassword;
    private char[] newPassword1;
    private char[] newPassword2;

    public PasswordContainer() {
    }

    public PasswordContainer(String userName, char[] oldPassword, char[] newPassword1, char[] newPassword2) {
        this.userName = userName;
        this.oldPassword = oldPassword;
        this.newPassword1 = newPassword1;
        this.newPassword2 = newPassword2;
    }

    public PasswordContainer(String userName, String oldPassword, String newPassword1, String newPassword2) {
        this.userName = userName;
        setOldPassword(oldPassword);
        setNewPassword1(newPassword1);
        setNewPassword2(newPassword2);
    }

    @JsonProperty("oldPassword")
    public String getOldPassword() {
        return oldPassword != null ? new String(oldPassword) : null;
    }

    @JsonProperty("oldPassword")
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword != null ? oldPassword.toCharArray() : null;
    }

    @JsonProperty("newPassword1")
    public String getNewPassword1() {
        return newPassword1 != null ? new String(newPassword1) : null;
    }

    @JsonProperty("newPassword1")
    public void setNewPassword1(String newPassword1) {
        this.newPassword1 = newPassword1 != null ? newPassword1.toCharArray() : null;
    }

    @JsonProperty("newPassword2")
    public String getNewPassword2() {
        return newPassword2 != null ? new String(newPassword2) : null;
    }

    @JsonProperty("newPassword2")
    public void setNewPassword2(String newPassword2) {
        this.newPassword2 = newPassword2 != null ? newPassword2.toCharArray() : null;
    }

    @JsonProperty("userName")
    public String getUserName() {
        return userName;
    }

    @JsonProperty("userName")
    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void eraseCredentials() {
        userName = null;
        oldPassword = null;
        newPassword1 = null;
        newPassword2 = null;
    }
}
