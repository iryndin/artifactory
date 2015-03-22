/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.storage;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.storage.StorageUnit;
import org.artifactory.security.crypto.CryptoHelper;
import org.artifactory.storage.db.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * A convenient class to parse the storage properties file.
 *
 * @author Yossi Shaul
 */
public class StorageProperties {
    protected static final int DEFAULT_MAX_ACTIVE_CONNECTIONS = 100;
    protected static final int DEFAULT_MAX_IDLE_CONNECTIONS = 10;
    private static final Logger log = LoggerFactory.getLogger(StorageProperties.class);
    private static final String DEFAULT_MAX_CACHE_SIZE = "5GB";

    private Properties props = null;
    private DbType dbType = null;

    public StorageProperties(File storagePropsFile) throws IOException {
        props = new Properties();
        try (FileInputStream pis = new FileInputStream(storagePropsFile)) {
            props.load(pis);
        }

        assertMandatoryProperties();

        // cache commonly used properties
        dbType = DbType.parse(getProperty(Key.type));

        // verify that the database is supported (will throw an exception if not found)
        log.debug("Loaded storage properties for supported database type: {}", getDbType());
    }

    public StorageProperties(File storagePropsFile, boolean useLinkedProperties) throws IOException {
        if (useLinkedProperties) {
            props = new LinkedProperties();
        } else {
            props = new Properties();
        }
        try (FileInputStream pis = new FileInputStream(storagePropsFile)) {
            props.load(pis);
        }
        assertMandatoryProperties();

        // cache commonly used properties
        dbType = DbType.parse(getProperty(Key.type));

        // verify that the database is supported (will throw an exception if not found)
        log.debug("Loaded storage properties for supported database type: {}", getDbType());
    }


    /**
     * update storage properties file;
     * @param updateStoragePropFile
     * @throws IOException
     */
    public void updateStoragePropertiesFile(File updateStoragePropFile) throws IOException {
        if (props != null) {
                OutputStream outputStream = new FileOutputStream(updateStoragePropFile);
                props.store(outputStream, "");
        }
    }

    public DbType getDbType() {
        return dbType;
    }

    public String getConnectionUrl() {
        return getProperty(Key.url);
    }

    /**
     * Update the connection URL property (should only be called for derby when the url contains place holders)
     *
     * @param connectionUrl The new connection URL
     */
    public void setConnectionUrl(String connectionUrl) {
        props.setProperty(Key.url.key, connectionUrl);
    }

    public String getDriverClass() {
        return getProperty(Key.driver);
    }

    public String getUsername() {
        return getProperty(Key.username);
    }

    public String getPassword() {
        String password = getProperty(Key.password);
        password = CryptoHelper.decryptIfNeeded(password);
        return password;
    }

    public void setPassword(String updatedPassword) {
        props.setProperty(Key.password.key, updatedPassword);
    }

    public int getMaxActiveConnections() {
        return Integer.parseInt(getProperty(Key.maxActiveConnections, DEFAULT_MAX_ACTIVE_CONNECTIONS + "").trim());
    }

    public int getMaxIdleConnections() {
        return Integer.parseInt(getProperty(Key.maxIdleConnections, DEFAULT_MAX_IDLE_CONNECTIONS + "").trim());
    }

    @Nonnull
    public BinaryProviderType getBinariesStorageType() {
        return BinaryProviderType.valueOf(
                getProperty(Key.binaryProviderType, BinaryProviderType.filesystem.name()).trim());
    }

    public String getS3BucketName() {
        return getProperty(Key.binaryProviderS3BucketName, null);
    }

    public String getS3Region() {
        return getProperty(Key.binaryProviderS3region, null);
    }

    public int getS3MaxRetriesNumber() {
        return getIntProperty(Key.binaryProviderS3MaxRetryNumber.key, 5);
    }

    public String getS3BucketPath() {
        return getProperty(Key.binaryProviderS3BucketPath, null);
    }

    public String getS3Credential() {
        return getProperty(Key.binaryProviderS3Credential, null);
    }

    public String getS3Identity() {
        return getProperty(Key.binaryProviderS3Identity, null);
    }

    public String getBinaryProviderExternalDir() {
        return getProperty(Key.binaryProviderExternalDir);
    }

    public String getBinaryProviderExternalMode() {
        return getProperty(Key.binaryProviderExternalMode);
    }

    public String getBinaryProviderFilesystemSecondDir() {
        return getProperty(Key.binaryProviderFilesystemSecondDir);
    }

    public long getBinaryProviderCacheMaxSize() {
        return StorageUnit.fromReadableString(getProperty(Key.binaryProviderCacheMaxSize, DEFAULT_MAX_CACHE_SIZE));
    }

    public String getProperty(Key property) {
        return props.getProperty(property.key);
    }

    public String getProperty(Key property, String defaultValue) {
        return  props.getProperty(property.key, defaultValue);
    }

    public String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(props.getProperty(key, defaultValue + "").trim());
    }

    public int getIntProperty(String key, int defaultValue) {
        return Integer.parseInt(props.getProperty(key, defaultValue + "").trim());
    }

    public long getLongProperty(String key, long defaultValue) {
        return Long.parseLong(props.getProperty(key, defaultValue + "").trim());
    }

    private void assertMandatoryProperties() {
        Key[] mandatory = {Key.type, Key.url, Key.driver};
        for (Key mandatoryProperty : mandatory) {
            String value = getProperty(mandatoryProperty);
            if (StringUtils.isBlank(value)) {
                throw new IllegalStateException("Mandatory storage property '" + mandatoryProperty + "' doesn't exist");
            }
        }
    }

    public boolean isDerby() {
        return dbType == DbType.DERBY;
    }

    public boolean isPostgres() {
        return dbType == DbType.POSTGRESQL;
    }

    public enum Key {
        username, password, type, url, driver,
        maxActiveConnections("pool.max.active"), maxIdleConnections("pool.max.idle"),
        binaryProviderType("binary.provider.type"),  // see BinaryProviderType
        binaryProviderCacheMaxSize("binary.provider.cache.maxSize"),
        binaryProviderCacheDir("binary.provider.cache.dir"),
        binaryProviderFilesystemDir("binary.provider.filesystem.dir"),
        binaryProviderFilesystemSecondDir("binary.provider.filesystem2.dir"),
        binaryProviderFilesystemSecondCheckPeriod("binary.provider.filesystem2.checkPeriod"),
        binaryProviderExternalDir("binary.provider.external.dir"),
        binaryProviderS3BucketName("binary.provider.s3.bucket.name"),
        binaryProviderS3region("binary.provider.s3.region"),
        binaryProviderS3BucketPath("binary.provider.s3.bucket.path"),
        binaryProviderExternalMode("binary.provider.external.mode"),
        binaryProviderS3Identity("binary.provider.s3.identity"),
        binaryProviderS3Credential("binary.provider.s3.credential"),
        binaryProviderS3MaxRetryNumber("binary.provider.s3.max.retry.number");


        private final String key;

        private Key() {
            this.key = name();
        }

        private Key(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum BinaryProviderType {
        filesystem, // binaries are stored in the filesystem
        fullDb,     // binaries are stored as blobs in the db, filesystem is used for caching unless cache size is 0
        cachedFS,   // binaries are stored in the filesystem, but a front cache (faster access) is added
        S3JClouds,         // binaries are stored in S3 JClouds API
        S3Amazon,          // binaries are stored in S3 amazon API
        JetS3,             // binaries are stored in S3 JetS3 API
    }
}
