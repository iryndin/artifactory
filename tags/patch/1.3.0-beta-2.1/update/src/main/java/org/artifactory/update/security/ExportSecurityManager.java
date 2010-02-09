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

import com.thoughtworks.xstream.XStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.config.ExportableConfig;
import org.artifactory.process.StatusHolder;
import org.artifactory.security.ArtifactoryRole;
import org.artifactory.security.ArtifactorySecurityManager;
import org.artifactory.security.RepoPathAce;
import org.artifactory.security.RepoPathAcl;
import org.artifactory.security.SecuredRepoPath;
import org.artifactory.security.SecurityConfig;
import org.artifactory.security.SimpleUser;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.update.VersionsHolder;
import org.artifactory.utils.SqlUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.acl.AclEntry;
import org.springframework.security.acl.basic.BasicAclEntry;
import org.springframework.security.acl.basic.BasicAclProvider;
import org.springframework.security.acl.basic.SimpleAclEntry;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.UserDetails;

import javax.sql.DataSource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ExportSecurityManager implements InitializingBean, ApplicationContextAware, ExportableConfig {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ExportSecurityManager.class);

    private AuthenticationManager authenticationManager;
    private CustomAclProvider aclProvider;
    private ArtifactoryContext context;
    private ExtendedUserDetailsService userDetailsService;
    private static XStream xstream;

    public static boolean isAdmin(UserDetails details) {
        UsernamePasswordAuthenticationToken authentication = getAuthentication(details);
        return isAdmin(authentication);
    }

    public static boolean isAdmin(Authentication authentication) {
        GrantedAuthority[] authorities = authentication.getAuthorities();
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
        UserDetails user = (UserDetails) authentication.getPrincipal();
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
        //TODO: [by yl] We do not account for updatableProfile when constructing the SimpleUser
        //getPrincipal() should return a SimpleUser instance
        User user = (User) authentication.getPrincipal();
        SimpleUser simpleUser = new SimpleUser(user);
        return simpleUser;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public static Authentication getAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext == null) {
            return null;
        }
        Authentication authentication = securityContext.getAuthentication();
        return authentication;
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
    private static UsernamePasswordAuthenticationToken getAuthentication(UserDetails details) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(details.getUsername(),
                        details.getPassword(), details.getAuthorities());
        return authentication;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public CustomAclProvider getAclProvider() {
        return aclProvider;
    }

    public void setAclProvider(CustomAclProvider aclProvider) {
        this.aclProvider = aclProvider;
    }

    public ExtendedJdbcAclDao getAclDao() {
        return (ExtendedJdbcAclDaoImpl) ((BasicAclProvider) aclProvider).getBasicAclDao();
    }

    public ExtendedUserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public void setUserDetailsService(ExtendedUserDetailsService extendedUserDetailsService) {
        this.userDetailsService = extendedUserDetailsService;
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
        int mask = ExportSecurityManager.maskForRole(ArtifactoryRole.ADMIN);
        return hasMask(repoPath, mask);
    }

    public boolean canDeploy(RepoPath repoPath) {
        int mask = ExportSecurityManager.maskForRole(ArtifactoryRole.DEPLOYER);
        return hasMask(repoPath, mask);
    }

    public boolean canRead(RepoPath repoPath) {
        int mask = ExportSecurityManager.maskForRole(ArtifactoryRole.READER);
        return hasMask(repoPath, mask);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = (ArtifactoryContext) applicationContext;
    }

    public void afterPropertiesSet() throws Exception {
        ExtendedJdbcAclDao dao = getAclDao();
        //Check if the users table exists - create it if not
        DataSource dataSource = dao.getDataSource();
        boolean tableExists = SqlUtils.tableExists("USERS", dataSource);
        if (!tableExists) {
            LOGGER.error("Migrating a database without security information!");
            throw new RuntimeException("Table USERS does not exists in dataSource " + dataSource);
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
        if (authentication == null) {
            return false;
        }
        //For admins return a full mask
        boolean admin = ExportSecurityManager.isAdmin(authentication);
        if (admin) {
            return true;
        }
        //Check backwards from the most specific group to the less focused group
        AclEntry[] entries = aclProvider.getAcls(repoPath, authentication);
        //Check against no mask found or zero mask found
        if (entries.length == 0 ||
                (((BasicAclEntry) entries[0]).getMask() & targetMask) == 0) {
            //try to get the superpath
            String path = repoPath.getPath();
            int parentPathEndIdx = path.lastIndexOf('/');
            String repoKey = repoPath.getRepoKey();
            if (parentPathEndIdx > 0) {
                String parentPath = path.substring(0, parentPathEndIdx);
                RepoPath parentRepoPath = new RepoPath(repoKey, parentPath);
                return hasMask(parentRepoPath, targetMask);
            }
            return false;
        } else {
            return (((BasicAclEntry) entries[0]).getMask() & targetMask) > 0;
        }
    }

    public void exportTo(File exportDir, StatusHolder status) {
        //Export the security settings as xml using xstream
        ExtendedUserDetailsService usersService = getUserDetailsService();
        List<SimpleUser> users = usersService.getAllUsers();
        List<SimpleUser> updatedUsers = users;
        // Test if hash password conversion needed
        if (VersionsHolder.getOriginalVersion().getRevision() < 913) {
            LOGGER.info("User passwords need hash conversion.");
            updatedUsers = new ArrayList<SimpleUser>(users.size());
            for (SimpleUser user : users) {
                String oldPassword = user.getPassword();
                String newPassword = DigestUtils.md5Hex(oldPassword);
                SimpleUser updatedUser = new SimpleUser(user.getUsername(), newPassword,
                        "", user.isEnabled(), user.isAccountNonExpired(),
                        user.isCredentialsNonExpired(), user.isAccountNonLocked(),
                        user.isUpdatableProfile(), user.isAdmin());
                updatedUsers.add(updatedUser);
                LOGGER.info(
                        "Password successfully updated for user '" + user.getUsername() + "'.");
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("User passwords do not need hash conversion.");
            }
        }
        List<RepoPath> repoPaths = getAclDao().getAllRepoPaths();
        List<RepoPathAcl> repoPathAcls = new ArrayList<RepoPathAcl>();
        for (RepoPath repoPath : repoPaths) {
            SecuredRepoPath secRepoPath = new SecuredRepoPath(repoPath.getRepoKey(), repoPath.getPath());
            RepoPathAcl repoPathAcl = new RepoPathAcl(secRepoPath);
            repoPathAcls.add(repoPathAcl);
            SimpleAclEntry[] entries = getAclProvider().getAcls(repoPath);
            for (SimpleAclEntry entry : entries) {
                PrincipalSid principalSid = new PrincipalSid(entry.getRecipient().toString());
                RepoPathAce ace = new RepoPathAce(repoPathAcl, BasePermission.buildFromMask(entry.getMask()), principalSid);
                repoPathAcl.updateOrCreateAce(ace);
            }
        }
        File path = new File(exportDir, ArtifactorySecurityManager.FILE_NAME);
        SecurityConfig config = new SecurityConfig(updatedUsers, repoPathAcls);
        XStream xstream = getXstream();
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(path));
            xstream.toXML(config, os);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to export security configuration.", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    public void importFrom(String basePath, StatusHolder status) {
        throw new RuntimeException("This class is used only for export");
    }

    private static synchronized XStream getXstream() {
        if (xstream == null) {
            xstream = new XStream();
            xstream.processAnnotations(SecurityConfig.class);
        }
        return xstream;
    }
}
