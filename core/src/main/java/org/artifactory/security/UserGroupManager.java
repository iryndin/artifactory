/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.spring.ReloadableBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Transactional
public interface UserGroupManager extends UserDetailsService, ReloadableBean {

    Collection<User> getAllUsers();

    boolean createUser(SimpleUser user);

    void updateUser(SimpleUser user);

    void removeUser(SimpleUser user);

    void removeUser(String username);

    /**
     * @param username The unique username
     * @return SimpleUser if user with the input username exists
     * @throws UsernameNotFoundException if user not found in the system
     */
    SimpleUser loadUserByUsername(String username) throws UsernameNotFoundException;

    void removeGroup(String groupname);

    Collection<Group> getAllGroups();

    boolean createGroup(Group group);

    void updateGroup(Group group);

    void deleteAllGroupsAndUsers();
}