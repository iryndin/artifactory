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

package org.artifactory.storage.db.security.itest.service;

import com.google.common.collect.Sets;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.security.SecurityListener;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.security.UserInfoBuilder;
import org.artifactory.descriptor.security.ldap.LdapSetting;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.model.xstream.security.UserGroupImpl;
import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.artifactory.security.*;
import org.artifactory.storage.db.itest.DbBaseTest;
import org.artifactory.storage.security.service.UserGroupStoreService;
import org.artifactory.util.SerializablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Date: 8/27/12
 * Time: 3:28 PM
 *
 * @author freds
 */
@Test
public class UserGroupServiceImplTest extends DbBaseTest {

    @Autowired
    private UserGroupStoreService userGroupService;

    private SecurityService securityService = new SecurityService() {
        @Override
        public SecurityInfo getSecurityData() {
            return null;
        }

        @Override
        public void importSecurityData(String securityXml) {

        }

        @Override
        public void importSecurityData(SecurityInfo descriptor) {

        }

        @Override
        public BasicStatusHolder testLdapConnection(LdapSetting ldapSetting, String username, String password) {
            return null;
        }

        @Override
        public boolean isPasswordEncryptionEnabled() {
            return false;
        }

        @Override
        public boolean userPasswordMatches(String passwordToCheck) {
            return false;
        }

        @Override
        public void generatePasswordResetKey(String username, String clientIp, String resetPageUrl) throws Exception {

        }

        @Override
        public SerializablePair<Date, String> getPasswordResetKeyInfo(String username) {
            return null;
        }

        @Override
        public SerializablePair<String, Long> getUserLastLoginInfo(String username) {
            return null;
        }

        @Override
        public void updateUserLastLogin(String username, String clientIp, long loginTimeMillis) {

        }

        @Override
        public SerializablePair<String, Long> getUserLastAccessInfo(String username) {
            return null;
        }

        @Override
        public void updateUserLastAccess(String username, String clientIp, long accessTimeMillis) {

        }

        @Override
        public boolean isHttpSsoProxied() {
            return false;
        }

        @Override
        public String getHttpSsoRemoteUserRequestVariable() {
            return null;
        }

        @Override
        public boolean isNoHttpSsoAutoUserCreation() {
            return false;
        }

        @Override
        public void addListener(SecurityListener listener) {

        }

        @Override
        public void removeListener(SecurityListener listener) {

        }

        @Override
        public void authenticateAsSystem() {

        }

        @Override
        public void nullifyContext() {

        }

        @Override
        public SaltedPassword generateSaltedPassword(String rawPassword) {
            return null;
        }

        @Override
        public SaltedPassword generateSaltedPassword(String rawPassword, @Nullable String salt) {
            return null;
        }

        @Override
        public String getDefaultSalt() {
            return null;
        }

        @Nonnull
        @Override
        public void interceptLoginFailure(String userName, long accessTime) {

        }

        @Nonnull
        @Override
        public void interceptLoginSuccess(String userName) {

        }

        @Nonnull
        @Override
        public void ensureUserIsNotLocked(String userName) {

        }

        @Nonnull
        @Override
        public void ensurePasswordIsNotExpired(String userName) {

        }

        @Nonnull
        @Override
        public void ensureSessionIsNotLocked(String sessionIdentifier) {

        }

        @Nonnull
        @Override
        public void ensureLoginShouldNotBeDelayed(String userName) {

        }

        @Nonnull
        @Override
        public void ensureSessionShouldNotBeDelayed(String sessionIdentifier) {

        }

        @Override
        public boolean isUserLocked(String userName) {
            return false;
        }

        @Override
        public void changePassword(String userName, String oldPassword, String newPassword1, String newPassword2) {

        }

        @Override
        public void expireUserCredentials(String userName) {

        }

        @Override
        public void unExpireUserCredentials(String userName) {

        }

        @Override
        public void expireCredentialsForAllUsers() {

        }

        @Override
        public void unExpireCredentialsForAllUsers() {

        }

        @Override
        public Map<UserInfo, Long> getUsersWitchPasswordIsAboutToExpire() {
            return null;
        }

        @Override
        public void markUsersCredentialsExpired(int daysToKeepPassword) {

        }

        @Override
        public Integer getUserPasswordDaysLeft(String userName) {
            return null;
        }

        @Override
        public boolean isUserLockPolicyEnabled() {
            return false;
        }

        @Override
        public void exportTo(ExportSettings settings) {

        }

        @Override
        public void importFrom(ImportSettings settings) {

        }
    };

    @BeforeClass
    public void setup() {
        importSql("/sql/user-group.sql");
        ReflectionTestUtils.setField(userGroupService, "securityService", securityService);

    }

    public void hasAnonymousUser() {
        UserInfo anonymousUser = userGroupService.findUser(UserInfo.ANONYMOUS);
        assertNotNull(anonymousUser);
        assertEquals(anonymousUser.getUsername(), UserInfo.ANONYMOUS);
        assertFalse(anonymousUser.isAdmin());
        assertTrue(anonymousUser.isEnabled());
    }

    public void hasDefaultAdmin() {
        UserInfo defaultAdmin = userGroupService.findUser(SecurityService.DEFAULT_ADMIN_USER);
        assertNotNull(defaultAdmin);
        assertEquals(defaultAdmin.getUsername(), SecurityService.DEFAULT_ADMIN_USER);
        assertTrue(defaultAdmin.isAdmin());
        assertTrue(defaultAdmin.isEnabled());
    }

    public void createUserTest() {
        //Create user with group
        String userName = "createUserByMutableUserInfo";
        UserInfoBuilder builder = new UserInfoBuilder(userName);
        UserGroupInfo expectedGroup = new UserGroupImpl("g1", "g1realm");
        Set<UserGroupInfo> groups = Sets.newHashSet(expectedGroup);
        builder.password(new SaltedPassword("password", "salt")).email("jfrog@jfrog.com").enabled(
                true).updatableProfile(true).groups(groups);
        MutableUserInfo expectedUser = builder.build();
        boolean success = userGroupService.createUser(expectedUser);
        Assert.assertTrue(success, "Fail to create user");
        UserInfo userFromDB = userGroupService.findUser(userName);

        // Make sure that the user is equals to the user in the db.
        Assert.assertEquals(expectedUser.getUsername(), userFromDB.getUsername());
        Assert.assertEquals(expectedUser.getPassword(), userFromDB.getPassword());
        Assert.assertEquals(expectedUser.getEmail(), userFromDB.getEmail());
        Assert.assertEquals(expectedUser.isEnabled(), userFromDB.isEnabled());
        Assert.assertEquals(expectedUser.isUpdatableProfile(), userFromDB.isUpdatableProfile());

        // Assert group
        Assert.assertEquals(userFromDB.getGroups().size(), 1);
        UserGroupInfo groupFromDb = userFromDB.getGroups().iterator().next();
        Assert.assertEquals(expectedGroup.getRealm(), groupFromDb.getRealm());
        Assert.assertEquals(expectedGroup.getRealm(), groupFromDb.getRealm());
    }

    public void createUserExistingUsername() {
        MutableUserInfo user = new UserInfoBuilder(SecurityService.DEFAULT_ADMIN_USER).build();
        Assert.assertFalse(userGroupService.createUser(user), "Should not be able to create duplicate username");
    }

    public void createGroupExistingGroupName() {
        MutableGroupInfo group = InfoFactoryHolder.get().createGroup("g1");
        Assert.assertFalse(userGroupService.createGroup(group), "Should not be able to create duplicate group name");
    }
}
