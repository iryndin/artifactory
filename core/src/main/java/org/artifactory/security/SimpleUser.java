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

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;

/**
 * Simple user - comparisson is done only by user name Created by IntelliJ IDEA. User: yoavl
 */
public class SimpleUser extends User implements Comparable {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SimpleUser.class);

    private boolean updatableProfile = true;

    public SimpleUser(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked, boolean updatableProfile,
            GrantedAuthority[] authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired,
                accountNonLocked, authorities);
        this.updatableProfile = updatableProfile;
    }

    public SimpleUser(String username) {
        super(username, "", true, true, true, true, new GrantedAuthority[]{});
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

    public boolean isUpdatableProfile() {
        return updatableProfile;
    }

    public void setUpdatableProfile(boolean updatableProfile) {
        this.updatableProfile = updatableProfile;
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
