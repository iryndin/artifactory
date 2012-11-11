/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettingsImpl;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.SecurityListener;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.api.security.ldap.LdapService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.config.ConfigurationException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.sso.HttpSsoSettings;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.security.interceptor.SecurityConfigurationChangesInterceptors;
import org.artifactory.security.jcr.JcrAclManager;
import org.artifactory.security.jcr.JcrUserGroupManager;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.update.security.SecurityInfoReader;
import org.artifactory.update.security.SecurityVersion;
import org.artifactory.util.EmailException;
import org.artifactory.util.PathMatcher;
import org.artifactory.util.SerializablePair;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.jcr.Session;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Reloadable(beanClass = InternalSecurityService.class,
        initAfter = {JcrService.class, UserGroupManager.class, InternalAclManager.class})
public class SecurityServiceImpl implements InternalSecurityService {
    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);

    private static final String DELETE_FOR_SECURITY_MARKER_FILENAME = ".deleteForSecurityMarker";

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

    @Autowired
    private CachedThreadPoolTaskExecutor executor;

    private InternalArtifactoryContext context;

    private TreeSet<SecurityListener> securityListeners = new TreeSet<SecurityListener>();

    @Autowired
    private void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (InternalArtifactoryContext) context;
    }

    @Override
    public void init() {
        // Check if we need to dump the current security config (.deleteForSecurityMarker doesn't exist)
        dumpCurrentSecurityConfig();

        //Locate and import external configuration file
        checkForExternalConfiguration();
        checkOcmFolders();
        CoreAddons coreAddon = addons.addonByType(CoreAddons.class);
        if (coreAddon.isCreateDefaultAdminAccountAllowed() && !adminUserExists()) {
            createDefaultAdminUser();
        }
        createDefaultAnonymousUser();
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        clearSecurityListeners();
    }

    @Override
    public void destroy() {
    }

    @Override
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

    private void dumpCurrentSecurityConfig() {
        File deleteForConsistencyFix = getSecurityDumpMarkerFile();
        if (deleteForConsistencyFix.exists()) {
            return;
        }

        File etcDir = ArtifactoryHome.get().getEtcDir();
        ExportSettingsImpl exportSettings = new ExportSettingsImpl(etcDir);

        DateFormat formatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
        String timestamp = formatter.format(exportSettings.getTime());
        String fileName = "security." + timestamp + ".xml";
        exportSecurityInfo(exportSettings, fileName);
        log.debug("Successfully dumped '{}' configuration file to '{}'", fileName, etcDir.getAbsolutePath());

        createSecurityDumpMarkerFile();
    }

    /**
     * Creates/recreates the file that enabled security descriptor dump.
     * Also checks we have proper write access to the data folder.
     *
     * @return true if the deleteForSecurityMarker file did not exist and was created
     */
    private void createSecurityDumpMarkerFile() {
        File securityDumpMarkerFile = getSecurityDumpMarkerFile();
        try {
            securityDumpMarkerFile.createNewFile();
        } catch (IOException e) {
            log.debug("Could not create file: '" + securityDumpMarkerFile.getAbsolutePath() + "'.", e);
        }
    }

    private File getSecurityDumpMarkerFile() {
        return new File(ArtifactoryHome.get().getDataDir(), DELETE_FOR_SECURITY_MARKER_FILENAME);
    }

    /**
     * Checks for an externally supplied configuration file ($ARTIFACTORY_HOME/etc/security.xml). If such a file is
     * found, it will be deserialized to a security info (descriptor) object and imported to the system. This option is
     * to be used in cases like when an administrator is locked out of the system, etc'.
     */
    private void checkForExternalConfiguration() {
        ArtifactoryContext ctx = ContextHelper.get();
        final File etcDir = ctx.getArtifactoryHome().getEtcDir();
        final File configurationFile = new File(etcDir, "security.import.xml");
        //Work around Jackrabbit state visibility issues within the same tx by forking a separate tx (RTFACT-4526)
        Callable callable = new Callable() {
            @Override
            public Object call() throws Exception {

                String configAbsolutePath = configurationFile.getAbsolutePath();
                if (configurationFile.isFile()) {
                    if (!configurationFile.canRead() || !configurationFile.canWrite()) {
                        throw new ConfigurationException(
                                "Insufficient permissions. Security configuration import requires " +
                                        "both read and write permissions for " + configAbsolutePath);
                    }
                    try {
                        SecurityInfo descriptorToSave = new SecurityInfoReader().read(configurationFile);
                        //InternalSecurityService txMe = ctx.beanForType(InternalSecurityService.class);
                        getAdvisedMe().importSecurityData(descriptorToSave);
                        org.artifactory.util.FileUtils
                                .switchFiles(configurationFile, new File(etcDir, "security.bootstrap.xml"));
                        log.info("Security configuration imported successfully from " + configAbsolutePath + ".");
                    } catch (Exception e) {
                        throw new IllegalArgumentException("An error has occurred while deserializing the file " +
                                configAbsolutePath +
                                ". Please assure it's validity or remove it from the 'etc' folder.", e);
                    }
                }
                return null;
            }
        };
        Future<Set<String>> future = executor.submit(callable);
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Could not import external security config.", e);
        }
    }

    @Override
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

    @Override
    public boolean isAnonymous() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return authentication != null && UserInfo.ANONYMOUS.equals(authentication.getName());
    }

    @Override
    public boolean isAnonAccessEnabled() {
        SecurityDescriptor security = centralConfig.getDescriptor().getSecurity();
        return security != null && security.isAnonAccessEnabled();
    }

    @Override
    public boolean isAuthenticated() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return isAuthenticated(authentication);
    }

    @Override
    public boolean isAdmin() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return isAdmin(authentication);
    }

    @Override
    public AclInfo createAcl(MutableAclInfo aclInfo) {
        assertAdmin();
        if (StringUtils.isEmpty(aclInfo.getPermissionTarget().getName())) {
            throw new IllegalArgumentException("ACL name cannot be null");
        }
        cleanupAclInfo(aclInfo);
        AclInfo createdAcl = internalAclManager.createAcl(aclInfo).getDescriptor();
        interceptors.onPermissionsAdd();
        return createdAcl;
    }

    @Override
    public void updateAcl(MutableAclInfo acl) {
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

    @Override
    public void deleteAcl(PermissionTargetInfo target) {
        internalAclManager.deleteAcl(new PermissionTarget(target));
        interceptors.onPermissionsDelete();
    }

    @Override
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

    @Override
    public boolean isUpdatableProfile() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && simpleUser.isUpdatableProfile();
    }

    @Override
    public boolean isTransientUser() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && simpleUser.isTransientUser();
    }

    @Override
    @Nonnull
    public String currentUsername() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        //Do not return a null username or this will cause a jcr constraint violation
        return (authentication != null ? authentication.getName() : SecurityService.USER_SYSTEM);
    }

    @Override
    public UserInfo currentUser() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (authentication == null) {
            return null;
        }
        SimpleUser user = getSimpleUser(authentication);
        return user.getDescriptor();
    }

    @Override
    public UserInfo findUser(String username) {
        return userGroupManager.loadUserByUsername(username).getDescriptor();
    }

    @Override
    public AclInfo getAcl(PermissionTargetInfo permissionTarget) {
        Acl acl = internalAclManager.findAclById(new PermissionTarget(permissionTarget));
        if (acl != null) {
            return acl.getDescriptor();
        }
        return null;
    }

    @Override
    public boolean permissionTargetExists(String key) {
        return internalAclManager.permissionTargetExists(key);
    }

    private void cleanupAclInfo(MutableAclInfo acl) {
        Iterator<MutableAceInfo> it = acl.getMutableAces().iterator();
        while (it.hasNext()) {
            AceInfo aceInfo = it.next();
            if (aceInfo.getMask() == 0) {
                it.remove();
            }
        }
    }

    @Override
    public List<AclInfo> getAllAcls() {
        Collection<Acl> acls = internalAclManager.getAllAcls();
        List<AclInfo> descriptors = new ArrayList<AclInfo>(acls.size());
        for (Acl acl : acls) {
            descriptors.add(acl.getDescriptor());
        }
        return descriptors;
    }

    @Override
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

    @Override
    public boolean createUser(MutableUserInfo user) {
        user.setUsername(user.getUsername().toLowerCase());
        boolean userCreated = userGroupManager.createUser(new SimpleUser(user));
        if (userCreated) {
            interceptors.onUserAdd(user.getUsername());
        }
        return userCreated;
    }

    @Override
    public void updateUser(MutableUserInfo user) {
        user.setUsername(user.getUsername().toLowerCase());
        userGroupManager.updateUser(new SimpleUser(user));
        for (SecurityListener listener : securityListeners) {
            listener.onUserUpdate(user.getUsername());
        }
    }

    @Override
    public void deleteUser(String username) {
        internalAclManager.removeAllUserAces(username);
        userGroupManager.removeUser(username);
        interceptors.onUserDelete(username);
        for (SecurityListener listener : securityListeners) {
            listener.onUserDelete(username);
        }
    }

    @Override
    public void updateGroup(MutableGroupInfo groupInfo) {
        userGroupManager.updateGroup(new Group(groupInfo));
    }

    @Override
    public boolean createGroup(MutableGroupInfo groupInfo) {
        boolean groupCreated = userGroupManager.createGroup(new Group(groupInfo));
        if (groupCreated) {
            interceptors.onGroupAdd(groupInfo.getGroupName());
        }
        return groupCreated;
    }

    @Override
    public void deleteGroup(String groupName) {
        List<UserInfo> userInGroup = findUsersInGroup(groupName);
        for (UserInfo userInfo : userInGroup) {
            removeUserFromGroup(userInfo.getUsername(), groupName);
        }
        userGroupManager.removeGroup(groupName);
        interceptors.onGroupDelete(groupName);
    }

    @Override
    public List<GroupInfo> getAllGroups() {
        Collection<Group> groups = userGroupManager.getAllGroups();
        List<GroupInfo> groupsInfo = new ArrayList<GroupInfo>(groups.size());
        for (Group group : groups) {
            groupsInfo.add(group.getInfo());
        }
        return groupsInfo;
    }

    @Override
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

    @Override
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

    @Override
    public List<GroupInfo> getInternalGroups() {
        List<GroupInfo> allGroups = getAllGroups();
        allGroups.removeAll(getAllExternalGroups());
        return allGroups;
    }

    @Override
    public Set<String> getNewUserDefaultGroupsNames() {
        Set<GroupInfo> defaultGroups = getNewUserDefaultGroups();
        Set<String> defaultGroupsNames = new HashSet<String>(defaultGroups.size());
        for (GroupInfo group : defaultGroups) {
            defaultGroupsNames.add(group.getGroupName());
        }
        return defaultGroupsNames;
    }

    @Override
    public void addUsersToGroup(String groupName, List<String> usernames) {
        for (String username : usernames) {
            addUserToGroup(username, groupName);
        }
        interceptors.onAddUsersToGroup(groupName, usernames);
    }

    private void addUserToGroup(String username, String groupName) {
        SimpleUser user = userGroupManager.loadUserByUsername(username);
        Group group = userGroupManager.findGroup(groupName);
        if (group.isExternal()) {
            throw new IllegalArgumentException("Cannot assign external  group " + groupName + "to an internal user");
        }
        MutableUserInfo userInfo = InfoFactoryHolder.get().copyUser(user.getDescriptor());
        if (!userInfo.isInGroup(groupName)) {
            // don't update if already in group
            userInfo.addGroup(groupName);
            updateUser(userInfo);
        }
    }

    @Override
    public void removeUsersFromGroup(String groupName, List<String> usernames) {
        for (String username : usernames) {
            removeUserFromGroup(username, groupName);
        }
        interceptors.onRemoveUsersFromGroup(groupName, usernames);
    }

    private void removeUserFromGroup(String username, String groupName) {
        SimpleUser user = userGroupManager.loadUserByUsername(username);
        MutableUserInfo userInfo = InfoFactoryHolder.get().copyUser(user.getDescriptor());
        if (userInfo.isInGroup(groupName)) {
            // update only if user is in the group
            userInfo.removeGroup(groupName);
            updateUser(userInfo);
        }
    }

    @Override
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

    @Override
    public String resetPassword(String userName, String remoteAddress, String resetPageUrl) {
        UserInfo userInfo = null;
        try {
            userInfo = findUser(userName);
        } catch (UsernameNotFoundException e) {
            //Alert in the log when trying to reset a password of an unknown user
            log.warn("An attempt has been made to reset a password of unknown user: {}", userName);
        }

        //If the user is found, and has an email address
        if (userInfo != null && !StringUtils.isEmpty(userInfo.getEmail())) {

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

    @Override
    public UserInfo findOrCreateExternalAuthUser(String userName, boolean transientUser) {
        UserInfo userInfo;
        try {
            userInfo = findUser(userName.toLowerCase());
        } catch (UsernameNotFoundException e) {
            log.debug("Creating new external user '{}'", userName);
            UserInfoBuilder userInfoBuilder = new UserInfoBuilder(userName.toLowerCase()).updatableProfile(false);
            userInfoBuilder.internalGroups(getNewUserDefaultGroupsNames());
            if (transientUser) {
                userInfoBuilder.transientUser();
            }
            userInfo = userInfoBuilder.build();

            // Save non transient user
            if (!transientUser) {
                boolean success = userGroupManager.createUser(new SimpleUser(userInfo));
                if (!success) {
                    log.error("User '{}' was not created!", userInfo);
                }
            }
        }
        return userInfo;
    }

    @Override
    public GroupInfo findGroup(String groupName) {
        return userGroupManager.findGroup(groupName).getInfo();
    }

    /**
     * Generates a password recovery key for the specified user and send it by mail
     *
     * @param username      User to rest his password
     * @param remoteAddress The IP of the client that sent the request
     * @param resetPageUrl  The URL to the password reset page
     * @throws Exception
     */
    @Override
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
            byte[] encodedKey = Base64.encodeBase64URLSafe(passwordKey.getBytes());
            String encodedKeyString = new String(encodedKey);

            MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
            mutableUser.setGenPasswordKey(encodedKeyString);
            updateUser(mutableUser);

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

    @Override
    public SerializablePair<Date, String> getPasswordResetKeyInfo(String username) {
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

        return new SerializablePair<Date, String>(date, ip);
    }

    @Override
    public SerializablePair<String, Long> getUserLastLoginInfo(String username) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user (might be transient user)
            log.trace("Could not retrieve last login info for username '{}'.", username);
            return null;
        }

        SerializablePair<String, Long> pair = null;
        String lastLoginClientIp = userInfo.getLastLoginClientIp();
        long lastLoginTimeMillis = userInfo.getLastLoginTimeMillis();
        if (!StringUtils.isEmpty(lastLoginClientIp) && (lastLoginTimeMillis != 0)) {
            pair = new SerializablePair<String, Long>(lastLoginClientIp, lastLoginTimeMillis);
        }
        return pair;
    }

    @Override
    public void updateUserLastLogin(String username, String clientIp, long loginTimeMillis) {
        long lastLoginBufferTimeSecs = ConstantValues.userLastAccessUpdatesResolutionSecs.getLong();
        if (lastLoginBufferTimeSecs < 1) {
            log.debug("Skipping the update of the last login time for the user '{}': tracking is disabled.", username);
            return;
        }
        long lastLoginBufferTimeMillis = TimeUnit.SECONDS.toMillis(lastLoginBufferTimeSecs);
        /**
         * Avoid throwing a UsernameNotFoundException by checking if the user exists, since we are in an
         * async-transactional method, and any unchecked exception thrown will fire a rollback and an ugly exception
         * stacktrace print
         */
        if (!userGroupManager.userExists(username)) {
            // user not found (might be a transient user)
            log.trace("Could not update non-exiting username: {}'.", username);
            return;
        }
        UserInfo userInfo = findUser(username);
        long timeSinceLastLogin = loginTimeMillis - userInfo.getLastLoginTimeMillis();
        if (timeSinceLastLogin < lastLoginBufferTimeMillis) {
            log.debug("Skipping the update of the last login time for the user '{}': " +
                    "was updated less than {} seconds ago.", username, lastLoginBufferTimeSecs);
            return;
        }
        MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
        mutableUser.setLastLoginTimeMillis(loginTimeMillis);
        mutableUser.setLastLoginClientIp(clientIp);
        updateUser(mutableUser);
    }

    @Override
    public SerializablePair<String, Long> getUserLastAccessInfo(String username) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user
            throw new IllegalArgumentException("Could not find specified username.", e);
        }

        SerializablePair<String, Long> pair = null;
        String lastAccessClientIp = userInfo.getLastAccessClientIp();
        long lastAccessTimeMillis = userInfo.getLastAccessTimeMillis();
        if (!StringUtils.isEmpty(lastAccessClientIp) && (lastAccessTimeMillis != 0)) {
            pair = new SerializablePair<String, Long>(lastAccessClientIp, lastAccessTimeMillis);
        }
        return pair;
    }

    @Override
    public void updateUserLastAccess(String username, String clientIp, long accessTimeMillis,
            long accessUpdatesResolutionMillis) {
        UserInfo userInfo;
        try {
            userInfo = findUser(username);
        } catch (UsernameNotFoundException e) {
            //If can't find user
            throw new IllegalArgumentException("Could not find specified username.", e);
        }
        //Check if we should update
        long existingLastAccess = userInfo.getLastAccessTimeMillis();
        if (existingLastAccess <= 0 || existingLastAccess + accessUpdatesResolutionMillis < accessTimeMillis) {
            MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
            mutableUser.setLastAccessTimeMillis(accessTimeMillis);
            mutableUser.setLastAccessClientIp(clientIp);
            updateUser(mutableUser);
        }
    }

    @Override
    public boolean isHttpSsoProxied() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        return httpSsoSettings != null && httpSsoSettings.isHttpSsoProxied();
    }

    @Override
    public boolean isNoHttpSsoAutoUserCreation() {
        HttpSsoSettings httpSsoSettings = centralConfig.getDescriptor().getSecurity().getHttpSsoSettings();
        return httpSsoSettings != null && httpSsoSettings.isNoAutoUserCreation();
    }

    @Override
    public String getHttpSsoRemoteUserRequestVariable() {
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

    @Override
    public boolean hasPermission(ArtifactoryPermission artifactoryPermission) {
        return isAdmin() || !getPermissionTargets(artifactoryPermission).isEmpty();
    }

    @Override
    public boolean canRead(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.READ);
    }

    @Override
    public boolean canImplicitlyReadParentPath(RepoPath repoPath) {
        return hasPermission(addSlashToRepoPath(repoPath), ArtifactoryPermission.READ);
    }

    public static RepoPath addSlashToRepoPath(final RepoPath repoPath) {
        return new RepoPath() {
            @Override
            public String getPath() {
                return repoPath.getPath() + "/";
            }

            @Override
            public String getRepoKey() {
                return repoPath.getRepoKey();
            }

            @Override
            public String getId() {
                return repoPath.getId();
            }

            @Override
            public String getName() {
                return repoPath.getName();
            }

            @Override
            public RepoPath getParent() {
                return repoPath.getParent();
            }

            @Override
            public boolean isRoot() {
                return repoPath.isRoot();
            }
        };
    }

    @Override
    public boolean canAnnotate(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.ANNOTATE);
    }

    @Override
    public boolean canDeploy(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.DEPLOY);
    }

    @Override
    public boolean canDelete(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.DELETE);
    }

    @Override
    public boolean canAdmin(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.ADMIN);
    }

    @Override
    public boolean canAdmin(PermissionTargetInfo target) {
        Permission adminPermission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), adminPermission);
    }

    @Override
    public boolean canRead(UserInfo user, PermissionTargetInfo target) {
        Permission readPermission = permissionFor(ArtifactoryPermission.READ);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), readPermission, new SimpleUser(user));
    }

    @Override
    public boolean canAnnotate(UserInfo user, PermissionTargetInfo target) {
        Permission annotatePermission = permissionFor(ArtifactoryPermission.ANNOTATE);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), annotatePermission, new SimpleUser(user));
    }

    @Override
    public boolean canDeploy(UserInfo user, PermissionTargetInfo target) {
        Permission deployPermission = permissionFor(ArtifactoryPermission.DEPLOY);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), deployPermission, new SimpleUser(user));
    }

    @Override
    public boolean canDelete(UserInfo user, PermissionTargetInfo target) {
        Permission deletePermission = permissionFor(ArtifactoryPermission.DELETE);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), deletePermission, new SimpleUser(user));
    }

    @Override
    public boolean canAdmin(UserInfo user, PermissionTargetInfo target) {
        Permission adminPermission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermissionOnPermissionTarget(new PermissionTarget(target), adminPermission, new SimpleUser(user));
    }

    @Override
    public boolean canRead(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.READ);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    @Override
    public boolean canAnnotate(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ANNOTATE);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    @Override
    public boolean canDelete(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DELETE);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    @Override
    public boolean canDeploy(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DEPLOY);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    @Override
    public boolean canAdmin(UserInfo user, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermission(new SimpleUser(user), path, permission);
    }

    @Override
    public boolean canRead(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.READ);
        return hasPermission(group, path, permission);
    }

    @Override
    public boolean canAnnotate(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ANNOTATE);
        return hasPermission(group, path, permission);
    }

    @Override
    public boolean canDelete(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DELETE);
        return hasPermission(group, path, permission);
    }

    @Override
    public boolean canDeploy(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.DEPLOY);
        return hasPermission(group, path, permission);
    }

    @Override
    public boolean canAdmin(GroupInfo group, RepoPath path) {
        Permission permission = permissionFor(ArtifactoryPermission.ADMIN);
        return hasPermission(group, path, permission);
    }

    @Override
    public boolean userHasPermissions(String username) {
        SimpleUser user = new SimpleUser(findUser(username.toLowerCase()));
        for (ArtifactoryPermission permission : ArtifactoryPermission.values()) {
            if (hasPermission(permission, user)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean userHasPermissionsOnRepositoryRoot(String repoKey) {
        Repo repo = repositoryService.repositoryByKey(repoKey);
        // If it is a real (i.e local or cached simply check permission on root.
        if (repo.isReal()) {
            // If repository is real, check if the user has any permission on the root.
            return hasPermissionOnRoot(repoKey);
        } else {
            // If repository is virtual go over all repository associated with it and check if user has permissions
            // on it root.
            VirtualRepo virtualRepo = repositoryService.virtualRepositoryByKey(repoKey);
            // Go over all resolved cached repos, i.e. if we have virtual repository aggregation,
            // This will give the resolved cached repos.
            Set<LocalCacheRepo> localCacheRepoList = virtualRepo.getResolvedLocalCachedRepos();
            for (LocalCacheRepo localCacheRepo : localCacheRepoList) {
                LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(localCacheRepo.getKey());
                if (localRepo != null) {
                    if (hasPermissionOnRoot(localRepo.getKey())) {
                        return true;
                    }
                }
            }
            // Go over all resolved local repositories, will bring me the resolved local repos from aggregation.
            Set<LocalRepo> repoList = virtualRepo.getResolvedLocalRepos();
            for (LocalRepo localCacheRepo : repoList) {
                LocalRepo localRepo = repositoryService.localOrCachedRepositoryByKey(localCacheRepo.getKey());
                if (localRepo != null) {
                    if (hasPermissionOnRoot(localRepo.getKey())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isDisableInternalPassword() {
        UserInfo simpleUser = currentUser();
        return simpleUser != null && MutableUserInfo.INVALID_PASSWORD.equals(simpleUser.getPassword());
    }

    private boolean hasPermissionOnRoot(String repoKey) {
        RepoPath path = InternalRepoPathFactory.repoRootPath(repoKey);
        for (ArtifactoryPermission permission : ArtifactoryPermission.values()) {
            Permission artifactoryPermission = permissionFor(permission);
            if (hasPermission(path, artifactoryPermission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(RepoPath repoPath, ArtifactoryPermission permission) {
        return hasPermission(repoPath, permissionFor(permission));
    }

    private boolean hasPermission(RepoPath repoPath, Permission permission) {
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
        return isGranted(repoPath, permission, sids);
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
        List<Acl> acls = internalAclManager.getAllAcls(sids);
        for (Acl acl : acls) {
            String repoKey = repoPath.getRepoKey();
            String path = repoPath.getPath();
            PermissionTarget aclPermissionTarget = acl.getPermissionTarget();
            if (isPermissionTargetIncludesRepoKey(repoKey, aclPermissionTarget)) {
                boolean checkPartialPath = (permission.getMask() & (ArtifactoryPermission.READ.getMask() | ArtifactoryPermission.DEPLOY.getMask())) != 0;
                boolean match = matches(aclPermissionTarget, path, checkPartialPath);
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

    @Override
    public void exportTo(ExportSettings settings) {
        exportSecurityInfo(settings, FILE_NAME);
    }

    private void exportSecurityInfo(ExportSettings settings, String fileName) {
        //Export the security settings as xml using xstream
        SecurityInfo descriptor = getSecurityData();
        String path = settings.getBaseDir() + "/" + fileName;
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

    @Override
    public SecurityInfo getSecurityData() {
        List<UserInfo> users = getAllUsers(true);
        List<GroupInfo> groups = getAllGroups();
        List<AclInfo> acls = getAllAcls();
        SecurityInfo descriptor = InfoFactoryHolder.get().createSecurityInfo(users, groups, acls);
        descriptor.setVersion(SecurityVersion.getCurrent().name());
        return descriptor;
    }

    @Override
    public void importFrom(ImportSettings settings) {
        MutableStatusHolder status = settings.getStatusHolder();
        status.setStatus("Importing security...", log);
        importSecurityXml(settings, status);
    }

    private void importSecurityXml(ImportSettings settings, MutableStatusHolder status) {
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
        builder.password(DigestUtils.md5Hex("")).email(null).updatableProfile(false);
        MutableUserInfo anonUser = builder.build();
        boolean createdAnonymousUser = createUser(anonUser);

        if (createdAnonymousUser) {
            MutableGroupInfo readersGroup = InfoFactoryHolder.get().createGroup("readers");
            readersGroup.setDescription("A group for read-only users");
            readersGroup.setNewUserDefault(true);
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
        Set<UserGroupInfo> groups = user.getDescriptor().getGroups();
        if (!groups.isEmpty()) {
            sids = new ArtifactorySid[groups.size() + 1];
            // add the current user
            sids[0] = new ArtifactorySid(user.getUsername());

            // add all the groups the user is a member of
            int sidsArrayIndex = 1;
            for (UserGroupInfo group : groups) {
                sids[sidsArrayIndex] = new ArtifactorySid(group.getGroupName(), true);
                sidsArrayIndex++;
            }
        } else {
            sids = new ArtifactorySid[]{new ArtifactorySid(user.getUsername())};
        }

        return sids;
    }

    @Override
    public void importSecurityData(String securityXml) {
        importSecurityData(new SecurityInfoReader().read(securityXml));
    }

    @Override
    public void importSecurityData(SecurityInfo securityInfo) {
        interceptors.onBeforeSecurityImport(securityInfo);
        clearSecurityData();
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

    private void clearSecurityData() {
        //Respect order for clean removal
        //Clean up all acls
        internalAclManager.deleteAllAcls();
        //Remove all existing groups
        userGroupManager.deleteAllGroupsAndUsers();
        clearSecurityListeners();
    }

    @Override
    public void addListener(SecurityListener listener) {
        securityListeners.add(listener);
    }

    @Override
    public void removeListener(SecurityListener listener) {
        securityListeners.remove(listener);
    }

    @Override
    public void authenticateAsSystem() {
        SecurityContextHolder.getContext().setAuthentication(new SystemAuthenticationToken());
    }

    @Override
    public void nullifyContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public MultiStatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
        return ldapService.testLdapConnection(ldapSetting, username, password);
    }

    @Override
    public boolean isPasswordEncryptionEnabled() {
        CentralConfigDescriptor cc = centralConfig.getDescriptor();
        return cc.getSecurity().getPasswordSettings().isEncryptionEnabled();
    }

    @Override
    public boolean userPasswordMatches(String passwordToCheck) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return authentication != null && passwordToCheck.equals(authentication.getCredentials());
    }

    @Override
    public boolean canDeployToLocalRepository() {
        return !repositoryService.getDeployableRepoDescriptors().isEmpty();
    }

    private void clearSecurityListeners() {
        //Notify security listeners
        for (SecurityListener listener : securityListeners) {
            listener.onClearSecurity();
        }
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

    private static boolean matches(PermissionTarget aclPermissionTarget, String path, boolean matchStart) {
        return PathMatcher.matches(path, aclPermissionTarget.getIncludes(), aclPermissionTarget.getExcludes(),
                matchStart);
    }

    private static XStream getXstream() {
        return InfoFactoryHolder.get().getSecurityXStream();
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

    /**
     * Retrieves the Async advised instance of the service
     *
     * @return InternalSecurityService - Async advised instance
     */
    private InternalSecurityService getAdvisedMe() {
        return context.beanForType(InternalSecurityService.class);
    }
}
