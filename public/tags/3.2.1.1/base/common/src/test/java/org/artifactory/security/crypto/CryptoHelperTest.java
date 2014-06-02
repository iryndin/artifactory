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

package org.artifactory.security.crypto;

import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import javax.crypto.SecretKey;
import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.artifactory.security.crypto.CryptoHelper.ASYM_ALGORITHM;
import static org.artifactory.security.crypto.CryptoHelper.SYM_ALGORITHM;
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
        SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(CryptoHelper.generateKeyPair());
        log.debug("SecretKey generation for " + SYM_ALGORITHM + ": " + (System.nanoTime() - start) / 1000000 + " ms.");
        assertNotNull(secretKey);
        assertNotNull(secretKey.getAlgorithm());
        // don't check the algorithm since the same key can be used by multiple algorithms and
        // in jdk5 the name returned may not be the one used when generating the key
        //assertEquals(secretKey.getAlgorithm(), SYM_ALGORITHM);
        log.debug("SecretKey: " + new String(secretKey.getEncoded()));
    }

    public void encryptSymmetric() {
        SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(CryptoHelper.generateKeyPair());
        String encrypted = CryptoHelper.encryptSymmetric("toto", secretKey);
        log.debug("Symmetric encrypted toto: {}", encrypted);
        assertNotNull(encrypted);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
    }

    public void encryptDecryptSymmetric() {
        SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(CryptoHelper.generateKeyPair());
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
        assertEquals(ArtifactoryBase64.escapeEncryptedPassword("{DESede}123"), "\\{DESede\\}123");
        assertEquals(ArtifactoryBase64.escapeEncryptedPassword("\\{DESede\\}123"), "\\{DESede\\}123");
        assertEquals(ArtifactoryBase64.escapeEncryptedPassword("123"), "123");
        assertEquals(ArtifactoryBase64.escapeEncryptedPassword("{DESede}"), "\\{DESede\\}");
        assertEquals(ArtifactoryBase64.escapeEncryptedPassword("%%DESede&&"), "%%DESede&&");
    }

    public void toBase64String() {
        KeyPair keyPair = CryptoHelper.generateKeyPair();
        String base64EncodedPrivate = CryptoHelper.convertToString(keyPair.getPrivate());
        assertNotNull(base64EncodedPrivate);
        String base64EncodedPublic = CryptoHelper.convertToString(keyPair.getPublic());
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

    public void restoreKeysFromStringBase() {
        KeyPair original = CryptoHelper.generateKeyPair();
        PrivateKey privateKey = original.getPrivate();
        PublicKey publicKey = original.getPublic();

        KeyPair restored = CryptoHelper.createKeyPair(CryptoHelper.convertToString(privateKey),
                CryptoHelper.convertToString(publicKey));
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
        SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(CryptoHelper.generateKeyPair());
        String encrypted = CryptoHelper.encryptSymmetric("toto", secretKey);
        assertNotNull(encrypted);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
    }

    public void encryptDecrypt() {
        SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(CryptoHelper.generateKeyPair());
        String encrypted = CryptoHelper.encryptSymmetric("momopopo", secretKey);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
        String decrypted = CryptoHelper.decryptSymmetric(encrypted, secretKey);
        assertNotNull(decrypted);
        assertFalse(CryptoHelper.isEncrypted(decrypted));
        assertEquals(decrypted, "momopopo");
    }

    class OldEncryptedPassword {
        final String clearText;
        final String privateKey;
        final String publicKey;
        final String encryptedPassword;

        OldEncryptedPassword(String clearText, String privateKey, String publicKey, String encryptedPassword) {
            this.clearText = clearText;
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.encryptedPassword = encryptedPassword;
        }
    }

    public void decryptOldBase64Format() {
        List<OldEncryptedPassword> oldEncrypted = new ArrayList<>(3);
        oldEncrypted.add(new OldEncryptedPassword(
                "jenkins",
                "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAhY7PeGz1M+TzcLeYPcdhtY+R8HuWY5ENZ15Iqi/75RRelhTvar5vdzMHSeaXcezVv/2a5sUUQ5ZoVQI6mBcVtQIDAQABAkAoEbZw/M976D6ZHJvSPRU1cYNpUMrHyGbrEkBevtKl4UpefDj3qUqusDR84fv4aeHzvKaxTx7s06k+/uNZUNBBAiEA65Zwxb6WqdGdpxrwB706bp5nUNZaFzVqmcYG2KAZ5ikCIQCRIUBVDogJqeuu6htlnrHnQNp05oxDwT24s80G4LusrQIgSy8IsGLhjDKEQJcdMSsXocPVrvupZqy6Z3bGKo31lfkCIHozhGbaUIPKlw/2QdFkOapd+lQ6mFqoyR7QDtA+xOgVAiEA0qlPXafzMLjEIjWZvHeg1oJhMsWsWGaAYlZtlkgsS4g=",
                "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIWOz3hs9TPk83C3mD3HYbWPkfB7lmORDWdeSKov++UUXpYU72q+b3czB0nml3Hs1b/9mubFFEOWaFUCOpgXFbUCAwEAAQ==",
                "{DESede}Iz5I9KOc0co="
        ));
        oldEncrypted.add(new OldEncryptedPassword(
                "password",
                "MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAvZPtw3g4MFCDzQ+gCP1gBeNr+0Enl2OHyItnVDFCQ/yMn8WtfKrZqpKdef4N5gwdLGe9neFD1BdUDnyVkG3jFQIDAQABAkAYc60WKjptGOV3HI3SuwOYntW9qZC2uRK5bimctWHLrN0CRxOhfiYZxiX22SID0nZtPlKTiQvD4xkHbdWqJLjdAiEA/Qhi6vRywkEswjdPxCPCI8UJjeFUkLeB88uWhjGy538CIQC/zQxjuPRbIazaSpzRWMLdZiJB+PMVoZHODyVDIMFfawIhALyNN0jmB24Bqxy+os4B53VIKqpzMtT0Kf5Fw1EUT8B5AiASsrWzfxNrUvQb78wr6IBOvyc10UQ5Zp/lO7rBOY9AcwIhAOiNRF3kFQim8LUOqORm7gMh4I91NdN4G3BGN0GtOFBk",
                "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAL2T7cN4ODBQg80PoAj9YAXja/tBJ5djh8iLZ1QxQkP8jJ/FrXyq2aqSnXn+DeYMHSxnvZ3hQ9QXVA58lZBt4xUCAwEAAQ==",
                "{DESede}nO1mRNRdxXk/hP3mIVLBuA=="
        ));
        for (OldEncryptedPassword oldData : oldEncrypted) {
            KeyPair keyPair = CryptoHelper.createKeyPair(oldData.privateKey, oldData.publicKey);
            SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(keyPair);
            String decrypted = CryptoHelper.decryptSymmetric(oldData.encryptedPassword, secretKey);
            assertNotNull(decrypted);
            assertFalse(CryptoHelper.isEncrypted(decrypted));
            assertEquals(decrypted, oldData.clearText);
        }
    }

    @Test(enabled = false)
    public void encryptDecryptMasterKey() {
        // First make sure no master key around
        File keyFile = CryptoHelper.getKeyFile();
        if (keyFile.exists()) {
            assertTrue(keyFile.delete());
            File parentFile = keyFile.getParentFile();
            if ("security".equals(parentFile.getName())) {
                assertTrue(parentFile.delete());
            }
        }
        String pass = "mySuper34Hard42Password";
        String nonEncryptPass = CryptoHelper.encryptIfNeeded(pass);
        assertEquals(nonEncryptPass, pass, "Before creating master key, no encryption should run");
        CryptoHelper.createKeyFile();
        String encryptPass = CryptoHelper.encryptIfNeeded(pass);
        assertNotEquals(encryptPass, pass, "After creating master key, encryption should run");
        assertTrue(encryptPass.startsWith("A"), "Encrypted password should start with A");
        assertTrue(CryptoHelper.isEncrypted(encryptPass), "Encrypted password should be encrypted");
        String decrypted = CryptoHelper.decryptIfNeeded(encryptPass);
        assertEquals(decrypted, pass, "decrypted password should go back to origin");
    }

    @Test(enabled = false)
    public void encryptDecryptLongString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb.append(i);
        }

        SecretKey secretKey = CryptoHelper.generatePbeKeyFromKeyPair(CryptoHelper.generateKeyPair());
        String encrypted = CryptoHelper.encryptSymmetric(sb.toString(), secretKey);
        log.debug("Encrypted: {}", encrypted);
        assertNotNull(encrypted);
        assertTrue(CryptoHelper.isEncrypted(encrypted));
        String decrypted = CryptoHelper.decryptSymmetric(encrypted, secretKey);
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
        Set<String> result = new HashSet<>();

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
        Set<String> result = new HashSet<>();

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
