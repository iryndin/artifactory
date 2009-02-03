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
package org.artifactory.webapp.wicket.security.users;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.apache.log4j.Logger;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.SimpleUser;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class User implements Serializable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(User.class);

    private String username;
    private String password;
    private String retypedPassword;
    private boolean admin;
    private boolean updatableProfile;
    private static final GrantedAuthority[] USER_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl("USER")};
    private static final GrantedAuthority[] ADMIN_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl("ADMIN"),
                    new GrantedAuthorityImpl("USER")};

    public User() {
    }

    public User(SimpleUser user) {
        this.username = user.getUsername();
        this.password = user.getPassword();
        admin = SecurityHelper.isAdmin(user);
        updatableProfile = user.isUpdatableProfile();
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

    public String getRetypedPassword() {
        return retypedPassword;
    }

    public void setRetypedPassword(String retypedPassword) {
        this.retypedPassword = retypedPassword;
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

    public SimpleUser getUser() {
        return new SimpleUser(
                username, password, true, true, true, true, true, (admin ? ADMIN_GAS : USER_GAS));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return username.equals(user.username);
    }

    public int hashCode() {
        return username.hashCode();
    }

    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", retypedPassword='" + retypedPassword + '\'' +
                ", admin=" + admin +
                ", updatableProfile=" + updatableProfile +
                '}';
    }
}
