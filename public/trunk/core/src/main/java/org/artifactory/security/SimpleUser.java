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

package org.artifactory.security;

import org.artifactory.api.security.UserInfo;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Simple user - comparison is done only by user name. This class is immutable and will return new instances when
 * getters are called.
 */
public class SimpleUser implements UserDetails, Comparable {

    public static final GrantedAuthority[] USER_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl(SecurityServiceImpl.ROLE_USER)};
    public static final GrantedAuthority[] ADMIN_GAS =
            new GrantedAuthority[]{new GrantedAuthorityImpl(InternalSecurityService.ROLE_ADMIN),
                    new GrantedAuthorityImpl(SecurityServiceImpl.ROLE_USER)};

    private UserInfo userInfo;

    private GrantedAuthority[] authorities;

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
            Assert.notNull(authorities[i], "Granted authority element " + i +
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

    public boolean isTransientUser() {
        return userInfo.isTransientUser();
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

    public boolean isAnonymous() {
        return UserInfo.ANONYMOUS.equals(userInfo.getUsername());
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
