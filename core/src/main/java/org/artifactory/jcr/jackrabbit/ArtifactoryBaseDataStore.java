/*
 * This file is part of Artifactory.
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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.data.db.DbDataStore;
import org.apache.jackrabbit.core.data.db.TempFileInputStream;
import org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager;
import org.apache.jackrabbit.core.persistence.bundle.util.TrackingInputStream;
import org.apache.jackrabbit.util.Text;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A data store implementation that stores the records in a database using JDBC.
 * <p/>
 * Configuration:<br> &lt;param name="{@link #setUrl(String) url}" value="jdbc:postgresql:test"/><br/> &lt;param
 * name="{@link #setUser(String) user}" value="sa"/><br/> &lt;param name="{@link #setPassword(String) password}"
 * value="sa"/><br/> &lt;param name="{@link #setDatabaseType(String) databaseType}" value="postgresql"/><br/> &lt;param
 * name="{@link #setDriver(String) driver}" value="org.postgresql.Driver"/><br/> &lt;param name="{@link
 * #setMaxConnections(int) maxConnections}" value="3"/><br/> &lt;param name="{@link #setMinRecordLength(int)
 * minRecordLength}" value="100"/><br/> &lt;param name="{@link #setCopyWhenReading(boolean) copyWhenReading}"
 * value="deprecated"/><br/>
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
public abstract class ArtifactoryBaseDataStore implements GenericDbDataStore {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryBaseDataStore.class);

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

    /**
     * All data identifiers that are currently in use are in this set until they are garbage collected.
     */
    private final ConcurrentMap<String, ArtifactoryDbDataRecord> allEntries
            = new ConcurrentHashMap<String, ArtifactoryDbDataRecord>();

    private AtomicLong dataStoreSize = new AtomicLong();

    private File tmpDir;

    /**
     * {@inheritDoc}
     */
    public DataRecord addRecord(InputStream stream) throws DataStoreException {
        ArtifactoryDbDataRecord dataRecord = null;
        File tempFile = null;
        try {
            // First create the temp file with checksum digest
            MessageDigest digest = getDigest();
            DigestInputStream dIn = new DigestInputStream(stream, digest);
            TrackingInputStream in = new TrackingInputStream(dIn);
            tempFile = moveToTempFile(in);

            // Then create the new DB record
            long now = System.currentTimeMillis();
            DataIdentifier identifier = new DataIdentifier(digest.digest());
            String id = identifier.toString();
            dataRecord = new ArtifactoryDbDataRecord(this, identifier, tempFile.length(), now);

            boolean isNew;
            // Find if an entry already exists with this checksum
            // Important: Only use putIfAbsent and never re-put the entry in it for concurrency control
            ArtifactoryDbDataRecord oldRecord = allEntries.putIfAbsent(id, dataRecord);
            if (oldRecord != null) {
                // Data record cannot be the new one created
                long length = dataRecord.length;
                dataRecord = null;

                // First check for improbable checksum collision
                long oldLength = oldRecord.length;
                if (oldLength != length) {
                    String msg = DIGEST + " collision: id=" + id + " length=" + length +
                            " oldLength=" + oldLength;
                    log.error(msg);
                    throw new DataStoreException(msg);
                }
                isNew = oldRecord.needsReinsert(now, tempFile);
                if (!isNew) {
                    oldRecord.markAccessed();
                    updateLastModifiedDate(oldRecord.getIdentifier().toString(), now);
                    return oldRecord;
                } else {
                    dataRecord = oldRecord;
                }
            }

            // Now inserting dataRecord to the DB
            insertRecordInDb(dataRecord, now, tempFile);
            dataRecord.setFile(tempFile);
            // Data record successfully inserted
            if (!dataRecord.setInDb()) {
                throw new DataStoreException(
                        "Cannot insert new record " + dataRecord + " since the old one is in invalid state");
            }
            return dataRecord;
        } catch (Exception e) {
            if (dataRecord != null) {
                dataRecord.setInError(e);
            }
            throw convert("Can not insert new record", e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    log.error("Could not delete temp file " + tempFile.getAbsolutePath());
                }
            }
        }
    }

    private void insertRecordInDb(ArtifactoryDbDataRecord dataRecord, long now, File tempFile)
            throws DataStoreException {
        String id = dataRecord.getIdentifier().toString();
        ArtifactoryConnectionRecoveryManager conn = getConnection();
        ResultSet rs = null;
        InputStream fileStream = null;
        try {
            PreparedStatement prep = conn.executeStmt(selectMetaSQL, new Object[]{id});
            rs = prep.getResultSet();
            boolean lineExists = rs.next();
            conn.closeSilently(rs);
            if (!lineExists) {
                // Need to insert a new row
                conn.executeStmt(insertTempSQL, new Object[]{id,
                        dataRecord.length, dataRecord.getLastModified()});
            } else {
                // TODO: Check the length should be the same, and stop updating the BLOB
                // Update the time stamp of last modified
                conn.executeStmt(updateLastModifiedSQL, new Object[]{now, id, now});
            }
            // Add the BLOB only if not full File System store
            if (saveBinariesAsBlobs()) {
                fileStream = new FileInputStream(tempFile);
                ArtifactoryConnectionRecoveryManager.StreamWrapper wrapper =
                        new ArtifactoryConnectionRecoveryManager.StreamWrapper(fileStream, dataRecord.length);
                // UPDATE DATASTORE SET DATA=? WHERE ID=?
                conn.executeStmt(updateDataSQL, new Object[]{wrapper, id});
            }
        } catch (Exception e) {
            throw convert("Can not insert new record", e);
        } finally {
            IOUtils.closeQuietly(fileStream);
            conn.closeSilently(rs);
            putBack(conn);
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
        File temp = File.createTempFile("dbRecord", null, tmpDir);
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
        for (ArtifactoryDbDataRecord record : getAllEntries()) {
            if (record.markForDeletion(now)) {
                try {
                    result[1] += deleteEntry(record);
                    result[0]++;
                } catch (DataStoreException e) {
                    log.warn("Error during deletion: " + e.getMessage());
                }
            }
        }
        dataStoreSize.getAndAdd(-result[1]);
        return result;
    }

    protected Collection<ArtifactoryDbDataRecord> getAllEntries() {
        return allEntries.values();
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
                // Mark as deleted anyway since no SQL exception means no entry
                record.setDeleted();
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
                DataIdentifier identifier = new DataIdentifier(id);
                list.add(identifier);
                totalSize += rs.getLong(2);
            }
            dataStoreSize.set(totalSize);
            return list.iterator();
        } catch (Exception e) {
            throw convert("Cannot read records", e);
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

                ArtifactoryDbDataRecord record = getFromAllEntries(id);
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
            dataStoreSize.set(totalSize);
        } catch (Exception e) {
            throw convert("Can not read records", e);
        } finally {
            conn.closeSilently(rs);
            putBack(conn);
        }
    }

    protected ArtifactoryDbDataRecord getFromAllEntries(String id) {
        return allEntries.get(id);
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

    protected abstract boolean saveBinariesAsBlobs();

    /**
     * {@inheritDoc}
     */
    public DataRecord getRecord(DataIdentifier identifier) throws DataStoreException {
        String id = identifier.toString();
        ArtifactoryDbDataRecord record = getFromAllEntries(id);
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
            initTmpDir(homeDir);
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

    private void initTmpDir(String homeDir) throws DataStoreException {
        tmpDir = new File(homeDir, "tmp/prefilestore");
        if (!tmpDir.exists()) {
            if (!tmpDir.mkdirs()) {
                throw new DataStoreException("Could not create temporary pre store folder " + tmpDir.getAbsolutePath());
            }
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
            Iterator<ArtifactoryDbDataRecord> recordIterator = getAllEntries().iterator();
            while (recordIterator.hasNext()) {
                ArtifactoryDbDataRecord record = recordIterator.next();
                if (!validState(record) || !record.initGCState()) {
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

    protected abstract boolean validState(ArtifactoryDbDataRecord record);

    public long getDataStoreSize() {
        return dataStoreSize.get();
    }

    public int getDataStoreNbElements() {
        return allEntries.size();
    }

    /**
     * Returns the identified file. This method implements the pattern used to avoid problems with too many files in a
     * single directory.
     * <p/>
     * No sanity checks are performed on the given identifier.
     *
     * @param identifier data identifier
     * @return identified file
     */
    public File getFile(DataIdentifier identifier) {
        String string = identifier.toString();
        File file = getBinariesFolder();
        file = new File(file, string.substring(0, 2));
        file = new File(file, string.substring(2, 4));
        file = new File(file, string.substring(4, 6));
        return new File(file, string);
    }

    /**
     * {@inheritDoc}
     */
    public abstract File getOrCreateFile(DataIdentifier identifier, long expectedLength) throws DataStoreException;

    public abstract File getBinariesFolder();

    private void updateLastModifiedDate(String id, long now) throws DataStoreException {
        Long n = now;
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
    public synchronized void close() throws DataStoreException {
        //If derby, prepare connection url for issuing shutdown command
        String derbyShutdownUrl = getDerbyShutdownUrl();

        //Close all connections in pool
        List<ArtifactoryConnectionRecoveryManager> list = connectionPool.getAll();
        for (ArtifactoryConnectionRecoveryManager conn : list) {
            conn.close();
        }
        list.clear();

        if (derbyShutdownUrl != null) {
            //If derby - shutdown the embedded Derby database
            try {
                DriverManager.getConnection(derbyShutdownUrl);
            } catch (SQLException e) {
                //A shutdown command always raises a SQLNonTransientConnectionException
                log.info(e.getMessage());
            }
        }
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

    public long getStorageSize() throws RepositoryException {
        return DataStoreHelper.calcStorageSize(createNewConnection());
    }

    /**
     * Is a stream copied to a temporary file before returning?
     *
     * @return the setting
     */
    public boolean getCopyWhenReading() {
        log.warn("Parameter copyWhenReading is deprecated and so unused here!");
        return true;
    }

    /**
     * The the copy setting. If enabled, a stream is always copied to a temporary file when reading a stream.
     *
     * @param copyWhenReading the new setting
     */
    public void setCopyWhenReading(boolean copyWhenReading) {
        log.warn("Parameter copyWhenReading is deprecated and so unused here!");
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

    private String getDerbyShutdownUrl() {
        String derbyShutdownUrl = null;
        if (DerbyPersistenceManager.DERBY_EMBEDDED_DRIVER.equals(driver)) {
            try {
                derbyShutdownUrl = getConnection().getConnection().getMetaData().getURL();
            } catch (Exception e) {
                log.warn("Cannot get Derby's shutdown URL: ", e.getMessage());
                return null;
            }
            int pos = derbyShutdownUrl.lastIndexOf(';');
            if (pos != -1) {
                // strip any attributes from connection url
                derbyShutdownUrl = derbyShutdownUrl.substring(0, pos);
            }
            derbyShutdownUrl += ";shutdown=true";
        }
        return derbyShutdownUrl;
    }
}
