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

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.test.ArtifactoryHomeStub;
import org.testng.annotations.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;

import static org.testng.Assert.assertTrue;

/**
 * Tests the customized encrypted password prefix brackets
 *
 * @author Noam Y. Tenne
 */
@Test
public class CustomizedEncryptedPasswordPrefixTest extends ArtifactoryHomeBoundTest {

    @Test
    public void testCustomizedEncryptedPasswordPrefixSurroundingCharacters() throws Exception {
        Field encryptionPrefix = CryptoHelper.class.getDeclaredField("encryptionPrefix");
        encryptionPrefix.setAccessible(true);
        encryptionPrefix.set(null, null);
        ((ArtifactoryHomeStub) ArtifactoryHome.get()).setProperty(
                ConstantValues.securityAuthenticationEncryptedPasswordSurroundChars, "%%&&");
        SecretKey secretKey = CryptoHelper.generatePbeKey("toto");
        String encrypted = CryptoHelper.encryptSymmetric("toto", secretKey);
        assertTrue(encrypted.startsWith("%%DESede&&"), "Encrypted password should have been prefixed with the " +
                "customized surrounding characters.");
    }

    @Test
    public void testCustomizedEncryptedPasswordPrefixWithInvalidSurroundingCharacters() throws Exception {
        Field encryptionPrefix = CryptoHelper.class.getDeclaredField("encryptionPrefix");
        encryptionPrefix.setAccessible(true);
        encryptionPrefix.set(null, null);
        ((ArtifactoryHomeStub) ArtifactoryHome.get()).setProperty(
                ConstantValues.securityAuthenticationEncryptedPasswordSurroundChars, "###*&");
        SecretKey secretKey = CryptoHelper.generatePbeKey("toto");
        String encrypted = CryptoHelper.encryptSymmetric("toto", secretKey);
        assertTrue(encrypted.startsWith("{DESede}"), "Encrypted password should have been prefixed with the " +
                "default surrounding characters since the customized ones were not of an even number.");
    }
}
