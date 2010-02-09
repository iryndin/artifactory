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
        //TODO: [by yl] When do we need to force load the authorities?
        if (authorities == null) {
            //Try to load the authorities first
            SecurityHelper security = ContextUtils.getContext().getSecurity();
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

    public static boolean isAdmin() {
        Authentication authentication = getAuthentication();
        return isAdmin(authentication);
    }

    public static boolean isUpdatableProfile() {
        Authentication authentication = getAuthentication();
        User user = (User) authentication.getPrincipal();
        String username = user.getUsername();
        return !username.equalsIgnoreCase("guest");
        //TODO: [by yl] Remove the above hack and test with simpleUser.isUpdatableProfile()
        /*SimpleUser user = (SimpleUser) authentication.getPrincipal();
        return user.isUpdatableProfile();*/
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static String getUsername() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        //Do not return a null username or this will cause a jcr constraint violation
        return (authentication != null ? authentication.getName() : "");
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static SimpleUser getSimpleUser() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        //TODO: [by yl] We do not account for updatableProfile when construction the SimpleUser
        //getPrincipal() should return a SimpleUser instance
        User user = (User) authentication.getPrincipal();
        SimpleUser simpleUser = new SimpleUser(user);
        return simpleUser;
    }

    public static int maskForRole(ArtifactoryRole role) {
        if (role == ArtifactoryRole.ADMIN) {
            return SimpleAclEntry.ADMINISTRATION;
        } else if (role == ArtifactoryRole.DEPLOYER) {
            return SimpleAclEntry.WRITE;
        } else if (role == ArtifactoryRole.READER) {
            return SimpleAclEntry.READ;
        } else {
            throw new IllegalArgumentException("Cannot determine mask for role '" + role + "'.");
        }
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    private static Authentication getAuthentication() {
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

    public boolean canAdmin(SecuredResource res) {
        return canAdmin(toRepoPath(res));
    }

    public boolean canDeploy(SecuredResource res) {
        return canDeploy(toRepoPath(res));
    }

    public boolean canRead(SecuredResource res) {
        return canRead(toRepoPath(res));
    }

    public boolean canAdmin(RepoPath repoPath) {
        int mask = SecurityHelper.maskForRole(ArtifactoryRole.ADMIN);
        return hasMask(repoPath, mask);
    }

    public boolean canDeploy(RepoPath repoPath) {
        int mask = SecurityHelper.maskForRole(ArtifactoryRole.DEPLOYER);
        return hasMask(repoPath, mask);
    }

    public boolean canRead(RepoPath repoPath) {
        int mask = SecurityHelper.maskForRole(ArtifactoryRole.READER);
        return hasMask(repoPath, mask);
    }

    public void afterPropertiesSet() throws Exception {
        ExtendedJdbcAclDao dao = getAclDao();
        //Check if the users table exists - create it if not
        DataSource dataSource = dao.getDataSource();
        boolean tableExists = SqlUtils.tableExists("USERS", dataSource);
        if (!tableExists) {
            SqlUtils.executeResourceScript("sql/acegi.sql", dataSource);
            //Add the default admin user
            SimpleUser user = new SimpleUser("admin", "password", true, true, true, true, true,
                    new GrantedAuthority[]{new GrantedAuthorityImpl("ADMIN"),
                            new GrantedAuthorityImpl("USER")});
            getUserDetailsService().createUser(user);
        }
    }

    public boolean hasMask(RepoPath repoPath, int targetMask) {
        //Try to get it by the normal path
        boolean result = hasMaskInternal(repoPath, targetMask);
        if (!result) {
            //Try to get for this repo for ANY path
            RepoPath anyPathRepoPath = RepoPath.forRepo(repoPath.getRepoKey());
            result = hasMaskInternal(anyPathRepoPath, targetMask);
        }
        if (!result) {
            //Try to get for ANY repo for this path
            RepoPath anyRepoRepoPath = RepoPath.forPath(repoPath.getPath());
            result = hasMaskInternal(anyRepoRepoPath, targetMask);
        }
        if (!result) {
            //Try to get for ANY repo for ANY path
            result = hasMaskInternal(RepoPath.ANY_REPO_AND_PATH, targetMask);
        }
        return result;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    static RepoPath toRepoPath(SecuredResource res) {
        String repoKey = res.getRepoKey();
        String path = res.getPath();
        RepoPath repoPath = new RepoPath(repoKey, path);
        return repoPath;
    }

    private boolean hasMaskInternal(RepoPath repoPath, int targetMask) {
        Authentication authentication = getAuthentication();
        //For admins return a full mask
        boolean admin = SecurityHelper.isAdmin(authentication);
        if (admin) {
            return true;
        }
        //Check backwards from the most specific group to the less focused group
        AclEntry[] entries = aclProvider.getAcls(repoPath, authentication);
        //Check against no mask found or zero mask found
        if (entries == null || entries.length == 0 ||
                (((BasicAclEntry) entries[0]).getMask() & targetMask) == 0) {
            //try to get the superpath
            String path = repoPath.getPath();
            int parentPathEndIdx = path.lastIndexOf('/');
            String repoKey = repoPath.getRepoKey();
            if (parentPathEndIdx > 0) {
                String parentPath = path.substring(0, parentPathEndIdx);
                RepoPath parentRepoPath =
                        new RepoPath(repoKey, parentPath);
                return hasMask(parentRepoPath, targetMask);
            }
            return false;
        } else {
            return (((BasicAclEntry) entries[0]).getMask() & targetMask) > 0;
        }
    }
}