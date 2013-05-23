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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.artifactory.common.ConstantValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Helper class for encrypting/decrypting passwords.
 *
 * @author Yossi Shaul
 */
public abstract class CryptoHelper {
    private static final Logger log = LoggerFactory.getLogger(CryptoHelper.class);

    static final String ASYM_ALGORITHM = "RSA";
    private static final String UTF8 = "UTF-8";

    static final String SYM_ALGORITHM = "PBEWithSHA1AndDESede";
    private static final byte[] PBE_SALT = new byte[]{
            (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
            (byte) 0xEB, (byte) 0xAB, (byte) 0xEF, (byte) 0xAC
    };
    private static final int PBE_ITERATION_COUNT = 20;
    private static final String DEFAULT_ENCRYPTION_PREFIX = "{DESede}";
    // since maven 2.1.0 the curly braces are treated as special characters and hence needs to be escaped
    // but still, maven sends the password with the escape characters. go figure...
    private static final String ESCAPED_DEFAULT_ENCRYPTION_PREFIX = "\\{DESede\\}";
    private static String encryptionPrefix;

    private CryptoHelper() {
        // utility class
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ASYM_ALGORITHM);
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(512, random);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm:" + e.getMessage());
        }
    }

    static KeyPair createKeyPair(byte[] encodedPrivateKey, byte[] encodedPublicKey) {
        try {
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
            KeyFactory generator = KeyFactory.getInstance(ASYM_ALGORITHM);
            PrivateKey privateKey = generator.generatePrivate(privateKeySpec);

            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
            PublicKey publicKey = generator.generatePublic(publicKeySpec);
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create KeyPair from provided encoded keys", e);
        }
    }

    public static String toBase64(Key key) {
        return toBase64(key.getEncoded());
    }

    private static String toBase64(byte[] bytes) {
        return bytesToString(Base64.encodeBase64(bytes));
    }

    static byte[] fromBase64(String base64Encoded) {
        return Base64.decodeBase64(stringToBytes(base64Encoded));
    }

    public static KeyPair createKeyPair(String base64PrivateKey, String base64PublicKey) {
        byte[] privateKeyEncoded = Base64.decodeBase64(stringToBytes(base64PrivateKey));
        byte[] publicKeyEncoded = Base64.decodeBase64(stringToBytes(base64PublicKey));
        return createKeyPair(privateKeyEncoded, publicKeyEncoded);
    }

    public static String encrypt(String plainText, PublicKey publicKey) {
        return getEncryptionPrefix() + toBase64(cleanEncrypt(stringToBytes(plainText), publicKey));
    }

    private static byte[] cleanEncrypt(byte[] in, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(ASYM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(in);
            return encryptedBytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt", e);
        }
    }

    public static String decrypt(String encrypted, PrivateKey privateKey) {
        if (!isEncrypted(encrypted)) {
            throw new IllegalArgumentException("Input string is not encrypted");
        }
        String stripped = StringUtils.removeStart(encrypted, getEncryptionPrefix());
        byte[] decoded = fromBase64(stripped);
        return bytesToString(cleanDecrypt((decoded), privateKey));
    }

    private static byte[] cleanDecrypt(byte[] encrypted, PrivateKey privateKey) {
        try {
            Cipher cipher = Cipher.getInstance(ASYM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt", e);
        }
    }

    public static boolean isEncrypted(String in) {
        if (in == null) {
            throw new IllegalArgumentException("Input cannot be null.");
        }

        return in.startsWith(getEncryptionPrefix()) || in.startsWith(ESCAPED_DEFAULT_ENCRYPTION_PREFIX);
    }

    public static SecretKey generatePbeKey(String password) {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(SYM_ALGORITHM);
            SecretKey secretKey = keyFactory.generateSecret(pbeKeySpec);
            return secretKey;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm: " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Unexpected exception: ", e);
        }
    }

    public static String encryptSymmetric(String plainText, SecretKey pbeKey) {
        try {
            Cipher pbeCipher = Cipher.getInstance(SYM_ALGORITHM);
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(PBE_SALT, PBE_ITERATION_COUNT);
            pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
            byte[] encrypted = pbeCipher.doFinal(stringToBytes(plainText));
            return getEncryptionPrefix() + toBase64(encrypted);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String decryptSymmetric(String encrypted, SecretKey pbeKey) {
        try {
            String stripped;
            if (encrypted.startsWith(ESCAPED_DEFAULT_ENCRYPTION_PREFIX)) {
                stripped = StringUtils.removeStart(encrypted, ESCAPED_DEFAULT_ENCRYPTION_PREFIX);
            } else {
                stripped = StringUtils.removeStart(encrypted, getEncryptionPrefix());
            }
            byte[] decoded = fromBase64(stripped);

            Cipher pbeCipher = Cipher.getInstance(SYM_ALGORITHM);
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(PBE_SALT, PBE_ITERATION_COUNT);
            pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
            byte[] decryptedBytes = pbeCipher.doFinal(decoded);
            return bytesToString(decryptedBytes);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private static String bytesToString(byte[] bytes) {
        try {
            return new String(bytes, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    private static byte[] stringToBytes(String string) {
        try {
            return (string.getBytes(UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Escape the encrypted password for maven usage.
     *
     * @param encryptedPassword Encrypted password to escape
     * @return Escaped encrypted password.
     */
    public static String escapeEncryptedPassword(String encryptedPassword) {
        if (encryptedPassword.startsWith(DEFAULT_ENCRYPTION_PREFIX)) {
            return encryptedPassword.replace(DEFAULT_ENCRYPTION_PREFIX, ESCAPED_DEFAULT_ENCRYPTION_PREFIX);
        }
        return encryptedPassword;
    }

    public static boolean isEncryptedPasswordPrefixedWithDefault(String encryptedPassword) {
        return encryptedPassword.startsWith(DEFAULT_ENCRYPTION_PREFIX);
    }

    private static String getEncryptionPrefix() {
        if (StringUtils.isBlank(encryptionPrefix)) {
            String surroundCharacters = ConstantValues.securityAuthenticationEncryptedPasswordSurroundChars.getString();
            if ((surroundCharacters.length() % 2) != 0) {
                log.error("Provided with an asymmetric pair of encrypted password prefix surrounding characters: " +
                        "falling back to the default.");
                surroundCharacters = ConstantValues.securityAuthenticationEncryptedPasswordSurroundChars.getDefValue();
            }

            int middle = surroundCharacters.length() / 2;
            String opening = surroundCharacters.substring(0, middle);
            String closing = surroundCharacters.substring(middle, surroundCharacters.length());
            encryptionPrefix = new StringBuilder(opening).append("DESede").append(closing).toString();
        }

        return encryptionPrefix;
    }
}
