package org.artifactory.security;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.acl.basic.BasicAclDao;
import org.acegisecurity.acl.basic.BasicAclProvider;
import org.acegisecurity.acl.basic.jdbc.JdbcExtendedDaoImpl;
import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;
import org.artifactory.utils.SqlUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecurityHelper implements InitializingBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecurityHelper.class);

    private DaoAuthenticationProvider authenticationProvider;
    private BasicAclProvider aclProvider;

    public DaoAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(DaoAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public BasicAclProvider getAclProvider() {
        return aclProvider;
    }

    public void setAclProvider(BasicAclProvider aclProvider) {
        this.aclProvider = aclProvider;
    }

    public BasicAclDao getBasicAclDao() {
        return aclProvider.getBasicAclDao();
    }

    public ExtendedUserDetailsService getUserDetailsService() {
        return (ExtendedUserDetailsService) authenticationProvider.getUserDetailsService();
    }

    public void afterPropertiesSet() throws Exception {
        JdbcExtendedDaoImpl dao = (JdbcExtendedDaoImpl) getBasicAclDao();
        DatabaseMetaData databaseMetaData = dao.getDataSource().getConnection().getMetaData();
        //Check if the users table exists - create it if not
        DataSource dataSource = dao.getDataSource();
        boolean tableExists = SqlUtils.tableExists("USERS", dataSource);
        if (!tableExists) {
            SqlUtils.executeResourceScript("sql/acegi.sql", dataSource);
            //Add the default admin user
            UserDetails user = new User("admin", "password", true, true, true, true,
                    new GrantedAuthority[]{new GrantedAuthorityImpl("ADMIN"),
                            new GrantedAuthorityImpl("USER")});
            getUserDetailsService().createUser(user);
        }
    }
}

/**
 Have a list of acls: groupId + user/role + mask (= objIdentity + recipient + mask).

 Have an ADMIN authority with all permissions on the ROOT group, and for accessing global admin pages.
 Have a USER authority for accessing the webapp pages. Webapp always requires a logged in user.
 Have a ROOT group for global admin purposes.

 Have the following permission masks for each group acl:
 1  = administer (manage permissions for users - roles will be supported in the future)
 2  = read - default (when a user acl exists for a group, he has read privieges at the minimum)
 6  = read and deploy/undeploy

 Display acls by group - display the defined groups hierarchically

 Checkpoints: Browse, Download, Deploy (web + cli), Delete, Administer group, Administer users.
 */
