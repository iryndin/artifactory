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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AceInfo;
import org.artifactory.api.security.AclInfo;
import org.artifactory.api.security.ArtifactoryPermisssion;
import org.artifactory.api.security.GroupInfo;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.api.security.SecurityInfo;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.jcr.JcrService;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.security.ldap.LdapConnectionTester;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.update.utils.BackupUtils;
import org.artifactory.util.PathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.Authentication;
import org.springframework.security.acls.Permission;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.sid.Sid;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Service
public class SecurityServiceImpl implements SecurityServiceInternal {
    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);

    @Autowired
    private AclManager aclManager;

    @Autowired
    private UserGroupManager userGroupManager;

    @Autowired
    private JcrService jcr;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    private InternalArtifactoryContext context;

    @PostConstruct
    public void register() {
        context.addReloadableBean(SecurityServiceInternal.class);
    }

    @Autowired
    private void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (InternalArtifactoryContext) context;
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{JcrService.class, UserGroupManager.class, AclManager.class};
    }

    public void init() {
        checkOcmFolders();
        createAdminUser();
        createAnonymousUser();
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // TODO: Change the PermissionTarget repoKey according to new config
    }

    public void destroy() {
    }

    private void checkOcmFolders() {
        if (!jcr.itemNodeExists(JcrUserGroupManager.getGroupsJcrPath()) ||
                !jcr.itemNodeExists(JcrUserGroupManager.getUsersJcrPath()) ||
                !jcr.itemNodeExists(JcrAclManager.getAclsJcrPath())
                ) {
            throw new RepositoryRuntimeException("Creation of root folders for OCM failed");
        }
    }

    public boolean isAnonymous() {
        Authentication authentication = getAuthentication();
        return authentication != null && UserInfo.ANONYMOUS.equals(authentication.getName());
    }

    public boolean isAnonAccessEnabled() {
        SecurityDescriptor security = centralConfig.getDescriptor().getSecurity();
        return security != null && security.isAnonAccessEnabled();
    }

    public boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return isAuthenticated(authentication);
    }

    public boolean isAdmin() {
        Authentication authentication = getAuthentication();
        return isAdmin(authentication);
    }

    public AclInfo createAcl(PermissionTargetInfo entity) {
        assertAdmin();
        return aclManager.createAcl(new PermissionTarget(entity)).getDescriptor();
    }

    public void deleteAcl(PermissionTargetInfo target) {
        aclManager.deleteAcl(new PermissionTarget(target));
    }

    public AclInfo updateAcl(PermissionTargetInfo target) {
        AclInfo aclInfo = getAcl(target);
        aclInfo.setPermissionTarget(target);
        return aclManager.updateAcl(new Acl(aclInfo)).getDescriptor();
    }

    public List<PermissionTargetInfo> getAdministrativePermissionTargets() {
        Permission adminPermission = permissionFor(ArtifactoryPermisssion.ADMIN);
        return getPermissionTargetsByPermission(adminPermission);
    }

    public List<PermissionTargetInfo> getDeployablePermissionTargets() {
        Permission deployPermission = permissionFor(ArtifactoryPermisssion.DEPLOY);
        return getPermissionTargetsByPermission(deployPermission);
    }

    private List<PermissionTargetInfo> getPermissionTargetsByPermission(Permission permission) {
        List<PermissionTarget> allTargets = aclManager.getAllPermissionTargets();
        List<PermissionTargetInfo> result = new ArrayList<PermissionTargetInfo>();
        for (PermissionTarget permissionTarget : allTargets) {
            if (hasPermissionOnPermissionTarget(permissionTarget, permission)) {
                result.add(permissionTarget.getDescriptor());
            }
        }
        return result;
    }

    public boolean isUpdatableProfile() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && simpleUser.isUpdatableProfile();
    }

    public String currentUsername() {
        Authentication authentication = getAuthentication();
        //Do not return a null username or this will cause a jcr constraint violation
        return (authentication != null ? authentication.getName() : "_system_");
    }

    public UserInfo currentUser() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        SimpleUser user = getSimpleUser(authentication);
        return (user.getDescriptor());
    }

    public UserInfo findUser(String username) {
        return userGroupManager.loadUserByUsername(username).getDescriptor();
    }

    public AclInfo getAcl(PermissionTargetInfo permissionTarget) {
        Acl acl = aclManager.findAclById(new PermissionTarget(permissionTarget));
        if (acl != null) {
            return acl.getDescriptor();
        }
        return null;
    }

    public void updateAcl(AclInfo acl) {
        // Removing empty Ace
        Iterator<AceInfo> it = acl.getAces().iterator();
        while (it.hasNext()) {
            AceInfo aceInfo = it.next();
            if (aceInfo.getMask() == 0) {
                it.remove();
            }
        }
        aclManager.updateAcl(new Acl(acl));
    }

    public List<UserInfo> getAllUsers(boolean includeAdmins) {
        Collection<User> simpleUsers = userGroupManager.getAllUsers(includeAdmins);
        List<UserInfo> usersInfo = new ArrayList<UserInfo>(simpleUsers.size());
        for (User user : simpleUsers) {
            UserInfo userInfo = user.getInfo();
            if (includeAdmins || !userInfo.isAdmin()) {
                //Only include non admin users if asked
                usersInfo.add(userInfo);
            }
        }
        return usersInfo;
    }

    public boolean createUser(UserInfo user) {
        return userGroupManager.createUser(new SimpleUser(user));
    }

    public void updateUser(UserInfo user) {
        userGroupManager.updateUser(new SimpleUser(user));
    }

    public void deleteUser(String username) {
        aclManager.removeAllUserAces(username);
        userGroupManager.removeUser(username);
    }

    public void updateGroup(GroupInfo groupInfo) {
        userGroupManager.updateGroup(new Group(groupInfo));
    }

    public boolean createGroup(GroupInfo groupInfo) {
        return userGroupManager.createGroup(new Group(groupInfo));
    }

    public void deleteGroup(String groupName) {
        List<UserInfo> userInGroup = findUsersInGroup(groupName);
        for (UserInfo userInfo : userInGroup) {
            removeUserFromGroup(userInfo.getUsername(), groupName);
        }
        userGroupManager.removeGroup(groupName);
    }

    public List<GroupInfo> getAllGroups() {
        Collection<Group> groups = userGroupManager.getAllGroups();
        List<GroupInfo> groupsInfo = new ArrayList<GroupInfo>(groups.size());
        for (Group group : groups) {
            groupsInfo.add(group.getInfo());
        }
        return groupsInfo;
    }

    public Set<GroupInfo> getNewUserDefaultGroups() {
        List<GroupInfo> allGroups = getAllGroups();
        Set<GroupInfo> defaultGroups = new HashSet<GroupInfo>();
        for (GroupInfo group : allGroups) {
            if (group.isNewUserDefault()) {
                defaultGroups.add(group);
            }
        }
        return defaultGroups;
    }

    public Set<String> getNewUserDefaultGroupsNames() {
        Set<GroupInfo> defaultGroups = getNewUserDefaultGroups();
        Set<String> defaultGroupsNames = new HashSet<String>(defaultGroups.size());
        for (GroupInfo group : defaultGroups) {
            defaultGroupsNames.add(group.getGroupName());
        }
        return defaultGroupsNames;
    }

    public void addUsersToGroup(String groupName, List<String> usernames) {
        for (String username : usernames) {
            addUserToGroup(username, groupName);
        }
    }

    private void addUserToGroup(String username, String groupName) {
        SimpleUser user = userGroupManager.loadUserByUsername(username);
        UserInfo userInfo = user.getDescriptor();
        if (!userInfo.isInGroup(groupName)) {
            // don't update if already in group
            userInfo.addGroup(groupName);
            updateUser(userInfo);
        }
    }

    public void removeUsersFromGroup(String groupName, List<String> usernames) {
        for (String username : usernames) {
            removeUserFromGroup(username, groupName);
        }
    }

    private void removeUserFromGroup(String username, String groupName) {
        SimpleUser user = userGroupManager.loadUserByUsername(username);
        UserInfo userInfo = user.getDescriptor();
        if (userInfo.isInGroup(groupName)) {
            // update only if user is in the group
            userInfo.removeGroup(groupName);
            updateUser(userInfo);
        }
    }

    public List<UserInfo> findUsersInGroup(String groupName) {
        List<UserInfo> allUsers = getAllUsers(true);
        List<UserInfo> groupUsers = new ArrayList<UserInfo>();
        for (UserInfo userInfo : allUsers) {
            if (userInfo.isInGroup(groupName)) {
                groupUsers.add(userInfo);
            }
        }
        return groupUsers;
    }

    static Permission permissionFor(ArtifactoryPermisssion permisssion) {
        if (permisssion == ArtifactoryPermisssion.ADMIN) {
            return BasePermission.ADMINISTRATION;
        } else if (permisssion == ArtifactoryPermisssion.DELETE) {
            return BasePermission.DELETE;
        } else if (permisssion == ArtifactoryPermisssion.DEPLOY) {
            return BasePermission.WRITE;
        } else if (permisssion == ArtifactoryPermisssion.READ) {
            return BasePermission.READ;
        } else {
            throw new IllegalArgumentException(
                    "Cannot determine mask for role '" + permisssion + "'.");
        }
    }

    public boolean canDeploy(RepoPath repoPath) {
        //TODO: [by yl] Change this to real deploy permissions assigned to anon on specific caches
        //Before checking permissions, check if the repository is a cache with anonymous access on
        String repoKey = repoPath.getRepoKey();
        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(repoKey);
        if (localRepo == null) {
            log.warn("Could not test deployment for non exiting repository '" + repoKey + "'.");
            return false;
        }
        if (localRepo.isCache() && localRepo.isAnonAccessEnabled()) {
            return true;
        }
        Permission permission = permissionFor(ArtifactoryPermisssion.DEPLOY);
        boolean hasPermission = hasPermission(repoPath, permission);
        return hasPermission;
    }

    public boolean canAdmin(RepoPath repoPath) {
        Permission permission = permissionFor(ArtifactoryPermisssion.ADMIN);
        return hasPermission(repoPath, permission);
    }

    public boolean hasDeployPermissions() {
        return isAdmin() || !getDeployablePermissionTargets().isEmpty();
    }

    public boolean canRead(RepoPath repoPath) {
        Permission permission = permissionFor(ArtifactoryPermisssion.READ);
        return hasPermission(repoPath, permission);
    }

    public boolean canDelete(RepoPath repoPath) {
        Permission permission = permissionFor(ArtifactoryPermisssion.DELETE);
        return hasPermission(repoPath, permission);
    }

    public boolean canAdmin(PermissionTargetInfo target) {
        Permission adminPermission = permissionFor(ArtifactoryPermisssion.ADMIN);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), adminPermission);
    }

    public boolean canAdminPermissionTarget() {
        return !getAdministrativePermissionTargets().isEmpty();
    }

    public boolean canRead(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.READ);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canDelete(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.DELETE);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canDeploy(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.DEPLOY);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canAdmin(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.ADMIN);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canRead(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.READ);
        return hasPermission(group, path, permission);
    }

    public boolean canDelete(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.DELETE);
        return hasPermission(group, path, permission);
    }

    public boolean canDeploy(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.DEPLOY);
        return hasPermission(group, path, permission);
    }

    public boolean canAdmin(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermisssion.ADMIN);
        return hasPermission(group, path, permission);
    }

    private boolean hasPermission(RepoPath repoPath, Permission permission) {
        Authentication authentication = getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }

        // Admins has permissions for all paths and all repositories
        if (isAdmin(authentication)) {
            return true;
        }

        ArtifactorySid[] sids = getUserEffectiveSids(getSimpleUser(authentication));
        return isGranted(repoPath, permission, sids);
    }

    private boolean hasPermission(SimpleUser user, RepoPath repoPath, Permission permission) {
        // Admins has permissions for all paths and all repositories
        if (user.isAdmin()) {
            return true;
        }

        ArtifactorySid[] sids = getUserEffectiveSids(user);
        return isGranted(repoPath, permission, sids);
    }

    private boolean hasPermission(GroupInfo group, RepoPath repoPath, Permission permission) {
        ArtifactorySid[] sid = {new ArtifactorySid(group.getGroupName(), true)};
        return isGranted(repoPath, permission, sid);
    }

    private boolean isGranted(RepoPath repoPath, Permission permission, ArtifactorySid[] sids) {
        List<Acl> acls = aclManager.getAllAcls(sids);
        for (Acl acl : acls) {
            String repoKey = repoPath.getRepoKey();
            String path = repoPath.getPath();
            PermissionTarget aclPermissionTarget = acl.getPermissionTarget();
            String aclRepoKey = aclPermissionTarget.getRepoKey();
            //Try ANY repo or the provided repo
            if (PermissionTargetInfo.ANY_REPO.equals(aclRepoKey) || aclRepoKey.equals(repoKey)) {
                boolean match = matches(aclPermissionTarget, path);
                if (match) {
                    boolean granted = acl.isGranted(new Permission[]{permission}, sids, false);
                    if (granted) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasPermissionOnPermissionTarget(PermissionTarget target,
            Permission permission) {
        Authentication authentication = getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }
        // Admins has permissions on any target
        if (isAdmin(authentication)) {
            return true;
        }

        Sid[] sids = getUserEffectiveSids(getSimpleUser(authentication));
        Acl acl = aclManager.findAclById(target);
        boolean granted = acl.isGranted(new Permission[]{permission}, sids, false);
        return granted;
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        //Export the security settings as xml using xstream
        SecurityInfo descriptor = getSecurityData();
        String path = settings.getBaseDir() + "/" + FILE_NAME;
        XStream xstream = getXstream();
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(path));
            xstream.toXML(descriptor, os);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Failed to export security configuration.", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    public SecurityInfo getSecurityData() {
        List<UserInfo> users = getAllUsers(true);
        List<GroupInfo> groups = getAllGroups();
        List<AclInfo> acls = aclManager.getAllAclDescriptors();
        SecurityInfo descriptor = new SecurityInfo(users, groups, acls);
        return descriptor;
    }

    public void importFrom(ImportSettings settings, StatusHolder status) {
        status.setStatus("Importing security...", log);
        //Import the new security definitions
        File baseDir = settings.getBaseDir();
        String securityData;
        // First check for security.xml file
        File securityXmlFile = new File(baseDir, FILE_NAME);
        if (!securityXmlFile.exists()) {
            String msg = "Security file " + securityXmlFile +
                    " does not exists no import of security will be done.";
            if (settings.isFailIfEmpty()) {
                status.setError(msg, log);
            } else {
                status.setWarning(msg, log);
            }
            return;
        }
        try {
            // Check for version
            if (settings.getExportVersion() != null) {
                securityData = BackupUtils.convertSecurityFile(baseDir, settings.getExportVersion());
            } else {
                securityData = FileUtils.readFileToString(securityXmlFile);
            }
        } catch (IOException e) {
            status.setWarning("Could not read security file", log);
            return;
        }
        SecurityService me = InternalContextHelper.get().beanForType(SecurityService.class);
        me.removeAllSecurityData();
        XStream xstream = getXstream();
        SecurityInfo descriptor = (SecurityInfo) xstream.fromXML(securityData);
        me.importSecurityData(descriptor);
    }

    static Authentication getAuthentication() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext == null) {
            return null;
        }
        Authentication authentication = securityContext.getAuthentication();
        return authentication;
    }

    private void createAdminUser() {
        SimpleUser user =
                new SimpleUser(USER_DEFAULT_ADMIN, DigestUtils.md5Hex(DEFAULT_ADMIN_PASSWORD), null,
                        true, true, true, true, true, true);
        userGroupManager.createUser(user);
    }

    private void createAnonymousUser() {
        SimpleUser anonUser;
        anonUser = new SimpleUser(UserInfo.ANONYMOUS, DigestUtils.md5Hex(""), null, false, true, false, true,
                true, true);
        boolean created = userGroupManager.createUser(anonUser);
        if (created) {
            aclManager.createAnythingPermision(anonUser);
        }
    }

    /**
     * @param user The authentication token.
     * @return An array of sids of the current user and all it's groups.
     */
    private static ArtifactorySid[] getUserEffectiveSids(SimpleUser user) {
        ArtifactorySid[] sids;
        Set<String> groups = user.getDescriptor().getGroups();
        if (!groups.isEmpty()) {
            sids = new ArtifactorySid[groups.size() + 1];
            // add the current user
            sids[0] = new ArtifactorySid(user.getUsername());

            // add all the groups the user is a member of
            int sidsArrayIndex = 1;
            for (String groupName : groups) {
                sids[sidsArrayIndex] = new ArtifactorySid(groupName, true);
                sidsArrayIndex++;
            }
        } else {
            sids = new ArtifactorySid[]{new ArtifactorySid(user.getUsername())};
        }

        return sids;
    }

    public void importSecurityData(SecurityInfo descriptor) {
        List<GroupInfo> groups = descriptor.getGroups();
        if (groups != null) {
            for (GroupInfo group : groups) {
                userGroupManager.createGroup(new Group(group));
            }
        }
        List<UserInfo> users = descriptor.getUsers();
        boolean hasAnonymous = false;
        if (users != null) {
            for (UserInfo user : users) {
                userGroupManager.createUser(new SimpleUser(user));
                if (user.isAnonymous()) {
                    hasAnonymous = true;
                }
            }
        }
        List<AclInfo> acls = descriptor.getAcls();
        if (acls != null) {
            for (AclInfo acl : acls) {
                aclManager.createAcl(new Acl(acl));
            }
        }
        if (!hasAnonymous) {
            createAnonymousUser();
        }
    }

    public void removeAllSecurityData() {
        //Remove all existing users
        List<UserInfo> oldUsers = getAllUsers(true);
        for (UserInfo oldUser : oldUsers) {
            userGroupManager.removeUser(oldUser.getUsername());
        }
        //Remove all existing groups
        List<GroupInfo> oldGroups = getAllGroups();
        for (GroupInfo oldGroup : oldGroups) {
            userGroupManager.removeGroup(oldGroup.getGroupName());
        }
        //Clean up all acls
        aclManager.deleteAllAcls();
    }

    public StatusHolder testLdapConnection(LdapSetting ldapSetting, String username,
            String password) {
        LdapConnectionTester ldapTester = new LdapConnectionTester();
        return ldapTester.testLdapConnection(ldapSetting, username, password);
    }

    public boolean isPasswordEncryptionEnabled() {
        CentralConfigDescriptor cc = centralConfig.getDescriptor();
        return cc.getSecurity().getPasswordSettings().isEncryptionEnabled();
    }

    public boolean userPasswordMatches(String passwordToCheck) {
        Authentication authentication = getAuthentication();
        return authentication != null && passwordToCheck.equals(authentication.getCredentials());
    }

    private static boolean isAdmin(Authentication authentication) {
        return isAuthenticated(authentication) && getSimpleUser(authentication).isAdmin();
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private static SimpleUser getSimpleUser(Authentication authentication) {
        return (SimpleUser) authentication.getPrincipal();
    }

    private static boolean matches(PermissionTarget aclPermissionTarget, String path) {
        return PathMatcher.matches(path,
                aclPermissionTarget.getIncludes(), aclPermissionTarget.getExcludes());
    }

    private static XStream getXstream() {
        XStream xstream = new XStream();
        xstream.processAnnotations(SecurityInfo.class);
        return xstream;
    }

    private void assertAdmin() {
        if (!isAdmin()) {
            throw new SecurityException(
                    "The attempted action is permitted to users with administrative privileges only.");
        }
    }
}