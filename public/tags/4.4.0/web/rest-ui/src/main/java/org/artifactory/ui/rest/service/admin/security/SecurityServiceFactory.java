package org.artifactory.ui.rest.service.admin.security;

import org.artifactory.descriptor.security.UserLockPolicy;
import org.artifactory.rest.common.service.admin.userprofile.CreateApiKeyService;
import org.artifactory.rest.common.service.admin.userprofile.GetApiKeyService;
import org.artifactory.rest.common.service.admin.userprofile.RevokeApiKeyService;
import org.artifactory.rest.common.service.admin.userprofile.UpdateApiKeyService;
import org.artifactory.ui.rest.model.admin.security.oauth.OAuthUserToken;
import org.artifactory.ui.rest.model.admin.security.user.User;
import org.artifactory.ui.rest.service.admin.security.auth.annotate.GetCanAnnotateService;
import org.artifactory.ui.rest.service.admin.security.auth.currentuser.GetCurrentUserService;
import org.artifactory.ui.rest.service.admin.security.auth.forgotpassword.ForgotPasswordService;
import org.artifactory.ui.rest.service.admin.security.auth.forgotpassword.LoginRelatedDataService;
import org.artifactory.ui.rest.service.admin.security.auth.forgotpassword.ResetPasswordService;
import org.artifactory.ui.rest.service.admin.security.auth.forgotpassword.ValidateResetTokenService;
import org.artifactory.ui.rest.service.admin.security.auth.login.LoginService;
import org.artifactory.ui.rest.service.admin.security.auth.logout.LogoutService;
import org.artifactory.ui.rest.service.admin.security.crowdsso.GetCrowdIntegrationService;
import org.artifactory.ui.rest.service.admin.security.crowdsso.ImportCrowdGroupsService;
import org.artifactory.ui.rest.service.admin.security.crowdsso.RefreshCrowdGroupsService;
import org.artifactory.ui.rest.service.admin.security.crowdsso.TestCrowdConnectionService;
import org.artifactory.ui.rest.service.admin.security.crowdsso.UpdateCrowdIntegration;
import org.artifactory.ui.rest.service.admin.security.general.*;
import org.artifactory.ui.rest.service.admin.security.group.CreateGroupService;
import org.artifactory.ui.rest.service.admin.security.group.DeleteGroupService;
import org.artifactory.ui.rest.service.admin.security.group.GetGroupService;
import org.artifactory.ui.rest.service.admin.security.group.UpdateGroupService;
import org.artifactory.ui.rest.service.admin.security.httpsso.GetHttpSsoService;
import org.artifactory.ui.rest.service.admin.security.httpsso.UpdateHttpSsoService;
import org.artifactory.ui.rest.service.admin.security.ldap.groups.CreateLdapGroupService;
import org.artifactory.ui.rest.service.admin.security.ldap.groups.DeleteLdapGroupService;
import org.artifactory.ui.rest.service.admin.security.ldap.groups.GetLdapGroupService;
import org.artifactory.ui.rest.service.admin.security.ldap.groups.GroupMappingStrategyService;
import org.artifactory.ui.rest.service.admin.security.ldap.groups.ImportLdapGroupService;
import org.artifactory.ui.rest.service.admin.security.ldap.groups.RefreshLdapGroupService;
import org.artifactory.ui.rest.service.admin.security.ldap.groups.UpdateLdapGroupService;
import org.artifactory.ui.rest.service.admin.security.ldap.ldapsettings.CreateLdapSettingsService;
import org.artifactory.ui.rest.service.admin.security.ldap.ldapsettings.DeleteLdapSettingsService;
import org.artifactory.ui.rest.service.admin.security.ldap.ldapsettings.GetLdapSettingsService;
import org.artifactory.ui.rest.service.admin.security.ldap.ldapsettings.ReorderLdapSettingsService;
import org.artifactory.ui.rest.service.admin.security.ldap.ldapsettings.TestLdapSettingsService;
import org.artifactory.ui.rest.service.admin.security.ldap.ldapsettings.UpdateLdapSettingsService;
import org.artifactory.ui.rest.service.admin.security.oauth.AddOAuthProviderSettings;
import org.artifactory.ui.rest.service.admin.security.oauth.DeleteOAuthProviderSettings;
import org.artifactory.ui.rest.service.admin.security.oauth.DeleteOAuthUserToken;
import org.artifactory.ui.rest.service.admin.security.oauth.GetOAuthSettings;
import org.artifactory.ui.rest.service.admin.security.oauth.GetOAuthTokensForUser;
import org.artifactory.ui.rest.service.admin.security.oauth.UpdateOAuthProviderSettings;
import org.artifactory.ui.rest.service.admin.security.oauth.UpdateOrCreateOAuthSettings;
import org.artifactory.ui.rest.service.admin.security.permissions.CreatePermissionsTargetService;
import org.artifactory.ui.rest.service.admin.security.permissions.DeletePermissionsTargetService;
import org.artifactory.ui.rest.service.admin.security.permissions.GetAllUsersAndGroupsService;
import org.artifactory.ui.rest.service.admin.security.permissions.GetPermissionsTargetService;
import org.artifactory.ui.rest.service.admin.security.permissions.UpdatePermissionsTargetService;
import org.artifactory.ui.rest.service.admin.security.saml.GetSamlLoginRequestService;
import org.artifactory.ui.rest.service.admin.security.saml.GetSamlLoginResponseService;
import org.artifactory.ui.rest.service.admin.security.saml.GetSamlLogoutRequestService;
import org.artifactory.ui.rest.service.admin.security.saml.GetSamlService;
import org.artifactory.ui.rest.service.admin.security.saml.IsSamlAuthentication;
import org.artifactory.ui.rest.service.admin.security.saml.UpdateSamlService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.debiankeys.GetDebianSigningKeyService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.debiankeys.InstallDebianKeyService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.debiankeys.RemoveDebianKeyService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.debiankeys.UpdateDebianKeyService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.debiankeys.VerifyDebianKeyService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.keystore.AddKeyStoreService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.keystore.CancelKeyPairService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.keystore.ChangeKeyStorePasswordService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.keystore.GetKeyStoreService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.keystore.RemoveKeyStorePasswordService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.keystore.RemoveKeyStoreService;
import org.artifactory.ui.rest.service.admin.security.signingkeys.keystore.SaveKeyStoreService;
import org.artifactory.ui.rest.service.admin.security.sshserver.GetSshServerKeyService;
import org.artifactory.ui.rest.service.admin.security.sshserver.GetSshServerService;
import org.artifactory.ui.rest.service.admin.security.sshserver.InstallSshServerKeyService;
import org.artifactory.ui.rest.service.admin.security.sshserver.RemoveSshServerKeyService;
import org.artifactory.ui.rest.service.admin.security.sshserver.UpdateSshServerService;
import org.artifactory.ui.rest.service.admin.security.user.*;
import org.artifactory.ui.rest.service.admin.security.user.userprofile.UnlockUserProfileService;
import org.artifactory.ui.rest.service.admin.security.user.userprofile.UpdateUserProfileService;
import org.springframework.beans.factory.annotation.Lookup;

import java.util.List;

/**
 * @author Chen Keinan
 */
public abstract class SecurityServiceFactory {

    //authentication service
    @Lookup
    public abstract LoginService loginService();
    @Lookup
    public abstract ForgotPasswordService forgotPassword();
    @Lookup
    public abstract IsSamlAuthentication isSamlAuthentication();
    @Lookup
    public abstract GetOAuthSettings getOAuthtSettings();
    @Lookup
    public abstract UpdateOrCreateOAuthSettings updateOAuthSettings();
    @Lookup
    public abstract AddOAuthProviderSettings addOAuthProviderSettings();
    @Lookup
    public abstract UpdateOAuthProviderSettings updateOAuthProviderSettings();
    @Lookup
    public abstract DeleteOAuthProviderSettings deleteOAuthProviderSettings();

    @Lookup
    public abstract GetOAuthTokensForUser getOAuthTokensForUser();

    @Lookup
    public abstract DeleteOAuthUserToken<OAuthUserToken> deleteOAuthUserToken();

    @Lookup
    public abstract ValidateResetTokenService validateToken();

    @Lookup
    public abstract LoginRelatedDataService loginRelatedData();

    @Lookup
    public abstract GetCurrentUserService getCurrentUser();

    @Lookup
    public abstract GetCanAnnotateService getCanAnnotateService();

    @Lookup
    public abstract ResetPasswordService resetPassword();

    @Lookup
    public abstract LogoutService logoutService();
    // user services
    @Lookup
    public abstract CreateUserService<User> createUser();

    @Lookup
    public abstract CheckExternalStatusService<User> checkExternalStatus();

    @Lookup
    public abstract UpdateUserService<User> updateUser();
    @Lookup
    public abstract DeleteUserService deleteUser();
    @Lookup
    public abstract GetUsersService getUsers();

    @Lookup
    public abstract GetUserPermissionsService getUserPermissions();
    @Lookup
    public abstract CreateGroupService createGroup();
    @Lookup
    public abstract UpdateGroupService updateGroup();
    @Lookup
    public abstract DeleteGroupService deleteGroup();
    @Lookup
    public abstract GetGroupService getGroup();
    @Lookup
    public abstract UpdateSecurityConfigService updateSecurityConfig();
    @Lookup
    public abstract EncryptDecryptService encryptPassword();
    @Lookup
    public abstract GetSecurityConfigService getSecurityConfig();
    @Lookup
    public abstract UpdateUserLockPolicyService<UserLockPolicy> updateUserLockPolicy();
    @Lookup
    public abstract GetUserLockPolicyService getUserLockPolicy();
    @Lookup
    public abstract UnlockUserService<String> unlockUser();
    @Lookup
    public abstract UnlockUsersService<List> unlockUsers();
    @Lookup
    public abstract UnlockAllUsersService unlockAllUsers();
    @Lookup
    public abstract GetAllLockedUsersService getAllLockedUsers();
    @Lookup
    public abstract GetMasterKeyService getMasterKey();
    @Lookup
    public abstract UpdateHttpSsoService updateHttpSso();
    @Lookup
    public abstract GetHttpSsoService getHttpSso();
    @Lookup
    public abstract UpdateSshServerService updateSshServer();
    @Lookup
    public abstract GetSshServerService getSshServer();
    @Lookup
    public abstract InstallSshServerKeyService uploadSshServerKey();
    @Lookup
    public abstract GetSshServerKeyService getSshServerKey();
    @Lookup
    public abstract RemoveSshServerKeyService removeSshServerKeyService();
    @Lookup
    public abstract UpdateSamlService updateSaml();
    @Lookup
    public abstract GetSamlService getSaml();
    @Lookup
    public abstract GetSamlLoginRequestService handleLoginRequest();
    @Lookup
    public abstract GetSamlLoginResponseService handleLoginResponse();
    @Lookup
    public abstract GetSamlLogoutRequestService handleLogoutRequest();
    @Lookup
    public abstract GetCrowdIntegrationService getCrowdIntegration();
    @Lookup
    public abstract UpdateCrowdIntegration updateCrowdIntegration();
    @Lookup
    public abstract RefreshCrowdGroupsService refreshCrowdGroups();
    @Lookup
    public abstract ImportCrowdGroupsService importCrowdGroups();
    @Lookup
    public abstract TestCrowdConnectionService testCrowdConnectionService();
    @Lookup
    public abstract CreateLdapSettingsService createLdapSettings();
    @Lookup
    public abstract UpdateLdapSettingsService updateLdapSettings();
    @Lookup
    public abstract GetLdapSettingsService getLdapSettings();
    @Lookup
    public abstract DeleteLdapSettingsService deleteLdapSettings();
    @Lookup
    public abstract TestLdapSettingsService testLdapSettingsService();
    @Lookup
    public abstract ReorderLdapSettingsService reorderLdapSettings();
    @Lookup
    public abstract CreateLdapGroupService createLdapGroup();
    @Lookup
    public abstract UpdateLdapGroupService updateLdapGroup();
    @Lookup
    public abstract GetLdapGroupService getLdapGroup();
    @Lookup
    public abstract GroupMappingStrategyService groupMappingStrategy();
    @Lookup
    public abstract DeleteLdapGroupService deleteLdapGroup();
    @Lookup
    public abstract RefreshLdapGroupService refreshLdapGroup();
    @Lookup
    public abstract ImportLdapGroupService importLdapGroup();
    @Lookup
    public abstract UnlockUserProfileService unlockUserProfile();
    @Lookup
    public abstract UpdateUserProfileService updateUserProfile();

    @Lookup
    public abstract GetApiKeyService getApiKey();

    @Lookup
    public abstract CreateApiKeyService createApiKey();

    @Lookup
    public abstract RevokeApiKeyService revokeApiKey();

    @Lookup
    public abstract UpdateApiKeyService regenerateApiKey();

    @Lookup
    public abstract InstallDebianKeyService uploadDebianKey();
    @Lookup
    public abstract GetDebianSigningKeyService getDebianSigningKey();
    @Lookup
    public abstract RemoveDebianKeyService removeDebianKeyService();
    @Lookup
    public abstract VerifyDebianKeyService verifyDebianKey();
    @Lookup
    public abstract UpdateDebianKeyService updateDebianKey();
    @Lookup
    public abstract AddKeyStoreService addKeyStore();
    @Lookup
    public abstract SaveKeyStoreService saveKeyStore();
    @Lookup
    public abstract GetKeyStoreService getKeyStore();

    @Lookup
    public abstract RemoveKeyStorePasswordService removeKeystorePassword();
    @Lookup
    public abstract CancelKeyPairService cancelKeyPair();
    @Lookup
    public abstract RemoveKeyStoreService removeKeyStore();
    @Lookup
    public abstract ChangeKeyStorePasswordService changeKeyStorePassword();
    @Lookup
    public abstract GetPermissionsTargetService getPermissionsTarget();
    @Lookup
    public abstract GetAllUsersAndGroupsService getAllUsersAndGroups();
    @Lookup
    public abstract UpdatePermissionsTargetService updatePermissionsTarget();
    @Lookup
    public abstract CreatePermissionsTargetService createPermissionsTarget();
    @Lookup
    public abstract DeletePermissionsTargetService deletePermissionsTarget();

}
