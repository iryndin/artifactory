/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.security.user;

import com.ocpsoft.pretty.time.PrettyTime;
import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.page.security.profile.ProfileModel;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserModel extends ProfileModel {

    private String username;
    private boolean disableInternalPassword;
    private boolean admin;
    private boolean updatableProfile;
    private boolean selected;
    private Set<UserInfo.UserGroupInfo> groups;
    private long lastLoginTimeMillis;
    private long lastAccessTimeMillis;
    private Status status = Status.UNKNOWN;

    public UserModel(Set<String> defaultGroupsNames) {
        super();
        admin = false;
        updatableProfile = true;
        selected = false;
        groups = UserInfo.UserGroupInfo.getInternalGroups(defaultGroupsNames);
    }

    public UserModel(UserInfo userInfo) {
        this.username = userInfo.getUsername();
        setEmail(userInfo.getEmail());
        this.admin = userInfo.isAdmin();
        this.updatableProfile = userInfo.isUpdatableProfile();
        groups = userInfo.getGroups();
        // if user we update has invalid password the internal password is disabled
        disableInternalPassword = userInfo.hasInvalidPassword();
        lastLoginTimeMillis = userInfo.getLastLoginTimeMillis();
        lastAccessTimeMillis = userInfo.getLastAccessTimeMillis();
        if (!disableInternalPassword) {
            setStatus(Status.NOT_LDAP_USER);
        }
    }

    public void removeGroup(UserInfo.UserGroupInfo group) {
        this.groups.remove(group);
    }

    public void addGroups(Set<UserInfo.UserGroupInfo> groups) {
        this.groups.addAll(groups);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return getNewPassword();
    }

    public void setPassword(String password) {
        setNewPassword(password);
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isUpdatableProfile() {
        return updatableProfile;
    }

    public void setUpdatableProfile(boolean updatableProfile) {
        this.updatableProfile = updatableProfile;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDisableInternalPassword() {
        return disableInternalPassword;
    }

    public void setDisableInternalPassword(boolean disableInternalPassword) {
        this.disableInternalPassword = disableInternalPassword;
    }

    /**
     * @return A copy of the user group. We delibarately Don't allow updating it directly.
     */
    public Set<UserInfo.UserGroupInfo> getGroups() {
        return (groups == null ? groups : new HashSet<UserInfo.UserGroupInfo>(groups));
    }

    public long getLastLoginTimeMillis() {
        return lastLoginTimeMillis;
    }

    public String getLastLoginString() {
        if (lastLoginTimeMillis == 0) {
            return "Never";
        }
        PrettyTime prettyTime = new PrettyTime();
        return prettyTime.format(new Date(lastLoginTimeMillis));
    }

    public void setLastLoginTimeMillis(long lastLoginTimeMillis) {
        this.lastLoginTimeMillis = lastLoginTimeMillis;
    }

    public long getLastAccessTimeMillis() {
        return lastAccessTimeMillis;
    }

    public String getLastAccessString() {
        if (lastAccessTimeMillis == 0) {
            return "N/A";
        }
        PrettyTime prettyTime = new PrettyTime();
        Date lastAccessDate = new Date(lastAccessTimeMillis);
        return prettyTime.format(lastAccessDate) + " (" + lastAccessDate.toString() + ")";
    }

    public void setLastAccessTimeMillis(long lastAccessTimeMillis) {
        this.lastAccessTimeMillis = lastAccessTimeMillis;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        ACTIVE_USER("User exists in Artifactory and LDAP"),
        INACTIVE_USER("Artifactory user does not exist in LDAP or has no password."),
        NOT_LDAP_USER(""),
        UNKNOWN("");

        private String description;

        Status(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserModel user = (UserModel) o;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", admin=" + admin +
                ", updatableProfile=" + updatableProfile +
                '}';
    }
}
