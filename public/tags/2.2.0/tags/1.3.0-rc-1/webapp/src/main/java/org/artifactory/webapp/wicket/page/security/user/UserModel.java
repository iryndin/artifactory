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
package org.artifactory.webapp.wicket.page.security.user;

import org.artifactory.api.security.UserInfo;
import org.artifactory.webapp.wicket.page.security.profile.ProfileModel;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class UserModel extends ProfileModel {

    private String username;
    private boolean admin;
    private boolean updatableProfile;
    private boolean selected;
    private Set<String> groups;

    public UserModel(Set<String> defaultGroups) {
        super();
        admin = false;
        updatableProfile = true;
        selected = false;
        groups = defaultGroups;
    }

    public UserModel(UserInfo userInfo) {
        this.username = userInfo.getUsername();
        setEmail(userInfo.getEmail());
        this.admin = userInfo.isAdmin();
        this.updatableProfile = userInfo.isUpdatableProfile();
        groups = userInfo.getGroups();
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

    /**
     * @return A copy of the user group. We delibarately Don't allow updating it directly.
     */
    public Set<String> getGroups() {
        return (groups == null ? groups : new HashSet<String>(groups));
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
