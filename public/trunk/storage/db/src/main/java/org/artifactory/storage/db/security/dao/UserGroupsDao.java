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

package org.artifactory.storage.db.security.dao;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.common.Info;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.model.xstream.security.UserImpl;
import org.artifactory.sapi.security.SecurityConstants;
import org.artifactory.security.*;
import org.artifactory.storage.StorageProperties;
import org.artifactory.storage.db.DbType;
import org.artifactory.storage.db.security.entity.Group;
import org.artifactory.storage.db.security.entity.User;
import org.artifactory.storage.db.security.entity.UserGroup;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: 8/26/12
 * Time: 11:08 PM
 *
 * @author freds
 */
@Repository
public class UserGroupsDao extends BaseDao {

    private static final String INCORRECT_LOGIN_ATTEMPTS = "incorrect_login_attempts";

    @Autowired
    private UserPropertiesDao userPropertiesDao;

    @Autowired
    private StorageProperties storageProperties;

    @Autowired
    public UserGroupsDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    public int createGroup(Group group) throws SQLException {
        return jdbcHelper.executeUpdate("INSERT INTO groups VALUES(?, ?, ?, ?, ?, ?)",
                group.getGroupId(), group.getGroupName(), group.getDescription(),
                booleanAsByte(group.isNewUserDefault()), group.getRealm(), group.getRealmAttributes());
    }

    public int updateGroup(Group group) throws SQLException {
        return jdbcHelper.executeUpdate("UPDATE groups SET " +
                " description = ?, default_new_user = ?," +
                " realm = ?, realm_attributes = ?" +
                " WHERE group_id = ? AND group_name = ?",
                group.getDescription(), booleanAsByte(group.isNewUserDefault()),
                group.getRealm(), group.getRealmAttributes(),
                group.getGroupId(), group.getGroupName());
    }

    public int deleteGroup(String groupName) throws SQLException {
        Group group = findGroupByName(groupName);
        if (group == null) {
            // Group doesn't exist
            return 0;
        }
        int res = jdbcHelper.executeUpdate("DELETE FROM users_groups WHERE group_id = ?", group.getGroupId());
        res += jdbcHelper.executeUpdate("DELETE FROM groups WHERE group_id = ?", group.getGroupId());
        return res;
    }

    public int createUser(User user) throws SQLException {
        int res = jdbcHelper.executeUpdate("INSERT INTO users VALUES(" +
                        " ?," +
                        " ?, ?, ?," +
                        " ?, ?," +
                        " ?, ?, ?," +
                        " ?, ?, ?," +
                        " ?, ?," +
                        " ?, ?," +
                        " ?, ?, ?)",
                user.getUserId(),
                user.getUsername(), nullIfEmpty(user.getPassword()), nullIfEmpty(user.getSalt()),
                nullIfEmpty(user.getEmail()), nullIfEmpty(user.getGenPasswordKey()),
                booleanAsByte(user.isAdmin()), booleanAsByte(user.isEnabled()),
                booleanAsByte(user.isUpdatableProfile()),
                nullIfEmpty(user.getRealm()), nullIfEmpty(user.getPrivateKey()), nullIfEmpty(user.getPublicKey()),
                user.getLastLoginTimeMillis(), nullIfEmpty(user.getLastLoginClientIp()),
                user.getLastAccessTimeMillis(), nullIfEmpty(user.getLastAccessClientIp()),
                nullIfEmpty(user.getBintrayAuth()), booleanAsByte(user.isLocked()), booleanAsByte(user.isCredentialsExpired()));

        for (UserGroup userGroup : user.getGroups()) {
            res += jdbcHelper.executeUpdate("INSERT INTO users_groups VALUES (?, ?, ?)",
                    userGroup.getUserId(), userGroup.getGroupId(), userGroup.getRealm());
        }

        setPasswordCreatedNow(user.getUsername());

        return res;
    }

    /**
     * Updates user access details
     *
     * @param lastAccessTimeMillis
     * @param lastAccessClientIp
     * @param userName
     *
     * @return
     * @throws SQLException
     */
    public int updateUserAccess(long lastAccessTimeMillis, String lastAccessClientIp,
                                String userName) throws SQLException {
        return jdbcHelper.executeUpdate("UPDATE users SET " +
                        " last_access_time = ?, last_access_ip = ?" +
                        " WHERE username = ?",
                lastAccessTimeMillis, nullIfEmpty(lastAccessClientIp),
                userName
        );
    }

    public int updateUser(User user) throws SQLException {
        int res = jdbcHelper.executeUpdate("UPDATE users SET " +
                        " password = ?, salt = ?," +
                        " email = ?, gen_password_key = ?," +
                        " admin = ?, enabled = ?, updatable_profile = ?," +
                        " realm = ?, private_key = ?, public_key = ?," +
                        " last_login_time = ?, last_login_ip = ?," +
                        " last_access_time = ?, last_access_ip = ?," +
                        " bintray_auth = ?, credentials_expired = ?" +
                        " WHERE user_id = ? AND username = ?",
                nullIfEmpty(user.getPassword()), nullIfEmpty(user.getSalt()),
                nullIfEmpty(user.getEmail()), nullIfEmpty(user.getGenPasswordKey()),
                booleanAsByte(user.isAdmin()), booleanAsByte(user.isEnabled()),
                booleanAsByte(user.isUpdatableProfile()),
                nullIfEmpty(user.getRealm()), nullIfEmpty(user.getPrivateKey()), nullIfEmpty(user.getPublicKey()),
                user.getLastLoginTimeMillis(), nullIfEmpty(user.getLastLoginClientIp()),
                user.getLastAccessTimeMillis(), nullIfEmpty(user.getLastAccessClientIp()),
                nullIfEmpty(user.getBintrayAuth()), booleanAsByte(user.isCredentialsExpired()),
                user.getUserId(), user.getUsername());
        if (res == 1) {
            jdbcHelper.executeUpdate("DELETE FROM users_groups WHERE user_id = ?", user.getUserId());
            for (UserGroup userGroup : user.getGroups()) {
                res += jdbcHelper.executeUpdate("INSERT INTO users_groups VALUES (?, ?, ?)",
                        userGroup.getUserId(), userGroup.getGroupId(), userGroup.getRealm());
            }
        }
        return res;
    }

    public int deleteUser(String username) throws SQLException {
        long userId = findUserIdByUsername(username);
        if (userId == 0L) {
            // User already deleted
            return 0;
        }
        int res = jdbcHelper.executeUpdate("DELETE FROM users_groups WHERE user_id = ?", userId);
        res += jdbcHelper.executeUpdate("DELETE FROM user_props WHERE user_id = ?", userId);
        res += jdbcHelper.executeUpdate("DELETE FROM users WHERE user_id = ?", userId);
        return res;
    }

    public long findUserIdByUsername(String username) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT user_id FROM users WHERE username = ?", username);
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0L;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    public String findUsernameByUserId(long userId) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT username FROM users WHERE user_id = ?", userId);
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return null;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    public Map<Long, String> getAllUsernamePerIds() throws SQLException {
        ResultSet resultSet = null;
        Map<Long, String> results = new HashMap<>();
        try {
            String query = "SELECT user_id, username FROM users";
            resultSet = jdbcHelper.executeSelect(query);
            while (resultSet.next()) {
                results.put(resultSet.getLong(1), resultSet.getString(2));
            }
        } finally {
            DbUtils.close(resultSet);
        }
        return results;
    }

    public Map<Long, String> getAllGroupNamePerIds() throws SQLException {
        ResultSet resultSet = null;
        Map<Long, String> results = new HashMap<>();
        try {
            String query = "SELECT group_id, group_name FROM groups";
            resultSet = jdbcHelper.executeSelect(query);
            while (resultSet.next()) {
                results.put(resultSet.getLong(1), resultSet.getString(2));
            }
        } finally {
            DbUtils.close(resultSet);
        }
        return results;
    }

    public Collection<User> getAllUsers(boolean includeAdmins) throws SQLException {
        ResultSet resultSet = null;
        Map<Long, User> results = new HashMap<>();
        Map<Long, Set<UserGroup>> groups = new HashMap<>();
        try {
            String query = "SELECT * FROM users";
            if (!includeAdmins) {
                query += " WHERE admin != 1";
            }
            resultSet = jdbcHelper.executeSelect(query);
            while (resultSet.next()) {
                User user = userFromResultSet(resultSet);
                results.put(user.getUserId(), user);
                groups.put(user.getUserId(), new HashSet<UserGroup>());
            }
        } finally {
            DbUtils.close(resultSet);
        }
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM users_groups");
            while (resultSet.next()) {
                UserGroup userGroup = userGroupFromResultSet(resultSet);
                Set<UserGroup> userGroups = groups.get(userGroup.getUserId());
                // Group not found due to admin filtering
                if (userGroups != null) {
                    userGroups.add(userGroup);
                }
            }
        } finally {
            DbUtils.close(resultSet);
        }
        for (Map.Entry<Long, Set<UserGroup>> entry : groups.entrySet()) {
            User user = results.get(entry.getKey());
            if (user == null) {
                throw new IllegalStateException("Map population of users and groups failed!");
            } else {
                user.setGroups(entry.getValue());
            }
        }
        return results.values();
    }


    /**
     * get users and groups info with paging
     *
     * @param includeAdmins - include admin
     * @return list of users and group info
     * @throws SQLException
     */
    public Collection<Info> getUsersGroupsPaging(boolean includeAdmins, String orderBy,
            String startOffset, String limit, String direction) throws SQLException {
        ResultSet resultSet = null;
        String userGroupTable;
        /// get base user group query
        userGroupTable = getBaseUserGroupQuery(includeAdmins);
        // get paginated query
        Map<Long, Info> results = new LinkedHashMap<>();
        Map<Long, Set<UserGroupInfo>> usrGroupsMap = new LinkedHashMap<>();
        try {
            resultSet = jdbcHelper.executeSelect(userGroupTable);
            while (resultSet.next()) {
                userAndGroupFromResultSet(resultSet, results, usrGroupsMap);
            }
        } finally {
            DbUtils.close(resultSet);
        }
        return results.values();
    }

    /**
     * get all users count
     *
     * @param includeAdmins - if true include admin
     * @return
     */
    public long getAllUsersGroupsCount(boolean includeAdmins) {
        String countQuery;
        countQuery = getUserGroupCountQuery(includeAdmins);
        Long count = null;
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect(countQuery);
            while (resultSet.next()) {
                count = resultSet.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                DbUtils.close(resultSet);
            }
        }
        return count;
    }

    /**
     * get user groups count query with or without admin
     *
     * @param includeAdmins - if true include admin
     * @return count query
     */
    private String getUserGroupCountQuery(boolean includeAdmins) {
        String countQuery;
        if (includeAdmins) {
            countQuery = "select count(*) as cnt from (\n" +
                    "\t\t\tselect distinct users.user_id as id,username as principal,admin, 'user' as type\n" +
                    "\t\t\tfrom users\n" +
                    "\t\tunion\n" +
                    "\t\t\tselect distinct groups.group_id as id ,group_name as principal,0 as admin, 'group' as type\n" +
                    "\t\t\t from groups \n" +
                    "\t\t)   usergroups  ";
        } else {
            countQuery = "select count(*) as cnt from (\n" +
                    "\t\t\tselect distinct users.user_id as id,username as principal,admin,'user' as type\n" +
                    "\t\t\tfrom users WHERE admin != 1\n" +
                    "\t\tunion\n" +
                    "\t\t\tselect distinct groups.group_id as id ,group_name as principal,0 as admin,'group' as type\n" +
                    "\t\t\t from groups \n" +
                    "\t\t)   usergroups  ";
        }
        return countQuery;
    }

    /**
     * get base query and add pagination keys based on db type
     *
     * @param userGroupTable - user group base query
     * @return query with pagination support
     */
    private String getPaginatedQuery(String userGroupTable) {
        StringBuilder queryWriter = new StringBuilder();

        String innerQuery = queryWriter.append("Select * from (").append(userGroupTable + " as userAndGroups").
                append(" LEFT JOIN users_groups ON userAndGroups.id = users_groups.user_id").toString();
        return innerQuery;
    }

    /**
     * get base query to fetch users and groups data
     *
     * @param includeAdmins - fetch include admin
     * @return base query
     */
    private String getBaseUserGroupQuery(boolean includeAdmins) {
        String userGroupTable;
        if (!includeAdmins) {
            userGroupTable = "Select * from (select distinct users.user_id as id,username as principal,admin,'user' as type\n" +
                    "\t\t\tfrom users where admin != 1\n" +
                    "\t\tunion\n" +
                    "select distinct groups.group_id as id ,group_name as principal,0 as admin, 'group' as type\n" +
                    "\t\t\t from groups ) as userAndGroups \n" +
                    "\t\tLEFT JOIN users_groups ON userAndGroups.id = users_groups.user_id";
        } else {
            userGroupTable = "Select * from (select distinct users.user_id as id,username as principal,admin,'user' as type\n" +
                    "\t\t\tfrom users\n" +
                    "\t\tunion\n" +
                    "select distinct groups.group_id as id ,group_name as principal,0 as admin, 'group' as type\n" +
                    "\t\t\t from groups ) as userAndGroups \n" +
                    "\t\tLEFT JOIN users_groups ON userAndGroups.id = users_groups.user_id";
        }
        return userGroupTable;
    }

    public User findUserById(long userId) throws SQLException {
        ResultSet resultSet = null;
        User user = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM users WHERE user_id = ?", userId);
            if (resultSet.next()) {
                user = userFromResultSet(resultSet);
            }
        } finally {
            DbUtils.close(resultSet);
        }
        if (user != null) {
            user.setGroups(findUserGroupByUserId(userId));
        }
        return user;
    }

    /**
     * Searches user in DB
     *
     * @param username user to look for
     * @return {@link User}
     *
     * @throws SQLException in case of lookup failure
     */
    public User findUserByName(String username) throws SQLException {
        return findUserByName(username, true);
    }

    /**
     * Searches user in DB
     *
     * @param username user to look for
     * @param includeGroups if user groups to be included
     * @return {@link User}
     *
     * @throws SQLException in case of lookup failure
     */
    public User findUserByName(String username, boolean includeGroups) throws SQLException {
        ResultSet resultSet = null;
        User user = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM users WHERE username = ?", username);
            if (resultSet.next()) {
                user = userFromResultSet(resultSet);
            }
        } finally {
            DbUtils.close(resultSet);
        }
        if (user != null && includeGroups) {
            user.setGroups(findUserGroupByUserId(user.getUserId()));
        }
        return user;
    }

    public Group findGroupById(long groupId) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM groups WHERE group_id = ?", groupId);
            if (resultSet.next()) {
                return groupFromResultSet(resultSet);
            }
            return null;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    /**
     * Fond the group entity by name if found.
     * Returns null if no group with this name found.
     *
     * @param groupName The name of the group to find
     * @return The Group DB entity object
     * @throws SQLException If the query cannot be executed
     */
    @Nullable
    public Group findGroupByName(String groupName) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM groups WHERE group_name = ?", groupName);
            if (resultSet.next()) {
                return groupFromResultSet(resultSet);
            }
            return null;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    public int addUsersToGroup(long groupId, Collection<String> usernames, String realm) throws SQLException {
        if (usernames == null || usernames.isEmpty()) {
            throw new IllegalArgumentException("List of usernames to add group " + groupId + " to cannot be empty!");
        }
        // Find if the users passed already have the group
        Collection<String> toAddUsernames = usernames;
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT u.username" +
                    " FROM users u, users_groups ug" +
                    " WHERE ug.group_id = ?" +
                    " AND ug.user_id = u.user_id" +
                    " AND u.username IN (#)", groupId, usernames);
            if (resultSet.next()) {
                // Found some usernames that needs to be removed
                toAddUsernames = new HashSet<>(usernames);
                toAddUsernames.remove(resultSet.getString(1));
                while (resultSet.next()) {
                    toAddUsernames.remove(resultSet.getString(1));
                }
            }
        } finally {
            DbUtils.close(resultSet);
        }

        if (!toAddUsernames.isEmpty()) {
            return jdbcHelper.executeUpdate("INSERT INTO users_groups (user_id, group_id, realm)" +
                    " SELECT u.user_id, ?, ? FROM users u WHERE u.username IN (#)", groupId, realm, toAddUsernames);
        }
        return 0;
    }

    public int removeUsersFromGroup(long groupId, List<String> usernames) throws SQLException {
        return jdbcHelper.executeUpdate("DELETE FROM users_groups " +
                "WHERE group_id = ? " +
                "AND user_id IN (SELECT u.user_id FROM users u WHERE username IN (#))", groupId, usernames);
    }

    public Collection<Group> findGroups(GroupFilter filter) throws SQLException {
        List<Group> results = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM groups" + filter.filter);
            while (resultSet.next()) {
                results.add(groupFromResultSet(resultSet));
            }
            return results;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    private Set<UserGroup> findUserGroupByUserId(long userId) throws SQLException {
        final Set<UserGroup> result = new HashSet<>(1);
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM users_groups WHERE user_id = ?", userId);
            while (resultSet.next()) {
                result.add(userGroupFromResultSet(resultSet));
            }
            return result;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    public List<User> findUsersInGroup(long groupId) throws SQLException {
        List<User> results = new ArrayList<>();
        Set<UserGroup> userGroups = findUserGroupByGroupId(groupId);
        for (UserGroup userGroup : userGroups) {
            results.add(findUserById(userGroup.getUserId()));
        }
        return results;
    }

    public boolean adminUserExists() throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT COUNT(user_id) FROM users WHERE admin = 1");
            if (resultSet.next()) {
                return resultSet.getLong(1) > 0L;
            }
            return false;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    public int deleteAllGroupsAndUsers() throws SQLException {
        int res = jdbcHelper.executeUpdate("DELETE FROM users_groups");
        res += jdbcHelper.executeUpdate("DELETE FROM groups");
        res += jdbcHelper.executeUpdate("DELETE FROM user_props");
        res += jdbcHelper.executeUpdate("DELETE FROM users");
        return res;
    }

    private Set<UserGroup> findUserGroupByGroupId(long groupId) throws SQLException {
        Set<UserGroup> result = new HashSet<>(1);
        ResultSet resultSet = null;
        try {
            resultSet = jdbcHelper.executeSelect("SELECT * FROM users_groups WHERE group_id = ?", groupId);
            while (resultSet.next()) {
                result.add(userGroupFromResultSet(resultSet));
            }
            return result;
        } finally {
            DbUtils.close(resultSet);
        }
    }

    private User userFromResultSet(ResultSet rs) throws SQLException {
        String userName = rs.getString(2);
        return new User(rs.getLong(1), userName, emptyIfNull(rs.getString(3)),
                nullIfEmpty(rs.getString(4)), nullIfEmpty(rs.getString(5)), nullIfEmpty(rs.getString(6)),
                rs.getBoolean(7), rs.getBoolean(8), rs.getBoolean(9),
                nullIfEmpty(rs.getString(10)), nullIfEmpty(rs.getString(11)), nullIfEmpty(rs.getString(12)),
                rs.getLong(13), nullIfEmpty(rs.getString(14)), rs.getLong(15), nullIfEmpty(rs.getString(16)),
                nullIfEmpty(rs.getString(17)), rs.getBoolean(18), rs.getBoolean(19)
        );
    }

    /**
     * populate user row data to info (users & groups) data
     *
     * @param rs            - users  and group info result set
     * @param userGroupInfo - user group info map
     * @throws SQLException
     */
    private void userAndGroupFromResultSet(ResultSet rs, Map<Long, Info> userGroupInfo,
            Map<Long, Set<UserGroupInfo>> usrGroupsMap) throws SQLException {
        Long id = rs.getLong(1);
        String type = rs.getString(4);
        if (type.trim().equals("user")) {
            addUserInfo(rs, userGroupInfo, id, usrGroupsMap);
        } else {
            addGroupInfo(rs, userGroupInfo, id);
        }
        usrGroupsMap.forEach((keyID, groupValue) -> ((MutableUserInfo) userGroupInfo.get(keyID)).setGroups(groupValue));
    }

    /**
     * populate group row data to info (users & groups) data
     *
     * @param rs            - users  and group info result set
     * @param userGroupInfo - user group info map
     * @param id            - group id
     * @throws SQLException
     */
    private void addGroupInfo(ResultSet rs, Map<Long, Info> userGroupInfo, Long id) throws SQLException {
        String groupName = rs.getString(2);
        MutableGroupInfo groupInfo = InfoFactoryHolder.get().createGroup(groupName);
        userGroupInfo.put(id, groupInfo);
    }

    /**
     * add user Info to user & group info map
     *
     * @param rs            - users  and group info result set
     * @param userGroupInfo - user group info map
     * @param id            - row id
     * @throws SQLException
     */
    private void addUserInfo(ResultSet rs, Map<Long, Info> userGroupInfo, Long id,
            Map<Long, Set<UserGroupInfo>> usrGroupsMap) throws SQLException {
        if (userGroupInfo.get(id) == null) {
            MutableUserInfo userInfo = new UserInfoBuilder(rs.getString(2)).admin(rs.getBoolean(3)).build();
            Set<UserGroupInfo> groups = new HashSet<>();
            Long groupID = rs.getLong(6);
            String realm = rs.getString(7);
            addGroupToUser(groups, groupID, realm);
            usrGroupsMap.put(id, groups);
            userGroupInfo.put(id, userInfo);
        } else {
            Set<UserGroupInfo> groups = usrGroupsMap.get(id);
            Long groupID = rs.getLong(6);
            String realm = rs.getString(7);
            addGroupToUser(groups, groupID, realm);
        }
    }

    /**
     * @param groups
     * @param groupID
     * @throws SQLException
     */
    private void addGroupToUser(Set<UserGroupInfo> groups, Long groupID, String realm) throws SQLException {
        Group groupById = findGroupById(groupID);
        if (groupById != null) {
            String groupName = groupById.getGroupName();
            UserGroupInfo userGroup = InfoFactoryHolder.get().createUserGroup(groupName, realm);
            groups.add(userGroup);
        }
    }

    private Group groupFromResultSet(ResultSet rs) throws SQLException {
        return new Group(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4),
                rs.getString(5), rs.getString(6));
    }

    private UserGroup userGroupFromResultSet(ResultSet rs) throws SQLException {
        return new UserGroup(rs.getLong(1), rs.getLong(2), rs.getString(3));
    }

    public static enum GroupFilter {
        ALL(""),
        DEFAULTS(" WHERE default_new_user=1"),
        EXTERNAL(" WHERE realm IS NOT NULL AND realm != '" + SecurityConstants.DEFAULT_REALM + "'"),
        INTERNAL(" WHERE realm IS NULL OR realm = '" + SecurityConstants.DEFAULT_REALM + "'");

        final String filter;

        GroupFilter(String filter) {
            this.filter = filter;
        }
    }

    /**
     * Locks user on login failure
     *
     * @param user
     * @return result
     * @throws SQLException
     */
    public int lockUser(User user) throws SQLException {
        return jdbcHelper.executeUpdate("UPDATE users SET " +
                        " locked = ?" +
                        " WHERE user_id = ? AND username = ?",
                1, user.getUserId(), user.getUsername());
    }

    /**
     * Makes user password expired
     *
     * @param userName
     * @return result
     * @throws SQLException
     */
    public int expireUserPassword(String userName) throws SQLException {
        return setUserPasswordExpired(userName, true);
    }

    /**
     * Unexpires user's password
     *
     * @param userName
     * @return result
     * @throws SQLException
     */
    public int unExpireUserPassword(String userName) throws SQLException {
        int result1 = setUserPasswordExpired(userName, false);
        boolean result2 = setPasswordCreatedNow(userName);
        return !result2 ? -1 : result1;
    }

    /**
     * Sets password creation time to NOW
     *
     * @param userName
     * @return boolean
     *
     * @throws SQLException
     */
    private boolean setPasswordCreatedNow(String userName) throws SQLException {
        return userPropertiesDao.addUserPropertyByUserName(
                userName,
                "passwordCreated",
                Long.toString(DateTime.now().getMillis())
        );
    }

    /**
     * Sets credentials_expired value
     *
     * @param userName
     * @param passwordExpired
     *
     * @return result
     * @throws SQLException
     */
    private int setUserPasswordExpired(String userName, boolean passwordExpired) throws SQLException {
        return jdbcHelper.executeUpdate("UPDATE users SET " +
                        " credentials_expired = ?" +
                        " WHERE username = ?",
                passwordExpired ? 1 : 0, userName);
    }

    /**
     * Unlocks user after it has been locked out
     *
     * @param user
     * @return result
     * @throws SQLException
     */
    public boolean unlockUser(User user) throws SQLException {
        boolean resetIncorrectLoginAttemptsResult = resetIncorrectLoginAttempts(user.getUsername());
        int unlockResult = jdbcHelper.executeUpdate("UPDATE users SET " +
                        " locked = ?" +
                        " WHERE user_id = ? AND username = ?",
                0, user.getUserId(), user.getUsername()
        );

        return resetIncorrectLoginAttemptsResult && unlockResult == 1;
    }

    /**
     * Unlocks all locked out users
     *
     * @return result
     * @throws SQLException
     */
    public int unlockAdminUsers() throws SQLException {
        getLockedUsers().parallelStream()
            .filter(u -> u.isAdmin())
            .forEach(u -> {
                try {
                    userPropertiesDao.addUserPropertyById(
                            u.getUserId(),
                            INCORRECT_LOGIN_ATTEMPTS,
                            "0"
                    );
                } catch (SQLException e) {
                    ;
                }
            });
        return jdbcHelper.executeUpdate("UPDATE users SET " +
                        " locked = ?" +
                        " WHERE locked = ? and admin = ?",
                0, 1, 1);
    }

    /**
     * Unlocks all locked out users
     *
     * @return result
     * @throws SQLException
     */
    public int unlockAllUsers() throws SQLException {
        getLockedUsers().parallelStream().forEach(u -> {
            try {
                userPropertiesDao.addUserPropertyById(
                        u.getUserId(),
                        INCORRECT_LOGIN_ATTEMPTS,
                        "0"
                );
            } catch (SQLException e) {
                ;
            }
        });
        return jdbcHelper.executeUpdate("UPDATE users SET " +
                        " locked = ?" +
                        " WHERE locked = ?",
               0, 1);
    }

    /**
     * Sets whether password is expired for all users
     *
     * @return result
     * @throws SQLException
     */
    private int setPasswordExpiredForAllUsers(boolean passwordExpired) throws SQLException {
        return jdbcHelper.executeUpdate(
                "UPDATE users SET credentials_expired = ? " +
                        "WHERE password is not NULL and username != 'anonymous'",
                passwordExpired ? 1 : 0
        );
    }

    /**
     * Expires password for all users
     *
     * @return result
     * @throws SQLException
     */
    public int expirePasswordForAllUsers() throws SQLException {
        return setPasswordExpiredForAllUsers(true);
    }

    /**
     * Unexpires password for all users
     *
     * @return result
     * @throws SQLException
     */
    public int unExpirePasswordForAllUsers() throws SQLException {
        int result1 = setPasswordExpiredForAllUsers(false);
        boolean result2 = userPropertiesDao.resetPasswordCreatedForAllUsers(Long.toString(new DateTime().getMillis()));
        return !result2 ? -1 : result1;
    }

    /**
     * Registers logon failure
     *
     * @param user
     * @return result
     * @throws SQLException
     */
    public boolean registerIncorrectLoginAttempt(User user) throws SQLException {
        return userPropertiesDao.addUserPropertyById(
                user.getUserId(),
                INCORRECT_LOGIN_ATTEMPTS,
                String.valueOf(getIncorrectLoginAttempts(user.getUsername()) + 1)
        );
    }

    /**
     * @param userName
     * @return amount of logon failures
     *
     * @throws SQLException
     */
    public int getIncorrectLoginAttempts(String userName) throws SQLException {
        String ilp = userPropertiesDao.getUserProperty(
                userName,
                INCORRECT_LOGIN_ATTEMPTS
        );
        return Strings.isNullOrEmpty(ilp) ? 0 : Integer.valueOf(ilp).intValue();
    }

    /**
     * Resets logon failures
     *
     * @param userName
     * @return result
     * @throws SQLException
     */
    public boolean resetIncorrectLoginAttempts(String userName) throws SQLException {
        return userPropertiesDao.addUserPropertyByUserName(
                userName,
                INCORRECT_LOGIN_ATTEMPTS,
                "0"
        );
    }

    /**
     * @return locked out users
     */
    public List<User> getLockedUsers() throws SQLException {
        return getAllUsers(true).parallelStream()
                .filter(u -> u.isLocked())
                .collect(Collectors.toList());
    }

    /**
     * @return locked out users (usernames)
     */
    public Set <String> getLockedUsersNames() throws SQLException {
        ResultSet resultSet = null;
        Set <String> users = Sets.newHashSet();
        try {
            String query = "SELECT username FROM users where locked = 1";

            resultSet = jdbcHelper.executeSelect(query);
            if (resultSet!=null)
                while (resultSet.next()) {
                    users.add(resultSet.getString(1));
                }
        } finally {
            DbUtils.close(resultSet);
        }
        return users;
    }

    /**
     * Changes user's password
     *
     * @param userName
     * @param saltedPassword
     *
     * @return result
     * @throws SQLException
     */
    public int changePassword(String userName, SaltedPassword saltedPassword) throws SQLException {
        return jdbcHelper.executeUpdate("UPDATE users SET " +
                        " password = ?, salt = ?" +
                        " WHERE username = ?",
                saltedPassword.getPassword(),
                saltedPassword.getSalt(),
                userName
        );
    }


    /**
     * Retrieves all users that have passwordCreated in USER_PROPS and
     * (passwordCreated + dueDate) > pointer,
     *
     * e.g password will expire in les then defined 'pointer'
     *
     * @param millisNow today's date
     * @param millisDaysToStartNotificationsFrom back offset to start notifying from
     * @param millisDaysToKeepPassword expiration offset
     *
     * @return list of user's emails
     *
     * @throws SQLException
     */
    public Map<UserInfo, Long> getUsersWitchPasswordIsAboutToExpire(long millisNow,
            long millisDaysToStartNotificationsFrom, long millisDaysToKeepPassword) throws SQLException {
        ResultSet rs = null;
        String select;
        Map<UserInfo, Long> results = Maps.newHashMap();
        try {
            // Oracle (unlike all other supported DBs) does not support BIGINT
            // thus we using dedicated sql query using Oracle proprietary function
            if (DbType.ORACLE.equals(storageProperties.getDbType())) {
                select =
                        "SELECT username, email, prop_value FROM users u INNER JOIN user_props d ON (u.user_id = d.user_id) " +
                                "WHERE u.user_id = d.user_id " +
                                "and u.username != 'anonymous' " +
                                "and u.credentials_expired != 1 " +
                                "and u.password is NOT NULL " +
                                "and u.email is not NULL " +
                                "and d.prop_key = 'passwordCreated' " +
                                "and d.prop_value is not NULL " +
                                "and (? >= ((cast (d.prop_value as NUMBER(19, 0)) + ?) - ?)) " +
                                "and (? < (cast (d.prop_value as NUMBER(19, 0)) + ?))";
            } else {
                select =
                        "SELECT username, email, prop_value FROM users u INNER JOIN user_props d ON (u.user_id = d.user_id) " +
                                "WHERE u.user_id = d.user_id " +
                                "and u.username != 'anonymous' " +
                                "and u.credentials_expired != 1 " +
                                "and u.password is NOT NULL " +
                                "and u.email is not NULL " +
                                "and d.prop_key = 'passwordCreated' " +
                                "and d.prop_value is not NULL " +
                                "and (? >= ((CAST(d.prop_value as BIGINT) + ?) - ?)) " +
                                "and (? < (CAST(d.prop_value as BIGINT) + ?))";
            }

            rs = jdbcHelper.executeSelect(select,
                    millisNow, millisDaysToKeepPassword, millisDaysToStartNotificationsFrom,
                    millisNow, millisDaysToKeepPassword
            );

            while (rs.next()) {
                UserImpl u = new UserImpl();
                u.setUsername(rs.getString(1));
                u.setEmail(rs.getString(2));
                Long pc = rs.getLong(3);
                results.put(u, pc);
            }
            return results;
        } finally {
            DbUtils.close(rs);
        }
    }

    /**
     * Marks user.credentials_expired=1 where password has expired
     *
     * @param millisDaysToKeepPassword millis for password to stay valid after creation
     * @return result
     * @throws SQLException
     */
    public int markCredentialsExpired(long millisDaysToKeepPassword) throws SQLException {
        String query;
        // Oracle (unlike all other supported DBs) does not support BIGINT
        // thus we using dedicated sql query using Oracle proprietary function
        if (DbType.ORACLE.equals(storageProperties.getDbType())) {
            query = "update users set credentials_expired = 1 where username in (" +
                    "SELECT username FROM users u INNER JOIN user_props d ON (u.user_id = d.user_id) " +
                    "WHERE u.user_id = d.user_id " +
                    "and u.username != 'anonymous' " +
                    "and u.credentials_expired != 1 " +
                    "and u.password is NOT NULL " +
                    "and d.prop_key = 'passwordCreated' " +
                    "and (? > (cast (d.prop_value as NUMBER(19, 0)) + ?))" +
                    ")";
        } else {
            query = "update users set credentials_expired = 1 where username in (" +
                    "SELECT username FROM users u INNER JOIN user_props d ON (u.user_id = d.user_id) " +
                    "WHERE u.user_id = d.user_id " +
                    "and u.username != 'anonymous' " +
                    "and u.credentials_expired != 1 " +
                    "and u.password is NOT NULL " +
                    "and d.prop_key = 'passwordCreated' " +
                    "and (? > (CAST(d.prop_value as BIGINT) + ?))" +
                    ")";
        }
        return jdbcHelper.executeUpdate(query, DateTime.now().getMillis(), millisDaysToKeepPassword);
    }

    /**
     * @param userName
     * @return the date when last password was created
     * 
     * @throws SQLException
     */
    public Long getUserPasswordCreationTime(String userName) throws SQLException {
        String passwordCreated = userPropertiesDao.getUserProperty(userName, "passwordCreated");
        if (!Strings.isNullOrEmpty(passwordCreated))
            return Long.valueOf(passwordCreated);
        return null;
    }
}
