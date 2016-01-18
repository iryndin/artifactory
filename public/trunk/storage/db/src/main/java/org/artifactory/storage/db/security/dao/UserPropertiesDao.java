package org.artifactory.storage.db.security.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.artifactory.model.xstream.security.UserProperty;
import org.artifactory.security.UserPropertyInfo;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A dao for the user_props table.
 * This table contains any extra data or properties connected to users that may
 * be required by external authentication methods.
 *
 * @author Travis Foster
 */
@Repository
public class UserPropertiesDao extends BaseDao {

    @Autowired
    public UserPropertiesDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    public long getUserIdByProperty(String key, String val) throws SQLException {
        ResultSet rs = null;
        try {
            String sel = "SELECT user_id FROM user_props ";
            sel += "WHERE prop_key like '%" + key + "' AND prop_value = ?";
            rs = jdbcHelper.executeSelect(sel, val);
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } finally {
            DbUtils.close(rs);
        }
    }

    public String getUserProperty(String username, String key) throws SQLException {
        ResultSet rs = null;
        try {
            String sel = "SELECT d.prop_value FROM users u INNER JOIN user_props d ON (u.user_id = d.user_id) ";
            sel += "WHERE u.username = ? AND d.prop_key = ?";
            rs = jdbcHelper.executeSelect(sel, username, key);
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } finally {
            DbUtils.close(rs);
        }
    }

    public boolean deleteProperty(long uid, String key) throws SQLException {
        String del = "DELETE FROM user_props WHERE user_id = ? AND prop_key = ?";
        return jdbcHelper.executeUpdate(del, uid, key) == 1;
    }

    public List<UserProperty> getPropertiesForUser(String username) throws SQLException {
        ResultSet rs = null;
        List<UserProperty> results = Lists.newArrayList();
        try {
            String sel = "SELECT d.user_id,d.prop_key,d.prop_value FROM users u INNER JOIN user_props d ON (u.user_id = d.user_id) ";
            sel += "WHERE u.username = ?";
            rs = jdbcHelper.executeSelect(sel, username);
            while (rs.next()) {
                results.add(propertyFromResultSet(rs));
            }
            return results;
        } finally {
            DbUtils.close(rs);
        }
    }

    public Map<Long, Set<UserPropertyInfo>> getAllUserProperties() throws SQLException {
        ResultSet rs = null;
        Set<UserPropertyInfo> results = null;
        Map<Long, Set<UserPropertyInfo>> userPropertyMap = Maps.newHashMap();
        try {
            String sel = "SELECT user_id,prop_key,prop_value FROM user_props order by user_id ";
            rs = jdbcHelper.executeSelect(sel);
            while (rs.next()) {
                Long userId = rs.getLong(1);
                if (userPropertyMap.get(userId) == null) {
                    results = Sets.newHashSet();
                    results.add(propertyFromData(userId, rs));
                    userPropertyMap.put(userId, results);
                } else {
                    results.add(propertyFromData(userId, rs));
                }
            }
            return userPropertyMap;
        } finally {
            DbUtils.close(rs);
        }
    }


    private UserProperty propertyFromResultSet(ResultSet resultSet) throws SQLException {
        long propId = resultSet.getLong(1);
        String propKey = resultSet.getString(2);
        String propValue = emptyIfNull(resultSet.getString(3));
        return new UserProperty(propId, propKey, propValue);
    }

    private UserProperty propertyFromData(long userId, ResultSet resultSet) throws SQLException {
        String propKey = resultSet.getString(2);
        String propValue = emptyIfNull(resultSet.getString(3));
        return new UserProperty(userId, propKey, propValue);
    }
    public void deletePropertyFromAllUsers(String propertyKey) throws SQLException {
        String del = "DELETE FROM user_props WHERE prop_key = ?";
        jdbcHelper.executeUpdate(del, propertyKey);
    }
    /**
     * find user id by name and add property to that user
     *
     * @param id - user id
     * @param key      - prop key
     * @param val      - prop password
     * @return - if true adding property succeeded
     * @throws SQLException
     */
    public boolean addUserPropertyById(long id, String key, String val) throws SQLException {
        deleteProperty(id, key);
        String ins = "INSERT INTO user_props (user_id, prop_key, prop_value) VALUES (?, ?, ?)";
        int updateStatus = jdbcHelper.executeUpdate(ins, id, key, val);
        return updateStatus == 1;
    }
    /**
     * Sets new passwordCreated for all users where it available
     *
     * @param dateInMillis
     * @return result
     * @throws SQLException
     */
    public boolean resetPasswordCreatedForAllUsers(String dateInMillis) throws SQLException {
        String ins = "UPDATE user_props SET prop_value = ? WHERE prop_key = 'passwordCreated'";
        int updateStatus = jdbcHelper.executeUpdate(ins, dateInMillis);
        return updateStatus == 1;
    }
    /**
     * find user id by name and add property to that user
     *
     * @param userName - user name
     * @param key      - prop key
     * @param val      - prop password
     * @return - if true adding property succeeded
     * @throws SQLException
     */
    public boolean addUserPropertyByUserName(String userName, String key, String val) throws SQLException {
        long userId = 0;
        userId = getUserIdByUserName(userName, userId);
        if (userId > 0) {
            return addUserPropertyById(userId, key, val);
        }
        return false;
    }

    /**
     * @param userName
     * @param userId
     * @return
     * @throws SQLException
     */
    private long getUserIdByUserName(String userName, long userId) throws SQLException {
        ResultSet rs = null;
        try {
            String sel = "SELECT user_id from users where username=? ";
            rs = jdbcHelper.executeSelect(sel, userName);
            if (rs.next()) {
                userId = rs.getLong(1);
            }
        } finally {
            DbUtils.close(rs);
        }
        return userId;
    }

    /**
     *
     * @param userName
     * @param key
     * @return
     * @throws SQLException
     */
    public boolean deletePropertyByUserName(String userName, String key) throws SQLException {
        long userId = 0;
        userId = getUserIdByUserName(userName, userId);
        if (userId > 0) {
            return deleteProperty(userId, key);
        }
        return false;
    }
}
