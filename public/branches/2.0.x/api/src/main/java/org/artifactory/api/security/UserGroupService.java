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
package org.artifactory.api.security;

import org.artifactory.api.repo.Lock;

import java.util.List;
import java.util.Set;

/**
 * User: freds Date: Aug 5, 2008 Time: 6:50:30 PM
 */
public interface UserGroupService {
    // Own profile page
    UserInfo currentUser();

    /**
     * @param username The unique username
     * @return UserInfo if user with the input username exists, null otherwise
     */
    UserInfo findUser(String username);

    void updateUser(UserInfo user);

    @Lock(transactional = true)
    boolean createUser(UserInfo user);

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
     * @return A set of all the groups names that should be added by default to newly created users.
     */
    Set<String> getNewUserDefaultGroupsNames();

    /**
     * Updates a users group. Group name update is not allowed.
     *
     * @param groupInfo The updated group info
     */
    void updateGroup(GroupInfo groupInfo);

    @Lock(transactional = true)
    boolean createGroup(GroupInfo groupInfo);

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

}
