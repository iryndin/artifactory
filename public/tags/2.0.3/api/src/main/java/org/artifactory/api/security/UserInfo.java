/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.api.security;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.artifactory.api.common.Info;

import java.util.HashSet;
import java.util.Set;

@XStreamAlias("user")
public class UserInfo implements Info {
    public static final String ANONYMOUS = "anonymous";
    /** Users with invalid password can only authenticate externally */
    public static String INVALID_PASSWORD = "";

    private String username;
    private String password;
    private String email;
    private String privateKey;
    private String publicKey;
    private boolean admin;
    private boolean enabled;
    private boolean updatableProfile;
    private boolean accountNonExpired;
    private boolean credentialsNonExpired;
    private boolean accountNonLocked;

    private Set<String> groups = new HashSet<String>();

    public UserInfo() {
    }

    public UserInfo(String username) {
        this.username = username;
    }

    public UserInfo(String username,
            String password,
            String email,
            boolean admin,
            boolean enabled,
            boolean updatableProfile,
            boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.admin = admin;
        this.enabled = enabled;
        this.updatableProfile = updatableProfile;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public UserInfo(UserInfo user) {
        this(
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.isAdmin(),
                user.isEnabled(),
                user.isUpdatableProfile(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                user.isAccountNonLocked()
        );
        Set<String> groups = user.getGroups();
        if (groups != null) {
            setGroups(new HashSet<String>(groups));
        } else {
            setGroups(new HashSet<String>(1));
        }

        setPrivateKey(user.getPrivateKey());
        setPublicKey(user.getPublicKey());
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