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

package org.artifactory.storage.db.security.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.GroupNotFoundException;
import org.artifactory.api.security.PasswordExpiryUser;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.Info;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.md.Properties;
import org.artifactory.model.xstream.fs.PropertiesImpl;
import org.artifactory.model.xstream.security.UserProperty;
import org.artifactory.security.GroupInfo;
import org.artifactory.security.MutableGroupInfo;
import org.artifactory.security.MutableUserInfo;
import org.artifactory.security.SaltedPassword;
import org.artifactory.security.UserGroupInfo;
import org.artifactory.security.UserInfo;
import org.artifactory.security.UserPropertyInfo;
import org.artifactory.security.exceptions.PasswordChangeException;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.security.dao.UserGroupsDao;
import org.artifactory.storage.db.security.dao.UserPropertiesDao;
import org.artifactory.storage.db.security.entity.Group;
import org.artifactory.storage.db.security.entity.User;
import org.artifactory.storage.db.security.entity.UserGroup;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.util.Strings;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Date: 8/27/12
 * Time: 2:47 PM
 *
 * @author freds
 */
@Service
public class UserGroupServiceImpl implements UserGroupStoreService {
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = LoggerFactory.getLogger(UserGroupServiceImpl.class);
    private static final String API_KEY = "api_key";
    private static final String ALL_USERS = "*";
    public static final int MAX_USERS_TO_TRACK = 10000; // max locked users to keep in cache
    // max delay for user to be suspended (5 seconds)
    private static final int MAX_LOGIN_DELAY = 5000;
    private static final int OK_INCORRECT_LOGINS = 2; // delay will start after OK_INCORRECT_LOGINS+1 attempts
    private static final int LOGIN_DELAY_MULTIPLIER = getLoginDelayMultiplier();
    private static final boolean CACHE_BLOCKED_USERS = ConstantValues.useFrontCacheForBlockedUsers.getBoolean();
    private final long MILLIS_IN_DAY = /* secs in day */ 86400 * 1000 /* millis in sec */;

    SecurityService securityService;

    // cache meaning  <userName, lock-time>
    private final Cache<String, Long> lockedUsersCache = CacheBuilder.newBuilder().maximumSize(MAX_USERS_TO_TRACK).
            expireAfterWrite(24, TimeUnit.HOURS).build();
    private final Cache<String, Long> userAccessUsersCache = CacheBuilder.newBuilder().maximumSize(MAX_USERS_TO_TRACK).
            expireAfterWrite(24, TimeUnit.HOURS).build();

    @Autowired
    private DbService dbService;

    @Autowired
    private UserGroupsDao userGroupsDao;

    @Autowired
    private UserPropertiesDao userPropertiesDao;

    @Override
    public void deleteAllGroupsAndUsers() {
        try {
            userGroupsDao.deleteAllGroupsAndUsers();
        } catch (SQLException e) {
            throw new StorageException("Could not delete all users and groups", e);
        }
    }

    @Override
    public boolean adminUserExists() {
        try {
            return userGroupsDao.adminUserExists();
        } catch (SQLException e) {
            throw new StorageException("Could not determine if admin users exists due to: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean userExists(String username) {
        try {
            return userGroupsDao.findUserIdByUsername(username) > 0L;
        } catch (SQLException e) {
            throw new StorageException("Could not execute exists query for username='" + username + "'", e);
        }
    }

    @Override
    public UserInfo findUser(String username) {
        try {
            User user = userGroupsDao.findUserByName(username);
            if (user != null) {
                return userToUserInfo(user);
            }
        } catch (SQLException e) {
            throw new StorageException("Could not execute search query for username='" + username + "'", e);
        }
        return null;
    }

    /**
     * Finds user in DB and returns it as is
     * without attaching any extra metadata such
     * as groups for instance
     *
     * @param userName
     * @return {@link User}
     */
    private User getUser(String userName) {
        try {
            return userGroupsDao.findUserByName(userName, false);
        } catch (SQLException e) {
            throw new StorageException("Could not execute search query for username='" + userName + "'", e);
        }
    }

    @Override
    public void updateUser(MutableUserInfo userInfo) {
        try {
            User originalUser = userGroupsDao.findUserByName(userInfo.getUsername());
            if (originalUser == null) {
                throw new UsernameNotFoundException(
                        "Cannot update user with user name '" + userInfo.getUsername() + "' since it does not exists!");
            }

            User updatedUser = userInfoToUser(originalUser.getUserId(), userInfo);
            userGroupsDao.updateUser(updatedUser);

            // change passwordExpired state if user changes password during update
            if (originalUser.isCredentialsExpired() == updatedUser.isCredentialsExpired() && // admin not changing password expired state
                    !originalUser.getPassword().equals(updatedUser.getPassword())) {   // user indeed changed password
                userGroupsDao.revalidatePassword(originalUser.getUsername());
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to update user " + userInfo.getUsername(), e);
        }
    }

    /**
     * Updates user access details
     *
     * @param userName
     * @param clientIp
     * @param accessTimeMillis
     */
    @Override
    public void updateUserAccess(String userName, String clientIp, long accessTimeMillis) {
        if (StringUtils.isNotBlank(userName) && !UserInfo.ANONYMOUS.equals(userName)) {
            User user = getUser(userName);
            if (user == null) return;
            if (!user.isLocked() || !getSecurityService().isUserLockPolicyEnabled()) {
                userAccessUsersCache.put(userName, accessTimeMillis);
            }
        }
    }

    @Override
    public boolean createUser(UserInfo user) {
        return createUserWithProperties(user, false);
    }

    @Override
    public boolean createUserWithProperties(UserInfo user, boolean addUserProperties) {
        try {
            if (userExists(user.getUsername())) {
                return false;
            }
            User u = userInfoToUser(dbService.nextId(), user);
            int createUserSucceeded = userGroupsDao.createUser(u);
            Set<UserPropertyInfo> userProperties = user.getUserProperties();
            if (addUserProperties && userProperties != null && !userProperties.isEmpty()) {
                for (UserPropertyInfo userPropertyInfo : userProperties) {
                    userPropertiesDao.addUserPropertyById(new Long(u.getUserId()).intValue(),
                            userPropertyInfo.getPropKey(), userPropertyInfo.getPropValue());
                }
            }
            boolean result = createUserSucceeded > 0;
            if (result) {
                SecurityService securityService = getSecurityService();
                // if user was previously locked as unknown user
                // and create succeeded, we unlock it
                if (securityService instanceof UserGroupService)
                    ((UserGroupService) securityService).unlockUser(user.getUsername());
            }
            return result;
        } catch (SQLException e) {
            throw new StorageException("Failed to create user " + user.getUsername(), e);
        }
    }

    /**
     * @return {@link SecurityService}
     */
    private SecurityService getSecurityService() {
        if (securityService == null)
            securityService = ContextHelper.get().beanForType(SecurityService.class);
        return securityService;
    }

    @Override
    public void deleteUser(String username) {
        try {
            userGroupsDao.deleteUser(username);
        } catch (SQLException e) {
            throw new StorageException("Failed to delete user " + username, e);
        }
    }

    @Override
    public List<UserInfo> getAllUsers(boolean includeAdmins) {
        List<UserInfo> results = new ArrayList<>();
        try {
            Collection<User> allUsers = userGroupsDao.getAllUsers(includeAdmins);
            Map<Long, Set<UserPropertyInfo>> allUserProperties = userPropertiesDao.getAllUserProperties();
            for (User user : allUsers) {
                UserInfo userInfo = userToUserInfoWithProperties(user, allUserProperties);
                results.add(userInfo);
            }
            return results;
        } catch (SQLException e) {
            throw new StorageException("Could not execute get all users query", e);
        }
    }

    @Override
    public Collection<Info> getUsersGroupsPaging(boolean includeAdmins, String orderBy,
                                                 String startOffset, String limit, String direction) {
        Collection<Info> infoCollection;
        try {
            infoCollection = userGroupsDao.getUsersGroupsPaging(includeAdmins, orderBy,
                    startOffset, limit, direction);
        } catch (SQLException e) {
            throw new StorageException("Failed to get users  group ");
        }
        return infoCollection;
    }

    public long getAllUsersGroupsCount(boolean includeAdmins) {
        return userGroupsDao.getAllUsersGroupsCount(includeAdmins);
    }

    @Override
    public boolean deleteGroup(String groupName) {
        try {
            return userGroupsDao.deleteGroup(groupName) > 0;
        } catch (SQLException e) {
            throw new StorageException("Failed to delete group " + groupName, e);
        }
    }

    private List<GroupInfo> findAllGroups(UserGroupsDao.GroupFilter groupFilter) {
        List<GroupInfo> results = new ArrayList<>();
        try {
            Collection<Group> allGroups = userGroupsDao.findGroups(groupFilter);
            for (Group group : allGroups) {
                results.add(groupToGroupInfo(group));
            }
            return results;
        } catch (SQLException e) {
            throw new StorageException("Could not execute get all groups query", e);
        }
    }

    @Override
    public List<GroupInfo> getAllGroups() {
        return findAllGroups(UserGroupsDao.GroupFilter.ALL);
    }

    @Override
    public List<GroupInfo> getNewUserDefaultGroups() {
        return findAllGroups(UserGroupsDao.GroupFilter.DEFAULTS);
    }

    @Override
    public List<GroupInfo> getAllExternalGroups() {
        return findAllGroups(UserGroupsDao.GroupFilter.EXTERNAL);
    }

    @Override
    public List<GroupInfo> getInternalGroups() {
        return findAllGroups(UserGroupsDao.GroupFilter.INTERNAL);
    }

    @Override
    public Set<String> getNewUserDefaultGroupsNames() {
        Set<String> results = new HashSet<>();
        try {
            Collection<Group> allGroups = userGroupsDao.findGroups(UserGroupsDao.GroupFilter.DEFAULTS);
            for (Group group : allGroups) {
                results.add(group.getGroupName());
            }
            return results;
        } catch (SQLException e) {
            throw new StorageException("Could not execute get all default group names query", e);
        }
    }

    @Override
    public void updateGroup(MutableGroupInfo groupInfo) {
        try {
            Group originalGroup = userGroupsDao.findGroupByName(groupInfo.getGroupName());
            if (originalGroup == null) {
                throw new GroupNotFoundException("Cannot update non existent group '" + groupInfo.getGroupName() + "'");
            }
            Group newGroup = groupInfoToGroup(originalGroup.getGroupId(), groupInfo);
            if (userGroupsDao.updateGroup(newGroup) != 1) {
                throw new StorageException("Updating group did not find corresponding entity" +
                        " based on name='" + groupInfo.getGroupName() + "' and id=" + originalGroup.getGroupId());
            }
        } catch (SQLException e) {
            throw new StorageException("Could not update group " + groupInfo.getGroupName(), e);
        }
    }

    @Override
    public boolean createGroup(GroupInfo groupInfo) {
        try {
            if (userGroupsDao.findGroupByName(groupInfo.getGroupName()) != null) {
                // Group already exists
                return false;
            }
            Group g = groupInfoToGroup(dbService.nextId(), groupInfo);
            return userGroupsDao.createGroup(g) > 0;
        } catch (SQLException e) {
            throw new StorageException("Could not create group " + groupInfo.getGroupName(), e);
        }
    }

    @Override
    public void addUsersToGroup(String groupName, List<String> usernames) {
        try {
            Group group = userGroupsDao.findGroupByName(groupName);
            if (group == null) {
                throw new GroupNotFoundException("Cannot add users to non existent group " + groupName);
            }
            userGroupsDao.addUsersToGroup(group.getGroupId(), usernames, group.getRealm());
        } catch (SQLException e) {
            throw new StorageException("Could not add users " + usernames + " to group " + groupName, e);
        }
    }

    @Override
    public void removeUsersFromGroup(String groupName, List<String> usernames) {
        try {
            Group group = userGroupsDao.findGroupByName(groupName);
            if (group == null) {
                throw new GroupNotFoundException("Cannot remove users to non existent group " + groupName);
            }
            userGroupsDao.removeUsersFromGroup(group.getGroupId(), usernames);
        } catch (SQLException e) {
            throw new StorageException("Could not add users " + usernames + " to group " + groupName, e);
        }
    }

    @Override
    public List<UserInfo> findUsersInGroup(String groupName) {
        List<UserInfo> results = new ArrayList<>();
        try {
            Group group = userGroupsDao.findGroupByName(groupName);
            if (group == null) {
                return results;
            }
            List<User> users = userGroupsDao.findUsersInGroup(group.getGroupId());
            for (User user : users) {
                results.add(userToUserInfo(user));
            }
            return results;
        } catch (SQLException e) {
            throw new StorageException("Could not find users for group with name='" + groupName + "'", e);
        }
    }

    @Override
    @Nullable
    public GroupInfo findGroup(String groupName) {
        try {
            Group group = userGroupsDao.findGroupByName(groupName);
            if (group != null) {
                return groupToGroupInfo(group);
            }
            return null;
        } catch (SQLException e) {
            throw new StorageException("Could not search for group with name='" + groupName + "'", e);
        }
    }

    @Override
    @Nullable
    public UserInfo findUserByProperty(String key, String val) {
        try {
            long userId = userPropertiesDao.getUserIdByProperty(key, val);
            if (userId == 0L) {
                return null;
            } else {
                return userToUserInfo(userGroupsDao.findUserById(userId));
            }
        } catch (SQLException e) {
            throw new StorageException("Could not search for user with property " + key + ":" + val, e);
        }
    }

    @Override
    @Nullable
    public String findUserProperty(String username, String key) {
        try {
            return userPropertiesDao.getUserProperty(username, key);
        } catch (SQLException e) {
            throw new StorageException("Could not search for datum " + key + " of user " + username, e);
        }
    }

    @Override
    public boolean addUserProperty(String username, String key, String val) {
        try {
            return userPropertiesDao.addUserPropertyByUserName(username, key, val);
        } catch (SQLException e) {
            throw new StorageException("Could not add external data " + key + ":" + val + " to user " + username, e);
        }
    }

    @Override
    public boolean deleteUserProperty(String username, String key) {
        try {
            return userPropertiesDao.deleteProperty(userGroupsDao.findUserIdByUsername(username), key);
        } catch (SQLException e) {
            throw new StorageException("Could not delete external data " + key + " from user " + username, e);
        }
    }

    @Override
    public Properties findPropertiesForUser(String username) {
        try {
            List<UserProperty> userProperties = userPropertiesDao.getPropertiesForUser(username);
            PropertiesImpl properties = new PropertiesImpl();
            for (UserProperty userProperty : userProperties) {
                properties.put(userProperty.getPropKey(), userProperty.getPropValue());
            }
            return properties;
        } catch (SQLException e) {
            throw new StorageException("Failed to load user properties for " + username, e);
        }
    }

    @Override
    public void deletePropertyFromAllUsers(String propertyKey) {
        try {
            userPropertiesDao.deletePropertyFromAllUsers(propertyKey);
        } catch (SQLException e) {
            throw new StorageException("Could not delete property by key" + propertyKey + " from all users");
        }

    }

    /**
     * Locks user upon incorrect login attempt
     *
     * @param userName
     * @throws StorageException
     */
    @Nonnull
    @Override
    public void lockUser(String userName) {
        User user = null;
        try {
            user = getUser(userName);
            synchronized (lockedUsersCache) {
                // despite we use concurrency ready cache,
                // we lock it externally in sake of db/cache
                // synchronisation
                if (user != null)
                    // we want to block non-existing users as well
                    userGroupsDao.lockUser(user);
                registerLockedOutUser(userName);
            }
        } catch (SQLException e) {
            log.debug("Could not lock user {}, cause: {}", userName, e);
            throw new StorageException(
                    "Could not lock user '" + userName + "', because " + e.getMessage()
            );
        }
    }

    /**
     * Unlocks locked in user
     *
     * @param userName
     * @throws StorageException
     */
    @Nonnull
    @Override
    public void unlockUser(String userName) {
        User user = null;
        try {
            user = getUser(userName);
            synchronized (lockedUsersCache) {
                // despite we use concurrency ready cache,
                // we lock it externally in sake of db/cache
                // synchronisation
                if (user != null)
                    userGroupsDao.unlockUser(user);
                unRegisterLockedOutUsers(userName);
            }
        } catch (SQLException e) {
            log.debug("Could not unlock user {}, cause: {}", userName, e);
            throw new StorageException(
                    "Could not unlock user '" + userName + "', because " + e.getMessage()
            );
        }
    }

    /**
     * Unlocks all locked out users
     */
    @Nonnull
    @Override
    public void unlockAllUsers() {
        try {
            synchronized (lockedUsersCache) {
                // despite we use concurrency ready cache,
                // we lock it externally in sake of db/cache
                // synchronisation
                userGroupsDao.unlockAllUsers();
                unRegisterLockedOutUsers(ALL_USERS);
            }
        } catch (SQLException e) {
            log.debug("Could not unlock all users, cause: {}", e);
            throw new StorageException(
                    "Could not unlock all users, because " + e.getMessage()
            );
        }
    }

    /**
     * Unlocks all locked out admin users
     */
    @Nonnull
    @Override
    public void unlockAdminUsers() {
        try {
            synchronized (lockedUsersCache) {
                // despite we use concurrency ready cache,
                // we lock it externally in sake of db/cache
                // synchronisation
                userGroupsDao.unlockAdminUsers();
                getAllUsers(true).stream()
                        .filter(u -> u.isAdmin())
                        .forEach(u -> {
                                    unRegisterLockedOutUsers(u.getUsername());
                                }
                        );
            }
        } catch (SQLException e) {
            log.debug("Could not unlock all admin users, cause: {}", e);
            throw new StorageException(
                    "Could not unlock all admin users, because " + e.getMessage()
            );
        }
    }

    /**
     * @param userName
     * @return incorrect login attempts for user
     */
    @Nonnull
    public int getIncorrectLoginAttempts(String userName) {
        try {
            return userGroupsDao.getIncorrectLoginAttempts(userName);
        } catch (SQLException e) {
            log.debug("Could not fetch incorrect login attempts for user {}, cause: {}", userName, e);
            throw new StorageException(
                    "Could not fetch incorrect login attempts for user '" +
                            userName + "', because " + e.getMessage()
            );
        }
    }

    /**
     * Registers incorrect login attempt
     *
     * @param userName
     * @throws StorageException
     */
    @Nonnull
    @Override
    public void registerIncorrectLoginAttempt(String userName) {
        User user = null;
        try {
            user = getUser(userName);
            if (user == null) return;
            userGroupsDao.registerIncorrectLoginAttempt(user);
            return;
        } catch (SQLException e) {
            log.debug("Could not register incorrect login attempt for user {}, cause: {}", userName, e);
            throw new StorageException(
                    "Could not register incorrect login attempt for user '" +
                            userName + "', because " + e.getMessage()
            );
        }
    }

    /**
     * Resets incorrect login attempts
     *
     * @param userName
     * @throws StorageException
     */
    @Nonnull
    @Override
    public void resetIncorrectLoginAttempts(String userName) {
        try {
            userGroupsDao.resetIncorrectLoginAttempts(userName);
        } catch (SQLException e) {
            log.debug("Could not reset incorrect login attempts for user {}, cause: {}", userName, e);
            throw new StorageException(
                    "Could not reset incorrect login attempts for user '" +
                            userName + "', because " + e.getMessage()
            );
        }
    }

    /**
     * @return Collection of locked out users
     */
    @Override
    public Set<String> getLockedUsers() {
        try {
            Set<String> lockedUsers = userGroupsDao.getLockedUsersNames();
            lockedUsers.addAll(lockedUsersCache.asMap().keySet());
            return lockedUsers;
        } catch (SQLException e) {
            log.debug("Could not list locked in users, cause: {}", e);
            throw new StorageException(
                    "Could not list locked in users, because " + e.getMessage()
            );
        }
    }

    /**
     * Checks whether given user is locked
     * <p>
     * note: this method using caching in sake
     * of DB load preventing
     *
     * @param userName
     * @return boolean
     */
    @Override
    public boolean isUserLocked(String userName) {
        if (shouldCacheLockedUsers()) {
            if (lockedUsersCache.getIfPresent(userName) != null)
                return true;
            User user = getUser(userName);
            if (user != null && user.isLocked()) {
                registerLockedOutUser(user.getUsername());
                return user.isLocked();
            }
        } else {
            User user = getUser(userName);
            if (user != null) {
                return user.isLocked();
            }
        }
        return false;
    }

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
    @Override
    public long getNextLogin(String userName) {
        try {
            Long lastAccessTime = userAccessUsersCache.getIfPresent(userName);
            if (lastAccessTime != null) {
                int incorrectLoginAttempts = userGroupsDao.getIncorrectLoginAttempts(userName);
                return getNextLogin(incorrectLoginAttempts, lastAccessTime);
            }
        } catch (SQLException e) {
            log.debug("getNextLogin() failed, Cause: {}", e);
        }
        return -1;
    }

    /**
     * Calculates next login if user previously
     * failed to authenticate, and now have to
     * wait before before performing another login
     * attempt,
     * <p>
     * - delay becomes affective after {@link UserGroupServiceImpl.OK_INCORRECT_LOGINS}
     * - maximum possible delay is {@link UserGroupServiceImpl.MAX_LOGIN_DELAY}
     *
     * @param incorrectLoginAttempts
     * @param lastAccessTimeMillis
     * @return login delay or -1 if user login should
     * not be delayed (e.g last login was successful) or
     * OK_INCORRECT_LOGINS not exceeded yet
     */
    @Nonnull
    @Override
    public long getNextLogin(int incorrectLoginAttempts, long lastAccessTimeMillis) {
        if (incorrectLoginAttempts >= OK_INCORRECT_LOGINS) {
            long delay = Long.valueOf(
                    (incorrectLoginAttempts - OK_INCORRECT_LOGINS) * LOGIN_DELAY_MULTIPLIER).longValue();
            if (delay != 0)
                return lastAccessTimeMillis + (delay <= MAX_LOGIN_DELAY ? delay : MAX_LOGIN_DELAY);
        }
        return -1;
    }

    /**
     * Changes user password
     *
     * @param user
     * @param newSaltedPassword
     * @return sucess/failure
     * @throws StorageException if persisting password fails
     */
    @Override
    public void changePassword(UserInfo user, SaltedPassword newSaltedPassword) {
        try {
            if (user == null) throw new UsernameNotFoundException("User '" + user.getUsername() + "' is not exist");
            if (!user.isUpdatableProfile()) {
                throw new PasswordChangeException("The specified user is not permitted to reset his password.");
            }
            userGroupsDao.changePassword(user.getUsername(), newSaltedPassword);
            userGroupsDao.revalidatePassword(user.getUsername());
        } catch (SQLException e) {
            throw new StorageException(
                    "Changing password for \"" + user.getUsername() + "\" has failed, " + e.getMessage(), e);
        }
    }

    /**
     * Makes user password expired
     *
     * @param userName
     */
    @Override
    public void expireUserPassword(String userName) {
        try {
            if (getUser(userName) != null) {
                userGroupsDao.expireUserPassword(userName);
                return;
            }
            throw new UsernameNotFoundException("User " + userName + " is not exist");
        } catch (SQLException e) {
            throw new StorageException("Expiring user password for \"" + userName + "\" has failed, " + e.getMessage(),
                    e);
        }
    }

    /**
     * Makes user password not expired
     *
     * @param userName
     */
    @Override
    public void revalidatePassword(String userName) {
        try {
            if (getUser(userName) != null) {
                userGroupsDao.revalidatePassword(userName);
                return;
            }
            throw new UsernameNotFoundException("User " + userName + " is not exist");
        } catch (SQLException e) {
            throw new StorageException("Expiring user password for \"" + userName + "\" has failed, " + e.getMessage(), e);
        }
    }

    /**
     * Makes all users passwords expired
     */
    @Override
    public void expirePasswordForAllUsers() {
        try {
            userGroupsDao.expirePasswordForAllUsers();
        } catch (SQLException e) {
            throw new StorageException("Expiring passwords for all users has failed, " + e.getMessage(), e);
        }
    }

    /**
     * Makes all users passwords not expired
     */
    @Override
    public void revalidatePasswordForAllUsers() {
        try {
            userGroupsDao.revalidatePasswordForAllUsers();
        } catch (SQLException e) {
            throw new StorageException("UnExpiring passwords for all users has failed, " + e.getMessage(), e);
        }
    }

    /**
     * @return whether locked out users should be cached
     */
    private boolean shouldCacheLockedUsers() {
        return CACHE_BLOCKED_USERS;
    }

    /**
     * Registers locked out user in cache
     *
     * @param userName
     */
    private void registerLockedOutUser(String userName) {
        if (shouldCacheLockedUsers())
            lockedUsersCache.put(userName, System.currentTimeMillis());
    }

    /**
     * Unregisters locked out user/s from cache
     *
     * @param user a user name to unlock or all users via ALL_USERS
     *             {@see UserGroupServiceImpl.ALL_USERS}
     */
    private void unRegisterLockedOutUsers(String user) {
        if (shouldCacheLockedUsers()) {
            if (ALL_USERS.equals(user)) {
                lockedUsersCache.invalidateAll();
                ;
            } else {
                lockedUsersCache.invalidate(user);
            }
        }
    }

    private GroupInfo groupToGroupInfo(Group group) {
        MutableGroupInfo result = InfoFactoryHolder.get().createGroup(group.getGroupName());
        result.setDescription(group.getDescription());
        result.setNewUserDefault(group.isNewUserDefault());
        result.setRealm(group.getRealm());
        result.setRealmAttributes(group.getRealmAttributes());
        return result;
    }

    private Group groupInfoToGroup(long groupId, GroupInfo groupInfo) {
        return new Group(groupId, groupInfo.getGroupName(), groupInfo.getDescription(),
                groupInfo.isNewUserDefault(), groupInfo.getRealm(), groupInfo.getRealmAttributes());
    }

    private UserInfo userToUserInfo(User user) throws SQLException {
        return userToUserInfoWithProperties(user, null);
    }

    private UserInfo userToUserInfoWithProperties(User user, Map<Long, Set<UserPropertyInfo>> allUserProperties)
            throws SQLException {
        UserInfoBuilder builder = new UserInfoBuilder(user.getUsername());
        Set<UserGroupInfo> groups = new HashSet<>(user.getGroups().size());
        for (UserGroup userGroup : user.getGroups()) {
            Group groupById = userGroupsDao.findGroupById(userGroup.getGroupId());
            if (groupById != null) {
                String groupname = groupById.getGroupName();
                groups.add(InfoFactoryHolder.get().createUserGroup(groupname, userGroup.getRealm()));
            } else {
                log.error("Group ID " + userGroup.getGroupId() + " does not exists!" +
                        " Skipping add group for user " + user.getUsername());
            }
        }
        builder.password(new SaltedPassword(user.getPassword(), user.getSalt())).email(user.getEmail())
                .admin(user.isAdmin()).enabled(user.isEnabled()).updatableProfile(user.isUpdatableProfile())
                .groups(groups);
        MutableUserInfo userInfo = builder.build();
        userInfo.setTransientUser(false);
        userInfo.setGenPasswordKey(user.getGenPasswordKey());
        userInfo.setRealm(user.getRealm());
        userInfo.setPrivateKey(user.getPrivateKey());
        userInfo.setPublicKey(user.getPublicKey());
        userInfo.setLastLoginTimeMillis(user.getLastLoginTimeMillis());
        userInfo.setLastLoginClientIp(user.getLastLoginClientIp());
        userInfo.setBintrayAuth(user.getBintrayAuth());
        userInfo.setLocked(user.isLocked());
        userInfo.setCredentialsExpired(user.isCredentialsExpired());
        if (MapUtils.isNotEmpty(allUserProperties) && allUserProperties.get(user.getUserId()) != null) {
            userInfo.setUserProperties(allUserProperties.get(user.getUserId()));
        }
        return userInfo;
    }

    private User userInfoToUser(long userId, UserInfo userInfo) throws SQLException {
        User u = new User(userId, userInfo.getUsername(), userInfo.getPassword(), userInfo.getSalt(),
                userInfo.getEmail(), userInfo.getGenPasswordKey(),
                userInfo.isAdmin(), userInfo.isEnabled(), userInfo.isUpdatableProfile(), userInfo.getRealm(),
                userInfo.getPrivateKey(),
                userInfo.getPublicKey(), userInfo.getLastLoginTimeMillis(), userInfo.getLastLoginClientIp(),
                userInfo.getBintrayAuth(), userInfo.isLocked(), userInfo.isCredentialsExpired());
        Set<UserGroupInfo> groups = userInfo.getGroups();
        Set<UserGroup> userGroups = new HashSet<>(groups.size());
        for (UserGroupInfo groupInfo : groups) {
            Group groupByName = userGroupsDao.findGroupByName(groupInfo.getGroupName());
            if (groupByName != null) {
                userGroups.add(new UserGroup(u.getUserId(), groupByName.getGroupId(), groupInfo.getRealm()));
            } else {
                log.error("Group named " + groupInfo.getGroupName() + " does not exists!" +
                        " Skipping add group for user " + userInfo.getUsername());
            }
        }
        u.setGroups(userGroups);
        return u;
    }

    /**
     * Calculates user login delay multiplier,
     * the value (security.loginBlockDelay) is
     * taken from system properties file,
     * <p>
     * delay may not exceed {@link UserGroupServiceImpl#MAX_LOGIN_DELAY}
     *
     * @return user login delay multiplier
     */
    private static int getLoginDelayMultiplier() {
        int userDefinedDelayMultiplier = ConstantValues.loginBlockDelay.getInt();
        if (userDefinedDelayMultiplier <= MAX_LOGIN_DELAY)
            return userDefinedDelayMultiplier;
        log.warn(
                String.format(
                        "loginBlockDelay '%d' has exceeded maximum allowed delay '%d', " +
                                "which will be used instead", userDefinedDelayMultiplier, MAX_LOGIN_DELAY
                )
        );
        return MAX_LOGIN_DELAY;
    }

    @Override
    public Set<PasswordExpiryUser> getUsersWhichPasswordIsAboutToExpire(int daysToNotifyBefore, int daysToKeepPassword) {
        //User error = notification day > expiration day will cause mail to never be sent. Just send a day before
        if(daysToKeepPassword < daysToNotifyBefore) {
            if(daysToKeepPassword >= 2) {
                daysToNotifyBefore = daysToKeepPassword - 1;
            } else {
                daysToNotifyBefore = 1;
            }
        }
        long millisDaysToNotifyBefore = daysToNotifyBefore * MILLIS_IN_DAY;
        long millisDaysToKeepPassword = daysToKeepPassword * MILLIS_IN_DAY;
        long now = DateTime.now().getMillis();
        try {
            return userGroupsDao.getPasswordExpiredUsersByFilter(
                    (Long creationTime) ->
                            shouldNotifyUser(creationTime, now, millisDaysToKeepPassword, millisDaysToNotifyBefore));
        } catch (SQLException e) {
            log.debug("Could not fetch users with passwords that about to expire, cause: {}", e);
            throw new StorageException("Could not fetch users with passwords that about to expire");
        }
    }

    /**
     * Marks user.credentialsExpired=True where password has expired
     *
     * @param daysToKeepPassword after what period password should be changed
     */
    @Override
    public void markUsersCredentialsExpired(int daysToKeepPassword) {
        long millisDaysToKeepPassword = daysToKeepPassword * MILLIS_IN_DAY;
        long now = DateTime.now().getMillis();
        try {
            Set<PasswordExpiryUser> usersToExpire = userGroupsDao.getPasswordExpiredUsersByFilter(
                    (Long creationTime) -> passwordExpired(creationTime, now, millisDaysToKeepPassword));
            boolean hadErrors = markExpiryByBatch(usersToExpire);
            if(hadErrors) {
                log.debug("Could not mark credentials expired.");
                throw new StorageException("Could not mark credentials expired, see logs for more details");
            }
        } catch (SQLException e) {
            log.debug("Could not mark credentials expired, cause: {}", e);
            throw new StorageException("Could not mark credentials expired, see logs for more details", e);
        }
    }

    private boolean markExpiryByBatch(Set<PasswordExpiryUser> expiredPasswordsUsers) throws SQLException {
        boolean hadErrors = false;
        int batchCounter = 0;
        Set<Long> batchOp = Sets.newHashSet();
        //Run in 20 user batches
        UserGroupStoreService txMe = ContextHelper.get().beanForType(UserGroupStoreService.class);
        for (PasswordExpiryUser expiredUser : expiredPasswordsUsers) {
            batchOp.add(expiredUser.getUserId());
            batchCounter++;
            if (batchCounter == 100) {
                try {
                    txMe.expirePasswordForUserIds(batchOp);
                } catch (SQLException se) {
                    log.error(se.getMessage(), se);
                    hadErrors = true;
                }
                batchCounter = 0;
                batchOp.clear();
            }
        }
        //If we had less then 100 at the end make sure to mark them as well
        if(batchCounter > 0) {
            txMe.expirePasswordForUserIds(batchOp);
        }
        return hadErrors;
    }

    public void expirePasswordForUserIds(Set<Long> userIds) throws SQLException {
        userGroupsDao.markCredentialsExpired(userIds.toArray(new Long[userIds.size()]));
    }

    /**
     * Checks whether user password is expired
     *
     * @param userName  a user name
     * @param expiresIn days for password to stay valid after creation
     * @return boolean
     */
    @Override
    public boolean isUserPasswordExpired(String userName, int expiresIn) {
        boolean isExpired = false;

        String passwordCreatedProperty = findUserProperty(userName, "passwordCreated");
        DateTime created = null;
        if (Strings.isNullOrEmpty(passwordCreatedProperty)) {
            log.debug("Password creation for user {} was not found, initiating default value (now)", userName);
            created = DateTime.now();
            addUserProperty(userName, "passwordCreated", Long.toString(created.getMillis()));
        } else {
            created = new DateTime(Long.valueOf(passwordCreatedProperty));
        }

        log.debug("Password creation for user {} is {}, password expires after {}", userName, created, expiresIn);
        if (isExpired = created.plusDays(expiresIn).isBeforeNow()) {
            log.debug("User {} credentials have expired (updating profile accordingly)", userName);
            expireUserPassword(userName);
        }

        // todo: [mp] set user.credentialsExpired=True if expired indeed

        return isExpired;
    }

    /**
     * @param userName
     * @return the date when last password was created
     * @throws SQLException
     */
    @Override
    public Long getUserPasswordCreationTime(String userName) {
        try {
            return userGroupsDao.getUserPasswordCreationTime(userName);
        } catch (SQLException e) {
            log.debug("Could not check when credentials were created, cause: {}", e);
            throw new StorageException("Could not check when credentials were created, see logs for more details");
        }
    }

    private boolean passwordExpired(long creationTime, long millisNow, long millisDaysToKeepPassword) {
        return millisNow > (creationTime + millisDaysToKeepPassword);
    }

    public boolean shouldNotifyUser (long creationTime, long millisNow, long millisDaysToKeepPassword,
                                     long millisDaysToNotifyBefore) {
        // now is after the (expiration - notification days) == inside the notification window
        return (millisNow >= ((creationTime + millisDaysToKeepPassword) - millisDaysToNotifyBefore))
                // now is not pass expiration notification day + 1 day (end of notification window)
                && (millisNow <= ((creationTime + millisDaysToKeepPassword) - millisDaysToNotifyBefore) + MILLIS_IN_DAY);
    }
}