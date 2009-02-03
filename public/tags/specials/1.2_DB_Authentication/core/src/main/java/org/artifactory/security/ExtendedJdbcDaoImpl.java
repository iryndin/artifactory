package org.artifactory.security;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.userdetails.UserDetails;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.userdetails.jdbc.JdbcDaoImpl;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.object.MappingSqlQuery;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
//TODO: [by yl] Cleanup persistency - move to hb8 (and have xml import/export)
public class ExtendedJdbcDaoImpl extends JdbcDaoImpl implements ExtendedUserDetailsService {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ExtendedJdbcDaoImpl.class);

    public static final String ALL_USERS_QUERY = "SELECT username FROM users";
    public static final String ALL_AUTHORITIES_QUERY = "SELECT authority FROM authorities";
    public static final String USER_INSERT_STATEMENT =
            "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)";

    protected MappingSqlQuery allUsersMapping;
    protected MappingSqlQuery allAuthoritiesMapping;


    @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
    public List<UserDetails> getAllUsers() {
        List<UserDetails> users = allUsersMapping.execute();
        return users;
    }

    public boolean createUser(UserDetails details) {
        String username = details.getUsername();
        JdbcTemplate userTemplate = getJdbcTemplate();
        try {
            loadUserByUsername(username);
            //Return false if the user already exists
            return false;
        } catch (UsernameNotFoundException e) {
            String password = details.getPassword();
            userTemplate.execute(
                    "INSERT INTO users VALUES ('" + username + "', '" + password + "', 1)");
            GrantedAuthority[] authorities = details.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                userTemplate.execute(
                        "INSERT INTO authorities VALUES ('" + username + "', '" +
                                authority.getAuthority() + "')");
            }
        }
        return true;
    }

    public void updateUser(UserDetails details) {
        String username = details.getUsername();
        JdbcTemplate userTemplate = getJdbcTemplate();
        String password = details.getPassword();
        if (password != null) {
            userTemplate.execute(
                    "UPDATE users set password ='" + password + "' where username = '" + username +
                            "'");
        }
        userTemplate.execute("DELETE from authorities where username = '" + username + "'");
        GrantedAuthority[] authorities = details.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            userTemplate.execute(
                    "INSERT INTO authorities VALUES ('" + username + "', '" +
                            authority.getAuthority() + "')");
        }
    }

    public void deleteUser(String username) {
        JdbcTemplate userTemplate = getJdbcTemplate();
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
            UserDetails user = loadUserByUsername(username);
            return user;
        }
    }
}
