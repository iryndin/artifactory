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

import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Builder for user info with sensible defaults.
 *
 * @author Yossi Shaul
 */
public class UserInfoBuilder {

    private final String username;

    private String password = UserInfo.INVALID_PASSWORD;
    private String email = "";
    private boolean admin = false;
    private boolean enabled = true;
    private boolean updatableProfile = false;
    private boolean accountNonExpired = true;
    private boolean credentialsNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean transientUser = false;
    private Set<String> groups = new HashSet<String>();

    public UserInfoBuilder(String username) {
        this.username = username;
    }

    /**
     * @return The user.
     */
    public UserInfo build() {
        if (StringUtils.isBlank(username)) {
            throw new IllegalStateException("User must have a username");
        }

        UserInfo user = new UserInfo(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setAdmin(admin);
        user.setEnabled(enabled);
        user.setUpdatableProfile(updatableProfile);
        user.setAccountNonExpired(accountNonExpired);
        user.setCredentialsNonExpired(credentialsNonExpired);
        user.setAccountNonLocked(accountNonLocked);
        user.setTransientUser(transientUser);
        user.setGroups(groups);
        return user;
    }

    public UserInfoBuilder email(String email) {
        this.email = email;
        return this;
    }

    public UserInfoBuilder password(String password) {
        this.password = password;
        return this;
    }

    public UserInfoBuilder admin(boolean admin) {
        this.admin = admin;
        return this;
    }

    public UserInfoBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public UserInfoBuilder updatableProfile(boolean updatableProfile) {
        this.updatableProfile = updatableProfile;
        return this;
    }

    public UserInfoBuilder transientUser(boolean transientUser) {
        this.transientUser = transientUser;
        return this;
    }

    public UserInfoBuilder groups(Set<String> groups) {
        if (groups != null) {
            this.groups = new HashSet<String>(groups);
        } else {
            this.groups = Collections.emptySet();
        }
        return this;
    }
}
