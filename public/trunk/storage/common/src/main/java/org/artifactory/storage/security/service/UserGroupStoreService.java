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

package org.artifactory.storage.security.service;

import org.artifactory.api.repo.Async;
import org.artifactory.common.Info;
import org.artifactory.md.Properties;
import org.artifactory.sapi.common.Lock;
import org.artifactory.security.GroupInfo;
import org.artifactory.security.MutableGroupInfo;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.User;
import org.artifactory.security.UserInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Date: 8/27/12
 * Time: 2:44 PM
 *
 * @author freds
 */
public interface UserGroupStoreService {

    /**
     * @param username The unique username
     * @return UserInfo if user with the input username exists, null otherwise
     */
    @Nullable
    UserInfo findUser(String username);

    @Lock
    void updateUser(MutableUserInfo user);

    /**
     * Updates user access details
     *
     * @param userName
     * @param clientIp
     * @param accessTimeMillis
     */
    @Async(transactional = true, delayUntilAfterCommit = true)
    void updateUserAccess(String userName, String clientIp, long accessTimeMillis);

    @Lock
    boolean createUser(UserInfo user);

    @Lock
    boolean createUserWithProperties(UserInfo user, boolean addUserProperties);


    @Lock
    void deleteUser(String username);

    List<UserInfo> getAllUsers(boolean includeAdmins);

    /**
     * Deletes the group from the database including any group membership users have to this group.
     *
     * @param groupName The group name to delete
     * @return True if the group and/or membership in this group was deleted
     */
    @Lock
    boolean deleteGroup(String groupName);

    List<GroupInfo> getAllGroups();

    /**
     * @return A set of all the groups that should be added by default to newly created users.
     */
    List<GroupInfo> getNewUserDefaultGroups();

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
    @Lock
    void updateGroup(MutableGroupInfo groupInfo);

    @Lock
    boolean createGroup(GroupInfo groupInfo);

    /**
     * Adds a list of users to a group.
     *
     * @param groupName The group's unique name.
     * @param usernames The list of usernames.
     */
    @Lock
    void addUsersToGroup(String groupName, List<String> usernames);

    /**
     * Deletes the user's membership of a group.
     *
     * @param groupName The group name
     * @param usernames The list of usernames
     */
    @Lock
    void removeUsersFromGroup(String groupName, List<String> usernames);

    /**
     * Locates the users who are members of a group
     *
     * @param groupName the group whose members are required
     * @return the usernames of the group members
     */
    List<UserInfo> findUsersInGroup(String groupName);

    /**
     * Find the group info object for this group name
     *
     * @param groupName The name of the group to find
     * @return the group information if group with name found, null otherwise
     */
    @Nullable
    GroupInfo findGroup(String groupName);

    @Lock
    void deleteAllGroupsAndUsers();

    boolean adminUserExists();

    boolean userExists(String username);

    /**
     * get user groups paging
     *
     * @param includeAdmins include admin
     * @return list
     */
    Collection<Info> getUsersGroupsPaging(boolean includeAdmins, String orderBy,
                                          String startOffset, String limit, String direction);

    /**
     * get all users and groups count
     *
     * @param includeAdmins include admin
     * @return num of records
     */
    long getAllUsersGroupsCount(boolean includeAdmins);

    /**
     * Find the user associated with the given external user datum
     *
     * @param key The key associated with this datum
     * @param val The value to search for
     * @return The user, or null if no user was found
     */
    @Nullable
    UserInfo findUserByProperty(String key, String val);

    /**
     * Find the datum associated with the given user
     *
     * @param username The user holding this datum
     * @param key      The key associated with this datum
     * @return The datum, or null if no datum was found
     */
    @Nullable
    String findUserProperty(String username, String key);

    /**
     * Add or alter an external user datum
     *
     * @param username The name of the user to alter
     * @param key      The key associated with this datum
     * @param val      The value to write
     * @return True if the write succeeded, false otherwise
     */
    @Lock
    boolean addUserProperty(String username, String key, String val);

    /**
     * Delete an external user datum
     *
     * @param username The name of the user to alter
     * @param key      The key associated with this datum
     * @return True if the delete succeeded, false otherwise
     */
    boolean deleteUserProperty(String username, String key);

    Properties findPropertiesForUser(String username);

    void deletePropertyFromAllUsers(String propertyKey);

    /**
     * Locks user upon incorrect login attempt
     *
     * @param userName
     */
    @Nonnull
    @Lock
    void lockUser(String userName);

    /**
     * Unlocks locked out user
     *
     * @param userName
     */
    @Nonnull
    @Lock
    void unlockUser(String userName);

    /**
     * Unlocks all locked out users
     */
    @Nonnull
    @Lock
    void unlockAllUsers();

    /**
     * Unlocks all locked out admin users
     */
    @Nonnull
    @Lock
    void unlockAdminUsers();

    /**
     * Registers incorrect login attempt
     *
     * @param userName
     */
    @Nonnull
    @Lock
    void registerIncorrectLoginAttempt(String userName);

    /**
     * Resets logon failures
     *
     * @param userName
     */
    @Nonnull
    @Async(transactional = true, delayUntilAfterCommit = true)
    void resetIncorrectLoginAttempts(String userName);

    /**
     * @param userName
     * @return incorrect login attempts for user
     */
    @Nonnull
    int getIncorrectLoginAttempts(String userName);

    /**
     * @return List of locked in users
     */
    Set<String> getLockedUsers();

    /**
     * Checks whether given user is locked
     * <p>
     * note: this method using caching in sake
     * of DB load preventing
     *
     * @param userName
     * @return boolean
     */
    @Nonnull
    boolean isUserLocked(String userName);

    /**
     * Calculates next login if user previously
     * failed to login due to incorrect credentials
     * and now have to wait before trying to login again
     *
     * @param userName
     * @return login delay or -1 if user login should
     * not be delayed (e.g last login was successful)
     */
    @Nonnull
    long getNextLogin(String userName);

    /**
     * Calculates next login if user previously
     * failed to login due to incorrect credentials
     * and now have to wait before trying to login again
     *
     * @param incorrectLoginAttempts
     * @param lastAccessTimeMillis
     * @return login delay or -1 if user login should
     * not be delayed (e.g last login was successful)
     */
    @Nonnull
    long getNextLogin(int incorrectLoginAttempts, long lastAccessTimeMillis);
}
