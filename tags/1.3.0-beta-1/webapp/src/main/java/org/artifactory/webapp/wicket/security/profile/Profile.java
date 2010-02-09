package org.artifactory.webapp.wicket.security.profile;

import java.io.Serializable;

/**
 * @author Yoav Hakman
 */
public class Profile implements Serializable {

    private String currentPassword;
    private String newPassword;
    private String retypedPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getRetypedPassword() {
        return retypedPassword;
    }

    public void setRetypedPassword(String retypedPassword) {
        this.retypedPassword = retypedPassword;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Profile profile = (Profile) o;

        if (currentPassword != null ? !currentPassword.equals(profile.currentPassword) :
                profile.currentPassword != null) {
            return false;
        }
        if (newPassword != null ? !newPassword.equals(profile.newPassword) :
                profile.newPassword != null) {
            return false;
        }
        if (retypedPassword != null ? !retypedPassword.equals(profile.retypedPassword) :
                profile.retypedPassword != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (currentPassword != null ? currentPassword.hashCode() : 0);
        result = 31 * result + (newPassword != null ? newPassword.hashCode() : 0);
        result = 31 * result + (retypedPassword != null ? retypedPassword.hashCode() : 0);
        return result;
    }
}
