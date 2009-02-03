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

import org.apache.log4j.Logger;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple user - comparisson is done only by user name Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleUser extends User implements Comparable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SimpleUser.class);

    private final static Set<String> SYSTEM_USERS = new HashSet<String>() {
        {
            add(ArtifactorySecurityManager.USER_ANONYMOUS);
        }
    };
    private final static GrantedAuthority[] USER_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl(ArtifactorySecurityManager.ROLE_USER)};
    private final static GrantedAuthority[] ADMIN_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl(ArtifactorySecurityManager.ROLE_ADMIN),
                    new GrantedAuthorityImpl(ArtifactorySecurityManager.ROLE_USER)};

    private String email;
    private boolean admin;
    private boolean updatableProfile = true;

    public SimpleUser(String username, String password, String email, boolean enabled,
            boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked,
            boolean updatableProfile, boolean admin) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired,
                accountNonLocked, admin ? ADMIN_GAS : USER_GAS);
        this.email = email;
        this.admin = admin;
        this.updatableProfile = updatableProfile;
    }

    public SimpleUser(String username) {
        this(username, "", "", true, true, true, true, true, false);
    }

    public SimpleUser(UserDetails recipient) {
        super(recipient.getUsername(),
                recipient.getPassword(),
                recipient.isEnabled(),
                recipient.isAccountNonExpired(),
                recipient.isCredentialsNonExpired(),
                recipient.isAccountNonLocked(),
                recipient.getAuthorities());
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isUpdatableProfile() {
        return updatableProfile;
    }

    public boolean isSystemUser() {
        return SYSTEM_USERS.contains(getUsername());
    }

    public PrincipalSid toPrincipalSid() {
        return new PrincipalSid(getUsername());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleUser user = (SimpleUser) o;
        return getUsername().equals(user.getUsername());

    }

    public int hashCode() {
        return getUsername().hashCode();
    }

    public String toString() {
        return getUsername();
    }

    public int compareTo(Object o) {
        if (o == null || !getClass().isAssignableFrom(o.getClass())) {
            throw new IllegalArgumentException();
        }
        return getUsername().compareTo(((SimpleUser) o).getUsername());
    }
}
