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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationManager;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.acl.AclEntry;
import org.acegisecurity.acl.basic.BasicAclEntry;
import org.acegisecurity.acl.basic.BasicAclProvider;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.artifactory.config.ExportableConfig;
import org.artifactory.keyval.KeyVals;
import org.artifactory.process.StatusHolder;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.artifactory.utils.SqlUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class SecurityHelper implements InitializingBean, ApplicationContextAware, ExportableConfig {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(SecurityHelper.class);

    public static final String FILE_NAME = "security.xml";

    private AuthenticationManager authenticationManager;
    private CustomAclProvider aclProvider;
    private ArtifactoryContext context;
    private ExtendedUserDetailsService userDetailsService;

    public static boolean isAdmin(UserDetails details) {
        UsernamePasswordAuthenticationToken authentication = getAuthentication(details);
        return isAdmin(authentication);
    }

    public static boolean isAdmin(Authentication authentication) {
        GrantedAuthority[] authorities = authentication.getAuthorities();
        //TODO: [by yl] When do we need to force load the authorities?
        if (authorities == null) {
            //Try to load the authorities first
            SecurityHelper security = ContextHelper.get().getSecurity();
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
    public void setUserDetailsService(ExtendedUserDetailsService extendedUserDetailsService){
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = (ArtifactoryContext) applicationContext;
    }

    public void afterPropertiesSet() throws Exception {
        ExtendedJdbcAclDao dao = getAclDao();
        //Check if the users table exists - create it if not
        DataSource dataSource = dao.getDataSource();
        boolean tableExists = SqlUtils.tableExists("USERS", dataSource);
        if (!tableExists) {
            SqlUtils.executeResourceScript("sql/acegi.sql", dataSource);
            //Add the default admin user
            SimpleUser user = new SimpleUser("admin", DigestUtils.md5Hex("password"), true, true,
                    true, true, true,
                    new GrantedAuthority[]{new GrantedAuthorityImpl("ADMIN"),
                            new GrantedAuthorityImpl("USER")});
            getUserDetailsService().createUser(user);
        } else {
            //If using an older revision, update all passwords to be stored as hashes
            KeyVals keyVals = context.getKeyVal();
            String prevRevision = keyVals.getPrevRevision();
            boolean needsUpdating = StringUtils.isEmpty(prevRevision);
            if (!needsUpdating && !prevRevision.startsWith("${")) {
                int len = Math.max(3, prevRevision.length());
                String paddedRevision = StringUtils.leftPad("913", len, '0');
                String paddedPrevRevision = StringUtils.leftPad(prevRevision, len, '0');
                //Update if the old revision was lower than 913
                needsUpdating = paddedPrevRevision.compareTo(paddedRevision) < 0;
            }
            if (needsUpdating) {
                LOGGER.info("User passwords need hash conversion.");
                ExtendedUserDetailsService userDetailsService = getUserDetailsService();
                List<SimpleUser> users = userDetailsService.getAllUsers();
                for (SimpleUser user : users) {
                    String oldPassword = user.getPassword();
                    String newPassword = DigestUtils.md5Hex(oldPassword);
                    SimpleUser updatedUser = new SimpleUser(user.getUsername(), newPassword,
                            user.isEnabled(), user.isAccountNonExpired(),
                            user.isCredentialsNonExpired(), user.isAccountNonLocked(),
                            user.isUpdatableProfile(), user.getAuthorities());
                    userDetailsService.updateUser(updatedUser);
                    LOGGER.info(
                            "Password successfully updated for user '" + user.getUsername() + "'.");
                }
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("User passwords do not need hash conversion.");
                }
            }
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
        boolean admin = SecurityHelper.isAdmin(authentication);
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

    public void exportTo(String basePath, StatusHolder status) {
        //Export the security settings as xml using xstream
        List<SimpleUser> users = getUserDetailsService().getAllUsers();
        List<RepoPath> repoPaths = getAclDao().getAllRepoPaths();
        List<SimpleAclEntry> acls = new ArrayList<SimpleAclEntry>();
        for (RepoPath repoPath : repoPaths) {
            SimpleAclEntry[] entries = getAclProvider().getAcls(repoPath);
            acls.addAll(Arrays.asList(entries));
        }
        String path = basePath + "/" + FILE_NAME;
        SecuritycConfig config = new SecuritycConfig(users, repoPaths, acls);
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
        //Remove all exisitng users, authorities and repoPaths
        ExtendedUserDetailsService uds = getUserDetailsService();
        List<SimpleUser> users = uds.getAllUsers();
        for (SimpleUser user : users) {
            uds.deleteUser(user.getUsername());
        }
        ExtendedJdbcAclDao aclDao = getAclDao();
        List<RepoPath> repoPaths = aclDao.getAllRepoPaths();
        for (RepoPath repoPath : repoPaths) {
            aclDao.delete(repoPath);
        }
        //Import the new security definitions
        String path = basePath + "/" + FILE_NAME;
        XStream xstream = getXstream();
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(path));
            SecuritycConfig config = (SecuritycConfig) xstream.fromXML(is);
            List<SimpleUser> newUsers = config.getUsers();
            for (SimpleUser user : newUsers) {
                uds.createUser(user);
            }
            List<RepoPath> newRepoPaths = config.getRepoPaths();
            for (RepoPath repoPath : newRepoPaths) {
                aclDao.createAclObjectIdentity(repoPath, null);
            }
            List<SimpleAclEntry> acls = config.getAcls();
            for (SimpleAclEntry acl : acls) {
                aclDao.create(acl);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to import security configuration.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private XStream getXstream() {
        XStream xstream = new XStream();
        Annotations.configureAliases(xstream, SecuritycConfig.class);
        return xstream;
    }


    @XStreamAlias("security")
    private static class SecuritycConfig {
        private List<SimpleUser> users;
        private List<RepoPath> repoPaths;
        private List<SimpleAclEntry> acls;

        private SecuritycConfig(
                List<SimpleUser> users, List<RepoPath> repoPaths, List<SimpleAclEntry> acls) {
            this.users = users;
            this.repoPaths = repoPaths;
            this.acls = acls;
        }

        public List<SimpleUser> getUsers() {
            return users;
        }

        public void setUsers(List<SimpleUser> users) {
            this.users = users;
        }

        public List<RepoPath> getRepoPaths() {
            return repoPaths;
        }

        public void setRepoPaths(List<RepoPath> repoPaths) {
            this.repoPaths = repoPaths;
        }

        public List<SimpleAclEntry> getAcls() {
            return acls;
        }

        public void setAcls(List<SimpleAclEntry> acls) {
            this.acls = acls;
        }
    }
}
