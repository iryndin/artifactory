/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.data.db.TempFileInputStream;
import org.apache.jackrabbit.core.persistence.bundle.util.TrackingInputStream;
import org.apache.jackrabbit.util.Text;
import org.artifactory.common.ConstantsValue;
import org.artifactory.update.jcr.ArtifactoryDbDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A data store implementation that stores the records in a database using JDBC.
 * <p/>
 * Configuration:<br> <ul> <li>&lt;param name="className" value="org.apache.jackrabbit.core.data.db.DbDataStore"/>
 * <li>&lt;param name="{@link #setUrl(String) url}" value="jdbc:postgresql:test"/> <li>&lt;param name="{@link
 * #setUser(String) user}" value="sa"/> <li>&lt;param name="{@link #setPassword(String) password}" value="sa"/>
 * <li>&lt;param name="{@link #setDatabaseType(String) databaseType}" value="postgresql"/> <li>&lt;param name="{@link
 * #setDriver(String) driver}" value="org.postgresql.Driver"/> <li>&lt;param name="{@link #setMinRecordLength(int)
 * minRecordLength}" value="1024"/> <li>&lt;param name="{@link #setMaxConnections(int) maxConnections}" value="2"/>
 * <li>&lt;param name="{@link #setCopyWhenReading(boolean) copyWhenReading}" value="true"/> </ul>
 * <p/>
 * <p/>
 * Only URL, user name and password usually need to be set. The remaining settings are generated using the database URL
 * sub-protocol from the database type resource file.
 * <p/>
 * A three level directory structure is used to avoid placing too many files in a single directory. The chosen structure
 * is designed to scale up to billions of distinct records.
 * <p/>
 * For Microsoft SQL Server 2005, there is a problem reading large BLOBs. You will need to use the JDBC driver version
 * 1.2 or newer, and append ;responseBuffering=adaptive to the database URL. Don't append ;selectMethod=cursor,
 * otherwise it can still run out of memory. Example database URL: jdbc:sqlserver://localhost:4220;DatabaseName=test;responseBuffering=adaptive
 * <p/>
 * By default, the data is copied to a temp file when reading, to avoid problems when reading multiple blobs at the same
 * time.
 * <p/>
 * JFrog Ltd modifications for better concurrency and garbage collection.<br/> Garbage collection is done the opposite
 * way:<br/> <ol><li>First collect all data identifier present in the data store into a toRemove concurrent map.</li>
 * <li>Then activate the scanning of the different persistence manager that will call touch.</li> <li>Touch will remove
 * the entry from the toRemove concurrent map.</li> <li>After scanning bulk deletes from the remaining data identifier
 * in the toRemove map will be send to the DB. The default is 20 lines at a time. It can be tune depending on your DB
 * rollback segment size.</li></ol>
 *
 * @author freds
 * @date Mar 12, 2009
 */
public class ArtifactoryDbDataStoreImpl implements ArtifactoryDbDataStore {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryDbDataStoreImpl.class);

    /**
     * The digest algorithm used to uniquely identify records.
     */
    protected static final String DIGEST = "SHA-1";

    /**
     * The default value for the minimum object size.
     */
    public static final int DEFAULT_MIN_RECORD_LENGTH = 100;

    /**
     * The default value for the maximum connections.
     */
    public static final int DEFAULT_MAX_CONNECTIONS = 3;

    /**
     * The maximum number of rows deleted in one SQL query.
     */
    protected int batchDeleteSize = ConstantsValue.gcBatchDeleteMaxSize.getInt();

    /**
     * The database URL used.
     */
    protected String url;

    /**
     * The database driver.
     */
    protected String driver;

    /**
     * The user name.
     */
    protected String user;

    /**
     * The password
     */
    protected String password;

    /**
     * The database type used.
     */
    protected String databaseType;

    /**
     * The minimum size of an object that should be stored in this data store.
     */
    protected int minRecordLength = DEFAULT_MIN_RECORD_LENGTH;

    /**
     * The maximum number of open connections.
     */
    protected int maxConnections = DEFAULT_MAX_CONNECTIONS;

    /**
     * A list of connections
     */
    protected ArtifactoryPool connectionPool;

    /**
     * The prefix used for temporary objects.
     */
    protected static final String TEMP_PREFIX = "TEMP_";

    /**
     * The prefix for the datastore table, empty by default.
     */
    protected String tablePrefix = "";

    /**
     * This is the property 'table' in the [databaseType].properties file, initialized with the default value.
     */
    protected String tableSQL = "DATASTORE";

    /**
     * This is the property 'createTable' in the [databaseType].properties file, initialized with the default value.
     */
    protected String createTableSQL =
            "CREATE TABLE ${tablePrefix}${table}(ID VARCHAR(255) PRIMARY KEY, LENGTH BIGINT, LAST_MODIFIED BIGINT, DATA BLOB)";

    /**
     * This is the property 'insertTemp' in the [databaseType].properties file, initialized with the default value.
     */
    protected String insertTempSQL =
            "INSERT INTO ${tablePrefix}${table} VALUES(?, 0, ?, NULL)";

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

    /**
     * Copy the stream to a temp file before returning it. Enabled by default to support concurrent reads.
     */
    private boolean copyWhenReading = true;

    /**
     * All data identifiers that are currently in use are in this set until they are garbage collected.
     */
    private final ConcurrentMap<String, ArtifactoryDbDataRecord> allEntries
            = new ConcurrentHashMap<String, ArtifactoryDbDataRecord>();

    private long dataStoreSize;

    /**
     * {@inheritDoc}
     */
    public DataRecord addRecord(InputStream stream) throws DataStoreException {
        ResultSet rs = null;
        TempFileInputStream fileInput = null;
        ArtifactoryConnectionRecoveryManager conn = null;
        String id = null, tempId = null;
        ArtifactoryDbDataRecord dataRecord = null;
        try {
            // First create the temp file with checksum digest
            MessageDigest digest = getDigest();
            DigestInputStream dIn = new DigestInputStream(stream, digest);
            TrackingInputStream in = new TrackingInputStream(dIn);
            ArtifactoryConnectionRecoveryManager.StreamWrapper wrapper;
            File temp = moveToTempFile(in);
            fileInput = new TempFileInputStream(temp);

            // Then create the new DB record
            long now = System.currentTimeMillis();
            long length = in.getPosition();
            DataIdentifier identifier = new DataIdentifier(digest.digest());
            id = identifier.toString();
            dataRecord = new ArtifactoryDbDataRecord(this, identifier, length, now);

            boolean isNew = true;
            // Find if an entry already exists with this checksum
            // Important: Only use putIfAbsent and never re-put the entry in it for concurrency control
            ArtifactoryDbDataRecord oldRecord = allEntries.putIfAbsent(id, dataRecord);
            if (oldRecord != null) {
                // Data record cannot be the new one created
                dataRecord = null;

                // First check for improbable checksum collision
                long oldLength = oldRecord.length;
                if (oldLength != length) {
                    String msg = DIGEST + " collision: temp=" + tempId + " id=" + id + " length=" + length +
                            " oldLength=" + oldLength;
                    log.error(msg);
                    throw new DataStoreException(msg);
                }
                isNew = oldRecord.needReinsert(now);
                if (!isNew) {
                    updateLastModifiedDate(oldRecord.getIdentifier().toString(), now);
                    return oldRecord;
                } else {
                    dataRecord = oldRecord;
                }
            }

            // Now inserting dataRecord to the DB
            // TODO: remove all the temp id issue
            wrapper = new ArtifactoryConnectionRecoveryManager.StreamWrapper(fileInput, temp.length());
            conn = getConnection();
            for (int i = 0; i < ArtifactoryConnectionRecoveryManager.TRIALS; i++) {
                try {
                    now = System.currentTimeMillis();
                    tempId = TEMP_PREFIX + org.apache.jackrabbit.uuid.UUID.randomUUID().toString();
                    PreparedStatement prep = conn.executeStmt(selectMetaSQL, new Object[]{tempId});
                    rs = prep.getResultSet();
                    if (rs.next()) {
                        conn.closeSilently(rs);
                        // re-try in the very, very unlikely event that the row already exists
                        continue;
                    }
                    conn.executeStmt(insertTempSQL, new Object[]{tempId, new Long(now)});
                    break;
                } catch (Exception e) {
                    throw convert("Can not insert new record", e);
                } finally {
                    conn.closeSilently(rs);
                }
            }

            // UPDATE DATASTORE SET DATA=? WHERE ID=?
            conn.executeStmt(updateDataSQL, new Object[]{wrapper, tempId});
            // UPDATE DATASTORE SET ID=?, LENGTH=?, LAST_MODIFIED=?
            // WHERE ID=?
            // AND NOT EXISTS(SELECT ID FROM DATASTORE WHERE ID=?)
            PreparedStatement prep = conn.executeStmt(updateSQL, new Object[]{
                    id, new Long(length), new Long(now),
                    tempId, id});
            int count = prep.getUpdateCount();
            if (count == 0) {
                log.info("Created the entry " + tempId + " for id=" + id + " but already exists. Deleting temp entry.");
                // update count is 0, meaning such a row already exists
                // DELETE FROM DATASTORE WHERE ID=?
                conn.executeStmt(deleteSQL, new Object[]{tempId});
                // SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID=?
                prep = conn.executeStmt(selectMetaSQL, new Object[]{id});
                rs = prep.getResultSet();
                if (rs.next()) {
                    long oldLength = rs.getLong(1);
                    long lastModified = rs.getLong(2);
                    if (oldLength != length) {
                        String msg = DIGEST + " collision: temp=" + tempId + " id=" + id + " length=" + length +
                                " oldLength=" + oldLength;
                        log.error(msg);
                        throw new DataStoreException(msg);
                    }
                }
            }

            // Data record succesfully inserted
            if (!dataRecord.setInDb()) {
                throw new DataStoreException(
                        "Cannot insert new record " + dataRecord + " since the old one is in invalid state");
            }
            return dataRecord;
        } catch (Exception e) {
            //Try to delete an entry that wasn't updated successfully
            if (conn != null) {
                deleteTempEntry(conn, tempId);
                deleteTempEntry(conn, id);
            }
            if (dataRecord != null) {
                dataRecord.setInError(e);
            }
            throw convert("Can not insert new record", e);
        } finally {
            if (conn != null) {
                conn.closeSilently(rs);
                putBack(conn);
            }
            if (fileInput != null) {
                try {
                    fileInput.close();
                } catch (IOException e) {
                    throw convert("Can not close temporary file", e);
                }
            }
        }
    }

    private void deleteTempEntry(ArtifactoryConnectionRecoveryManager conn, String id) {
        log.debug("Trying to delete partially created datastore entry with id {}.", id);
        if (id != null) {
            try {
                int count = conn.executeStmt(deleteSQL, new Object[]{id}).getUpdateCount();
                if (count == 0) {
                    log.debug("No partially created datastore entry {} was deleted.", id);
                }
            } catch (Exception e) {
                log.error("Could not delete partially created datastore entry {}: {}.", id, e.getMessage());
            }
        }
    }

    /**
     * Creates a temp file and copies the data there. The input stream is closed afterwards.
     *
     * @param in the input stream
     * @return the file
     * @throws IOException
     */
    private File moveToTempFile(InputStream in) throws IOException {
        File temp = File.createTempFile("dbRecord", null);
        TempFileInputStream.writeToFileAndClose(in, temp);
        return temp;
    }

    /**
     * {@inheritDoc}
     */
    public int deleteAllOlderThan(long min) throws DataStoreException {
        throw new UnsupportedOperationException("Delete by timestamp not supported");
    }

    /**
     * Delte all unused Db entry from datastore
     *
     * @return the amount of elements on pos 0, pos 1 = the amount of bytes removed from DB
     * @throws DataStoreException
     */
    public long[] cleanUnreferencedItems() throws DataStoreException {
        long now = System.currentTimeMillis();
        long[] result = new long[]{0L, 0L};
        for (ArtifactoryDbDataRecord record : allEntries.values()) {
            if (record.markForDeletion(now)) {
                try {
                    result[1] += deleteEntry(record);
                    result[0]++;
                } catch (DataStoreException e) {
                    log.warn("Error during deletion: " + e.getMessage());
                }
            }
        }
        dataStoreSize -= result[1];
        return result;
    }

    /**
     * Delete the record from the DB
     *
     * @param record to delete from DB
     * @return the amount of bytes deleted
     * @throws DataStoreException
     */
    private long deleteEntry(ArtifactoryDbDataRecord record) throws DataStoreException {
        ArtifactoryConnectionRecoveryManager conn = getConnection();
        try {
            long length = record.length;
            // DELETE FROM DATASTORE WHERE ID=? (toremove)
            PreparedStatement prep = conn.executeStmt(deleteSQL, new Object[]{record.getIdentifier().toString()});
            int res = prep.getUpdateCount();
            if (res != 1) {
                log.error("Deleting record " + record + " returned " + res + " updated.");
            } else {
                log.debug("Deleted record " + record + " from data store.");
                record.setDeleted();
                return length;
            }
        } catch (Exception e) {
            record.setInError(e);
            throw convert("Can not delete records", e);
        } finally {
            putBack(conn);
        }
        return 0L;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getAllIdentifiers() throws DataStoreException {
        long totalSize = 0L;
        ArtifactoryConnectionRecoveryManager conn = getConnection();
        List<DataIdentifier> list = new ArrayList<DataIdentifier>();
        ResultSet rs = null;
        try {
            // SELECT ID FROM DATASTORE
            PreparedStatement prep = conn.executeStmt(selectAllSQL, new Object[0]);
            rs = prep.getResultSet();
            while (rs.next()) {
                String id = rs.getString(1);
                if (!id.startsWith(TEMP_PREFIX)) {
                    DataIdentifier identifier = new DataIdentifier(id);
                    list.add(identifier);
                }
                totalSize += rs.getLong(2);
            }
            dataStoreSize = totalSize;
            return list.iterator();
        } catch (Exception e) {
            throw convert("Can not read records", e);
        } finally {
            conn.closeSilently(rs);
            putBack(conn);
        }
    }

    private void loadAllDbRecords() throws DataStoreException {
        long totalSize = 0L;
        ArtifactoryConnectionRecoveryManager conn = getConnection();
        ResultSet rs = null;
        try {
            // SELECT ID, LENGTH, LAST_MODIFIED FROM DATASTORE
            PreparedStatement prep = conn.executeStmt(selectAllSQL, new Object[0]);
            rs = prep.getResultSet();
            while (rs.next()) {
                String id = rs.getString(1);
                long length = rs.getLong(2);
                long lastModified = rs.getLong(3);
                if (!id.startsWith(TEMP_PREFIX)) {
                    ArtifactoryDbDataRecord record = allEntries.get(id);
                    if (record == null) {
                        ArtifactoryDbDataRecord dbRecord =
                                new ArtifactoryDbDataRecord(this, new DataIdentifier(id), length, lastModified);
                        record = allEntries.putIfAbsent(id, dbRecord);
                        if (record == null) {
                            record = dbRecord;
                        }
                    }
                    record.setLastModified(lastModified);
                    record.setInDb();
                    totalSize += length;
                }
            }
            dataStoreSize = totalSize;
        } catch (Exception e) {
            throw convert("Can not read records", e);
        } finally {
            conn.closeSilently(rs);
            putBack(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getMinRecordLength() {
        return minRecordLength;
    }

    /**
     * Set the minimum object length.
     *
     * @param minRecordLength the length
     */
    public void setMinRecordLength(int minRecordLength) {
        this.minRecordLength = minRecordLength;
    }

    /**
     * {@inheritDoc}
     */
    public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        String id = identifier.toString();
        ArtifactoryDbDataRecord record = allEntries.get(id);
        if (record != null && record.setInUse()) {
            // The record is OK return it
            return record;
        }
        ArtifactoryConnectionRecoveryManager conn = getConnection();
        ResultSet rs = null;
        try {
            //Long result = allEntries.get(id);
            // SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID = ?
            PreparedStatement prep = conn.executeStmt(selectMetaSQL, new Object[]{id});
            rs = prep.getResultSet();
            if (!rs.next()) {
                throw new DataStoreRecordNotFoundException("Record not found: " + identifier);
            }
            long length = rs.getLong(1);
            long lastModified = rs.getLong(2);
            ArtifactoryDbDataRecord dbRecord = new ArtifactoryDbDataRecord(this, identifier, length, lastModified);
            record = allEntries.putIfAbsent(id, dbRecord);
            if (record == null) {
                record = dbRecord;
            } else {
                record.setLastModified(lastModified);
            }
            record.setInDb();
            return record;
        } catch (Exception e) {
            if (record != null) {
                record.setInError(e);
            }
            throw convert("Can not read identifier " + identifier, e);
        } finally {
            conn.closeSilently(rs);
            putBack(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void init(String homeDir) throws DataStoreException {
        try {
            initDatabaseType();
            connectionPool = new ArtifactoryPool(this, maxConnections);
            ArtifactoryConnectionRecoveryManager conn = getConnection();
            DatabaseMetaData meta = conn.getConnection().getMetaData();
            log.info("Using JDBC driver " + meta.getDriverName() + " " + meta.getDriverVersion());
            meta.getDriverVersion();
            ResultSet rs = meta.getTables(null, null, tableSQL, null);
            boolean exists = rs.next();
            rs.close();
            if (!exists) {
                conn.executeStmt(createTableSQL, null);
            }
            putBack(conn);
        } catch (Exception e) {
            throw convert("Can not init data store, driver=" + driver + " url=" + url + " user=" + user, e);
        }
    }

    protected void initDatabaseType() throws DataStoreException {
        boolean failIfNotFound;
        if (databaseType == null) {
            if (!url.startsWith("jdbc:")) {
                return;
            }
            failIfNotFound = false;
            int start = "jdbc:".length();
            int end = url.indexOf(':', start);
            databaseType = url.substring(start, end);
        } else {
            failIfNotFound = true;
        }

        InputStream in = DbDataStore.class.getResourceAsStream(databaseType + ".properties");
        if (in == null) {
            if (failIfNotFound) {
                String msg = "Configuration error: The resource '" + databaseType +
                        ".properties' could not be found; Please verify the databaseType property";
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
            throw new DataStoreException(msg);
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
    }

    /**
     * Get the expanded property value. The following placeholders are supported: ${table}: the table name (the default
     * is DATASTORE) and ${tablePrefix}: the prefix as set in the configuration (empty by default).
     *
     * @param prop         the properties object
     * @param key          the key
     * @param defaultValue the default value
     * @return the property value (placeholders are replaced)
     */
    protected String getProperty(Properties prop, String key, String defaultValue) {
        String sql = prop.getProperty(key, defaultValue);
        sql = Text.replace(sql, "${table}", tableSQL).trim();
        sql = Text.replace(sql, "${tablePrefix}", tablePrefix).trim();
        return sql;
    }

    /**
     * Convert an exception to a data store exception.
     *
     * @param cause the message
     * @param e     the root cause
     * @return the data store exception
     */
    protected DataStoreException convert(String cause, Exception e) {
        if (log.isDebugEnabled()) {
            log.warn(cause, e);
        } else {
            log.warn(cause + ": " + e.getMessage());
        }
        if (e instanceof DataStoreException) {
            return (DataStoreException) e;
        } else {
            return new DataStoreException(cause, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateModifiedDateOnAccess(long before) {
        throw new UnsupportedOperationException("Modified on access forbidden");
    }

    public long scanDataStore() {
        long start = System.currentTimeMillis();
        log.debug("Starting scanning of all datastore entries");
        try {
            // Add all identifiers
            loadAllDbRecords();
            // Remove all entries in delete or error state
            Iterator<ArtifactoryDbDataRecord> recordIterator = allEntries.values().iterator();
            while (recordIterator.hasNext()) {
                ArtifactoryDbDataRecord record = recordIterator.next();
                if (!record.initGCState()) {
                    recordIterator.remove();
                }
            }
        } catch (DataStoreException e) {
            throw new RuntimeException("Could not load all data store identifier: " + e.getMessage(), e);
        }
        long dataStoreQueryTime = System.currentTimeMillis() - start;
        log.debug("Scanning data store of {} elements and {} bytes in {}ms", new Object[]{
                allEntries.size(), dataStoreSize, dataStoreQueryTime});
        return dataStoreQueryTime;
    }

    public long getDataStoreSize() {
        return dataStoreSize;
    }

    public int getDataStoreNbElements() {
        return allEntries.size();
    }

    private void updateLastModifiedDate(String id, long now) throws DataStoreException {
        Long n = new Long(now);
        ArtifactoryConnectionRecoveryManager conn = getConnection();
        try {
            // UPDATE DATASTORE SET LAST_MODIFIED = ? WHERE ID = ? AND LAST_MODIFIED < ?
            conn.executeStmt(updateLastModifiedSQL, new Object[]{
                    n, id, n
            });
        } catch (Exception e) {
            throw convert("Can not update lastModified", e);
        } finally {
            putBack(conn);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream(DataIdentifier identifier) throws DataStoreException {
        ArtifactoryConnectionRecoveryManager conn = getConnection();
        ResultSet rs = null;
        try {
            String id = identifier.toString();
            // SELECT ID, DATA FROM DATASTORE WHERE ID = ?
            PreparedStatement prep = conn.executeStmt(selectDataSQL, new Object[]{id});
            rs = prep.getResultSet();
            if (!rs.next()) {
                throw new DataStoreRecordNotFoundException("Record not found: " + identifier);
            }
            InputStream in = new BufferedInputStream(rs.getBinaryStream(2));
            if (copyWhenReading) {
                File temp = moveToTempFile(in);
                in = new TempFileInputStream(temp);
            }
            return in;
        } catch (Exception e) {
            throw convert("Can not read identifier " + identifier, e);
        } finally {
            if (copyWhenReading) {
                conn.closeSilently(rs);
            }
            putBack(conn);
        }
    }

    /**
     * Get the database type (if set).
     *
     * @return the database type
     */
    public String getDatabaseType() {
        return databaseType;
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
     * Get the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password.
     *
     * @param password
     */
    public void setPassword(String password) {
        this.password = password;
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

    /**
     * Get the user name.
     *
     * @return the user name
     */
    public String getUser() {
        return user;
    }

    /**
     * Set the user name.
     *
     * @param user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() {
        List<ArtifactoryConnectionRecoveryManager> list = connectionPool.getAll();
        for (ArtifactoryConnectionRecoveryManager conn : list) {
            conn.close();
        }
        list.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void clearInUse() {
    }

    protected synchronized MessageDigest getDigest() throws DataStoreException {
        try {
            // TODO: Create a pool
            return MessageDigest.getInstance(DIGEST);
        } catch (NoSuchAlgorithmException e) {
            throw convert("No such algorithm: " + DIGEST, e);
        }
    }

    protected ArtifactoryConnectionRecoveryManager getConnection() throws DataStoreException {
        try {
            ArtifactoryConnectionRecoveryManager conn = connectionPool.get();
            conn.setAutoReconnect(true);
            return conn;
        } catch (InterruptedException e) {
            throw new DataStoreException("Interrupted", e);
        } catch (RepositoryException e) {
            throw new DataStoreException("Can not open a new connection", e);
        }
    }

    protected void putBack(ArtifactoryConnectionRecoveryManager conn) throws DataStoreException {
        try {
            connectionPool.add(conn);
        } catch (InterruptedException e) {
            throw new DataStoreException("Interrupted", e);
        }
    }

    /**
     * Get the maximum number of concurrent connections.
     *
     * @return the maximum number of connections.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    /**
     * Set the maximum number of concurrent connections.
     *
     * @param maxConnections the new value
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Create a new connection.
     *
     * @return the new connection
     */
    public ArtifactoryConnectionRecoveryManager createNewConnection() throws RepositoryException {
        ArtifactoryConnectionRecoveryManager conn =
                new ArtifactoryConnectionRecoveryManager(false, driver, url, user, password);
        return conn;
    }

    /**
     * Is a stream copied to a temporary file before returning?
     *
     * @return the setting
     */
    public boolean getCopyWhenReading() {
        return copyWhenReading;
    }

    /**
     * The the copy setting. If enabled, a stream is always copied to a temporary file when reading a stream.
     *
     * @param copyWhenReading the new setting
     */
    public void setCopyWhenReading(boolean copyWhenReading) {
        this.copyWhenReading = copyWhenReading;
    }

    /**
     * Get the table prefix. The default is empty.
     *
     * @return the table prefix.
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    /**
     * Set the new table prefix.
     *
     * @param tablePrefix the new value
     */
    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public int getBatchDeleteSize() {
        return batchDeleteSize;
    }

    public void setBatchDeleteSize(int batchDeleteSize) {
        this.batchDeleteSize = batchDeleteSize;
    }
}
