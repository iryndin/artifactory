/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.api.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.artifactory.common.Info;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@XStreamAlias("user")
public class UserInfo implements Info {
    public static final String ANONYMOUS = "anonymous";
    /**
     * Users with invalid password can only authenticate externally
     */
    public static final String INVALID_PASSWORD = "";

    private String username;
    private String password;
    private String email;
    private String genPasswordKey;
    private boolean admin;
    private boolean enabled;
    private boolean updatableProfile;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;
    private boolean accountNonLocked;

    private String realm;
    private String privateKey;
    private String publicKey;
    private boolean transientUser;

    private Set<UserGroupInfo> groups = new HashSet<UserGroupInfo>();

    private long lastLoginTimeMillis;
    private String lastLoginClientIp;

    private long lastAccessTimeMillis;
    private String lastAccessClientIp;

    public UserInfo() {
    }

    public UserInfo(String username) {
        this.username = username;
    }

    public UserInfo(UserInfo user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.admin = user.isAdmin();
        this.enabled = user.isEnabled();
        this.updatableProfile = user.isUpdatableProfile();
        this.accountNonExpired = user.isAccountNonExpired();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        this.accountNonLocked = user.isAccountNonLocked();
        this.transientUser = user.isTransientUser();
        this.realm = user.getRealm();

        Set<UserGroupInfo> groups = user.getGroups();
        if (groups != null) {
            this.groups = new HashSet<UserGroupInfo>(groups);
        } else {
            this.groups = new HashSet<UserGroupInfo>(1);
        }

        setPrivateKey(user.getPrivateKey());
        setPublicKey(user.getPublicKey());
        setGenPasswordKey(user.getGenPasswordKey());
        setLastLoginClientIp(user.getLastLoginClientIp());
        setLastLoginTimeMillis(user.getLastLoginTimeMillis());
        setLastAccessClientIp(user.getLastAccessClientIp());
        setLastAccessTimeMillis(user.getLastAccessTimeMillis());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getGenPasswordKey() {
        return genPasswordKey;
    }

    public void setGenPasswordKey(String genPasswordKey) {
        this.genPasswordKey = genPasswordKey;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUpdatableProfile() {
        return updatableProfile;
    }

    public void setUpdatableProfile(boolean updatableProfile) {
        this.updatableProfile = updatableProfile;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public boolean isTransientUser() {
        return transientUser;
    }

    public void setTransientUser(boolean transientUser) {
        this.transientUser = transientUser;
    }

    public String getRealm() {
        return realm;
    }

    public boolean isExternal() {
        return !SecurityService.DEFAULT_REALM.equals(realm);
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public boolean isAnonymous() {
        return (username != null && username.equalsIgnoreCase(ANONYMOUS));
    }

    public boolean isInGroup(String groupName) {
        //Use the equals() behavior with a dummy userGroupInfo
        UserGroupInfo userGroupInfo = getDummyGroup(groupName);
        return getGroups().contains(userGroupInfo);
    }

    public void addGroup(String groupName) {
        addGroup(groupName, SecurityService.DEFAULT_REALM);
    }

    public void addGroup(String groupName, String realm) {
        UserGroupInfo userGroupInfo = new UserGroupInfo(groupName, realm);
        // group equality is currently using group name only, so make sure to remove existing group with the same name
        groups.remove(userGroupInfo);
        groups.add(userGroupInfo);
    }

    public void removeGroup(String groupName) {
        //Use the equals() behavior with a dummy userGroupInfo
        UserGroupInfo userGroupInfo = getDummyGroup(groupName);
        groups.remove(userGroupInfo);
    }

    /**
     * @return The groups names this user belongs to. Empty list if none.
     */
    public Set<UserGroupInfo> getGroups() {
        return groups;
    }

    public void setGroups(Set<UserGroupInfo> groups) {
        this.groups = groups;
    }

    public void setInternalGroups(Set<String> groups) {
        if (groups == null) {
            groups = new HashSet<String>();
        }
        //Add groups with the default internal realm
        this.groups.clear();
        for (String group : groups) {
            addGroup(group);
        }
    }

    public void checkInternalGroups(Set<String> defaultGroups) {
        for (String defaultGroupName : defaultGroups) {
            if (!isInGroup(defaultGroupName)) {
                addGroup(defaultGroupName);
            }
        }
    }

    public long getLastLoginTimeMillis() {
        return lastLoginTimeMillis;
    }

    public void setLastLoginTimeMillis(long lastLoginTimeMillis) {
        this.lastLoginTimeMillis = lastLoginTimeMillis;
    }

    public String getLastLoginClientIp() {
        return lastLoginClientIp;
    }

    public void setLastLoginClientIp(String lastLoginClientIp) {
        this.lastLoginClientIp = lastLoginClientIp;
    }

    public long getLastAccessTimeMillis() {
        return lastAccessTimeMillis;
    }

    public void setLastAccessTimeMillis(long lastAccessTimeMillis) {
        this.lastAccessTimeMillis = lastAccessTimeMillis;
    }

    public String getLastAccessClientIp() {
        return lastAccessClientIp;
    }

    public void setLastAccessClientIp(String lastAccessClientIp) {
        this.lastAccessClientIp = lastAccessClientIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserInfo info = (UserInfo) o;

        return !(username != null ? !username.equals(info.username) : info.username != null);
    }

    @Override
    public int hashCode() {
        return (username != null ? username.hashCode() : 0);
    }

    private static UserGroupInfo getDummyGroup(String groupName) {
        UserGroupInfo userGroupInfo = new UserGroupInfo(groupName, "whatever");
        return userGroupInfo;
    }

    public boolean hasInvalidPassword() {
        return INVALID_PASSWORD.equals(getPassword());
    }

    /**
     * An object representing the resolved groups of a user, including the external realm the group may belong to
     */
    @XStreamAlias("userGroup")
    public static class UserGroupInfo implements Serializable {

        final String groupName;

        @XStreamOmitField
        String realm = SecurityService.DEFAULT_REALM;

        public UserGroupInfo(String groupName) {
            this(groupName, SecurityService.DEFAULT_REALM);
        }

        public UserGroupInfo(String groupName, String realm) {
            this.groupName = groupName;
            this.realm = realm;
        }

        public String getGroupName() {
            return groupName;
        }

        public String getRealm() {
            return realm;
        }

        public boolean isExternal() {
            return !SecurityService.DEFAULT_REALM.equals(realm);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UserGroupInfo info = (UserGroupInfo) o;
            return groupName.equals(info.groupName);
        }

        @Override
        public int hashCode() {
            return groupName.hashCode();
        }

        public static Set<UserGroupInfo> getInternalGroups(Set<String> names) {
            if (names == null) {
                return null;
            }
            //Create a list of default groups
            Set<UserGroupInfo> userGroupInfos = new HashSet<UserGroupInfo>(names.size());
            for (String name : names) {
                UserInfo.UserGroupInfo userGroupInfo = new UserInfo.UserGroupInfo(name);
                userGroupInfos.add(userGroupInfo);
            }
            return userGroupInfos;
        }

        @Override
        public String toString() {
            return groupName;
        }
    }
}