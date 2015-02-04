package org.artifactory.security.interceptor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.storage.StorageProperties;
import org.artifactory.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @Author Chen Keinan
 */
public class StoragePropertiesEncryptInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StoragePropertiesEncryptInterceptor.class);

    /**
     * encrypt or decrypt storage properties file
     *
     * @param encrypt - if true encrypt else decrypt
     * @throws java.io.IOException
     */
    public void encryptOrDecryptStoragePropertiesFile(boolean encrypt) {
        try {
            File propertiesFile = getPropertiesStorageFile();
            StorageProperties storageProperties = getStoragePropertiesFile();
            String password = storageProperties.getProperty(StorageProperties.Key.password);
            storageProperties.setPassword(getNewPassword(encrypt, password));
            storageProperties.updateStoragePropertiesFile(propertiesFile);
        } catch (IOException e) {
            log.error("Error Loading encrypt storage properties File" + e.getMessage(), e, log);
        }
    }

    /**
     * get storage properties file from context
     *
     * @return
     * @throws IOException
     */
    private StorageProperties getStoragePropertiesFile() throws IOException {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File storagePropsFile = getPropertiesStorageFile();
        if (!storagePropsFile.exists()) {
            if (artifactoryHome.isHaConfigured()) {
                throw new IllegalStateException(
                        "Artifactory could not proceed with encryption/decryption due to storage.properties " +
                                "could not be found.");
            }
            copyDefaultDerbyConfig(storagePropsFile);
        }
        StorageProperties storageProps = new StorageProperties(storagePropsFile, true);
        return storageProps;
    }

    /**
     * get properties file from context Artifactory home
     * getPropertiesStorageFile@return Storage properties File
     */
    private File getPropertiesStorageFile() {
        ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
        File storagePropsFile = artifactoryHome.getStoragePropertiesFile();
        return storagePropsFile;
    }

    /**
     * copy default (derby db configuration)
     *
     * @param targetStorageFile
     * @throws IOException
     */
    private void copyDefaultDerbyConfig(File targetStorageFile) throws IOException {
        try (InputStream pis = ResourceUtils.getResource("/META-INF/default/db/derby.properties")) {
            FileUtils.copyInputStreamToFile(pis, targetStorageFile);
        }
    }

    private String getNewPassword(boolean encrypt, String password) {
        if (StringUtils.isNotBlank(password)) {
            if (encrypt) {
                return CryptoHelper.encryptIfNeeded(password);
            } else {
                return CryptoHelper.decryptIfNeeded(password);
            }
        }
        return null;
    }

}
