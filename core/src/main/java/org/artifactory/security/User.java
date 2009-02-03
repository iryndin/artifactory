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
package org.artifactory.security;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Collection;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.artifactory.api.security.UserInfo;
import org.artifactory.jcr.ocm.OcmStorable;

import java.util.Set;

/**
 * Ocm storable user that doesn't extend any ss classes
 */
@Node(extend = OcmStorable.class)
public class User implements OcmStorable {

    private final UserInfo info;

    public User() {
        info = new UserInfo();
    }

    public User(String username) {
        info = new UserInfo(username);
    }

    public User(String username, String password, String email, boolean admin, boolean enabled,
            boolean updatableProfile, boolean accountNonExpired, boolean credentialsNonExpired,
            boolean accountNonLocked) {
        info = new UserInfo(username, password, email, admin, enabled, updatableProfile,
                accountNonExpired,
                credentialsNonExpired, accountNonLocked);
    }

    public User(UserInfo user) {
        info = new UserInfo(user);
    }

    public UserInfo getInfo() {
        return new UserInfo(info);
    }

    public String getJcrPath() {
        return JcrUserGroupManager.getUsersJcrPath() + "/" + getUsername();
    }

    public void setJcrPath(String path) {
        //noop
    }

    @Field
    public String getUsername() {
        return info.getUsername();
    }

    public void setUsername(String username) {
        info.setUsername(username);
    }

    @Field
    public String getPassword() {
        return info.getPassword();
    }

    public void setPassword(String password) {
        info.setPassword(password);
    }

    @Field
    public String getEmail() {
        return info.getEmail();
    }

    public void setEmail(String email) {
        info.setEmail(email);
    }

    @Field
    public boolean isAdmin() {
        return info.isAdmin();
    }

    public void setAdmin(boolean admin) {
        info.setAdmin(admin);
    }

    @Field
    public boolean isEnabled() {
        return info.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        info.setEnabled(enabled);
    }

    @Field
    public boolean isUpdatableProfile() {
        return info.isUpdatableProfile();
    }

    public void setUpdatableProfile(boolean updatableProfile) {
        info.setUpdatableProfile(updatableProfile);
    }

    @Field
    public boolean isAccountNonExpired() {
        return info.isAccountNonExpired();
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        info.setAccountNonExpired(accountNonExpired);
    }

    @Field
    public boolean isAccountNonLocked() {
        return info.isAccountNonLocked();
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        info.setAccountNonLocked(accountNonLocked);
    }

    @Field
    public boolean isCredentialsNonExpired() {
        return info.isCredentialsNonExpired();
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        info.setCredentialsNonExpired(credentialsNonExpired);
    }

    @Collection(elementClassName = String.class,
            collectionConverter = MultiValueCollectionConverterImpl.class)
    public Set<String> getGroups() {
        return info.getGroups();
    }

    public void setGroups(Set<String> groups) {
        info.setGroups(groups);
    }
}