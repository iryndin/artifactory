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
package org.artifactory.security;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.userdetails.jdbc.JdbcDaoImpl;
import org.apache.log4j.Logger;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.object.MappingSqlQuery;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
//TODO: [by yl] Cleanup persistency - move to hb8
public class ExtendedJdbcDaoImpl extends JdbcDaoImpl implements ExtendedUserDetailsService {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ExtendedJdbcDaoImpl.class);

    public static final String ALL_USERS_QUERY = "SELECT username FROM users";
    public static final String ALL_AUTHORITIES_QUERY = "SELECT authority FROM authorities";

    protected MappingSqlQuery allUsersMapping;
    protected MappingSqlQuery allAuthoritiesMapping;

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
                boolean admin = SecurityHelper.isAdmin(user);
                if (!admin) {
                    nonAdmins.add(user);
                }
            }
            return nonAdmins;
        }
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
        ExtendedJdbcAclDao aclDao = context.getSecurity().getAclDao();
        aclDao.deleteAcls(username);
        userTemplate.execute("DELETE from authorities where username = '" + username + "'");
        userTemplate.execute("DELETE from users where username = '" + username + "'");
    }

    protected void initMappingSqlQueries() {
        super.initMappingSqlQueries();
        DataSource ds = getDataSource();
        allUsersMapping = new AllUsersMapping(ds);
    }

    /**
     * Query object to look up all users.
     */
    protected class AllUsersMapping extends MappingSqlQuery {
        protected AllUsersMapping(DataSource ds) {
            super(ds, ALL_USERS_QUERY);
            compile();
        }

        @SuppressWarnings({"UnnecessaryLocalVariable"})
        protected Object mapRow(ResultSet rs, int rownum)
                throws SQLException {
            String username = rs.getString(1);
            //TODO: [by yl] We have n+1 here (it's a local db but is still ugly)
            //TODO: [by yl] We do not account for updatableProfile when construction the SimpleUser
            UserDetails details = loadUserByUsername(username);
            SimpleUser user = new SimpleUser(details);
            return user;
        }
    }
}
