/*
 * This file is part of Artifactory.
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
import org.artifactory.api.common.Info;

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
    private String privateKey;
    private String publicKey;
    private String genPasswordKey;
    private boolean admin;
    private boolean enabled;
    private boolean updatableProfile;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;
    private boolean accountNonLocked;
    private boolean transientUser = false;

    private Set<String> groups = new HashSet<String>();

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

        Set<String> groups = user.getGroups();
        if (groups != null) {
            setGroups(new HashSet<String>(groups));
        } else {
            setGroups(new HashSet<String>(1));
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
        return getGroups().contains(groupName);
    }

    public void addGroup(String groupName) {
        getGroups().add(groupName);
    }

    public void removeGroup(String groupName) {
        getGroups().remove(groupName);
    }

    /**
     * @return The groups names this user belongs to. Empty list if none.
     */
    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        if (groups == null) {
            groups = new HashSet<String>();
        }
        this.groups = groups;
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
}
