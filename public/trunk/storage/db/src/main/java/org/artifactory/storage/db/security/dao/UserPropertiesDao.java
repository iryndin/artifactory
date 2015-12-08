package org.artifactory.storage.db.security.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.artifactory.model.xstream.security.UserProperty;
import org.artifactory.security.UserPropertyInfo;
import org.artifactory.storage.db.util.BaseDao;
import org.artifactory.storage.db.util.DbUtils;
import org.artifactory.storage.db.util.JdbcHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A dao for the user_props table.
 * This table contains any extra data or properties connected to users that may
 * be required by external authentication methods.
 *
 * @author Travis Foster
 */
@Repository
public class UserPropertiesDao extends BaseDao {
    private static final Logger log = LoggerFactory.getLogger(UserPropertiesDao.class);


    @Autowired
    public UserPropertiesDao(JdbcHelper jdbcHelper) {
        super(jdbcHelper);
    }

    public long getUserIdByProperty(String key, String val) throws SQLException {
        ResultSet rs = null;
        try {
            String sel = "SELECT user_id FROM user_props ";
            sel += "WHERE prop_key = ? AND prop_value = ?";
            rs = jdbcHelper.executeSelect(sel, key, val);
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
            String sel = "SELECT d.user_id,d.prop_key,d.prop_value FROM users u INNER JOIN user_props d USING (user_id) ";
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
     * find user id by name and add property to that user
     *
     * @param userName - user name
     * @param key      - prop key
     * @param val      - prop password
     * @return - if true adding property succeeded
     * @throws SQLException
     */
    public boolean addUserPropertyByUserName(String userName, String key, String val) throws SQLException {
        ResultSet rs = null;
        try {
            String sel = "SELECT user_id from users where username=? ";
            rs = jdbcHelper.executeSelect(sel, userName);
            if (rs.next()) {
                long userId = rs.getLong(1);
                if (addUserPropertyById(userId, key, val)) {
                    return true;
                }
            }
            return false;
        } finally {
            DbUtils.close(rs);
        }
    }

    public boolean deletePropertyByUserName(String userName, String key) throws SQLException {
        ResultSet rs = null;
        try {
            String sel = "SELECT user_id from users where username=? ";
            rs = jdbcHelper.executeSelect(sel, userName);
            if (rs.next()) {
                int userId = rs.getInt(1);
                if (deleteProperty(userId, key)) {
                    return true;
                }
            }
            return false;
        } finally {
            DbUtils.close(rs);
        }
    }
}
