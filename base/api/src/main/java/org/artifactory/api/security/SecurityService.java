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

package org.artifactory.api.security;

import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.config.ImportableExportable;
import org.artifactory.api.repo.Async;
import org.artifactory.descriptor.security.PasswordExpirationPolicy;
import org.artifactory.descriptor.security.UserLockPolicy;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.sapi.common.Lock;
import org.artifactory.security.SaltedPassword;
import org.artifactory.security.SecurityInfo;
import org.artifactory.util.SerializablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Set;

/**
 * User: freds Date: Aug 13, 2008 Time: 5:17:47 PM
 */
public interface SecurityService extends ImportableExportable {
    String FILE_NAME = "security.xml";

    String DEFAULT_ADMIN_USER = "admin";

    String DEFAULT_ADMIN_PASSWORD = "password";

    String USER_SYSTEM = "_system_";

    SecurityInfo getSecurityData();

    @Lock
    void importSecurityData(String securityXml);

    @Lock
    void importSecurityData(SecurityInfo descriptor);

    /**
     * @see org.artifactory.security.ldap.LdapConnectionTester#testLdapConnection
     */
    BasicStatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password);

    /**
     * @return True if password encryption is enabled (supported or required).
     */
    public boolean isPasswordEncryptionEnabled();

    /**
     * @return True if the password matches to the password of the currently logged-in user.
     */
    public boolean userPasswordMatches(String passwordToCheck);

    /**
     * Generates a password recovery key for the specified user and send it by mail
     *
     * @param username     User to rest his password
     * @param clientIp     The IP of the client that sent the request
     * @param resetPageUrl The URL of the reset page we refer to
     */
    @Async(transactional = true)
    void generatePasswordResetKey(String username, String clientIp, String resetPageUrl) throws Exception;

    /**
     * Returns a pair object with the given users password reset key info. If user doesn't exist, a
     * UsernameNotFoundException will be thrown. When the user is not associated with a key, return a null object. In a
     * case where the key is invalid (has less than 3 parts), an IllegalArgumentException is thrown
     *
     * @param username User to retrieve password reset info about
     * @return Pair<Date, String> - Pair containing key generation time and client ip (respectively)
     */
    SerializablePair<Date, String> getPasswordResetKeyInfo(String username);

    /**
     * Returns the given user's last login information
     *
     * @param username Logged in user's name
     * @return Pair<String, Long> - Containing the client IP and last logged in time millis
     */
    SerializablePair<String, Long> getUserLastLoginInfo(String username);

    /**
     * Updates the user last login information
     *
     * @param username        Logged in user's name
     * @param clientIp        The IP of the client that was logged in from
     * @param loginTimeMillis The time of login
     */
    @Async(transactional = true)
    void updateUserLastLogin(String username, String clientIp, long loginTimeMillis);

    /**
     * Updates the user last access information
     *
     * @param username                     Name of user that performed an action
     * @param clientIp                     The IP of the client that has accessed
     * @param accessTimeMillis             The time of access
     */
    void updateUserLastAccess(String username, String clientIp, long accessTimeMillis);

    /**
     * Indicates if Artifactory is configured as proxied by Apache
     *
     * @return True if is proxied. False if not
     */
    boolean isHttpSsoProxied();

    /**
     * Returns the HTTP SSO remote user request variable
     *
     * @return Remote user request variable
     */
    String getHttpSsoRemoteUserRequestVariable();

    /**
     * Indicates if artifactory shouldn't automatically create a user object in the DB for an SSO authenticated user
     *
     * @return True if user should be created in memory. False if user should be created in the DB
     */
    boolean isNoHttpSsoAutoUserCreation();

    void addListener(SecurityListener listener);

    void removeListener(SecurityListener listener);

    void authenticateAsSystem();

    void nullifyContext();

    SaltedPassword generateSaltedPassword(String rawPassword);

    SaltedPassword generateSaltedPassword(String rawPassword, @Nullable String salt);

    String getDefaultSalt();

    /**
     * Triggered when user fails to login and
     * locks it if amount of login failures exceeds
     * {@see LockPolicy#loginAttempts}
     *
     * @param userName user to intercept
     * @param accessTime session creation time
     */
    @Nonnull
    void interceptLoginFailure(String userName, long accessTime);

    /**
     * Triggered when user success to login
     *
     * @param userName user to intercept
     */
    @Nonnull
    void interceptLoginSuccess(String userName);

    /**
     * Throws LockedException if user is locked
     *
     * @param userName
     * @throws org.springframework.security.authentication.LockedException
     */
    @Nonnull
    void ensureUserIsNotLocked(String userName);

    /**
     * Throws CredentialsExpiredException if user's credentials have expired
     *
     * @param userName a user name
     *
     * @throws org.springframework.security.authentication.CredentialsExpiredException
     */
    @Nonnull
    void ensurePasswordIsNotExpired(String userName);

    /**
     * Throws LockedException if user is locked
     *
     * @param sessionIdentifier
     * @throws org.springframework.security.authentication.LockedException
     */
    @Nonnull
    void ensureSessionIsNotLocked(String sessionIdentifier);

    /**
     * Throws LoginDelayedException if user has performed
     * incorrect login in past and now should wait before
     * performing another login attempt
     *
     * @param userName
     * @throws {@link org.artifactory.api.security.exceptions.LoginDelayedException}
     */
    @Nonnull
    void ensureLoginShouldNotBeDelayed(String userName);

    /**
     * Throws LoginDelayedException if session has performed
     * incorrect login in past and now should wait before
     * performing another login attempt
     *
     * @param sessionIdentifier
     * @throws {@link org.artifactory.api.security.exceptions.LoginDelayedException}
     */
    @Nonnull
    void ensureSessionShouldNotBeDelayed(String sessionIdentifier);

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
    boolean isUserLocked(String userName);

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
    void changePassword(String userName, String oldPassword, String newPassword1, String newPassword2);

    /**
     * Makes user password expired
     *
     * @param userName
     */
    void expireUserCredentials(String userName);

    /**
     * Makes user password not expired
     *
     * @param userName
     */
    void unexpirePassword(String userName);

    /**
     * Makes all users passwords expired
     *
     */
    void expireCredentialsForAllUsers();

    /**
     * Makes all users passwords not expired
     *
     */
    void unexpirePasswordForAllUsers();

    /**
     * Fetches users with password is about to expire
     *
     * @return list of users
     */
    Set<PasswordExpiryUser> getUsersWhichPasswordIsAboutToExpire();

    /**
     * Marks user.credentialsExpired=True where password has expired
     *
     * @param daysToKeepPassword after what period password should be changed
     */
    void markUsersCredentialsExpired(int daysToKeepPassword);

    /**
     * @param userName
     * @return number of days left till password will expire
     *         or -1 if password already expired or NULL if
     *         password expiration feature is disabled
     */
    Integer getUserPasswordDaysLeft(String userName);

    /**
     * @return whether {@link UserLockPolicy} is enabled
     */
    boolean isUserLockPolicyEnabled();

    /**
     * @return whether {@link PasswordExpirationPolicy} is enabled
     */
    boolean isPasswordExpirationPolicyEnabled();
}
