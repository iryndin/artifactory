package org.artifactory.storage;

import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.util.ResourceUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for the storage properties encrypt decrypt
 *
 * @author Chen Keinan
 */

public class StoragePropertiesEncryptDecryptTest extends ArtifactoryHomeBoundTest {
    StorageProperties sp;

    @BeforeMethod
    public void loadProperties() throws IOException {
        String filePath = "/storage/storagepostgres.properties";
        sp = new StorageProperties(
                ResourceUtils.getResourceAsFile(filePath), true);
    }

    @Test
    public void propertiesPasswordEncryptionTest() throws IOException {
        String filePath = "/storage/storagepostgres.properties";
        if (!CryptoHelper.hasMasterKey()) {
            CryptoHelper.createMasterKeyFile();
        }
        String pass = sp.getProperty(StorageProperties.Key.password);
        assertTrue(!CryptoHelper.isPasswordEncrypted(pass));
        pass = CryptoHelper.encryptIfNeeded(pass);
        int numOfLineBeforeEncryptAndSaving = getFileNumOfLines(filePath);
        int passwordLinePositionBeforeEncryptAndSave = getPasswordPositionLine(filePath);
        sp.setPassword(pass);
        sp.updateStoragePropertiesFile(getPropertiesStorageFile(filePath));
        int numOfLineAfterEncryptAndSaving = getFileNumOfLines(filePath);
        int passwordLinePositionAfterEncryptAndSave = getPasswordPositionLine(filePath);
        // check that comments are maintain
        assertEquals(numOfLineBeforeEncryptAndSaving, numOfLineAfterEncryptAndSaving);
        // check that order is maintain
        assertEquals(passwordLinePositionBeforeEncryptAndSave, passwordLinePositionAfterEncryptAndSave);

    }

    private File getPropertiesStorageFile(String filePath) {
        File storagePropsFile = ResourceUtils.getResourceAsFile(filePath);
        return storagePropsFile;
    }

    private int getFileNumOfLines(String filePath) {
        int lineCount = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(ResourceUtils.getResourceAsFile(filePath)));
            while ((br.readLine()) != null) {
                lineCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lineCount;
    }

    private int getPasswordPositionLine(String filePath) {
        int lineCount = 0;
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader((ResourceUtils.getResourceAsFile(filePath))));
            while ((line = br.readLine()) != null) {
                if (line.startsWith("password")) {
                    lineCount++;
                    return lineCount;
                }
                lineCount++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lineCount;
    }
}
