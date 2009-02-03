package org.artifactory.security;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.acl.AclEntry;
import org.acegisecurity.acl.AclProvider;
import org.acegisecurity.acl.basic.BasicAclEntry;
import org.acegisecurity.acl.basic.BasicAclProvider;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.providers.dao.DaoAuthenticationProvider;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.artifactory.spring.ContextUtils;
import org.artifactory.utils.SqlUtils;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecurityHelper implements InitializingBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecurityHelper.class);

    private DaoAuthenticationProvider authenticationProvider;
    private AclProvider aclProvider;

    public static boolean isAdmin(UserDetails details) {
        UsernamePasswordAuthenticationToken authentication = getAuthentication(details);
        return isAdmin(authentication);
    }

    public static boolean isAdmin(Authentication authentication) {
        GrantedAuthority[] authorities = authentication.getAuthorities();
        if (authorities == null) {
            //Try to load the authorities first
            SecurityHelper security = ContextUtils.getArtifactoryContext().getSecurity();
            String username = authentication.getName();
            UserDetails details = security.getUserDetailsService().loadUserByUsername(username);
            if (details != null) {
                authorities = details.getAuthorities();
            }
        }
        if (authorities == null) {
            return false;
        }
        boolean result = false;
        for (GrantedAuthority authority : authorities) {
            if (authority.getAuthority().equalsIgnoreCase("admin")) {
                result = true;
                break;
            }
        }
        return result;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static Authentication getAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        return authentication;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private static UsernamePasswordAuthenticationToken getAuthentication(UserDetails details) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(details.getUsername(),
                        details.getPassword(), details.getAuthorities());
        return authentication;
    }

    public DaoAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(DaoAuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public AclProvider getAclProvider() {
        return aclProvider;
    }

    public void setAclProvider(AclProvider aclProvider) {
        this.aclProvider = aclProvider;
    }

    public ExtendedJdbcAclDao getAclDao() {
        return (ExtendedJdbcAclDaoImpl) ((BasicAclProvider) aclProvider).getBasicAclDao();
    }

    public ExtendedUserDetailsService getUserDetailsService() {
        return (ExtendedUserDetailsService) authenticationProvider.getUserDetailsService();
    }

    public int getMask(String groupId) {
        //Sanity check
        if (groupId == null) {
            throw new IllegalArgumentException("Group cannot be null");
        }
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        //For admins return a full mask
        boolean admin = SecurityHelper.isAdmin(authentication);
        if (admin) {
            return SimpleAclEntry.ADMINISTRATION | SimpleAclEntry.READ_WRITE_CREATE_DELETE;
        }
        //Check backwards from the most specific group to the less focused group
        AclEntry[] entries = aclProvider.getAcls(groupId, authentication);
        if (entries == null || entries.length == 0) {
            int superGroupEndIdx = groupId.lastIndexOf('.');
            if (superGroupEndIdx > 0) {
                String superGroup = groupId.substring(0, superGroupEndIdx);
                return getMask(superGroup);
            } else {
                return SimpleAclEntry.NOTHING;
            }
        } else {
            return ((BasicAclEntry) entries[0]).getMask();
        }
    }

    public boolean canAdmin(Artifact artifact) {
        String groupId = artifact.getGroupId();
        return canAdmin(groupId);
    }

    public boolean canAdmin(String groupId) {
        int mask = getMask(groupId);
        return (mask & SimpleAclEntry.ADMINISTRATION) > 0;
    }

    public boolean canDeploy(Artifact artifact) {
        String groupId = artifact.getGroupId();
        return canDeploy(groupId);
    }

    public boolean canDeploy(String groupId) {
        int mask = getMask(groupId);
        return (mask & SimpleAclEntry.WRITE) > 0;
    }

    public boolean canView(Artifact artifact) {
        String groupId = artifact.getGroupId();
        return canView(groupId);
    }

    public boolean canView(String groupId) {
        int mask = getMask(groupId);
        return (mask & SimpleAclEntry.READ) > 0;
    }

    public void afterPropertiesSet() throws Exception {
        ExtendedJdbcAclDao dao = getAclDao();
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

 Have a ROOT group for global admin purposes.
 Have an ADMIN authority with all permissions on the ROOT group, and for accessing global admin pages.
 Have a USER authority for accessing the webapp pages. Webapp always requires a logged in user.

 Checkpoints: Browse, Download, Deploy (web + cli), Delete, Administer group, Administer users.
 */
