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
package org.artifactory.update.security;

import org.apache.log4j.Logger;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.config.User;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.springframework.context.ApplicationContextException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.object.MappingSqlQuery;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.security.userdetails.jdbc.JdbcDaoImpl;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ExtendedJdbcDaoImpl extends JdbcDaoImpl implements ExtendedUserDetailsService {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ExtendedJdbcDaoImpl.class);

    public static final String ALL_USERS_QUERY = "SELECT * FROM users";
    public static final String ALL_ADMIN_QUERY = "SELECT username FROM authorities where authority = 'ADMIN'";

    protected MappingSqlQuery allUsersMapping;
    protected MappingSqlQuery allAdmins;
    private Set<String> admins;

    public List<SimpleUser> getAllUsers() {
        return getAllUsers(true);
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
    public List<SimpleUser> getAllUsers(boolean includeAdmins) {
        List<SimpleUser> users = allUsersMapping.execute();
        if (includeAdmins) {
            return users;
        } else {
            //Filter out admins
            List<SimpleUser> nonAdmins = new ArrayList<SimpleUser>(users.size());
            for (SimpleUser user : users) {
                boolean admin = ExportSecurityManager.isAdmin(user);
                if (!admin) {
                    nonAdmins.add(user);
                }
            }
            return nonAdmins;
        }
    }

    private Set<String> getAdmins() {
        if (admins == null) {
            allAdmins.execute();
            //noinspection unchecked
            admins = new HashSet<String>(allAdmins.execute());
        }
        return admins;
    }

    public boolean createUser(SimpleUser user) {
        String username = user.getUsername();
        JdbcTemplate userTemplate = getJdbcTemplate();
        try {
            loadUserByUsername(username);
            //Return false if the user already exists
            return false;
        } catch (UsernameNotFoundException e) {
            String password = user.getPassword();
            userTemplate.execute(
                    "INSERT INTO users VALUES ('" + username + "', '" + password + "', 1, 1)");
            GrantedAuthority[] authorities = user.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                userTemplate.execute(
                        "INSERT INTO authorities VALUES ('" + username + "', '" +
                                authority.getAuthority() + "')");
            }
        }
        return true;
    }

    public void updateUser(SimpleUser user) {
        String username = user.getUsername();
        JdbcTemplate userTemplate = getJdbcTemplate();
        String password = user.getPassword();
        if (password != null) {
            userTemplate.execute(
                    "UPDATE users set password ='" + password + "' where username = '" + username +
                            "'");
        }
        userTemplate.execute("DELETE from authorities where username = '" + username + "'");
        GrantedAuthority[] authorities = user.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            userTemplate.execute(
                    "INSERT INTO authorities VALUES ('" + username + "', '" +
                            authority.getAuthority() + "')");
        }
    }

    public void deleteUser(String username) {
        JdbcTemplate userTemplate = getJdbcTemplate();
        //Delete user's acls
        ArtifactoryContext context = ContextHelper.get();
        ExtendedJdbcAclDao aclDao = context.beanForType(ExportSecurityManager.class).getAclDao();
        aclDao.deleteAcls(username);
        userTemplate.execute("DELETE from authorities where username = '" + username + "'");
        userTemplate.execute("DELETE from users where username = '" + username + "'");
    }

    protected void initDao() throws ApplicationContextException {
        super.initDao();
        DataSource ds = getDataSource();
        allUsersMapping = new AllUsersMapping(ds);
        allAdmins = new AllAdmins(ds);
    }

    /**
     * Query object to look up all users.
     */
    protected class AllUsersMapping extends MappingSqlQuery {
        /*
	username VARCHAR(50) NOT NULL PRIMARY KEY,
	password VARCHAR(50) NOT NULL,
	enabled SMALLINT NOT NULL,
	updatable_profile SMALLINT NOT NULL
         */
        private ResultSetMetaData rsmd = null;
        private int enabledColumn = 0;
        private int updatedProfileColumn = 0;

        protected AllUsersMapping(DataSource ds) {
            super(ds, ALL_USERS_QUERY);
            compile();
        }

        @SuppressWarnings({"UnnecessaryLocalVariable"})
        protected Object mapRow(ResultSet rs, int rownum)
                throws SQLException {
            if (rsmd == null) {
                rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    if ("enabled".equalsIgnoreCase(rsmd.getColumnName(i))) {
                        enabledColumn = i;
                    } else if ("updatable_profile".equalsIgnoreCase(rsmd.getColumnName(i))) {
                        updatedProfileColumn = i;
                    }
                }
            }
            String username = rs.getString("username");
            String password = rs.getString("password");
            boolean enabled = true;
            if (enabledColumn != 0) {
                enabled = rs.getShort(enabledColumn) != 0;
            }
            boolean updProfile = true;
            if (updatedProfileColumn != 0) {
                updProfile = rs.getShort(updatedProfileColumn) != 0;
            }
            User user = new User(username);
            user.setPassword(password);
            user.setEnabled(enabled);
            user.setUpdatableProfile(updProfile);
            user.setAdmin(getAdmins().contains(username));
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);

            return user.toSimpleUser();
        }
    }

    private class AllAdmins extends MappingSqlQuery {
        /*
	username VARCHAR(50) NOT NULL,
	authority VARCHAR(50) NOT NULL
         */
        protected AllAdmins(DataSource ds) {
            super(ds, ALL_ADMIN_QUERY);
            compile();
        }

        protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
            return rs.getString(1);
        }
    }
}
