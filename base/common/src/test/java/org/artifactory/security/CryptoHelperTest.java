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

import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.artifactory.security.CryptoHelper.ASYM_ALGORITHM;
import static org.artifactory.security.CryptoHelper.SYM_ALGORITHM;
import static org.testng.Assert.*;

/**
 * Tests the CryptoHelper.
 *
 * @author Yossi Shaul
 * @author Noam Tenne
 */
@Test
public class CryptoHelperTest extends ArtifactoryHomeBoundTest {
    private static final Logger log = LoggerFactory.getLogger(CryptoHelperTest.class);

    public void generateKeyPair() throws Exception {
        long start = System.nanoTime();
        KeyPair keyPair = CryptoHelper.generateKeyPair();
        log.debug("KeyPair generation for " + ASYM_ALGORITHM + ": " + (System.nanoTime() - start) / 1000000 + " ms.");
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate().getAlgorithm());
        assertNotNull(keyPair.getPublic().getAlgorithm());
        assertEquals(keyPair.getPublic().getAlgorithm(), ASYM_ALGORITHM);
        assertEquals(keyPair.getPublic().getAlgorithm(), keyPair.getPrivate().getAlgorithm());
        /*log.debug("Public: " + new String(keyPair.getPublic().getEncoded())
                + " Private: " + new String(keyPair.getPrivate().getEncoded()));*/
    }

    public void generateSecretKey() throws Exception {
        long start = System.nanoTime();
        SecretKey secretKey = CryptoHelper.generatePbeKey("mypassword");
        log.debug("SecretKey generation for " + SYM_ALGORITHM + ": " + (System.nanoTime() - start) / 1000000 + " ms.");
        assertNotNull(secretKey);
        assertNotNull(secretKey.getAlgorithm());
        // don't check the algorithm since the same key can be used by multiple algorithms and
        // in jdk5 the name returned may not be the one used when generating the key
        //assertEquals(secretKey.getAlgorithm(), SYM_ALGORITHM);
        log.debug("SecretKey: " + new String(secretKey.getEncoded()));
    }

    public void encryptSymmetric() {
        SecretKey secretKey = CryptoHelper.generatePbeKey("toto");
        String encrypted = CryptoHelper.encryptSymmetric("toto", secretKey);
        log.debug("Symmetric encrypted toto: {}", encrypted);
        assertNotNull(encrypted);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
    }

    public void encryptDecryptSymmetric() {
        SecretKey secretKey = CryptoHelper.generatePbeKey("popo");
        String toEncrypt = "12345678901234567890";
        String encrypted = CryptoHelper.encryptSymmetric(toEncrypt, secretKey);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
        log.debug("Symmetric encrypted {}: {}", toEncrypt, encrypted);
        String decrypted = CryptoHelper.decryptSymmetric(encrypted, secretKey);
        assertNotNull(decrypted);
        assertFalse(CryptoHelper.isEncrypted(decrypted));
        assertEquals(decrypted, toEncrypt);
    }

    public void escapeEncryptedPassword() {
        assertEquals(CryptoHelper.escapeEncryptedPassword("{DESede}123"), "\\{DESede\\}123");
        assertEquals(CryptoHelper.escapeEncryptedPassword("\\{DESede\\}123"), "\\{DESede\\}123");
        assertEquals(CryptoHelper.escapeEncryptedPassword("123"), "123");
        assertEquals(CryptoHelper.escapeEncryptedPassword("{DESede}"), "\\{DESede\\}");
        assertEquals(CryptoHelper.escapeEncryptedPassword("%%DESede&&"), "%%DESede&&");
    }

    public void toBase64String() {
        KeyPair keyPair = CryptoHelper.generateKeyPair();
        String base64EncodedPrivate = CryptoHelper.toBase64(keyPair.getPrivate());
        assertNotNull(base64EncodedPrivate);
        String base64EncodedPublic = CryptoHelper.toBase64(keyPair.getPublic());
        assertNotNull(base64EncodedPublic);
    }

    public void restorePrivateKey() {
        KeyPair original = CryptoHelper.generateKeyPair();
        PrivateKey privateKey = original.getPrivate();
        PublicKey publicKey = original.getPublic();

        KeyPair restored = CryptoHelper.createKeyPair(privateKey.getEncoded(), publicKey.getEncoded());
        assertEquals(restored.getPrivate(), original.getPrivate());
        assertEquals(restored.getPublic(), original.getPublic());
    }

    public void restoreKeysFromBase64() {
        KeyPair original = CryptoHelper.generateKeyPair();
        PrivateKey privateKey = original.getPrivate();
        PublicKey publicKey = original.getPublic();

        KeyPair restored = CryptoHelper.createKeyPair(CryptoHelper.toBase64(privateKey),
                CryptoHelper.toBase64(publicKey));
        assertEquals(restored.getPrivate(), original.getPrivate());
        assertEquals(restored.getPublic(), original.getPublic());
    }

    public void isEncrypted() {
        assertFalse(CryptoHelper.isEncrypted("blabla"));
        assertFalse(CryptoHelper.isEncrypted("{RSA}blabla"));
        assertFalse(CryptoHelper.isEncrypted("{ENC}blabla"));
        assertTrue(CryptoHelper.isEncrypted("{DESede}blabla"));
        assertTrue(CryptoHelper.isEncrypted("\\{DESede\\}blabla"), "Escaped maven encryption prefix");
        assertFalse(CryptoHelper.isEncrypted("\\{DESede}blabla"));
        assertFalse(CryptoHelper.isEncrypted("{DESede\\}blabla"));
    }

    public void encrypt() {
        KeyPair keyPair = CryptoHelper.generateKeyPair();
        String encrypted = CryptoHelper.encrypt("toto", keyPair.getPublic());
        assertNotNull(encrypted);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
    }

    public void encryptDecrypt() {
        KeyPair keyPair = CryptoHelper.generateKeyPair();
        String encrypted = CryptoHelper.encrypt("momopopo", keyPair.getPublic());
        assertTrue(CryptoHelper.isEncrypted(encrypted));
        String decrypted = CryptoHelper.decrypt(encrypted, keyPair.getPrivate());
        assertNotNull(decrypted);
        assertFalse(CryptoHelper.isEncrypted(decrypted));
        assertEquals(decrypted, "momopopo");
    }

    @Test(enabled = false)
    public void encryptDecryptLongString() {
        KeyPair keyPair = CryptoHelper.generateKeyPair();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append(i);
        }

        String encrypted = CryptoHelper.encrypt(sb.toString(), keyPair.getPublic());
        log.debug("Encrypted: {}", encrypted);
        assertNotNull(encrypted);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
        String decrypted = CryptoHelper.decrypt(encrypted, keyPair.getPrivate());
        assertEquals(decrypted, sb.toString());
        assertFalse(CryptoHelper.isEncrypted(decrypted));
    }

    public void availableAlgorithms() {
        List<String> securityProviders = Arrays.asList(getServiceTypes());
        for (String provider : securityProviders) {
            if ("KeyPairGenerator".equals(provider) || "Cipher".equals(provider)) {
                log.debug("Provider: " + provider);
                log.debug("Crypto:" + Arrays.asList(getProviderImpls(provider)));
            }
        }
    }

    // This method returns all available services types

    private String[] getServiceTypes() {
        Set<String> result = new HashSet<String>();

        // All all providers
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            // Get services provided by each provider
            Set keys = provider.keySet();
            for (Object key1 : keys) {
                String key = (String) key1;
                key = key.split(" ")[0];

                if (key.startsWith("Alg.Alias.")) {
                    // Strip the alias
                    key = key.substring(10);
                }
                int ix = key.indexOf('.');
                result.add(key.substring(0, ix));
            }
        }
        return result.toArray(new String[result.size()]);
    }

    // This method returns the available implementations for a service type

    private String[] getProviderImpls(String serviceType) {
        Set<String> result = new HashSet<String>();

        // All all providers
        Provider[] providers = Security.getProviders();
        for (Provider provider : providers) {
            // Get services provided by each provider
            Set keys = provider.keySet();
            for (Object key1 : keys) {
                String key = (String) key1;
                key = key.split(" ")[0];

                if (key.startsWith(serviceType + ".")) {
                    result.add(key.substring(serviceType.length() + 1));
                } else if (key.startsWith("Alg.Alias." + serviceType + ".")) {
                    // This is an alias
                    result.add(key.substring(serviceType.length() + 11));
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

}
