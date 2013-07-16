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
import org.artifactory.storage.db.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A convenient class to parse the storage properties file.
 *
 * @author Yossi Shaul
 */
public class StorageProperties {
    private static final Logger log = LoggerFactory.getLogger(StorageProperties.class);

    protected final static int DEFAULT_MAX_ACTIVE_CONNECTIONS = 100;
    protected final static int DEFAULT_MAX_IDLE_CONNECTIONS = 10;
    private final static String DEFAULT_MAX_CACHE_SIZE = "5GB";

    private final Properties props;
    private final DbType dbType;

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

    public DbType getDbType() {
        return dbType;
    }

    public String getConnectionUrl() {
        return getProperty(Key.url);
    }

    public String getDriverClass() {
        return getProperty(Key.driver);
    }

    public String getUsername() {
        return getProperty(Key.username);
    }

    public String getPassword() {
        return getProperty(Key.password);
    }

    public int getMaxActiveConnections() {
        return Integer.parseInt(getProperty(Key.maxActiveConnections, DEFAULT_MAX_ACTIVE_CONNECTIONS + ""));
    }

    public int getMaxIdleConnections() {
        return Integer.parseInt(getProperty(Key.maxIdleConnections, DEFAULT_MAX_IDLE_CONNECTIONS + ""));
    }

    @Nonnull
    public BinaryStorageType getBinariesStorageType() {
        return BinaryStorageType.valueOf(getProperty(Key.binaryProviderType, BinaryStorageType.filesystem.name()));
    }

    @Nullable
    public String getBinaryProviderDir() {
        return getProperty(Key.binaryProviderFilesystemDir);
    }

    public String getBinaryProviderExternalDir() {
        return getProperty(Key.binaryProviderExternalDir);
    }

    public String getBinaryProviderExternalMode() {
        return getProperty(Key.binaryProviderExternalMode);
    }

    public long getBinaryProviderCacheMaxSize() {
        return StorageUnit.fromReadableString(getProperty(Key.binaryProviderCacheMaxSize, DEFAULT_MAX_CACHE_SIZE));
    }

    /**
     * Update the connection URL property (should only be called for derby when the url contains place holders)
     *
     * @param connectionUrl The new connection URL
     */
    public void setConnectionUrl(String connectionUrl) {
        props.setProperty("url", connectionUrl);
    }

    private String getProperty(Key property) {
        return props.getProperty(property.key);
    }

    private String getProperty(Key property, String defaultValue) {
        return props.getProperty(property.key, defaultValue);
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
        binaryProviderType("binary.provider.type"),  // see BinaryStorageType
        binaryProviderCacheMaxSize("binary.provider.cache.maxSize"),
        binaryProviderFilesystemDir("binary.provider.filesystem.dir"),
        binaryProviderExternalDir("binary.provider.external.dir"),
        binaryProviderExternalMode("binary.provider.external.mode");

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

    public enum BinaryStorageType {
        filesystem, // binaries are stored in the filesystem
        fullDb,    // binaries are stored as blobs in the db, filesystem is used for caching unless cache size is 0
    }
}
