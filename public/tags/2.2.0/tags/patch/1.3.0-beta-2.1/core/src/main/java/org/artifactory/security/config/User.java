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
package org.artifactory.security.config;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.apache.log4j.Logger;
import org.artifactory.jcr.ocm.OcmStorable;
import org.artifactory.security.JcrUserDetailsService;
import org.artifactory.security.SimpleUser;

/**
 * Simple user - comparisson is done only by user name Created by IntelliJ IDEA. User: yoavl
 */
@Node(extend = OcmStorable.class)
public class User implements OcmStorable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(User.class);

    @Field
    private String password;
    @Field
    private String username;
    @Field
    private boolean accountNonExpired;
    @Field
    private boolean accountNonLocked;
    @Field
    private boolean credentialsNonExpired;
    @Field
    private boolean enabled;
    @Field
    private String email;
    @Field
    private boolean admin;
    @Field
    private boolean updatableProfile;

    public User() {
    }

    public User(String username) {
        this.username = username;
    }

    public User(SimpleUser user) {
        password = user.getPassword();
        username = user.getUsername();
        accountNonExpired = user.isAccountNonExpired();
        accountNonLocked = user.isAccountNonLocked();
        credentialsNonExpired = user.isCredentialsNonExpired();
        enabled = user.isEnabled();
        email = user.getEmail();
        admin = user.isAdmin();
        updatableProfile = user.isUpdatableProfile();
    }

    public SimpleUser toSimpleUser() {
        return new SimpleUser(username, password, email, enabled, accountNonExpired,
                credentialsNonExpired, accountNonLocked, updatableProfile, admin);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getJcrPath() {
        return JcrUserDetailsService.getUsersJcrPath() + "/" + getUsername();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void setJcrPath(String path) {
        //noop
    }
}