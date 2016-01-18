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

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.CoreAddons;
import org.artifactory.addon.HaAddon;
import org.artifactory.addon.ha.message.HaMessageTopic;
import org.artifactory.addon.sso.HttpSsoAddon;
import org.artifactory.addon.sso.saml.SamlSsoAddon;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettingsImpl;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.security.AuthorizationException;
import org.artifactory.api.security.PasswordExpiryUser;
import org.artifactory.api.security.SecurityListener;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.api.security.ldap.LdapService;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.Info;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.config.ConfigurationException;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.security.PasswordExpirationPolicy;
import org.artifactory.descriptor.security.SecurityDescriptor;
import org.artifactory.descriptor.security.UserLockPolicy;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.descriptor.security.oauth.OAuthSettings;
import org.artifactory.descriptor.security.sso.HttpSsoSettings;
import org.artifactory.descriptor.security.sso.SamlSettings;
import org.artifactory.exception.InvalidNameException;
import org.artifactory.exception.ValidationException;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.model.xstream.security.ImmutableAclInfo;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RemoteRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.repo.trash.TrashService;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.sapi.security.SecurityConstants;
import org.artifactory.schedule.CachedThreadPoolTaskExecutor;
import org.artifactory.schedule.TaskBase;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.security.exceptions.LoginDisabledException;
import org.artifactory.security.exceptions.PasswordChangeException;
import org.artifactory.security.exceptions.PasswordExpireException;
import org.artifactory.security.exceptions.UserCredentialsExpiredException;
import org.artifactory.security.exceptions.UserLockedException;
import org.artifactory.security.interceptor.SecurityConfigurationChangesInterceptors;
import org.artifactory.security.jobs.CredentialsWatchJob;
import org.artifactory.security.jobs.PasswordExpireNotificationJob;
import org.artifactory.security.props.auth.PropsAuthenticationToken;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.storage.StorageException;
import org.artifactory.storage.db.DbService;
import org.artifactory.storage.db.security.service.UserGroupServiceImpl;
import org.artifactory.storage.security.service.AclStoreService;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.update.security.SecurityInfoReader;
import org.artifactory.update.security.SecurityVersion;
import org.artifactory.util.EmailException;
import org.artifactory.util.Files;
import org.artifactory.util.NameValidator;
import org.artifactory.util.PathMatcher;
import org.artifactory.util.SerializablePair;
import org.artifactory.util.Strings;
import org.artifactory.version.CompoundVersionDetails;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.SecretKey;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Reloadable(beanClass = InternalSecurityService.class, initAfter = {DbService.class})
public class SecurityServiceImpl implements InternalSecurityService {
    private static final Logger log = LoggerFactory.getLogger(SecurityServiceImpl.class);

    private static final String DELETE_FOR_SECURITY_MARKER_FILENAME = ".deleteForSecurityMarker";

    // cache meaning  <userName, incorrect-login-timestampts>
    private final Cache<String, List<Long>> unknownUsersCache = CacheBuilder.newBuilder().maximumSize(UserGroupServiceImpl.MAX_USERS_TO_TRACK).
            expireAfterWrite(1, TimeUnit.HOURS).build();

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AclStoreService aclStoreService;

    @Autowired
    private UserGroupStoreService userGroupStoreService;

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

    @Autowired
    private CentralConfigService configService;

    @Autowired
    private TaskService taskService;

    private InternalArtifactoryContext context;

    private TreeSet<SecurityListener> securityListeners = new TreeSet<>();

    /**
     * @param user The authentication token.
     * @return An array of sids of the current user and all it's groups.
     */
    private static Set<ArtifactorySid> getUserEffectiveSids(SimpleUser user) {
        Set<ArtifactorySid> sids = new HashSet<>(2);
        Set<UserGroupInfo> groups = user.getDescriptor().getGroups();
        // add the current user
        sids.add(new ArtifactorySid(user.getUsername(), false));
        // add all the groups the user is a member of
        for (UserGroupInfo group : groups) {
            sids.add(new ArtifactorySid(group.getGroupName(), true));
        }
        return sids;
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

    private static boolean matches(PermissionTargetInfo aclPermissionTarget, String path, boolean folder) {
        return PathMatcher.matches(path, aclPermissionTarget.getIncludes(), aclPermissionTarget.getExcludes(), folder);
    }

    private static XStream getXstream() {
        return InfoFactoryHolder.get().getSecurityXStream();
    }

    @Autowired
    private void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (InternalArtifactoryContext) context;
    }

    @Override
    public void init() {
        // if we need to dump the current security config (.deleteForSecurityMarker doesn't exist)
        // and unlock all admin users
        if (shouldRestoreLoginCapabilities()) {
            dumpCurrentSecurityConfig();
            unlockAdminUsers();
        }

        //Locate and import external configuration file
        checkForExternalConfiguration();
        CoreAddons coreAddon = addons.addonByType(CoreAddons.class);
        if (coreAddon.isCreateDefaultAdminAccountAllowed() && !userGroupStoreService.adminUserExists()) {
            createDefaultAdminUser();
        }
        createDefaultAnonymousUser();

        // start CredentialsWatchJob
        TaskBase credentialsWatchJob = TaskUtils.createRepeatingTask(CredentialsWatchJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.passwordExpireJobIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(30l));
        taskService.startTask(credentialsWatchJob, false);

        // start PasswordExpireNotificationJob
        TaskBase passwordExpireNotificationJob = TaskUtils.createRepeatingTask(PasswordExpireNotificationJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.passwordExpireNotificationJobIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(60l));
        taskService.startTask(passwordExpireNotificationJob, false);
    }

    @Override
    public void reload(CentralConfigDescriptor oldDescriptor) {
        // Need to check if security conf changed then clear security caches
        if (!centralConfig.getDescriptor().getSecurity().equals(oldDescriptor.getSecurity())) {
            clearSecurityListeners();
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {

    }

    private void dumpCurrentSecurityConfig() {

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
     * @return true if SecurityDumpMarkerFile is unavailable
     *         otherwise false
     */
    private boolean shouldRestoreLoginCapabilities() {
        File deleteForConsistencyFix = getSecurityDumpMarkerFile();
        if (deleteForConsistencyFix.exists())
            return false;
        return true;
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
                                        "both read and write permissions for " + configAbsolutePath
                        );
                    }
                    try {
                        SecurityInfo descriptorToSave = new SecurityInfoReader().read(configurationFile);
                        //InternalSecurityService txMe = ctx.beanForType(InternalSecurityService.class);
                        getAdvisedMe().importSecurityData(descriptorToSave);
                        Files
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
        @SuppressWarnings("unchecked") Future<Set<String>> future = executor.submit(callable);
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException("Could not import external security config.", e);
        }
    }

    @Override
    public boolean isAnonymous() {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        return authentication != null && isAnonymousUser(authentication.getName());
    }

    @Override
    public boolean requireProfileUnlock() {
        SecurityDescriptor security = ContextHelper.get().getCentralConfig().getDescriptor().getSecurity();
        HttpSsoSettings httpSsoSettings = security.getHttpSsoSettings();
        boolean allowUserToAccessProfileSso = httpSsoSettings == null ? false : httpSsoSettings.isAllowUserToAccessProfile();
        SamlSettings samlSettings = security.getSamlSettings();
        boolean allowUserToAccessProfileSaml = samlSettings == null ? false : samlSettings.isAllowUserToAccessProfile();
        OAuthSettings oauthSettings = security.getOauthSettings();
        boolean allowUserToAccessProfileOauth = oauthSettings == null ? false : oauthSettings.isAllowUserToAccessProfile();
        Authentication authentication = AuthenticationHelper.getAuthentication();
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SamlSsoAddon samlSsoAddon = addonsManager.addonByType(SamlSsoAddon.class);
        HttpSsoAddon httpSsoAddon = addonsManager.addonByType(HttpSsoAddon.class);
        if ((authentication instanceof PropsAuthenticationToken && allowUserToAccessProfileOauth) ||
                (samlSsoAddon.isSamlAuthentication() && allowUserToAccessProfileSaml) ||
                (httpSsoAddon.isHttpSsoAuthentication() && allowUserToAccessProfileSso)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean requireProfilePassword() {
        UserGroupService userGroupService = ContextHelper.get().beanForType(UserGroupService.class);
        UserInfo userInfo = userGroupService.findUser(currentUsername(), false);
        boolean userHasPassword = false;
        if (userInfo != null && userInfo.getPassword() != null) {
            userHasPassword = userInfo.getPassword().length() > 0;
        }
        SecurityDescriptor security = ContextHelper.get().getCentralConfig().getDescriptor().getSecurity();
        HttpSsoSettings httpSsoSettings = security.getHttpSsoSettings();
        boolean allowUserToAccessProfileSso = httpSsoSettings == null ? false : httpSsoSettings.isAllowUserToAccessProfile();
        SamlSettings samlSettings = security.getSamlSettings();
        boolean allowUserToAccessProfileSaml = samlSettings == null ? false : samlSettings.isAllowUserToAccessProfile();
        OAuthSettings oauthSettings = security.getOauthSettings();
        boolean allowUserToAccessProfileOauth = oauthSettings == null ? false : oauthSettings.isAllowUserToAccessProfile();
        Authentication authentication = AuthenticationHelper.getAuthentication();
        AddonsManager addonsManager = ContextHelper.get().beanForType(AddonsManager.class);
        SamlSsoAddon samlSsoAddon = addonsManager.addonByType(SamlSsoAddon.class);
        HttpSsoAddon httpSsoAddon = addonsManager.addonByType(HttpSsoAddon.class);
        if (doNotRequirePasswordFromExtUser(userHasPassword, allowUserToAccessProfileSso,
                allowUserToAccessProfileSaml, allowUserToAccessProfileOauth,
                authentication, samlSsoAddon, httpSsoAddon)) {
            return false;
        }
        return true;
    }

    private boolean doNotRequirePasswordFromExtUser(boolean userHasPassword, boolean allowUserToAccessProfileSso, boolean allowUserToAccessProfileSaml, boolean allowUserToAccessProfileOauth, Authentication authentication, SamlSsoAddon samlSsoAddon, HttpSsoAddon httpSsoAddon) {
        return ((authentication instanceof PropsAuthenticationToken && !allowUserToAccessProfileOauth) ||
                (samlSsoAddon.isSamlAuthentication() && !allowUserToAccessProfileSaml) ||
                (httpSsoAddon.isHttpSsoAuthentication() && !allowUserToAccessProfileSso)) && !userHasPassword;
    }


    @Override
    public boolean isAnonAccessEnabled() {
        SecurityDescriptor security = centralConfig.getDescriptor().getSecurity();
        return security.isAnonAccessEnabled();
    }

    private boolean isAnonBuildInfoAccessDisabled() {
        SecurityDescriptor security = centralConfig.getDescriptor().getSecurity();
        return security.isAnonAccessToBuildInfosDisabled();
    }

    @Override
    public boolean isAnonUserAndAnonBuildInfoAccessDisabled() {
        return isAnonymous() && isAnonBuildInfoAccessDisabled();
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
    public void createAcl(MutableAclInfo aclInfo) {
        assertAdmin();
        if (StringUtils.isEmpty(aclInfo.getPermissionTarget().getName())) {
            throw new IllegalArgumentException("ACL name cannot be null");
        }

        MutableAclInfo compatibleAcl = makeNewAclRemoteRepoKeysAclCompatible(aclInfo);
        cleanupAclInfo(compatibleAcl);
        aclStoreService.createAcl(compatibleAcl);
        interceptors.onPermissionsAdd();
        addons.addonByType(HaAddon.class).notify(HaMessageTopic.ACL_CHANGE_TOPIC, null);
    }

    @Override
    public void updateAcl(MutableAclInfo acl) {
        //If the editing user is not a sys-admin
        if (!isAdmin()) {
            //Assert that no unauthorized modifications were performed
            validateUnmodifiedPermissionTarget(acl.getPermissionTarget());
        }

        MutableAclInfo compatibleAcl = makeNewAclRemoteRepoKeysAclCompatible(acl);

        // Removing empty Ace
        cleanupAclInfo(compatibleAcl);
        aclStoreService.updateAcl(compatibleAcl);
        interceptors.onPermissionsUpdate();
        addons.addonByType(HaAddon.class).notify(HaMessageTopic.ACL_CHANGE_TOPIC, null);
    }

    @Override
    public void deleteAcl(PermissionTargetInfo target) {
        aclStoreService.deleteAcl(target.getName());
        interceptors.onPermissionsDelete();
        addons.addonByType(HaAddon.class).notify(HaMessageTopic.ACL_CHANGE_TOPIC, null);
    }

    @Override
    public List<PermissionTargetInfo> getPermissionTargets(ArtifactoryPermission permission) {
        return getPermissionTargetsByPermission(permission);
    }

    private List<PermissionTargetInfo> getPermissionTargetsByPermission(ArtifactoryPermission permission) {
        List<PermissionTargetInfo> result = new ArrayList<>();
        Collection<AclInfo> allAcls = aclStoreService.getAllAcls();
        for (AclInfo acl : allAcls) {
            if (hasPermissionOnAcl(acl, permission)) {
                result.add(acl.getPermissionTarget());
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
        //Do not return a null username or this will cause a constraint violation
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
        return findUser(username, true);
    }

    /**
     * Returns the user details for the given username.
     *
     * @param username The unique username
     * @param errorOnAbsence throw error if user is not found
     * @return UserInfo if user with the input username exists
     */
    @Override
    public UserInfo findUser(String username, boolean errorOnAbsence) {
        UserInfo user = userGroupStoreService.findUser(username);
        if (errorOnAbsence && user == null) {
            throw new UsernameNotFoundException("User " + username + " does not exists!");
        }
        return user;
    }

    @Override
    public AclInfo getAcl(String permTargetName) {
        return aclStoreService.getAcl(permTargetName);
    }

    @Override
    public AclInfo getAcl(PermissionTargetInfo permissionTarget) {
        return aclStoreService.getAcl(permissionTarget.getName());
    }

    @Override
    public boolean permissionTargetExists(String key) {
        return aclStoreService.permissionTargetExists(key);
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
        return new ArrayList<>(aclStoreService.getAllAcls());
    }

    @Override
    public List<UserInfo> getAllUsers(boolean includeAdmins) {
        return userGroupStoreService.getAllUsers(includeAdmins);
    }

    @Override
    public Collection<Info> getUsersGroupsPaging(boolean includeAdmins, String orderBy,
            String startOffset, String limit, String direction) {
        return userGroupStoreService.getUsersGroupsPaging(includeAdmins, orderBy,
                startOffset, limit, direction);
    }

    @Override
    public long getAllUsersGroupsCount(boolean includeAdmins) {
        return userGroupStoreService.getAllUsersGroupsCount(includeAdmins);
    }

    @Override
    public org.artifactory.md.Properties findPropertiesForUser(String username) {
        return userGroupStoreService.findPropertiesForUser(username);
    }

    @Override
    public boolean addUserProperty(String username, String key, String value) {
        return userGroupStoreService.addUserProperty(username, key, value);
    }

    @Override
    public void deleteProperty(String userName, String propertyKey) {
        userGroupStoreService.deleteUserProperty(userName, propertyKey);
    }

    @Override
    public void deletePropertyFromAllUsers(String propertyKey) {
        userGroupStoreService.deletePropertyFromAllUsers(propertyKey);
    }

    @Override
    public String getPropsToken(String userName, String propsKey) {
        return userGroupStoreService.findUserProperty(userName, propsKey);
    }

    @Override
    public boolean revokePropsToken(String userName, String propsKey) throws SQLException {
        return userGroupStoreService.deleteUserProperty(userName, propsKey);
    }

    @Override
    public boolean createPropsToken(String userName, String propsKey, String propsValue) throws SQLException {
        boolean isPropsAddSucceeded = false;
        try {
            isPropsAddSucceeded = userGroupStoreService.addUserProperty(userName, propsKey, propsValue);

        } catch (Exception e) {
            log.debug("error adding {}:{} to db", propsKey, propsValue);
        }
        return isPropsAddSucceeded;
    }

    @Override
    public void revokeAllPropsTokens(String propsKey) throws SQLException {
        userGroupStoreService.deletePropertyFromAllUsers(propsKey);
    }

    @Override
    public boolean updatePropsToken(String userName, String propsKey, String propsValue) throws SQLException {
        boolean isUpdateSucceeded = userGroupStoreService.deleteUserProperty(userName, propsKey);
        if (isUpdateSucceeded) {
            isUpdateSucceeded = userGroupStoreService.addUserProperty(userName, propsKey, propsValue);
        }
        return isUpdateSucceeded;
    }

    /**
     * Locks user upon incorrect login attempt
     *
     * @param userName
     */
    @Nonnull
    @Override
    public void lockUser(String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Locking user {} due to incorrect login attempts", userName);
            userGroupStoreService.lockUser(userName);
        }
    }

    /**
     * Unlocks locked in user
     *
     * @param userName
     */
    @Nonnull
    @Override
    public void unlockUser(String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Unlocking user {}", userName);
            userGroupStoreService.unlockUser(userName);
            unknownUsersCache.invalidate(userName);
        }
    }

    /**
     * Unlocks all locked in users
     */
    @Nonnull
    @Override
    public void unlockAllUsers() {
        log.debug("Unlocking all users");
        userGroupStoreService.unlockAllUsers();
        unknownUsersCache.invalidateAll();
    }

    /**
     * Unlocks all locked out admin users
     */
    @Nonnull
    @Override
    public void unlockAdminUsers() {
        log.debug("Unlocking all admin users");
        userGroupStoreService.unlockAdminUsers();
    }

    /**
     * Registers incorrect login attempt
     *
     * @param userName
     */
    @Nonnull
    @Override
    public void registerIncorrectLoginAttempt(String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Registering incorrect login attempt for user {}", userName);
            userGroupStoreService.registerIncorrectLoginAttempt(userName);
        }
    }

    /**
     * @return List of locked in users
     */
    @Override
    public Set<String> getLockedUsers() {
        return userGroupStoreService.getLockedUsers();
    }

    /**
     * Triggered when user success to login
     *
     * @param userName user to intercept
     */
    @Override
    public void interceptLoginSuccess(String userName) {
        if (!isAnonymousUser(userName)) {
            log.debug("Resetting incorrect login attempt for user {}", userName);
            userGroupStoreService.resetIncorrectLoginAttempts(userName);
        }
    }

    /**
     * Resets incorrect login attempts
     *
     * @param userName
     */
    @Nonnull
    @Override
    public void resetIncorrectLoginAttempts(String userName) {
        log.debug("Resetting incorrect login attempts for '{}'", userName);
        userGroupStoreService.resetIncorrectLoginAttempts(userName);
    }

    /**
     * Triggered when user fails to login and
     * locks it if amount of login failures exceeds
     * {@see LockPolicy#loginAttempts}
     *
     * @param userName user to intercept
     * @param accessTime session creation time
     */
    @Override
    public void interceptLoginFailure(String userName, long accessTime){
        if (!isAnonymousUser(userName)) {
            log.debug("Registering login attempt failure for user {}", userName);
            registerIncorrectLoginAttempt(userName);
            UserInfo user = userGroupStoreService.findUser(userName);
            if (user == null) { // unknown user blocking
                log.trace("Memorizing {} (not a user) for blocking", userName);
                if (!userGroupStoreService.isUserLocked(userName)) {
                    List<Long> list = unknownUsersCache.getIfPresent(userName);
                    if(list ==null){
                        registerUnknownUser(userName);
                        // memorize incorrect login attempt
                        unknownUsersCache.put(userName,Lists.newArrayList(accessTime));
                    } else {
                        list.add(accessTime);
                        if (isUserLockPolicyEnabled() &&
                                list.size() >= getAllowedMaxLoginAttempts()) {
                            lockUser(userName);
                            unknownUsersCache.invalidate(userName); // no need to track this user as it got locked
                            return;
                        }
                    }
                }
            } else if(isUserLockPolicyEnabled() && !user.isLocked() && // known user blocking
                    userGroupStoreService.getIncorrectLoginAttempts(userName) >= getAllowedMaxLoginAttempts()) {
                lockUser(userName);
            }
        }
    }

    /**
     * Registers unknown user in cache
     *
     * @param userName
     */
    private void registerUnknownUser(String userName) {
        if (!isAnonymousUser(userName)) {
            log.trace("Registering incorrect login attempt for unknown user {}", userName);
            unknownUsersCache.put(userName, new ArrayList<>(getAllowedMaxLoginAttempts()));
        }
    }

    /**
     * @return whether {@link UserLockPolicy} is enabled
     */
    @Override
    public boolean isUserLockPolicyEnabled() {
        UserLockPolicy userLockPolicy =
                configService.getMutableDescriptor()
                        .getSecurity().getUserLockPolicy();

        return userLockPolicy.isEnabled();
    }

    /**
     * @return whether {@link PasswordExpirationPolicy} is enabled
     */
    @Override
    public boolean isPasswordExpirationPolicyEnabled() {
        if (configService.getMutableDescriptor().getSecurity() != null &&
                configService.getMutableDescriptor().getSecurity().getPasswordSettings() != null &&
                configService.getMutableDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy() != null) {

            return configService.getMutableDescriptor()
                    .getSecurity()
                    .getPasswordSettings()
                    .getExpirationPolicy()
                    .isEnabled();
        }
        return false;
    }

    /**
     * @return MaxLoginAttempts allowed before user gets locked out
     */
    private int getAllowedMaxLoginAttempts() {
        UserLockPolicy userLockPolicy =
                configService.getMutableDescriptor()
                        .getSecurity().getUserLockPolicy();

        return userLockPolicy.getLoginAttempts();
    }

    /**
     * @return Number of days for password to get expired
     */
    private int getPasswordExpirationDays() {
        PasswordExpirationPolicy passwordExpirationPolicy =
                configService.getMutableDescriptor()
                        .getSecurity().getPasswordSettings().getExpirationPolicy();
        return passwordExpirationPolicy.getPasswordMaxAge();
    }

    /**
     * Throws LockedException if user is locked
     *
     * @param userName
     * @throws UserLockedException
     */
    @Nonnull
    @Override
    public void ensureUserIsNotLocked(String userName) throws UserLockedException {
        log.debug("Checking if user {} is not locked", userName);
        if(!isAnonymousUser(userName) &&
                isUserLockPolicyEnabled() && isUserLocked(userName)) {
            log.debug("User {} is locked, denying login", userName);
            throw UserLockedException.userLocked(userName);
        }
    }

    /**
     * Throws CredentialsExpiredException if user's credentials have expired
     *
     * @param userName a user name
     *
     * @throws UserCredentialsExpiredException
     */
    @Nonnull
    @Override
    public void ensurePasswordIsNotExpired(String userName) throws UserCredentialsExpiredException {
        log.debug("Checking if user's {} credentials have expired", userName);
        if(!isAnonymousUser(userName) && isPasswordExpirationPolicyEnabled() &&
                ensurePasswordIsNotExpired(userName, getPasswordExpirationDays())) {
            throw UserCredentialsExpiredException.instance(userName);
        }
    }

    /**
     * Checks whether user password is expired,
     * if true, update user's profile accordingly
     *
     * @param userName
     * @param expiresIn
     *
     * @return whether user's credentials have expired
     */
    private boolean ensurePasswordIsNotExpired(String userName, int expiresIn) {
        log.debug("Verifying password expiration");
        //boolean isExpired = false;

        // registered unknown user
        if(unknownUsersCache.getIfPresent(userName)!=null) {
            log.debug("Not verifying unknown user {} password expiration", userName);
            return false;
        }

        // db user todo: use plain user rather than groups containing one
        UserInfo user = findUser(userName, false);
        if (user == null) {
            log.debug(userName + " was not found to verify password expiration");
            return false;
        }

        // either password expired or admin expired credentials for this user
        if (user.isCredentialsExpired()) {
            log.debug("User {} password has expired", userName);
            return true;
        }

        // check if this is not an external user so we do not maintaining pswd expiration
        if (Strings.isNullOrEmpty(user.getPassword())) {
            log.debug("User {} has no password, assuming external ...", userName);
            return false;
        }

        // actual password expiration check
        boolean userPasswordExpired = userGroupStoreService.isUserPasswordExpired(userName, expiresIn);
        if(userPasswordExpired)
            expireUserCredentials(userName);
        return userPasswordExpired;
    }

    /**
     * Throws LockedException if user is locked
     *
     * @param sessionIdentifier
     * @throws UserLockedException
     */
    @Nonnull
    @Override
    public void ensureSessionIsNotLocked(String sessionIdentifier) throws UserLockedException {
        log.debug("Checking if session {} is not locked", sessionIdentifier);
        if(isUserLockPolicyEnabled() && isUserLocked(sessionIdentifier)) {
            log.debug("Session {} is locked, denying login", sessionIdentifier);
            throw UserLockedException.sessionLocked();
        }
    }

    /**
     * Checks whether given user is locked
     *
     * note: this method using caching in sake
     * of DB load preventing
     *
     * @param userName
     *
     * @return boolean
     */
    @Nonnull
    @Override
    public boolean isUserLocked(String userName) {
        return !isAnonymousUser(userName) &&
                userGroupStoreService.isUserLocked(userName);
    }

    /**
     * Throws LoginDelayedException if user has performed
     * incorrect login in past and now should wait before
     * performing another login attempt
     *
     * @param userName
     * @throws {@link org.artifactory.security.exceptions.LoginDisabledException}
     */
    @Nonnull
    @Override
    public void ensureLoginShouldNotBeDelayed(String userName) {
        if(!isAnonymousUser(userName)) {
            log.debug("Ensuring that user {} should not be blocked", userName);
            long nextLogin = userGroupStoreService.getNextLogin(userName);
            List<Long> list = unknownUsersCache.getIfPresent(userName);
            if (nextLogin < 0 && list !=null) {
                // check frontend cache for unknown users
                nextLogin = userGroupStoreService.getNextLogin(
                        list.size(),
                        list.get(list.size()-1)
                );
            }

            if (nextLogin > 0 && nextLogin > System.currentTimeMillis()) {
                log.debug("User is blocked due to incorrect login attempts till {}", nextLogin);
                throw LoginDisabledException.userLocked(userName, nextLogin);
            }
        }
    }

    /**
     * Throws LoginDelayedException if session has performed
     * incorrect login in past and now should wait before
     * performing another login attempt
     *
     * @param sessionIdentifier
     * @throws {@link org.artifactory.security.exceptions.LoginDisabledException}
     */
    @Nonnull
    @Override
    public void ensureSessionShouldNotBeDelayed(String sessionIdentifier) {
        if(!isAnonymousUser(sessionIdentifier)) {
            log.debug("Ensuring that user {} should not be blocked", sessionIdentifier);
            long nextLogin = userGroupStoreService.getNextLogin(sessionIdentifier);
            List<Long> list = unknownUsersCache.getIfPresent(sessionIdentifier);
            if (nextLogin < 0 && list !=null) {
                // check frontend cache for unknown users
                nextLogin = userGroupStoreService.getNextLogin(list.size(),list.get(list.size() - 1));
            }

            if (nextLogin > 0 && nextLogin > System.currentTimeMillis()) {
                log.debug("Session is blocked due to incorrect login attempts till {}", nextLogin);
                throw LoginDisabledException.sessionLocked(sessionIdentifier, nextLogin);
            }
        }
    }

    /**
     * Performs check whther given user is anonymous
     *
     * @param userName
     *
     * @return true/false
     */
    private boolean isAnonymousUser(String userName) {
        return userName != null &&
                userName.length() == UserInfo.ANONYMOUS.length() &&
                UserInfo.ANONYMOUS.equals(userName);
    }

    @Override
    public boolean createUser(MutableUserInfo user) {
        user.setUsername(user.getUsername().toLowerCase());
        boolean userCreated = userGroupStoreService.createUser(user);
        if (userCreated) {
            interceptors.onUserAdd(user.getUsername());
        }
        return userCreated;
    }

    /**
     * Changes user password
     *
     * @param userName user name
     * @param oldPassword old password
     * @param newPassword1 new password
     * @param newPassword2 replication of new password
     *
     * @return sucess/failure
     */
    @Override
    public void changePassword(String userName, String oldPassword, String newPassword1, String newPassword2) throws PasswordChangeException {
        try {
            UserInfo user = findUser(userName); // todo: [mp] use plain user fetch (rather than heavy groups join)
            if (isOldPasswordValid(user, oldPassword, newPassword1, newPassword2)) {
                SaltedPassword newSaltedPassword = generateSaltedPassword(newPassword1);
                if ((user.getPassword() == null && newSaltedPassword.getPassword() != null) ||
                        !user.getPassword().equals(newSaltedPassword.getPassword())) {
                    userGroupStoreService.changePassword(user, newSaltedPassword);
                    log.info("Password for user '" + userName + "' has been successfully changed");
                } else {
                    log.debug("Passwords are equal, not taking any action");
                    throw new PasswordChangeException("New password has to be different from the old one");
                }
            }
        } catch (StorageException e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordChangeException("Changing password has failed, see logs for more details");
        } catch (UsernameNotFoundException e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordChangeException("Changing password has failed, " + e.getMessage());
        }
    }

    /**
     * Checks whether old password is valid
     *
     * @param user a user to validate password details for
     * @param oldPassword old password
     * @param newPassword1 new password
     * @param newPassword2 replication of new password
     *
     * @return true/false
     */
    private boolean isOldPasswordValid(UserInfo user, String oldPassword, String newPassword1, String newPassword2) {
        SaltedPassword oldSaltedPassword = generateSaltedPassword(oldPassword);
        if((user.getPassword() == null && oldSaltedPassword.getPassword() != null) ||
                !user.getPassword().equals(oldSaltedPassword.getPassword())) {
            throw new PasswordChangeException("Old password is incorrect");
        }
        if((newPassword1 != null && !newPassword1.equals(newPassword2)) ||
                (newPassword2 != null && !newPassword2.equals(newPassword1))) {
            throw new PasswordChangeException("New passwords do not match");
        }
        if (newPassword1 == null) {
            throw new PasswordChangeException("New passwords cannot be empty");
        }
        return true;
    }

    @Override
    public void updateUser(MutableUserInfo user, boolean activateListeners) {
        user.setUsername(user.getUsername().toLowerCase());
        userGroupStoreService.updateUser(user);
        if (activateListeners) {
            for (SecurityListener listener : securityListeners) {
                listener.onUserUpdate(user.getUsername());
            }
        }
    }

    @Override
    public void deleteUser(String username) {
        aclStoreService.removeAllUserAces(username);
        userGroupStoreService.deleteUser(username);
        interceptors.onUserDelete(username);
        for (SecurityListener listener : securityListeners) {
            listener.onUserDelete(username);
        }
    }

    @Override
    public void updateGroup(MutableGroupInfo groupInfo) {
        userGroupStoreService.updateGroup(groupInfo);
    }

    @Override
    public boolean createGroup(MutableGroupInfo groupInfo) {
        boolean groupCreated = userGroupStoreService.createGroup(groupInfo);
        if (groupCreated) {
            interceptors.onGroupAdd(groupInfo.getGroupName());
        }
        return groupCreated;
    }

    @Override
    public void updateGroupUsers(MutableGroupInfo group, List<String> usersInGroup) {
        // remove users from groups
        removePrevGroupUsers(group);
        // add users to group
        addUserToGroup(usersInGroup, group.getGroupName());
    }

    @Override
    public void deleteGroup(String groupName) {
        aclStoreService.removeAllGroupAces(groupName);
        if (userGroupStoreService.deleteGroup(groupName)) {
            interceptors.onGroupDelete(groupName);
        }
    }

    @Override
    public List<GroupInfo> getAllGroups() {
        return userGroupStoreService.getAllGroups();
    }

    @Override
    public List<GroupInfo> getNewUserDefaultGroups() {
        return userGroupStoreService.getNewUserDefaultGroups();
    }

    @Override
    public List<GroupInfo> getAllExternalGroups() {
        return userGroupStoreService.getAllExternalGroups();
    }

    @Override
    public List<GroupInfo> getInternalGroups() {
        return userGroupStoreService.getInternalGroups();
    }

    @Override
    public Set<String> getNewUserDefaultGroupsNames() {
        return userGroupStoreService.getNewUserDefaultGroupsNames();
    }

    @Override
    public void addUsersToGroup(String groupName, List<String> usernames) {
        userGroupStoreService.addUsersToGroup(groupName, usernames);
        interceptors.onAddUsersToGroup(groupName, usernames);
        for (String username : usernames) {
            for (SecurityListener listener : securityListeners) {
                listener.onUserUpdate(username);
            }
        }
    }

    @Override
    public void removeUsersFromGroup(String groupName, List<String> usernames) {
        userGroupStoreService.removeUsersFromGroup(groupName, usernames);
        interceptors.onRemoveUsersFromGroup(groupName, usernames);
        for (String username : usernames) {
            for (SecurityListener listener : securityListeners) {
                listener.onUserUpdate(username);
            }
        }
    }

    @Override
    public List<UserInfo> findUsersInGroup(String groupName) {
        return userGroupStoreService.findUsersInGroup(groupName);
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
        return findOrCreateExternalUser(userName, transientUser, false);
    }

    @Override
    public UserInfo findOrCreateExternalAuthUser(String userName, boolean transientUser, boolean updateProfile) {
        return findOrCreateExternalUser(userName, transientUser, updateProfile);
    }

    /**
     * find or create external user in db
     *
     * @param userName      - user name to find
     * @param transientUser - if true ,  user mark as transient (not created in db)
     * @param updateProfile - if true ,  user will be able to update it own profile
     * @return
     */
    private UserInfo findOrCreateExternalUser(String userName, boolean transientUser, boolean updateProfile) {
        UserInfo userInfo;
        try {
            userInfo = findUser(userName.toLowerCase());
        } catch (UsernameNotFoundException nfe) {
            try {
                userInfo = autoCreateUser(userName, transientUser, updateProfile);
            } catch (ValidationException ve) {
                log.error("Auto-Creation of '" + userName + "' has filed, " + ve.getMessage());
                throw new InvalidNameException(userName, ve.getMessage(), ve.getIndex());
            }
        }
        return userInfo;
    }


    /**
     * remove group users before update
     *
     * @param group - group data
     */
    private void removePrevGroupUsers(MutableGroupInfo group) {
        List<UserInfo> usersInGroup = findUsersInGroup(group.getGroupName());
        if (usersInGroup != null && !usersInGroup.isEmpty()) {
            List<String> userInGroupList = new ArrayList<>();
            usersInGroup.forEach(userInGroup -> userInGroupList.add(userInGroup.getUsername()));
            removeUsersFromGroup(group.getGroupName(), userInGroupList);
        }
    }

    /**
     * @param users     - user list to be added to group
     * @param groupName - group name
     */
    protected void addUserToGroup(List<String> users, String groupName) {
        if (users != null && !users.isEmpty()) {
            addUsersToGroup(groupName, users);
        }
    }

    /**
     * Auto create user
     *
     * @param userName
     * @param transientUser
     *
     * @return {@link UserInfo}
     * @throws ValidationException if userName is invalid
     */
    private UserInfo autoCreateUser(String userName, boolean transientUser,
            boolean updateProfile) throws ValidationException {
        UserInfo userInfo;
        log.debug("Creating new external user '{}'", userName);

        // make sure username answer artifactory standards RTFACT-8259
        NameValidator.validate(userName);

        UserInfoBuilder userInfoBuilder = new UserInfoBuilder(userName.toLowerCase()).updatableProfile(updateProfile);
        userInfoBuilder.internalGroups(getNewUserDefaultGroupsNames());
        if (transientUser) {
            userInfoBuilder.transientUser();
        }
        userInfo = userInfoBuilder.build();

        // Save non transient user
        if (!transientUser) {
            boolean success = userGroupStoreService.createUser(userInfo);
            if (!success) {
                log.error("User '{}' was not created!", userInfo);
            }
        }
        return userInfo;
    }

    @Override
    @Nullable
    public GroupInfo findGroup(String groupName) {
        return userGroupStoreService.findGroup(groupName);
    }

    @Override
    public String createEncryptedPasswordIfNeeded(UserInfo user, String password) {
        if (isPasswordEncryptionEnabled()) {
            KeyPair keyPair;
            if (StringUtils.isBlank(user.getPrivateKey())) {
                MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(user);
                keyPair = CryptoHelper.generateKeyPair();
                mutableUser.setPrivateKey(CryptoHelper.convertToString(keyPair.getPrivate()));
                mutableUser.setPublicKey(CryptoHelper.convertToString(keyPair.getPublic()));
                updateUser(mutableUser, false);
            } else {
                keyPair = CryptoHelper.createKeyPair(user.getPrivateKey(), user.getPublicKey(), false);
            }

            SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(keyPair);
            return CryptoHelper.encryptSymmetric(password, secretKey, false);
        }
        return password;
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
            byte[] encodedKey = Base64.encodeBase64URLSafe(passwordKey.getBytes(Charsets.UTF_8));
            String encodedKeyString = new String(encodedKey, Charsets.UTF_8);

            MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
            mutableUser.setGenPasswordKey(encodedKeyString);
            updateUser(mutableUser, false);

            //Add encoded key to page url
            String resetPage = resetPageUrl + "?key=" + encodedKeyString;

            //If there are any admins with valid email addresses, add them to the list that the message will contain
            //String adminList = getAdminListBlock(userInfo);
            InputStream stream = null;
            try {
                //Get message body from properties and substitute variables
                stream = getClass().getResourceAsStream("/org/artifactory/email/messages/resetPassword.properties");
                ResourceBundle resourceBundle = new PropertyResourceBundle(stream);
                String body = resourceBundle.getString("body");
                body = MessageFormat.format(body, username, remoteAddress, resetPage);
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

        byte[] decodedKey = Base64.decodeBase64(passwordKey.getBytes(Charsets.UTF_8));
        String decodedKeyString = new String(decodedKey, Charsets.UTF_8);
        String[] splitKey = decodedKeyString.split(":");

        //Key must be in 3 parts
        if (splitKey.length < 3) {
            throw new IllegalArgumentException("Password reset key must contain 3 parts - 'UUID:Date:IP'");
        }

        String time = splitKey[1];
        String ip = splitKey[2];

        Date date = new Date(Long.parseLong(time));

        return new SerializablePair<>(date, ip);
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
            pair = new SerializablePair<>(lastLoginClientIp, lastLoginTimeMillis);
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
        UserInfo userInfo = userGroupStoreService.findUser(username);
        if (userInfo == null) {
            // user not found (might be a transient user)
            log.trace("Could not update non-exiting username: {}'.", username);
            return;
        }
        long timeSinceLastLogin = loginTimeMillis - userInfo.getLastLoginTimeMillis();
        if (timeSinceLastLogin < lastLoginBufferTimeMillis) {
            log.debug("Skipping the update of the last login time for the user '{}': " +
                    "was updated less than {} seconds ago.", username, lastLoginBufferTimeSecs);
            return;
        }
        MutableUserInfo mutableUser = InfoFactoryHolder.get().copyUser(userInfo);
        mutableUser.setLastLoginTimeMillis(loginTimeMillis);
        mutableUser.setLastLoginClientIp(clientIp);
        updateUser(mutableUser, false);
    }

    /**
     * Updates user last access time, if user is not exist in artifactory
     * keeps track of it in volatile cache
     *
     * @param userName                     Name of user that performed an action
     * @param clientIp                     The IP of the client that has accessed
     * @param accessTimeMillis             The time of access
     *
     * @throws UsernameNotFoundException
     */
    @Override
    public void updateUserLastAccess(String userName, String clientIp, long accessTimeMillis){
        log.debug("Updating access details for user {}, time={}, ip={}", userName, accessTimeMillis, clientIp);
        userGroupStoreService.updateUserAccess(userName, clientIp, accessTimeMillis);
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

    @Override
    public boolean hasPermission(ArtifactoryPermission artifactoryPermission) {
        return isAdmin() || !getPermissionTargets(artifactoryPermission).isEmpty();
    }

    @Override
    public boolean canRead(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.READ);
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
    public boolean canManage(RepoPath repoPath) {
        return hasPermission(repoPath, ArtifactoryPermission.MANAGE);
    }

    @Override
    public boolean canManage(PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.MANAGE);
    }

    @Override
    public boolean canRead(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.READ, new SimpleUser(user));
    }

    @Override
    public boolean canAnnotate(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.ANNOTATE, new SimpleUser(user));
    }

    @Override
    public boolean canDeploy(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.DEPLOY, new SimpleUser(user));
    }

    @Override
    public boolean canDelete(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.DELETE, new SimpleUser(user));
    }

    @Override
    public boolean canManage(UserInfo user, PermissionTargetInfo target) {
        return hasPermissionOnPermissionTarget(target, ArtifactoryPermission.MANAGE, new SimpleUser(user));
    }

    @Override
    public boolean canRead(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.READ);
    }

    @Override
    public boolean canAnnotate(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.ANNOTATE);
    }

    @Override
    public boolean canDelete(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.DELETE);
    }

    @Override
    public boolean canDeploy(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.DEPLOY);
    }

    @Override
    public boolean canManage(UserInfo user, RepoPath path) {
        return hasPermission(new SimpleUser(user), path, ArtifactoryPermission.MANAGE);
    }

    @Override
    public boolean canRead(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.READ);
    }

    @Override
    public boolean canAnnotate(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.ANNOTATE);
    }

    @Override
    public boolean canDelete(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.DELETE);
    }

    @Override
    public boolean canDeploy(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.DEPLOY);
    }

    @Override
    public boolean canManage(GroupInfo group, RepoPath path) {
        return hasPermission(group, path, ArtifactoryPermission.MANAGE);
    }

    @Override
    public boolean userHasPermissions(String username) {
        UserInfo user = userGroupStoreService.findUser(username);
        if (user == null) {
            return false;
        }
        Set<ArtifactorySid> sids = getUserEffectiveSids(new SimpleUser(user));
        List<AclInfo> acls = getAllAcls();
        for (AclInfo acl : acls) {
            if (hasAceInAcl(acl, sids)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Map<PermissionTargetInfo, AceInfo> getUserPermissionByPrincipal(String username) {
        Map<PermissionTargetInfo, AceInfo> aceInfoMap = Maps.newHashMap();
        UserInfo user = userGroupStoreService.findUser(username);
        if (user == null) {
            return Maps.newHashMap();
        }
        Set<ArtifactorySid> sids = getUserEffectiveSids(new SimpleUser(user));
        List<AclInfo> acls = getAllAcls();
        for (AclInfo acl : acls) {
            addSidsPermissions(aceInfoMap, sids, acl);
        }
        return aceInfoMap;
    }

    public Map<PermissionTargetInfo, AceInfo> getGroupsPermissions(List<String> groups) {
        Map<PermissionTargetInfo, AceInfo> aceInfoMap = Maps.newHashMap();
        List<AclInfo> acls = getAllAcls();
        for (AclInfo acl : acls) {
            for (AceInfo ace : acl.getAces()) {
                if (ace.isGroup() && groups.contains(ace.getPrincipal())) {
                    aceInfoMap.put(acl.getPermissionTarget(), ace);
                }
            }
        }
        return aceInfoMap;
    }


    public Map<PermissionTargetInfo, AceInfo> getUserPermissions(String userName) {
        Map<PermissionTargetInfo, AceInfo> aceInfoMap = Maps.newHashMap();
        List<AclInfo> acls = getAllAcls();
        for (AclInfo acl : acls) {
            for (AceInfo ace : acl.getAces()) {
                if (!ace.isGroup() && userName.equals(ace.getPrincipal())) {
                    aceInfoMap.put(acl.getPermissionTarget(), ace);
                }
            }
        }
        return aceInfoMap;
    }


    /**
     * add artifactory sids permissions to map
     *
     * @param aceInfoMap - permission target and principal info map
     * @param sids       -permissions related sids
     * @param acl        - permissions acls
     */
    private void addSidsPermissions(Map<PermissionTargetInfo, AceInfo> aceInfoMap, Set<ArtifactorySid> sids,
            AclInfo acl) {
        for (AceInfo ace : acl.getAces()) {
            //Check that we match the sids
            if (sids.contains(new ArtifactorySid(ace.getPrincipal(), ace.isGroup()))) {
                aceInfoMap.put(acl.getPermissionTarget(), ace);
            }
        }
    }


    @Override
    public boolean userHasPermissionsOnRepositoryRoot(String repoKey) {
        Repo repo = repositoryService.repositoryByKey(repoKey);
        if (repo == null) {
            // Repo does not exists => No permissions
            return false;
        }
        // If it is a real (i.e local or cached simply check permission on root.
        if (repo.isReal()) {
            // If repository is real, check if the user has any permission on the root.
            if (repo instanceof RemoteRepo) {
                RepoPath remoteRepoPath = InternalRepoPathFactory.repoRootPath(repoKey);
                repoKey = InternalRepoPathFactory.cacheRepoPath(remoteRepoPath).getRepoKey();
            }
            return hasPermissionOnRoot(repoKey);
        } else {
            // If repository is virtual go over all repository associated with it and check if user has permissions
            // on it root.
            VirtualRepo virtualRepo = (VirtualRepo) repo;
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
        return simpleUser != null ? false : true;
    }

    @Override
    public String currentUserEncryptedPassword(boolean escape) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if ((authentication != null) && authentication.isAuthenticated()) {
            String authUsername = ((UserDetails) authentication.getPrincipal()).getUsername();
            String password = (String) authentication.getCredentials();
            if (StringUtils.isNotBlank(password)) {
                UserInfo user = userGroupStoreService.findUser(authUsername);
                if (user == null) {
                    log.warn("Can't return the encrypted password of the unfound user '{}'", authUsername);
                } else {
                    String encrypted = createEncryptedPasswordIfNeeded(user, password);
                    if (!encrypted.equals(password)) {
                        if (escape) {
                            return CryptoHelper.needsEscaping(encrypted);
                        } else {
                            return encrypted;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean hasPermissionOnRoot(String repoKey) {
        RepoPath path = InternalRepoPathFactory.repoRootPath(repoKey);
        for (ArtifactoryPermission permission : ArtifactoryPermission.values()) {
            if (hasPermission(path, permission)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermission(RepoPath repoPath, ArtifactoryPermission permission) {
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

        if (TrashService.TRASH_KEY.equals(repoPath.getRepoKey()) && !isAdmin()) {
            return false;
        }

        Set<ArtifactorySid> sids = getUserEffectiveSids(getSimpleUser(authentication));
        return isGranted(repoPath, permission, sids);
    }

    private boolean hasPermission(SimpleUser user, RepoPath repoPath, ArtifactoryPermission permission) {
        // Admins has permissions for all paths and all repositories
        if (user.isAdmin()) {
            return true;
        }

        // Anonymous users are checked only if anonymous access is enabled
        if (user.isAnonymous() && !isAnonAccessEnabled()) {
            return false;
        }

        Set<ArtifactorySid> sids = getUserEffectiveSids(user);
        return isGranted(repoPath, permission, sids);
    }

    private boolean hasPermission(final GroupInfo group, RepoPath repoPath, ArtifactoryPermission permission) {
        Set<ArtifactorySid> sid = new HashSet<ArtifactorySid>() {{
            add(new ArtifactorySid(group.getGroupName(), true));
        }};
        return isGranted(repoPath, permission, sid);
    }

    private boolean isPermissionTargetIncludesRepoKey(String repoKey, PermissionTargetInfo permissionTarget) {
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

    private boolean hasPermissionOnPermissionTarget(PermissionTargetInfo permTarget, ArtifactoryPermission permission) {
        AclInfo acl = aclStoreService.getAcl(permTarget.getName());
        return hasPermissionOnAcl(acl, permission);
    }

    private boolean hasPermissionOnPermissionTarget(PermissionTargetInfo permTarget, ArtifactoryPermission permission,
            SimpleUser user) {
        AclInfo acl = aclStoreService.getAcl(permTarget.getName());
        return hasPermissionOnAcl(acl, permission, user);
    }

    private boolean hasPermissionOnAcl(AclInfo acl, ArtifactoryPermission permission) {
        Authentication authentication = AuthenticationHelper.getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }
        // Admins has permissions on any target
        if (isAdmin(authentication)) {
            return true;
        }

        return hasPermissionOnAcl(acl, permission, getSimpleUser(authentication));
    }

    private boolean hasPermissionOnAcl(AclInfo acl, ArtifactoryPermission permission, SimpleUser user) {
        // Admins has permissions on any target
        if (user.isAdmin()) {
            return true;
        }

        return isGranted(acl, permission, getUserEffectiveSids(user));
    }

    private boolean isGranted(AclInfo acl, ArtifactoryPermission permission, Set<ArtifactorySid> sids) {
        for (AceInfo ace : acl.getAces()) {
            //Check that we match the sids
            if (sids.contains(new ArtifactorySid(ace.getPrincipal(), ace.isGroup()))) {
                if ((ace.getMask() & permission.getMask()) > 0) {
                    //Any of the permissions is enough for granting
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGranted(
            RepoPath repoPath, ArtifactoryPermission permission, Set<ArtifactorySid> sids) {
        Collection<AclInfo> acls = aclStoreService.getAllAcls();
        for (AclInfo acl : acls) {
            if (!(acl instanceof ImmutableAclInfo)) {
                RuntimeException runtimeException = new RuntimeException(
                        "Checking for permission on " + acl
                                + " should use only immutable security objects not " + acl.getClass());
                log.error(runtimeException.getMessage(), runtimeException);
            }
            if (!hasAceInAcl(acl, sids)) {
                // If no ACE skip path analysis
                continue;
            }
            String repoKey = repoPath.getRepoKey();
            String aclCompatibleRepoKey = makeRemoteRepoKeyAclCompatible(repoKey);  //acl compatible key for remotes
            String path = repoPath.getPath();
            boolean folder = repoPath.isFolder();
            PermissionTargetInfo aclPermissionTarget = acl.getPermissionTarget();
            if (isPermissionTargetIncludesRepoKey(repoKey, aclPermissionTarget)
                    || isPermissionTargetIncludesRepoKey(aclCompatibleRepoKey, aclPermissionTarget)) {
                boolean checkPartialPath = (permission.getMask() & (ArtifactoryPermission.READ.getMask() | ArtifactoryPermission.DEPLOY.getMask())) != 0;
                boolean behaveAsFolder = folder && checkPartialPath;
                boolean match = matches(aclPermissionTarget, path, behaveAsFolder);
                if (match) {
                    if (isGranted(acl, permission, sids)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasAceInAcl(AclInfo acl, Set<ArtifactorySid> sids) {
        for (AceInfo ace : acl.getAces()) {
            //Check that we match the sids
            if (sids.contains(new ArtifactorySid(ace.getPrincipal(), ace.isGroup()))) {
                return true;
            }
        }
        return false;
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
        status.status("Importing security...", log);
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
            status.warn("Could not read security file", log);
            return;
        }
        SecurityService me = InternalContextHelper.get().beanForType(SecurityService.class);
        me.importSecurityData(securityInfo);
    }

    private void createDefaultAdminUser() {
        log.info("Creating the default super user '" + DEFAULT_ADMIN_USER + "', since no admin user exists!");
        UserInfo defaultAdmin = userGroupStoreService.findUser(DEFAULT_ADMIN_USER);
        UserInfoBuilder builder = new UserInfoBuilder(DEFAULT_ADMIN_USER);
        if (defaultAdmin != null) {
            log.error("No admin user where found, but the default user named '" + DEFAULT_ADMIN_USER + "'" +
                    " exists and is not admin!\n" +
                    "Updating the super user '" + DEFAULT_ADMIN_USER + "' with default state and password!");
            builder.password(generateSaltedPassword(DEFAULT_ADMIN_PASSWORD))
                    .email(defaultAdmin.getEmail())
                    .admin(true).updatableProfile(true).enabled(true);
            MutableUserInfo newAdminUser = builder.build();
            newAdminUser.setLastLoginTimeMillis(defaultAdmin.getLastLoginTimeMillis());
            newAdminUser.setLastLoginClientIp(defaultAdmin.getLastLoginClientIp());
            updateUser(newAdminUser, false);
        } else {
            builder.password(generateSaltedPassword(DEFAULT_ADMIN_PASSWORD)).email(null)
                    .admin(true).updatableProfile(true);
            createUser(builder.build());
        }
    }

    private void createDefaultAnonymousUser() {
        UserInfo anonymousUser = userGroupStoreService.findUser(UserInfo.ANONYMOUS);
        if (anonymousUser != null) {
            log.debug("Anonymous user " + anonymousUser + " already exists");
            return;
        }
        log.info("Creating the default anonymous user, since it does not exists!");
        UserInfoBuilder builder = new UserInfoBuilder(UserInfo.ANONYMOUS);
        builder.password(generateSaltedPassword("", null)).email(null).enabled(true).updatableProfile(false);
        MutableUserInfo anonUser = builder.build();
        boolean createdAnonymousUser = createUser(anonUser);

        if (createdAnonymousUser) {
            MutableGroupInfo readersGroup = InfoFactoryHolder.get().createGroup("readers");
            readersGroup.setRealm(SecurityConstants.DEFAULT_REALM);
            readersGroup.setDescription("A group for read-only users");
            readersGroup.setNewUserDefault(true);
            createGroup(readersGroup);
            aclStoreService.createDefaultSecurityEntities(anonUser, readersGroup, currentUsername());
        }
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
                userGroupStoreService.createGroup(group);
            }
        }
        List<UserInfo> users = securityInfo.getUsers();
        boolean hasAnonymous = false;
        if (users != null) {
            for (UserInfo user : users) {
                userGroupStoreService.createUserWithProperties(user, true);
                if (user.isAnonymous()) {
                    hasAnonymous = true;
                }
            }
        }
        List<AclInfo> acls = securityInfo.getAcls();
        if (acls != null) {
            for (AclInfo acl : acls) {
                aclStoreService.createAcl(acl);
            }
        }
        if (!hasAnonymous) {
            createDefaultAnonymousUser();
        }
    }

    private void clearSecurityData() {
        //Respect order for clean removal
        //Clean up all acls
        log.debug("Clearing security data");
        aclStoreService.deleteAllAcls();
        //Remove all existing groups
        userGroupStoreService.deleteAllGroupsAndUsers();
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
    public SaltedPassword generateSaltedPassword(String rawPassword) {
        return generateSaltedPassword(rawPassword, getDefaultSalt());
    }

    @Override
    public SaltedPassword generateSaltedPassword(@Nonnull String rawPassword, @Nullable String salt) {
        return new SaltedPassword(passwordEncoder.encodePassword(rawPassword, salt), salt);
    }

    @Override
    public String getDefaultSalt() {
        return ConstantValues.defaultSaltValue.getString();
    }

    @Override
    public BasicStatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
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
//    private String getAdminListBlock(UserInfo recipientUser) {
//        boolean adminsExist = false;
//        StringBuilder adminSb = new StringBuilder();
//
//        //Add list title
//        adminSb.append("<p>If you believe this message was wrongly sent to you please disregard it.");
//
//        List<UserInfo> userInfoList = getAllUsers(true);
//        for (UserInfo user : userInfoList) {
//
//            //If user is admin and has a valid email
//            if ((user.isAdmin()) && (!StringUtils.isEmpty(user.getEmail())) && (!user.equals(recipientUser))) {
//                adminsExist = true;
//                adminSb.append(user.getEmail());
//
//                //If it is not the last item, add a line break
//                if ((userInfoList.indexOf(user) != userInfoList.size())) {
//                    adminSb.append("<br>");
//                }
//            }
//        }
//        adminSb.append("<p>");
//
//        //Make sure valid admins have been found before adding them to the message body
//        if (adminsExist) {
//            return adminSb.toString();
//        }
//        return "";
//    }

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

        Sets.SetView<String> excludes = Sets.symmetricDifference(Sets.newHashSet(oldInfo.getExcludes()),
                Sets.newHashSet(newInfo.getExcludes()));
        if (excludes != null && !excludes.isEmpty()) {
            alertModifiedField("excludes");
        }

        if (!oldInfo.getExcludesPattern().equals(newInfo.getExcludesPattern())) {
            alertModifiedField("excludes pattern");
        }

        Sets.SetView<String> includes = Sets.symmetricDifference(Sets.newHashSet(oldInfo.getIncludes()),
                Sets.newHashSet(newInfo.getIncludes()));
        if (includes != null && !includes.isEmpty()) {
            alertModifiedField("includes");
        }

        if (!oldInfo.getIncludesPattern().equals(newInfo.getIncludesPattern())) {
            alertModifiedField("includes pattern");
        }
        // make repo keys compatible with acl cached data
        List<String> compatibleRepoKeys = makeRemoteRepoKeysAclCompatible(newInfo.getRepoKeys());
        // validate repo keys data , make sure that old repo data and new repo data is the same
        Sets.SetView<String> repoKeys = Sets.symmetricDifference(Sets.newHashSet(oldInfo.getRepoKeys()),
                Sets.newHashSet(compatibleRepoKeys));
        if (repoKeys != null && !repoKeys.isEmpty()) {
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

    @Override
    public List<String> convertCachedRepoKeysToRemote(List<String> repoKeys) {
        List<String> altered = Lists.newArrayList();
        for (String repoKey : repoKeys) {
            String repoKeyCacheOmitted;

            if (repoKey.contains(LocalCacheRepoDescriptor.PATH_SUFFIX)) {
                repoKeyCacheOmitted = repoKey.substring(0,
                        repoKey.lastIndexOf(LocalCacheRepoDescriptor.PATH_SUFFIX.charAt(0)));
            } else {
                altered.add(repoKey);
                continue;
            }
            if (repositoryService.remoteRepoDescriptorByKey(repoKeyCacheOmitted) != null) {
                altered.add(repoKeyCacheOmitted);
            } else {
                altered.add(repoKey); //Its Possible that someone named their local repo '*-cache'
            }
        }
        return altered;
    }

    /**
     * Converts remote repo keys contained in the list to have the '-cache' suffix as acls currently
     * only support this notation.
     *
     * @param repoKeys
     * @return repoKeys with all remote repository keys concatenated with '-cache' suffix
     */
    private List<String> makeRemoteRepoKeysAclCompatible(List<String> repoKeys) {
        List<String> altered = Lists.newArrayList();
        for (String repoKey : repoKeys) {
            if (repositoryService.remoteRepoDescriptorByKey(repoKey) != null) {
                altered.add(repoKey.concat(LocalCacheRepoDescriptor.PATH_SUFFIX));
            } else {
                altered.add(repoKey);
            }
        }
        return altered;
    }

    private String makeRemoteRepoKeyAclCompatible(String repoKey) {
        List<String> repoKeyAsList = new ArrayList<>();
        repoKeyAsList.add(repoKey);
        return (makeRemoteRepoKeysAclCompatible(repoKeyAsList).get(0));
    }

    private MutableAclInfo makeNewAclRemoteRepoKeysAclCompatible(MutableAclInfo acl) {
        //Make repository keys acl-compatible before update
        MutablePermissionTargetInfo mutablePermissionTargetInfo = InfoFactoryHolder.get().copyPermissionTarget
                (acl.getPermissionTarget());
        List<String> compatibleRepoKeys = makeRemoteRepoKeysAclCompatible(mutablePermissionTargetInfo.getRepoKeys());
        mutablePermissionTargetInfo.setRepoKeys(compatibleRepoKeys);
        acl.setPermissionTarget(mutablePermissionTargetInfo);

        return acl;
    }

    public MutableAclInfo convertNewAclCachedRepoKeysToRemote(MutableAclInfo acl) {
        //Make repository keys acl-compatible before update
        MutablePermissionTargetInfo mutablePermissionTargetInfo = InfoFactoryHolder.get().copyPermissionTarget
                (acl.getPermissionTarget());
        List<String> compatibleRepoKeys = convertCachedRepoKeysToRemote(mutablePermissionTargetInfo.getRepoKeys());
        mutablePermissionTargetInfo.setRepoKeys(compatibleRepoKeys);
        acl.setPermissionTarget(mutablePermissionTargetInfo);

        return acl;
    }

    public String findUserByPropAuth(String key, String value) {
        UserInfo userByProperty = userGroupStoreService.findUserByProperty(key, value);
        if (userByProperty != null) {
            return userByProperty.getUsername();
        }
        return null;
    }

    /**
     * Makes user password expired
     *
     * @param userName
     */
    @Override
    public void expireUserCredentials(String userName) {
        if (!isPasswordExpirationPolicyEnabled()) {
            throw new PasswordExpireException("Password expiration policy is disabled");
        }
        try {
            if(unknownUsersCache.getIfPresent(userName)!=null) {
                log.debug("User {} is registered in unknown users cache, no password to expire ...");
                throw new UsernameNotFoundException("User " + userName + " does not exist");
            }
            UserInfo user = findUser(userName); // todo: [mp] use plain user fetch (rather than heavy groups join)
            if (Strings.isNullOrEmpty(user.getPassword())) {
                log.debug("User {} is not managed by system, ignoring expire request");
                throw new PasswordExpireException(
                        "User '" + userName + "' is not managed by the artifactory, can't expire credentials.");
            }
            userGroupStoreService.expireUserPassword(userName);
        } catch (StorageException e) {
            throw new PasswordExpireException("Expiring password for \"" + userName + "\" has failed, " + e.getMessage(), e);
        } catch (UsernameNotFoundException e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordExpireException("Expiring password has failed, " + e.getMessage());
        }
    }

    /**
     * Makes user password expired
     *
     * @param userName
     */
    @Override
    public void unexpirePassword(String userName) {
        if (!isPasswordExpirationPolicyEnabled()) {
            throw new PasswordExpireException("Password expiration policy is disabled");
        }
        try {
            if(unknownUsersCache.getIfPresent(userName)!=null) {
                log.debug("User {} is registered in unknown users cache, no password to expire ...");
                throw new UsernameNotFoundException("User " + userName + " does not exist");
            }
            findUser(userName); // todo: [mp] use plain user fetch (rather than heavy groups join)
            userGroupStoreService.revalidatePassword(userName);
        } catch (StorageException e) {
            throw new PasswordExpireException("Expiring password for \"" + userName + "\" has failed, " + e.getMessage(), e);
        } catch (UsernameNotFoundException e) {
            log.error(e.getMessage());
            log.debug("Cause: {}", e);
            throw new PasswordExpireException("Expiring password has failed, " + e.getMessage());
        }
    }

    /**
     * Makes all users passwords expired
     *
     */
    @Override
    public void expireCredentialsForAllUsers() {
        try {
            userGroupStoreService.expirePasswordForAllUsers();
        } catch(StorageException e) {
            log.debug("Expiring all users credentials have failed, cause: {}", e);
            throw new PasswordExpireException("Expiring all users credentials have failed, see logs for more details");
        }

    }

    /**
     * Makes all users passwords not expired
     *
     */
    @Override
    public void unexpirePasswordForAllUsers() {
        try {
            if (!isPasswordExpirationPolicyEnabled()) {
                throw new PasswordExpireException("Password expirable is not enabled");
            }
            userGroupStoreService.revalidatePasswordForAllUsers();
        } catch(StorageException e) {
            log.debug("Un-expiring all users credentials have failed, cause: {}", e);
            throw new PasswordExpireException("Un-expiring all users credentials have failed, see logs for more details");
        }
    }


    /**
     * Fetches users with password is about to expire
     *
     * @return list of users
     */
    @Override
    public Set<PasswordExpiryUser> getUsersWhichPasswordIsAboutToExpire() {
        return userGroupStoreService.getUsersWhichPasswordIsAboutToExpire(
                ConstantValues.passwordDaysToNotifyBeforeExpiry.getInt(), getPasswordExpirationDays());
    }

    /**
     * Marks user.credentialsExpired=True where password has expired
     *
     * @param daysToKeepPassword after what period password should be changed
     */
    @Override
    public void markUsersCredentialsExpired(int daysToKeepPassword) {
        userGroupStoreService.markUsersCredentialsExpired(daysToKeepPassword);
    }


    /**
     * @param userName
     * @return number of days left till password will expire
     *         or negative value if password already expired
     *         or NULL if password expiration feature is disabled
     */
    @Override
    public Integer getUserPasswordDaysLeft(String userName) {
        Integer daysLeft  = null;
        if(isPasswordExpirationPolicyEnabled()) {
            UserInfo user = userGroupStoreService.findUser(userName);
            if (user != null && !user.isAnonymous() && !user.hasInvalidPassword()) {
                Long userPasswordCreationTime = userGroupStoreService.getUserPasswordCreationTime(userName);
                if(userPasswordCreationTime != null) {
                    daysLeft = getDaysLeftUntilPasswordExpires(userPasswordCreationTime);
                } else {
                    log.debug("Password creation time for user {} returned no value", userName);
                }
            }
        }
        return daysLeft;
    }

    private Integer getDaysLeftUntilPasswordExpires(Long userPasswordCreationTime) {
        Integer daysLeft;
        DateTime created = new DateTime(userPasswordCreationTime.longValue());
        int expiresIn = getPasswordExpirationDays();
        DateTime now = DateTime.now();
        daysLeft = created.plusDays(expiresIn).minusDays(now.getDayOfYear()).getDayOfYear();
        if ((daysLeft == 365 || daysLeft == 366) && created.plusDays(expiresIn).dayOfYear().get() != daysLeft) {
            daysLeft = 0;
        }
        return daysLeft;
    }
}
