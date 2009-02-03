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

import org.artifactory.api.security.UserInfo;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Simple user - comparisson is done only by user name. This class is immutable and will return new
 * instances when getters are called.
 */
public class SimpleUser implements UserDetails, Comparable {
    /**
     * Used this empty non-hashed string as an invalid password.
     */
    private static String INVALID_PASSWORD = "";

    public final static GrantedAuthority[] USER_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl(SecurityServiceImpl.ROLE_USER)};
    public final static GrantedAuthority[] ADMIN_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl(SecurityServiceInternal.ROLE_ADMIN),
                    new GrantedAuthorityImpl(SecurityServiceImpl.ROLE_USER)};

    private UserInfo userInfo;

    private GrantedAuthority[] authorities;

    public SimpleUser(String username, String password, String email, boolean enabled,
            boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked,
            boolean updatableProfile, boolean admin) {

        this(new UserInfo(username, password, email, admin,
                enabled, updatableProfile, accountNonExpired, credentialsNonExpired,
                accountNonLocked));
    }

    /**
     * Creates a new user with invalid password, and user permissions. The created user cannot
     * update its profile.
     *
     * @param username The unique user name for the new user
     */
    public SimpleUser(String username) {
        this(username, INVALID_PASSWORD, "", true, true, true, true, false, false);
    }

    public SimpleUser(UserInfo userInfo) {
        this.userInfo = userInfo;
        setAuthorities(isAdmin() ? ADMIN_GAS : USER_GAS);
    }

    /**
     * @return A new instance of the underlying UserInfo.
     */
    public UserInfo getDescriptor() {
        return new UserInfo(userInfo);
    }

    private void setAuthorities(GrantedAuthority[] authorities) {
        Assert.notNull(authorities, "Cannot pass a null GrantedAuthority array");
        // Ensure array iteration order is predictable (as per UserDetails.getAuthorities()
        // contract and SEC-xxx)
        SortedSet<GrantedAuthority> sorter = new TreeSet<GrantedAuthority>();
        for (int i = 0; i < authorities.length; i++) {
            Assert.notNull(authorities[i],
                    "Granted authority element " + i +
                            " is null - GrantedAuthority[] cannot contain any null elements");
            sorter.add(authorities[i]);
        }

        this.authorities = sorter.toArray(new GrantedAuthority[sorter.size()]);
    }

    public GrantedAuthority[] getAuthorities() {
        return authorities;
    }

    public String getPassword() {
        return userInfo.getPassword();
    }

    public String getUsername() {
        return userInfo.getUsername();
    }

    public boolean isAccountNonExpired() {
        return userInfo.isAccountNonExpired();
    }

    public boolean isAccountNonLocked() {
        return userInfo.isAccountNonLocked();
    }

    public boolean isCredentialsNonExpired() {
        return userInfo.isCredentialsNonExpired();
    }

    public boolean isEnabled() {
        return userInfo.isEnabled();
    }

    public String getEmail() {
        return userInfo.getEmail();
    }

    public boolean isAdmin() {
        return userInfo.isAdmin();
    }

    public boolean isUpdatableProfile() {
        return userInfo.isUpdatableProfile();
    }

    public ArtifactorySid toArtifactorySid() {
        return new ArtifactorySid(getUsername());
    }

    @Override
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

    @Override
    public int hashCode() {
        return getUsername().hashCode();
    }

    @Override
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
