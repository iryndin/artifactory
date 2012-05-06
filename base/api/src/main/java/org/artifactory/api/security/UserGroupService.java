/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import org.artifactory.sapi.common.Lock;
import org.artifactory.security.GroupInfo;
import org.artifactory.security.MutableGroupInfo;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.UserInfo;

import java.util.List;
import java.util.Set;

/**
 * @author Fred Simon
 */
public interface UserGroupService {
    // Own profile page

    UserInfo currentUser();

    /**
     * @param username The unique username
     * @return UserInfo if user with the input username exists
     * @throws UsernameNotFoundException if user not found in the system
     */
    UserInfo findUser(String username);

    void updateUser(MutableUserInfo user);

    @Lock(transactional = true)
    boolean createUser(MutableUserInfo user);

    @Lock(transactional = true)
    void deleteUser(String username);

    List<UserInfo> getAllUsers(boolean includeAdmins);

    @Lock(transactional = true)
    void deleteGroup(String groupname);

    List<GroupInfo> getAllGroups();

    /**
     * @return A set of all the groups that should be added by default to newly created users.
     */
    Set<GroupInfo> getNewUserDefaultGroups();

    /**
     * @return A list of all groups that are of an external realm
     */
    List<GroupInfo> getAllExternalGroups();

    /**
     * @return A list of <b>internal</b> groups only
     */
    List<GroupInfo> getInternalGroups();

    /**
     * @return A set of all the groups names that should be added by default to newly created users.
     */
    Set<String> getNewUserDefaultGroupsNames();

    /**
     * Updates a users group. Group name update is not allowed.
     *
     * @param groupInfo The updated group info
     */
    void updateGroup(MutableGroupInfo groupInfo);

    @Lock(transactional = true)
    boolean createGroup(MutableGroupInfo groupInfo);

    /**
     * Adds a list of users to a group.
     *
     * @param groupName The group's unique name.
     * @param usernames The list of usernames.
     */
    @Lock(transactional = true)
    void addUsersToGroup(String groupName, List<String> usernames);

    /**
     * Deletes the user's membership of a group.
     *
     * @param groupName The group name
     * @param usernames The list of usernames
     */
    @Lock(transactional = true)
    void removeUsersFromGroup(String groupName, List<String> usernames);

    /**
     * Locates the users who are members of a group
     *
     * @param groupName the group whose members are required
     * @return the usernames of the group members
     */
    List<UserInfo> findUsersInGroup(String groupName);

    String resetPassword(String userName, String remoteAddress, String resetPageUrl);

    /**
     * For use with external authentication methods only (CAS/LDAP/SSO/Crowd) tries to locate a user with the given
     * name. When can't be found a new user will be created. The user will have no defined email, will not be an admin,
     * and will not have an updatable profile.
     *
     * @param username      The username to find/create
     * @param transientUser If true a transient user will be created and will cease to exists when the session ends. If
     *                      the user already exist in Artifactory users, this flag has no meaning.
     * @return Found\created user
     */
    UserInfo findOrCreateExternalAuthUser(String username, boolean transientUser);

    GroupInfo findGroup(String groupName);
}
