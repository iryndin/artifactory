/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.security;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.api.cache.ArtifactoryCache;
import org.artifactory.api.cache.CacheService;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.*;
import org.artifactory.api.util.Pair;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.config.ConfigurationException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.sso.HttpSsoSettings;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.security.interceptor.SecurityConfigurationChangesInterceptors;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.update.security.SecurityInfoReader;
import org.artifactory.update.security.SecurityVersion;
import org.artifactory.util.EmailException;
import org.artifactory.util.PathMatcher;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.jcr.Session;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.*;

@Service
@Reloadable(beanClass = InternalSecurityService.class,
        initAfter = {JcrService.class, UserGroupManager.class, InternalAclManager.class})
public class SecurityServiceImpl implements InternalSecurityService {
    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);

    @Autowired
    private InternalAclManager internalAclManager;

    @Autowired
    private UserGroupManager userGroupManager;

    @Autowired
    private JcrService jcr;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private MailService mailService;

    @Autowired
    private AddonsManager addons;

    @Autowired
    private LdapService ldapService;

    @Autowired
    private SecurityConfigurationChangesInterceptors interceptors;

    private InternalArtifactoryContext context;

    @Autowired
    private void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (InternalArtifactoryContext) context;
    }

    public void init() {
        //Locate and import external configuration file
        checkForExternalConfiguration();
        checkOcmFolders();
        CoreAddons coreAddon = addons.addonByType(CoreAddons.class);
        if (coreAddon.isNewAdminAccountAllowed() && !adminUserExists()) {
            createDefaultAdminUser();
        }
        createDefaultAnonymousUser();
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        // TODO: Change the PermissionTarget repoKey according to new config
    }

    public void destroy() {
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
        SecurityVersion.values();
        SecurityVersion originalVersion = source.getVersion().getSubConfigElementVersion(SecurityVersion.class);
        Session rawSession = context.getJcrService().getUnmanagedSession().getSession();
        //Convert the ocm storage
        try {
            originalVersion.convert(rawSession);
        } finally {
            rawSession.logout();
        }
    }

    /**
     * Checks for an externally supplied configuration file ($ARTIFACTORY_HOME/etc/security.xml). If such a file is
     * found, it will be deserialized to a security info (descriptor) object and imported to the system. This option is
     * to be used in cases like when an administrator is locked out of the system, etc'.
     */
    private void checkForExternalConfiguration() {
        ArtifactoryContext ctx = ContextHelper.get();
        File etcDir = ctx.getArtifactoryHome().getEtcDir();
        File configurationFile = new File(etcDir, "security.import.xml");
        String configAbsolutePath = configurationFile.getAbsolutePath();
        if (configurationFile.isFile()) {
            if (!configurationFile.canRead() || !configurationFile.canWrite()) {
                throw new ConfigurationException("Insufficient permissions. Security configuration import requires " +
                        "both read and write permissions for " + configAbsolutePath);
            }
            try {
                SecurityInfo descriptorToSave = new SecurityInfoReader().read(configurationFile);
                //InternalSecurityService txMe = ctx.beanForType(InternalSecurityService.class);
                importSecurityData(descriptorToSave);
                org.artifactory.util.FileUtils
                        .switchFiles(configurationFile, new File(etcDir, "security.bootstrap.xml"));
                log.info("Security configuration imported successfully from " + configAbsolutePath + ".");
            } catch (Exception e) {
                throw new IllegalArgumentException("An error has occurred while deserializing the file " +
                        configAbsolutePath + ". Please assure it's validity or remove it from the 'etc' folder.", e);
            }
        }
    }

    public void createOcmRoots() {
        jcr.getOrCreateUnstructuredNode(JcrAclManager.getAclsJcrPath());
        jcr.getOrCreateUnstructuredNode(JcrUserGroupManager.getUsersJcrPath());
        jcr.getOrCreateUnstructuredNode(JcrUserGroupManager.getGroupsJcrPath());
    }

    private void checkOcmFolders() {
        if (!jcr.itemNodeExists(JcrUserGroupManager.getGroupsJcrPath()) ||
                !jcr.itemNodeExists(JcrUserGroupManager.getUsersJcrPath()) ||
                !jcr.itemNodeExists(JcrAclManager.getAclsJcrPath())) {
            throw new RepositoryRuntimeException("Creation of root folders for OCM failed");
        }
    }

    public boolean isAnonymous() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return authentication != null && UserInfo.ANONYMOUS.equals(authentication.getName());
    }

    public boolean isAnonAccessEnabled() {
        SecurityDescriptor security = centralConfig.getDescriptor().getSecurity();
        return security != null && security.isAnonAccessEnabled();
    }

    public boolean isAuthenticated() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return isAuthenticated(authentication);
    }

    public boolean isAdmin() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return isAdmin(authentication);
    }

    public AclInfo createAcl(AclInfo aclInfo) {
        assertAdmin();
        cleanupAclInfo(aclInfo);
        AclInfo createdAcl = internalAclManager.createAcl(aclInfo).getDescriptor();
        interceptors.onPermissionsAdd();
        return createdAcl;
    }

    public void updateAcl(AclInfo acl) {
        //If the editing user is not a sys-admin
        if (!isAdmin()) {
            //Assert that no unauthorized modifications were performed
            validateUnmodifiedPermissionTarget(acl.getPermissionTarget());
        }

        // Removing empty Ace
        cleanupAclInfo(acl);
        internalAclManager.updateAcl(new Acl(acl));
        interceptors.onPermissionsUpdate();
    }

    public void deleteAcl(PermissionTargetInfo target) {
        internalAclManager.deleteAcl(new PermissionTarget(target));
        interceptors.onPermissionsDelete();
    }

    public List<PermissionTargetInfo> getPermissionTargets(ArtifactoryPermission artifactoryPermission) {
        Permission permission = permissionFor(artifactoryPermission);
        return getPermissionTargetsByPermission(permission);
    }

    private List<PermissionTargetInfo> getPermissionTargetsByPermission(Permission permission) {
        List<PermissionTarget> allTargets = internalAclManager.getAllPermissionTargets();
        List<PermissionTargetInfo> result = new ArrayList<PermissionTargetInfo>();
        for (PermissionTarget permissionTarget : allTargets) {
            if (hasPermissionOnPermissionTarget(permissionTarget, permission)) {
                result.add(permissionTarget.getDescriptor());
            }
        }
        return result;
    }

    private List<PermissionTargetInfo> getPermissionTargetsByPermission(Permission permission, SimpleUser user) {
        List<PermissionTarget> allTargets = internalAclManager.getAllPermissionTargets();
        List<PermissionTargetInfo> result = new ArrayList<PermissionTargetInfo>();
        for (PermissionTarget permissionTarget : allTargets) {
            if (hasPermissionOnPermissionTarget(permissionTarget, permission, user)) {
                result.add(permissionTarget.getDescriptor());
            }
        }
        return result;
    }

    public boolean isUpdatableProfile() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && simpleUser.isUpdatableProfile();
    }

    public boolean isTransientUser() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && simpleUser.isTransientUser();
    }

    public String currentUsername() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        //Do not return a null username or this will cause a jcr constraint violation
        return (authentication != null ? authentication.getName() : SecurityService.USER_SYSTEM);
    }

    public UserInfo currentUser() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (authentication == null) {
            return null;
        }
        SimpleUser user = getSimpleUser(authentication);
        return (user.getDescriptor());
    }

    public UserInfo findUser(String username) {
        UserInfo userInfo = userGroupManager.loadUserByUsername(username).getDescriptor();
        return userInfo;
    }

    public AclInfo getAcl(PermissionTargetInfo permissionTarget) {
        Acl acl = internalAclManager.findAclById(new PermissionTarget(permissionTarget));
        if (acl != null) {
            return acl.getDescriptor();
        }
        return null;
    }

    private void cleanupAclInfo(AclInfo acl) {
        Iterator<AceInfo> it = acl.getAces().iterator();
        while (it.hasNext()) {
            AceInfo aceInfo = it.next();
            if (aceInfo.getMask() == 0) {
                it.remove();
            }
        }
    }

    public List<AclInfo> getAllAcls() {
        Collection<Acl> acls = internalAclManager.getAllAcls();
        List<AclInfo> descriptors = new ArrayList<AclInfo>(acls.size());
        for (Acl acl : acls) {
            descriptors.add(acl.getDescriptor());
        }
        return descriptors;
    }

    public List<UserInfo> getAllUsers(boolean includeAdmins) {
        Collection<User> allUsers = userGroupManager.getAllUsers();
        List<UserInfo> usersInfo = new ArrayList<UserInfo>(allUsers.size());
        for (User user : allUsers) {
            UserInfo userInfo = user.getInfo();
            if (includeAdmins || !userInfo.isAdmin()) {
                //Only include non admin users if asked
                usersInfo.add(userInfo);
            }
        }
        return usersInfo;
    }

    private boolean adminUserExists() {
        List<UserInfo> users = getAllUsers(true);
        for (UserInfo user : users) {
            if (user.isAdmin()) {
                return true;
            }
        }
        return false;
    }

    public boolean createUser(UserInfo user) {
        if (user.isAdmin()) {
            CoreAddons coreAddon = addons.addonByType(CoreAddons.class);
            if (!coreAddon.isNewAdminAccountAllowed()) {
                return false;
            }
        }
        user.setUsername(user.getUsername().toLowerCase());
        boolean userCreated = userGroupManager.createUser(new SimpleUser(user));
        if (userCreated) {
            interceptors.onUserAdd(user.getUsername());
        }
        return userCreated;
    }

    public void updateUser(UserInfo user) {
        user.setUsername(user.getUsername().toLowerCase());
        userGroupManager.updateUser(new SimpleUser(user));
    }

    public void deleteUser(String username) {
        internalAclManager.removeAllUserAces(username);
        userGroupManager.removeUser(username);
        interceptors.onUserDelete(username);
    }

    public void updateGroup(GroupInfo groupInfo) {
        userGroupManager.updateGroup(new Group(groupInfo));
    }

    public boolean createGroup(GroupInfo groupInfo) {
        boolean groupCreated = userGroupManager.createGroup(new Group(groupInfo));
        if (groupCreated) {
            interceptors.onGroupAdd(groupInfo.getGroupName());
        }
        return groupCreated;
    }

    public void deleteGroup(String groupName) {
        List<UserInfo> userInGroup = findUsersInGroup(groupName);
        for (UserInfo userInfo : userInGroup) {
            removeUserFromGroup(userInfo.getUsername(), groupName);
        }
        userGroupManager.removeGroup(groupName);
        interceptors.onGroupDelete(groupName);
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

    public List<GroupInfo> getAllExternalGroups() {
        List<GroupInfo> externalGroups = Lists.newArrayList();
        List<GroupInfo> allGroups = getAllGroups();
        for (GroupInfo group : allGroups) {
            if (group.isExternal()) {
                externalGroups.add(group);
            }
        }
        return externalGroups;
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
        interceptors.onAddUsersToGroup(groupName, usernames);
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
        interceptors.onRemoveUsersFromGroup(groupName, usernames);
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

    public String resetPassword(String userName, String remoteAddress, String resetPageUrl) {
        boolean foundUser = false;
        UserInfo userInfo = null;
        try {
            userInfo = findUser(userName);
            foundUser = true;
        } catch (UsernameNotFoundException e) {
            //Alert in the log when trying to reset a password of an unknown user
            log.warn("An attempt has been made to reset a password of unknown user: {}", userName);
        }

        //If the user is found, and has an email address
        if (foundUser && (userInfo != null) && (!StringUtils.isEmpty(userInfo.getEmail()))) {

            //If the user hasn't got sufficient permissions
            if (!userInfo.isUpdatableProfile()) {
                throw new RuntimeException("The specified user is not permitted to reset his password.");
            }

            //Get client IP, then generate and send a password reset key
            try {
                generatePasswordResetKey(userName, remoteAddress, resetPageUrl);
            } catch (EmailException ex) {
                String message = ex.getMessage() + " Please contact your administrator.";
                throw new RuntimeException(message);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
        return "We have sent you via email a link for resetting your password. Please check your inbox.";
    }

    public UserInfo findOrCreateExternalAuthUser(String userName, boolean transientUser) {
        UserInfo userInfo;
        try {
            userInfo = findUser(userName.toLowerCase());
        } catch (UsernameNotFoundException e) {
            log.debug("Creating new external user '{}'", userName);
            userInfo = new UserInfoBuilder(userName.toLowerCase()).updatableProfile(false).build();
            userInfo.setInternalGroups(getNewUserDefaultGroupsNames());
            if (transientUser) {
                userInfo.setTransientUser(true);
            } else {
                boolean success = userGroupManager.createUser(new SimpleUser(userInfo));
                if (!success) {
                    log.error("User '{}' was not created!", userInfo);
                }
            }
        }
        return userInfo;
    }

    /**
     * Generates a password recovery key for the specified user and send it by mail
     *
     * @param username      User to rest his password
     * @param remoteAddress The IP of the client that sent the request
     * @param resetPageUrl  The URL to the password reset page
     * @throws Exception
     */
    public void generatePasswordResetKey(String username, String remoteAddress, String resetPageUrl) throws Exception {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user
            throw new IllegalArgumentException("Could not find specified username.", e);
        }

        //If user has valid email
        if (!StringUtils.isEmpty(userInfo.getEmail())) {
            if (!userInfo.isUpdatableProfile()) {
                //If user is not allowed to update his profile
                throw new AuthorizationException("User is not permitted to reset his password.");
            }

            //Build key by UUID + current time millis + client ip -> encoded in B64
            UUID uuid = UUID.randomUUID();
            String passwordKey = uuid.toString() + ":" + System.currentTimeMillis() + ":" + remoteAddress;
            byte[] encodedKey = Base64.encodeBase64(passwordKey.getBytes());
            String encodedKeyString = new String(encodedKey);

            userInfo.setGenPasswordKey(encodedKeyString);
            updateUser(userInfo);

            //Add encoded key to page url
            String resetPage = resetPageUrl + "?key=" + encodedKeyString;

            //If there are any admins with valid email addresses, add them to the list that the message will contain
            String adminList = getAdminListBlock(userInfo);
            InputStream stream = null;
            try {
                //Get message body from properties and substitute variables
                stream = getClass().getResourceAsStream("/org/artifactory/email/messages/resetPassword.properties");
                ResourceBundle resourceBundle = new PropertyResourceBundle(stream);
                String body = resourceBundle.getString("body");
                body = MessageFormat.format(body, username, remoteAddress, resetPage, adminList);
                mailService.sendMail(new String[]{userInfo.getEmail()}, "Reset password request", body);
            } catch (EmailException e) {
                log.error("Error while resetting password for user: '" + username + "'.", e);
                throw e;
            } finally {
                IOUtils.closeQuietly(stream);
            }
            log.info("The user: '{}' has been sent a password reset message by mail.", username);
        }
    }

    public Pair<Date, String> getPasswordResetKeyInfo(String username) {
        UserInfo userInfo = findUser(username);
        String passwordKey = userInfo.getGenPasswordKey();
        if (StringUtils.isEmpty(passwordKey)) {
            return null;
        }

        byte[] decodedKey = Base64.decodeBase64(passwordKey.getBytes());
        String decodedKeyString = new String(decodedKey);
        String[] splitKey = decodedKeyString.split(":");

        //Key must be in 3 parts
        if (splitKey.length < 3) {
            throw new IllegalArgumentException("Password reset key must contain 3 parts - 'UUID:Date:IP'");
        }

        String time = splitKey[1];
        String ip = splitKey[2];

        Date date = new Date(Long.parseLong(time));

        return new Pair<Date, String>(date, ip);
    }

    public Pair<String, Long> getUserLastLoginInfo(String username) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user (might be transient user)
            log.trace("Could not retrieve last login info for username '{}'.", username);
            return null;
        }

        Pair<String, Long> pair = null;
        String lastLoginClientIp = userInfo.getLastLoginClientIp();
        long lastLoginTimeMillis = userInfo.getLastLoginTimeMillis();
        if (!StringUtils.isEmpty(lastLoginClientIp) && (lastLoginTimeMillis != 0)) {
            pair = new Pair<String, Long>(lastLoginClientIp, lastLoginTimeMillis);
        }
        return pair;
    }

    public void updateUserLastLogin(String username, String clientIp, long loginTimeMillis) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            // user not found (might be a transient user)
            log.trace("Could not update non-exiting username: {}'.", username);
            return;
        }
        userInfo.setLastLoginTimeMillis(loginTimeMillis);
        userInfo.setLastLoginClientIp(clientIp);
        updateUser(userInfo);
    }

    public Pair<String, Long> getUserLastAccessInfo(String username) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user
            throw new IllegalArgumentException("Could not find specified username.", e);
        }

        Pair<String, Long> pair = null;
        String lastAccessClientIp = userInfo.getLastAccessClientIp();
        long lastAccessTimeMillis = userInfo.getLastAccessTimeMillis();
        if (!StringUtils.isEmpty(lastAccessClientIp) && (lastAccessTimeMillis != 0)) {
            pair = new Pair<String, Long>(lastAccessClientIp, lastAccessTimeMillis);
        }
        return pair;
    }

    public void updateUserLastAccess(String username, String clientIp, long accessTimeMillis,
            long acessUpdatesResolutionMillis) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user
            throw new IllegalArgumentException("Could not find specified username.", e);
        }
        //Check if we should update
        long existingLastAccess = userInfo.getLastAccessTimeMillis();
        if (existingLastAccess <= 0 || existingLastAccess + acessUpdatesResolutionMillis < accessTimeMillis) {
            userInfo.setLastAccessTimeMillis(accessTimeMillis);
            userInfo.setLastAccessClientIp(clientIp);
            updateUser(userInfo);
        }
    }

    public boolean isHttpSsoProxied() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        return httpSsoSettings != null && httpSsoSettings.isHttpSsoProxied();
    }

    public boolean isNoAutoUserCreation() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        return httpSsoSettings != null && httpSsoSettings.isNoAutoUserCreation();
    }

    public String getRemoteUserRequestVariable() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        if (httpSsoSettings == null) {
            return null;
        } else {
            return httpSsoSettings.getRemoteUserRequestVariable();
        }
    }

    private Permission permissionFor(ArtifactoryPermission permission) {
        if (permission == ArtifactoryPermission.ADMIN) {
            return BasePermission.ADMINISTRATION;
        } else if (permission == ArtifactoryPermission.DELETE) {
            return BasePermission.DELETE;
        } else if (permission == ArtifactoryPermission.DEPLOY) {
            return BasePermission.WRITE;
        } else if (permission == ArtifactoryPermission.ANNOTATE) {
            return BasePermission.CREATE;
        } else if (permission == ArtifactoryPermission.READ) {
            return BasePermission.READ;
        } else {
            throw new IllegalArgumentException("Cannot determine mask for role '" + permission + "'.");
        }
    }

    public boolean hasPermission(ArtifactoryPermission artifactoryPermission) {
        return isAdmin() || !getPermissionTargets(artifactoryPermission).isEmpty();
    }

    public boolean canRead(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.READ, false);
    }

    public boolean canImplicitlyReadParentPath(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.READ, true);
    }

    public boolean canAnnotate(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.ANNOTATE, false);
    }

    public boolean canDeploy(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.DEPLOY, false);
    }

    public boolean canDelete(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.DELETE, false);
    }

    public boolean canAdmin(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.ADMIN, false);
    }

    public boolean canAdmin(PermissionTargetInfo target) {
        Permission adminPermission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), adminPermission);
    }

    public boolean canRead(UserInfo user, PermissionTargetInfo target) {
        Permission readPermission = permissionFor(ArtifactoryPermission.READ);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), readPermission, new SimpleUser(user));
    }

    public boolean canAnnotate(UserInfo user, PermissionTargetInfo target) {
        Permission annotatePermission = permissionFor(ArtifactoryPermission.ANNOTATE);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), annotatePermission, new SimpleUser(user));
    }

    public boolean canDeploy(UserInfo user, PermissionTargetInfo target) {
        Permission deployPermission = permissionFor(ArtifactoryPermission.DEPLOY);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), deployPermission, new SimpleUser(user));
    }

    public boolean canDelete(UserInfo user, PermissionTargetInfo target) {
        Permission deletePermission = permissionFor(ArtifactoryPermission.DELETE);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), deletePermission, new SimpleUser(user));
    }

    public boolean canAdmin(UserInfo user, PermissionTargetInfo target) {
        Permission adminPermission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), adminPermission, new SimpleUser(user));
    }

    public boolean canRead(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.READ);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canAnnotate(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ANNOTATE);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canDelete(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DELETE);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canDeploy(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DEPLOY);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canAdmin(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    public boolean canRead(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.READ);
        return hasPermission(group, path, permission);
    }

    public boolean canAnnotate(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ANNOTATE);
        return hasPermission(group, path, permission);
    }

    public boolean canDelete(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DELETE);
        return hasPermission(group, path, permission);
    }

    public boolean canDeploy(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DEPLOY);
        return hasPermission(group, path, permission);
    }

    public boolean canAdmin(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermission(group, path, permission);
    }

    public boolean userHasPermissions(String username) {
        SimpleUser user = new SimpleUser(findUser(username.toLowerCase()));
        for (ArtifactoryPermission permission : ArtifactoryPermission.values()) {
            if (hasPermission(permission, user)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(RepoPath repoPath, ArtifactoryPermission permission, boolean checkPartialPath) {
        return hasPermission(repoPath, permissionFor(permission), checkPartialPath);
    }

    private boolean hasPermission(RepoPath repoPath, Permission permission, boolean checkPartialPath) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }

        // Admins has permissions for all paths and all repositories
        if (isAdmin(authentication)) {
            return true;
        }

        // Anonymous users are checked only if anonymous access is enabled
        if (isAnonymous() && !isAnonAccessEnabled()) {
            return false;
        }

        ArtifactorySid[] sids = getUserEffectiveSids(getSimpleUser(authentication));
        return isGranted(repoPath, permission, sids, checkPartialPath);
    }

    private boolean hasPermission(SimpleUser user, RepoPath repoPath, Permission permission) {
        // Admins has permissions for all paths and all repositories
        if (user.isAdmin()) {
            return true;
        }

        // Anonymous users are checked only if anonymous access is enabled
        if (user.isAnonymous() && !isAnonAccessEnabled()) {
            return false;
        }

        ArtifactorySid[] sids = getUserEffectiveSids(user);
        return isGranted(repoPath, permission, sids);
    }

    private boolean hasPermission(GroupInfo group, RepoPath repoPath, Permission permission) {
        ArtifactorySid[] sid = {new ArtifactorySid(group.getGroupName(), true)};
        return isGranted(repoPath, permission, sid);
    }

    private boolean hasPermission(ArtifactoryPermission artifactoryPermission, SimpleUser user) {
        return !getPermissionTargets(artifactoryPermission, user).isEmpty();
    }

    private List<PermissionTargetInfo> getPermissionTargets(ArtifactoryPermission artifactoryPermission,
            SimpleUser user) {
        Permission permission = permissionFor(artifactoryPermission);
        return getPermissionTargetsByPermission(permission, user);
    }

    private boolean isGranted(RepoPath repoPath, Permission permission, ArtifactorySid[] sids) {
        return isGranted(repoPath, permission, sids, false);
    }

    private boolean isGranted(
            RepoPath repoPath, Permission permission, ArtifactorySid[] sids, boolean checkPartialPath) {
        List<Acl> acls = internalAclManager.getAllAcls(sids);
        for (Acl acl : acls) {
            String repoKey = repoPath.getRepoKey();
            String path = repoPath.getPath();
            PermissionTarget aclPermissionTarget = acl.getPermissionTarget();
            if (isPermissionTargetIncludesRepoKey(repoKey, aclPermissionTarget)) {
                boolean containsPartialPath = false;
                if (checkPartialPath) {
                    containsPartialPath = isPartialPathIncluded(aclPermissionTarget, path);
                }
                boolean match = matches(aclPermissionTarget, path);
                if (match || containsPartialPath) {
                    boolean granted = acl.isGranted(new Permission[]{permission}, sids, false);
                    if (granted) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPartialPathIncluded(PermissionTarget aclPermissionTarget, String path) {
        List<String> includesList = aclPermissionTarget.getIncludes();
        for (String includes : includesList) {
            if (includes.contains(path)) {
                return true;
            }
        }

        return false;
    }

    private boolean isPermissionTargetIncludesRepoKey(String repoKey, PermissionTarget permissionTarget) {
        // checks if repo key is part of the permission target repository keys taking into account
        // the special logical repo keys of a permission target like "Any", "All Local" etc.
        List<String> repoKeys = permissionTarget.getRepoKeys();
        if (repoKeys.contains(PermissionTargetInfo.ANY_REPO)) {
            return true;
        }

        if (repoKeys.contains(repoKey)) {
            return true;
        }

        LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(repoKey);
        if (localRepo != null) {
            if (!localRepo.isCache() && repoKeys.contains(PermissionTargetInfo.ANY_LOCAL_REPO)) {
                return true;
            } else if (localRepo.isCache() && repoKeys.contains(PermissionTargetInfo.ANY_REMOTE_REPO)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    private boolean hasPermissionOnPermissionTarget(PermissionTarget target, Permission permission) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }
        // Admins has permissions on any target
        if (isAdmin(authentication)) {
            return true;
        }

        return hasPermissionOnPermissionTarget(target, permission, getSimpleUser(authentication));
    }

    private boolean hasPermissionOnPermissionTarget(PermissionTarget target, Permission permission, SimpleUser user) {
        // Admins has permissions on any target
        if (user.isAdmin()) {
            return true;
        }

        Sid[] sids = getUserEffectiveSids(user);
        Acl acl = internalAclManager.findAclById(target);
        return acl.isGranted(new Permission[]{permission}, sids, false);
    }

    public void exportTo(ExportSettings settings) {
        exportSecurityInfo(settings);
    }

    private void exportSecurityInfo(ExportSettings settings) {
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
        List<AclInfo> acls = getAllAcls();
        SecurityInfo descriptor = new SecurityInfo(users, groups, acls);
        descriptor.setVersion(SecurityVersion.getCurrent().name());
        return descriptor;
    }

    public void importFrom(ImportSettings settings) {
        MultiStatusHolder status = settings.getStatusHolder();
        status.setStatus("Importing security...", log);
        importSecurityXml(settings, status);
    }

    private void importSecurityXml(ImportSettings settings, StatusHolder status) {
        //Import the new security definitions
        File baseDir = settings.getBaseDir();
        // First check for security.xml file
        File securityXmlFile = new File(baseDir, FILE_NAME);
        if (!securityXmlFile.exists()) {
            String msg = "Security file " + securityXmlFile +
                    " does not exists no import of security will be done.";
            settings.alertFailIfEmpty(msg, log);
            return;
        }
        SecurityInfo securityInfo;
        try {
            securityInfo = new SecurityInfoReader().read(securityXmlFile);
        } catch (Exception e) {
            status.setWarning("Could not read security file", log);
            return;
        }
        SecurityService me = InternalContextHelper.get().beanForType(SecurityService.class);
        me.importSecurityData(securityInfo);
    }

    private void createDefaultAdminUser() {
        UserInfoBuilder builder = new UserInfoBuilder(DEFAULT_ADMIN_USER);
        builder.password(DigestUtils.md5Hex(DEFAULT_ADMIN_PASSWORD)).email(null).admin(true).updatableProfile(true);
        createUser(builder.build());
    }

    private void createDefaultAnonymousUser() {
        UserInfoBuilder builder = new UserInfoBuilder(UserInfo.ANONYMOUS);
        builder.password(DigestUtils.md5Hex("")).email(null).updatableProfile(true);
        UserInfo anonUser = builder.build();
        boolean createdAnonymousUser = createUser(anonUser);

        if (createdAnonymousUser) {
            GroupInfo readersGroup = new GroupInfo("readers", "A group for read-only users", true);
            createGroup(readersGroup);
            internalAclManager.createDefaultSecurityEntities(new SimpleUser(anonUser), new Group(readersGroup));
        }
    }

    /**
     * @param user The authentication token.
     * @return An array of sids of the current user and all it's groups.
     */
    private static ArtifactorySid[] getUserEffectiveSids(SimpleUser user) {
        ArtifactorySid[] sids;
        Set<UserInfo.UserGroupInfo> groups = user.getDescriptor().getGroups();
        if (!groups.isEmpty()) {
            sids = new ArtifactorySid[groups.size() + 1];
            // add the current user
            sids[0] = new ArtifactorySid(user.getUsername());

            // add all the groups the user is a member of
            int sidsArrayIndex = 1;
            for (UserInfo.UserGroupInfo group : groups) {
                sids[sidsArrayIndex] = new ArtifactorySid(group.getGroupName(), true);
                sidsArrayIndex++;
            }
        } else {
            sids = new ArtifactorySid[]{new ArtifactorySid(user.getUsername())};
        }

        return sids;
    }

    public void importSecurityData(String securityXml) {
        importSecurityData(new SecurityInfoReader().read(securityXml));
    }

    public void importSecurityData(SecurityInfo securityInfo) {
        interceptors.onBeforeSecurityImport(securityInfo);
        removeAllSecurityData();
        List<GroupInfo> groups = securityInfo.getGroups();
        if (groups != null) {
            for (GroupInfo group : groups) {
                userGroupManager.createGroup(new Group(group));
            }
        }
        List<UserInfo> users = securityInfo.getUsers();
        boolean hasAnonymous = false;
        if (users != null) {
            for (UserInfo user : users) {
                userGroupManager.createUser(new SimpleUser(user));
                if (user.isAnonymous()) {
                    hasAnonymous = true;
                }
            }
        }
        List<AclInfo> acls = securityInfo.getAcls();
        if (acls != null) {
            for (AclInfo acl : acls) {
                internalAclManager.createAcl(new Acl(acl));
            }
        }
        if (!hasAnonymous) {
            createDefaultAnonymousUser();
        }
    }

    private void removeAllSecurityData() {
        //Respect order for clean removal
        //Clean up all acls
        internalAclManager.deleteAllAcls();
        //Remove all existing groups
        userGroupManager.deleteAllGroupsAndUsers();
        // Clear authentication cache
        InternalContextHelper.get().beanForType(CacheService.class).getCache(ArtifactoryCache.authentication).clear();
    }

    public StatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
        return ldapService.testLdapConnection(ldapSetting, username, password);
    }

    public boolean isPasswordEncryptionEnabled() {
        CentralConfigDescriptor cc = centralConfig.getDescriptor();
        return cc.getSecurity().getPasswordSettings().isEncryptionEnabled();
    }

    public boolean userPasswordMatches(String passwordToCheck) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
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
        return PathMatcher.matches(path, aclPermissionTarget.getIncludes(), aclPermissionTarget.getExcludes());
    }

    private static XStream getXstream() {
        return XStreamFactory.create(SecurityInfo.class);
    }

    private void assertAdmin() {
        if (!isAdmin()) {
            throw new SecurityException(
                    "The attempted action is permitted to users with administrative privileges only.");
        }
    }

    /**
     * Returns the HTML block of the admin email addresses for the password reset message
     *
     * @param recipientUser - Recipient UserInfo object
     * @return String - HTML block of the admin email addresses
     */
    private String getAdminListBlock(UserInfo recipientUser) {
        boolean adminsExist = false;
        StringBuilder adminSb = new StringBuilder();

        //Add list title
        adminSb.append("<p>If you believe this message was wrongly sent to you, " +
                "please contact one of the server administrators below:<br>");

        List<UserInfo> userInfoList = getAllUsers(true);
        for (UserInfo user : userInfoList) {

            //If user is admin and has a valid email
            if ((user.isAdmin()) && (!StringUtils.isEmpty(user.getEmail())) && (!user.equals(recipientUser))) {
                adminsExist = true;
                adminSb.append(user.getEmail());

                //If it is not the last item, add a line break
                if ((userInfoList.indexOf(user) != userInfoList.size())) {
                    adminSb.append("<br>");
                }
            }
        }
        adminSb.append("<p>");

        //Make sure valid admins have been found before adding them to the message body
        if (adminsExist) {
            return adminSb.toString();
        }
        return "";
    }

    /**
     * Validates that the edited given permission target is not different from the existing one. This method should be
     * called before an ACL is being modified by a non-sys-admin user
     *
     * @param newInfo Edited permission target
     * @throws AuthorizationException Thrown in case an unauthorized modification has occurred
     */
    private void validateUnmodifiedPermissionTarget(PermissionTargetInfo newInfo) throws AuthorizationException {
        if (newInfo == null) {
            return;
        }

        AclInfo oldAcl = getAcl(newInfo);
        if (oldAcl == null) {
            return;
        }

        PermissionTargetInfo oldInfo = oldAcl.getPermissionTarget();
        if (oldInfo == null) {
            return;
        }

        if (!oldInfo.getExcludes().equals(newInfo.getExcludes())) {
            alertModifiedField("excludes");
        }

        if (!oldInfo.getExcludesPattern().equals(newInfo.getExcludesPattern())) {
            alertModifiedField("excludes pattern");
        }

        if (!oldInfo.getIncludes().equals(newInfo.getIncludes())) {
            alertModifiedField("includes");
        }

        if (!oldInfo.getIncludesPattern().equals(newInfo.getIncludesPattern())) {
            alertModifiedField("includes pattern");
        }

        if (!oldInfo.getRepoKeys().equals(newInfo.getRepoKeys())) {
            alertModifiedField("repositories");
        }
    }

    /**
     * Throws an AuthorizationException alerting an un-authorized change of configuration
     *
     * @param modifiedFieldName Name of modified field
     */
    private void alertModifiedField(String modifiedFieldName) {
        throw new AuthorizationException("User is not permitted to modify " + modifiedFieldName);
    }
}