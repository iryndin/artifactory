/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.util.db.ConnectionFactory;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.apache.jackrabbit.util.Text;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author Yoav Landman
 */
public abstract class ExtendedDbDataStoreBase implements ExtendedDbDataStore {

    private static final Logger log = LoggerFactory.getLogger(ExtendedDbDataStoreBase.class);

    /**
     * This is the property 'table' in the [databaseType].properties file, initialized with the default value.
     */
    protected String tableSQL = "DATASTORE";
    /**
     * The prefix for the datastore table, empty by default.
     */
    protected String tablePrefix = "";
    /**
     * The prefix of the table names. By default it is empty.
     */
    protected String schemaObjectPrefix = "";

    /**
     * This is the property 'createTable' in the [databaseType].properties file, initialized with the default value.
     */
    protected String createTableSQL =
            "CREATE TABLE ${tablePrefix}${table}(ID VARCHAR(255) PRIMARY KEY, LENGTH BIGINT, LAST_MODIFIED BIGINT, DATA BLOB)";

    /**
     * This is the property 'insertTemp' in the [databaseType].properties file, initialized with the default value.
     */
    protected String insertTempSQL =
            "INSERT INTO ${tablePrefix}${table} VALUES(?, ?, ?, NULL)";

    /**
     * This is the property 'updateData' in the [databaseType].properties file, initialized with the default value.
     */
    protected String updateDataSQL =
            "UPDATE ${tablePrefix}${table} SET DATA=? WHERE ID=?";

    /**
     * This is the property 'updateLastModified' in the [databaseType].properties file, initialized with the default
     * value.
     */
    protected String updateLastModifiedSQL =
            "UPDATE ${tablePrefix}${table} SET LAST_MODIFIED=? WHERE ID=? AND LAST_MODIFIED<?";

    /**
     * This is the property 'update' in the [databaseType].properties file, initialized with the default value.
     */
    protected String updateSQL =
            "UPDATE ${tablePrefix}${table} SET ID=?, LENGTH=?, LAST_MODIFIED=? WHERE ID=? AND NOT EXISTS(SELECT ID FROM ${tablePrefix}${table} WHERE ID=?)";

    /**
     * This is the property 'delete' in the [databaseType].properties file, initialized with the default value.
     */
    protected String deleteSQL =
            "DELETE FROM ${tablePrefix}${table} WHERE ID=?";

    /**
     * This is the property 'selectMeta' in the [databaseType].properties file, initialized with the default value.
     */
    protected String selectMetaSQL =
            "SELECT LENGTH, LAST_MODIFIED FROM ${tablePrefix}${table} WHERE ID=?";

    /**
     * This is the property 'selectAll' in the [databaseType].properties file, initialized with the default value.
     */
    protected String selectAllSQL =
            "SELECT ID, LENGTH, LAST_MODIFIED FROM ${tablePrefix}${table}";

    /**
     * This is the property 'selectData' in the [databaseType].properties file, initialized with the default value.
     */
    protected String selectDataSQL =
            "SELECT ID, DATA FROM ${tablePrefix}${table} WHERE ID=?";

    protected String selectDbStoreSizeSQL = "select SUM(LENGTH) from ${tablePrefix}${table}";
    /**
     * The database driver.
     */
    protected String driver;
    /**
     * The database type used.
     */
    protected String databaseType;
    /**
     * The repositories {@link org.apache.jackrabbit.core.util.db.ConnectionFactory}.
     */
    protected ConnectionFactory connectionFactory;
    /**
     * The logical name of the DataSource to use.
     */
    protected String dataSourceName;
    /**
     * The database URL used.
     */
    protected String url;


    protected void initDatabaseType() throws DataStoreException {
        boolean failIfNotFound = false;
        if (databaseType == null) {
            if (dataSourceName != null) {
                try {
                    databaseType = connectionFactory.getDataBaseType(dataSourceName);
                } catch (RepositoryException e) {
                    throw new DataStoreException(e);
                }
            } else {
                if (!url.startsWith("jdbc:")) {
                    return;
                }
                int start = "jdbc:".length();
                int end = url.indexOf(':', start);
                databaseType = url.substring(start, end);
            }
        } else {
            failIfNotFound = true;
        }

        InputStream in = DbDataStore.class.getResourceAsStream(databaseType + ".properties");
        if (in == null) {
            if (failIfNotFound) {
                String msg = "Configuration error: The resource '" + databaseType
                        + ".properties' could not be found; Please verify the databaseType property";
                log.debug(msg);
                throw new DataStoreException(msg);
            } else {
                return;
            }
        }
        Properties prop = new Properties();
        try {
            try {
                prop.load(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            String msg = "Configuration error: Could not read properties '" + databaseType + ".properties'";
            log.debug(msg);
            throw new DataStoreException(msg, e);
        }
        if (driver == null) {
            driver = getProperty(prop, "driver", driver);
        }
        tableSQL = getProperty(prop, "table", tableSQL);
        createTableSQL = getProperty(prop, "createTable", createTableSQL);
        insertTempSQL = getProperty(prop, "insertTemp", insertTempSQL);
        updateDataSQL = getProperty(prop, "updateData", updateDataSQL);
        updateLastModifiedSQL = getProperty(prop, "updateLastModified", updateLastModifiedSQL);
        updateSQL = getProperty(prop, "update", updateSQL);
        deleteSQL = getProperty(prop, "delete", deleteSQL);
        selectMetaSQL = getProperty(prop, "selectMeta", selectMetaSQL);
        selectAllSQL = getProperty(prop, "selectAll", selectAllSQL);
        selectDataSQL = getProperty(prop, "selectData", selectDataSQL);
        selectDbStoreSizeSQL = getProperty(prop, "selectDbStoreSize", selectDbStoreSizeSQL);
    }

    public long getStorageSize() throws RepositoryException {
        long totalSize = 0L;
        ResultSet rs = null;
        try {
            rs = getConnectionHelper().select(selectDbStoreSizeSQL);
            if (rs.next()) {
                totalSize = rs.getLong(1);
            }
            log.debug("Found total size of {} bytes.", totalSize);
        } catch (SQLException e) {
            throw new RepositoryRuntimeException("Could not calculate storage size.", e);
        } finally {
            DbUtility.close(rs);
        }
        return totalSize;
    }

    /**
     * Get the expanded property value. The following placeholders are supported: ${table}: the table name (the default
     * is DATASTORE) and ${tablePrefix}: tablePrefix plus schemaObjectPrefix as set in the configuration
     *
     * @param prop         the properties object
     * @param key          the key
     * @param defaultValue the default value
     * @return the property value (placeholders are replaced)
     */
    protected String getProperty(Properties prop, String key, String defaultValue) {
        String sql = prop.getProperty(key, defaultValue);
        sql = Text.replace(sql, "${table}", tableSQL).trim();
        sql = Text.replace(sql, "${tablePrefix}", tablePrefix + schemaObjectPrefix).trim();
        return sql;
    }

    /**
     * {@inheritDoc}
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Get the database type (if set).
     *
     * @return the database type
     */
    public String getDatabaseType() {
        return databaseType;
    }

    public String getDataStoreTableName() {
        return tableSQL.trim();
    }

    public String getDataStoreTablePrefix() {
        return (tablePrefix + schemaObjectPrefix).trim();
    }

    /**
     * Set the database type. By default the sub-protocol of the JDBC database URL is used if it is not set. It must
     * match the resource file [databaseType].properties. Example: mysql.
     *
     * @param databaseType
     */
    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    /**
     * Get the database driver
     *
     * @return the driver
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Set the database driver class name. If not set, the default driver class name for the database type is used, as
     * set in the [databaseType].properties resource; key 'driver'.
     *
     * @param driver
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Get the database URL.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the database URL. Example: jdbc:postgresql:test
     *
     * @param url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }
}