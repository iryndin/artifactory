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

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
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
import java.util.EnumSet;
import java.util.Set;

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

    private CryptoHelper() {
        // utility class
    }

    public static String convertToString(Key key) {
        return convertToString(key.getEncoded());
    }

    // Use whenever byte array needs to be converted to a readable string
    public static String convertToString(byte[] encrypted) {
        return ArtifactoryBase64.convertToString(encrypted);
        /*if (ConstantValues.securityUseBase64.getBoolean()) {
            return ArtifactoryBase64.convertToString(encrypted);
        } else {
            return ArtifactoryBase58.convertToString(encrypted);
        }*/
    }

    // Use whenever a string needs to be converted to the original byte array
    public static byte[] convertToBytes(String encrypted) {
        /*byte[] bytes = ArtifactoryBase58.extractBytes(encrypted);
        if (bytes == null) {
            bytes = ArtifactoryBase64.extractBytes(encrypted);
        }*/
        byte[] bytes = ArtifactoryBase64.extractBytes(encrypted);
        if (bytes == null) {
            throw new IllegalArgumentException("String " + encrypted + " was not encrypted by Artifactory!");
        }
        return bytes;
    }

    public static boolean isEncrypted(String in) {
        if (in == null || in.length() == 0) {
            return false;
        }

        return /*ArtifactoryBase58.isCorrectFormat(in) || */ArtifactoryBase64.isCorrectFormat(in);
    }

    public static String needsEscaping(String encrypted) {
        if (ConstantValues.securityUseBase64.getBoolean()
                && ArtifactoryBase64.isEncryptedPasswordPrefixedWithDefault(encrypted)) {
            return ArtifactoryBase64.escapeEncryptedPassword(encrypted);
        }
        return encrypted;
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

    public static KeyPair createKeyPair(String stringBasePrivateKey, String stringBasePublicKey) {
        byte[] privateKeyEncoded = convertToBytes(stringBasePrivateKey);
        byte[] publicKeyEncoded = convertToBytes(stringBasePublicKey);
        return createKeyPair(privateKeyEncoded, publicKeyEncoded);
    }

    public static SecretKey generatePbeKeyFromKeyPair(String privateKey, String publicKey) {
        KeyPair keyPair = createKeyPair(privateKey, publicKey);
        // Always use Base64 encoding (Historical and why not keep it reason)
        return generatePbeKeyFromKeyPair(keyPair);
    }

    public static SecretKey generatePbeKeyFromKeyPair(KeyPair keyPair) {
        return generatePbeKey(ArtifactoryBase64.toBase64(keyPair.getPrivate().getEncoded()));
    }

    private static SecretKey generatePbeKey(String password) {
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
            return convertToString(encrypted);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String decryptSymmetric(String encrypted, SecretKey pbeKey) {
        try {
            byte[] bytes = convertToBytes(encrypted);
            Cipher pbeCipher = Cipher.getInstance(SYM_ALGORITHM);
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(PBE_SALT, PBE_ITERATION_COUNT);
            pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
            byte[] decryptedBytes = pbeCipher.doFinal(bytes);
            return bytesToString(decryptedBytes);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    static String bytesToString(byte[] bytes) {
        try {
            return new String(bytes, UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    static byte[] stringToBytes(String string) {
        try {
            return (string.getBytes(UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public static String encryptIfNeeded(String password) {
        // If password null => no encryption
        if (password == null || password.length() == 0) {
            return password;
        }
        // If already encrypted => no encrypt again
        if (isEncrypted(password)) {
            return password;
        }

        File keyFile = getKeyFile();
        if (!keyFile.exists()) {
            return password;
        }
        SecretKey secretKey = getSecretKeyFromFile(keyFile);
        return encryptSymmetric(password, secretKey);
    }

    public static String decryptIfNeeded(String password) {
        if (isEncrypted(password)) {
            File keyFile = getKeyFile();
            if (!keyFile.exists()) {
                return password;
                //throw new IllegalArgumentException("The Password is encrypted.\n" +
                //        "And no Master Key file found at " + keyFile.getAbsolutePath());
            }
            SecretKey secretKey = getSecretKeyFromFile(keyFile);
            password = decryptSymmetric(password, secretKey);
        }
        return password;
    }

    public static void createKeyFile() {
        File keyFile = getKeyFile();
        if (keyFile.exists()) {
            throw new RuntimeException(
                    "Cannot create new master key file if it already exists at " + keyFile.getAbsolutePath());
        }
        KeyPair keyPair = generateKeyPair();
        try {
            File securityFolder = keyFile.getParentFile();
            if (!securityFolder.exists()) {
                if (!securityFolder.mkdirs()) {
                    throw new RuntimeException(
                            "Could not create the folder containing the key file " + securityFolder.getAbsolutePath());
                }
                // The security folder should accessible only by the owner
                if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                    Files.setPosixFilePermissions(securityFolder.toPath(), EnumSet.of(
                            PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_READ));
                }
            }

            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                Set<PosixFilePermission> filePermissions = Files.getPosixFilePermissions(securityFolder.toPath());
                if (filePermissions.contains(PosixFilePermission.GROUP_READ) || filePermissions.contains(
                        PosixFilePermission.OTHERS_READ)) {
                    throw new RuntimeException("The folder containing the key file " +
                            securityFolder.getAbsolutePath() + " has too broad permissions!\n" +
                            "Please limit access to the Artifactory user only!");
                }
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(keyFile))) {
                writer.write(convertToString(keyPair.getPrivate().getEncoded()));
                writer.newLine();
                writer.write(convertToString(keyPair.getPublic().getEncoded()));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not write the key into " + keyFile.getAbsolutePath(), e);
        }
    }

    private static SecretKey getSecretKeyFromFile(File keyFile) {
        SecretKey secretKey;
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(keyFile))) {
                String privateKey = reader.readLine();
                String publicKey = reader.readLine();
                secretKey = generatePbeKeyFromKeyPair(privateKey, publicKey);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not read master key " + keyFile.getAbsolutePath() + " to decrypt password!", e);
        }
        return secretKey;
    }

    public static File getKeyFile() {
        ArtifactoryHome home = ArtifactoryHome.get();
        String keyFileLocation = ConstantValues.securityMasterKeyLocation.getString();
        File keyFile = new File(keyFileLocation);
        if (!keyFile.isAbsolute()) {
            keyFile = new File(home.getHaAwareEtcDir(), keyFileLocation);
        }
        return keyFile;
    }
}
