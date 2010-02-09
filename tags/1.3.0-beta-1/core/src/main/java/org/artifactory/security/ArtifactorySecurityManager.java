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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.artifactory.config.ExportableConfig;
import org.artifactory.jcr.JcrCallback;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.jcr.JcrWrapper;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.Authentication;
import org.springframework.security.AuthenticationManager;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.acls.Permission;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.sid.PrincipalSid;
import org.springframework.security.acls.sid.Sid;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;

import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactorySecurityManager
        implements InitializingBean, ApplicationContextAware, ExportableConfig {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactorySecurityManager.class);

    public static final String FILE_NAME = "security.xml";

    private AuthenticationManager authenticationManager;
    private ExtendedAclService aclService;
    private ArtifactoryContext context;
    private JcrUserDetailsService userDetailsService;

    public static final String USER_ANONYMOUS = "anonymous";
    public static final String USER_ADMIN = "admin";
    public static final String USER_UNKNOWN = "unknown";
    private static final String DEFAULT_ADMIN_PASSWORD = "password";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ADMIN = "admin";

    public static boolean isAdmin(UserDetails details) {
        UsernamePasswordAuthenticationToken authentication = getAuthentication(details);
        return isAdmin(authentication);
    }

    public static boolean isAnonymous() {
        Authentication authentication = getAuthentication();
        return authentication != null && USER_ANONYMOUS.equals(authentication.getName());
    }

    public boolean isAnonAccessEnabled() {
        return context.getCentralConfig().isAnonAccessEnabled();
    }

    public static boolean isAdmin(Authentication authentication) {
        GrantedAuthority[] authorities = authentication.getAuthorities();
        //TODO: [by yl] When do we need to force load the authorities?
        if (authorities == null) {
            //Try to load the authorities first
            ArtifactorySecurityManager security = ContextHelper.get().getSecurity();
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
            if (authority.getAuthority().equalsIgnoreCase(USER_ADMIN)) {
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
        SimpleUser simpleUser = getSimpleUser();
        return simpleUser != null && simpleUser.isUpdatableProfile();
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
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        //getPrincipal() should return a SimpleUser instance
        UserDetails user = (UserDetails) authentication.getPrincipal();
        if (user == null) {
            return null;
        }
        if (user instanceof SimpleUser) {
            return (SimpleUser) user;
        }
        //TODO: [by yl] We do not account for updatableProfile when constructing the SimpleUser
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

    static Permission permissionForRole(ArtifactoryRole role) {
        if (role == ArtifactoryRole.ADMIN) {
            return BasePermission.ADMINISTRATION;
        } else if (role == ArtifactoryRole.DEPLOYER) {
            return BasePermission.WRITE;
        } else if (role == ArtifactoryRole.READER) {
            return BasePermission.READ;
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


    public ExtendedAclService getAclService() {
        return aclService;
    }

    public void setAclService(ExtendedAclService aclService) {
        this.aclService = aclService;
    }

    public JcrUserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    public void setUserDetailsService(JcrUserDetailsService jcrUserDetailsService) {
        this.userDetailsService = jcrUserDetailsService;
    }

    public boolean canAdmin(RepoResource res) {
        return canAdmin(toRepoPath(res));
    }

    public boolean canDeploy(RepoResource res) {
        return canDeploy(toRepoPath(res));
    }

    public boolean canRead(RepoResource res) {
        return canRead(toRepoPath(res));
    }

    public boolean canAdmin(RepoPath repoPath) {
        Permission permission = ArtifactorySecurityManager.permissionForRole(ArtifactoryRole.ADMIN);
        return hasPermission(repoPath, permission);
    }

    public boolean canDeploy(RepoPath repoPath) {
        Permission permission = ArtifactorySecurityManager.permissionForRole(ArtifactoryRole.DEPLOYER);
        return hasPermission(repoPath, permission);
    }

    public boolean canRead(RepoPath repoPath) {
        Permission permission = ArtifactorySecurityManager.permissionForRole(ArtifactoryRole.READER);
        return hasPermission(repoPath, permission);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = (ArtifactoryContext) applicationContext;
    }

    public void afterPropertiesSet() throws Exception {
        //Check if the deefault users exist and create them it if not
        /*JcrUserDetailsService userDetailsService = getUserDetailsService();
        List<SimpleUser> users = userDetailsService.getAllUsers();
        for (SimpleUser user : users) {
            String oldPassword = user.getPassword();
            String newPassword = DigestUtils.md5Hex(oldPassword);
            SimpleUser updatedUser =
                    new SimpleUser(user.getUsername(), newPassword, user.getEmail(),
                            user.isEnabled(), user.isAccountNonExpired(),
                            user.isCredentialsNonExpired(), user.isAccountNonLocked(),
                            user.isUpdatableProfile(), user.isAdmin());
            userDetailsService.updateUser(updatedUser);
            LOGGER.info(
                    "Password successfully updated for user '" + user.getUsername() + "'.");
        }*/
        createAdminUser();
        createAnonymousUser();
    }

    private void createAdminUser() {
        JcrWrapper jcr = this.context.getJcr();
        jcr.doInSession(new JcrCallback<Object>() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                SimpleUser user =
                        new SimpleUser(USER_ADMIN, DigestUtils.md5Hex(DEFAULT_ADMIN_PASSWORD), null,
                                true, true, true, true, true, true);
                getUserDetailsService().createUser(user);
                return null;
            }
        });
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void createAnonymousUser() {
        JcrWrapper jcr = this.context.getJcr();
        jcr.doInSession(new JcrCallback<Object>() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                SimpleUser anonUser;
                anonUser = new SimpleUser(USER_ANONYMOUS, DigestUtils.md5Hex(""), null, true, true,
                        true, true, false, false);
                boolean created = getUserDetailsService().createUser(anonUser);
                if (created) {
                    //Create the anon acl if the anonymous user was created
                    SecuredRepoPath anyAnyRepoPath = new SecuredRepoPath();
                    RepoPathAcl anyAnyAcl = aclService.readAclById(anyAnyRepoPath);
                    if (anyAnyAcl == null) {
                        anyAnyAcl = aclService.createAcl(anyAnyRepoPath);
                    }
                    //Force the read since the anonymous user is there for the first time
                    //After this update version if anonymous permissions changed we will not
                    //reach this method
                    List<RepoPathAce> aces = anyAnyAcl.getAces();
                    PrincipalSid anonSid = anonUser.toPrincipalSid();
                    RepoPathAce anonAnyAnyAce = new RepoPathAce(anyAnyAcl, BasePermission.READ, anonSid);
                    if (!aces.contains(anonAnyAnyAce)) {
                        anyAnyAcl.insertAce(0, BasePermission.READ, anonSid, true);
                    }
                    aclService.updateAcl(anyAnyAcl);
                }
                return null;
            }
        });
    }


    private boolean hasPermission(RepoPath repoPath, Permission permission) {
        //Try to get it by the normal path
        boolean result = hasPermissionInternal(repoPath, permission);
        if (!result) {
            //Try to get for this repo for ANY path
            SecuredRepoPath anyPathRepoPath = SecuredRepoPath.forRepo(repoPath.getRepoKey());
            result = hasPermissionInternal(anyPathRepoPath, permission);
        }
        if (!result) {
            //Try to get for ANY repo for this path
            SecuredRepoPath anyRepoRepoPath = SecuredRepoPath.forPath(repoPath.getPath());
            result = hasPermissionInternal(anyRepoRepoPath, permission);
        }
        if (!result) {
            //Try to get for ANY repo for ANY path
            result = hasPermissionInternal(SecuredRepoPath.ANY_REPO_AND_PATH, permission);
        }
        return result;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    static RepoPath toRepoPath(RepoResource res) {
        String repoKey = res.getRepoKey();
        String path = res.getPath();
        RepoPath repoPath = new RepoPath(repoKey, path);
        return repoPath;
    }

    private boolean hasPermissionInternal(RepoPath repoPath, Permission permission) {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }
        //For admins return a full mask
        boolean admin = ArtifactorySecurityManager.isAdmin(authentication);
        if (admin) {
            return true;
        }
        RepoPathAcl acl = aclService.readAclById(new SecuredRepoPath(repoPath));
        if (acl == null) {
            return false;
        }
        //Check backwards from the most specific group to the less focused group
        //Check against no mask found or zero mask found
        Sid[] sids = {new PrincipalSid(authentication)};
        boolean granted = acl.isGranted(new Permission[]{permission}, sids, false);
        if (!granted) {
            //try to get the superpath
            String path = repoPath.getPath();
            int parentPathEndIdx = path.lastIndexOf('/');
            String repoKey = repoPath.getRepoKey();
            if (parentPathEndIdx > 0) {
                String parentPath = path.substring(0, parentPathEndIdx);
                SecuredRepoPath parentRepoPath = new SecuredRepoPath(repoKey, parentPath);
                return hasPermission(parentRepoPath, permission);
            }
            return false;
        } else {
            return true;
        }
    }

    public void exportTo(File exportDir, StatusHolder status) {
        //Export the security settings as xml using xstream
        List<SimpleUser> users = getUserDetailsService().getAllUsers();
        List<RepoPathAcl> acls = aclService.getAllAcls();
        String path = exportDir + "/" + FILE_NAME;
        SecurityConfig config = new SecurityConfig(users, acls);
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

    public void importFrom(final String basePath, StatusHolder status) {
        JcrWrapper jcr = this.context.getJcr();
        jcr.doInSession(new JcrCallback<Object>() {
            public Object doInJcr(JcrSessionWrapper session) throws RepositoryException {
                //Remove all exisitng users, and acls
                List<SimpleUser> oldUsers = userDetailsService.getAllUsers();
                for (SimpleUser oldUser : oldUsers) {
                    userDetailsService.removeUser(oldUser.getUsername());
                }
                //Clean up all acls
                List<RepoPathAcl> oldAcls = aclService.getAllAcls();
                for (RepoPathAcl oldAcl : oldAcls) {
                    aclService.deleteAcl(oldAcl.getObjectIdentity(), true);
                }
                //Import the new security definitions
                String path = basePath + "/" + FILE_NAME;
                XStream xstream = getXstream();
                InputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(path));
                    SecurityConfig config = (SecurityConfig) xstream.fromXML(is);
                    List<SimpleUser> users = config.getUsers();
                    boolean hasAnonymous = false;
                    for (SimpleUser user : users) {
                        userDetailsService.createUser(user);
                        if (USER_ANONYMOUS.equals(user.getUsername())) {
                            hasAnonymous = true;
                        }
                    }
                    List<RepoPathAcl> acls = config.getAcls();
                    for (RepoPathAcl acl : acls) {
                        aclService.createAcl(acl);
                    }
                    if (!hasAnonymous) {
                        createAnonymousUser();
                    }
                } catch (FileNotFoundException e) {
                    throw new RuntimeException("Failed to import security configuration.", e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
                return null;
            }
        });
    }

    private static XStream getXstream() {
        XStream xstream = new XStream();
        xstream.processAnnotations(SecurityConfig.class);
        return xstream;
    }

}
