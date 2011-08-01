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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataRecord;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.apache.jackrabbit.core.persistence.pool.DerbyPersistenceManager;
import org.apache.jackrabbit.core.util.db.ArtifactoryConnectionHelper;
import org.apache.jackrabbit.core.util.db.CheckSchemaOperation;
import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.DbUtility;
import org.apache.jackrabbit.core.util.db.StreamWrapper;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.common.ConstantValues;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.TaskInterruptedException;
import org.artifactory.schedule.TaskUtils;
import org.artifactory.util.FileUtils;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
public abstract class ArtifactoryBaseDataStore extends ExtendedDbDataStoreBase {
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
     * The user name.
     */
    protected String user;

    /**
     * The password
     */
    protected String password;

    /**
     * The minimum size of an object that should be stored in this data store.
     */
    protected int minRecordLength = DEFAULT_MIN_RECORD_LENGTH;

    /**
     * The maximum number of open connections.
     */
    protected int maxConnections = DEFAULT_MAX_CONNECTIONS;

    /**
     * Whether the schema check must be done during initialization.
     */
    private boolean schemaCheckEnabled = true;

    /**
     * All data identifiers that are currently in use are in this set until they are garbage collected.
     */
    private final ConcurrentMap<String, ArtifactoryDbDataRecord> allEntries =
            new ConcurrentHashMap<String, ArtifactoryDbDataRecord>();

    private AtomicLong dataStoreSize = new AtomicLong();

    private File tmpDir;

    /**
     * The {@link ConnectionHelper} set in the {@link #init(String)} method.
     */
    protected ArtifactoryConnectionHelper conHelper;

    private boolean slowdownScanning;
    private long slowdownScanningMillis = ConstantValues.gcFileScanSleepMillis.getLong();
    private long lastSlowdownScanning = 0;

    /**
     * {@inheritDoc}
     */
    public DataRecord addRecord(InputStream stream) throws DataStoreException {
        ArtifactoryDbDataRecord dataRecord = null;
        File tempFile = null;
        DigestInputStream digestStream = null;
        try {
            // First create the temp file with checksum digest
            MessageDigest digest = getDigest();
            digestStream = new DigestInputStream(stream, digest);
            //TODO: [by yl] For blob store - write a temp file directly to jcr and delete it from jcr if exists,
            //instead of using a real temp file as medium between the input stream and the jcr stream
            tempFile = moveToTempFile(digestStream);

            // Then create the new DB record
            long now = System.currentTimeMillis();
            DataIdentifier identifier = new DataIdentifier(digest.digest());
            String id = identifier.toString();
            log.trace("Datastore checksum: 'MD5:{}", id);
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
            IOUtils.closeQuietly(digestStream);
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
        ResultSet rs = null;
        InputStream fileStream = null;
        try {
            rs = conHelper.select(selectMetaSQL, new Object[]{id});
            boolean lineExists = rs.next();
            DbUtility.close(rs);
            if (!lineExists) {
                // Need to insert a new row
                conHelper.exec(insertTempSQL, id, dataRecord.length, dataRecord.getLastModified());
            } else {
                // TODO: Check the length should be the same, and stop updating the BLOB
                // Update the time stamp of last modified
                conHelper.exec(updateLastModifiedSQL, now, id, now);
            }
            // Add the BLOB only if not full File System store
            if (saveBinariesAsBlobs()) {
                fileStream = new FileInputStream(tempFile);
                StreamWrapper wrapper = new StreamWrapper(fileStream, dataRecord.length);
                // UPDATE DATASTORE SET DATA=? WHERE ID=?
                conHelper.exec(updateDataSQL, wrapper, id);
            }
        } catch (Exception e) {
            throw convert("Cannot insert new record", e);
        } finally {
            IOUtils.closeQuietly(fileStream);
            DbUtility.close(rs);
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
        FileUtils.writeToFileAndClose(in, temp);
        return temp;
    }

    /**
     * {@inheritDoc}
     */
    public int deleteAllOlderThan(long min) throws DataStoreException {
        throw new UnsupportedOperationException("Delete by timestamp not supported");
    }

    /**
     * Delete all unused Db entry from datastore
     *
     * @return the amount of elements on pos 0, pos 1 = the amount of bytes removed from DB
     * @throws DataStoreException
     */
    public long[] cleanUnreferencedItems() throws DataStoreException {
        long now = System.currentTimeMillis();
        long[] result = new long[]{0L, 0L};
        long recordsCounter = 0;
        for (ArtifactoryDbDataRecord record : getAllEntries()) {
            if ((++recordsCounter % 100L) == 0) {
                if (TaskUtils.pauseOrBreak()) {
                    throw new TaskInterruptedException();
                }
            }
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
    protected long deleteEntry(ArtifactoryDbDataRecord record) throws DataStoreException {
        try {
            long length = record.length;
            // DELETE FROM DATASTORE WHERE ID=? (toremove)
            int res = conHelper.update(deleteSQL, new Object[]{record.getIdentifier().toString()});
            record.setDeleted();
            allEntries.remove(record.getIdentifier().toString());
            if (res != 1) {
                log.error("Deleting record " + record + " returned " + res + " updated.");
                // Mark as deleted anyway since no SQL exception means no entry
                return 0L;
            } else {
                log.debug("Deleted record " + record + " from data store.");
                return length;
            }
        } catch (Exception e) {
            record.setInError(e);
            throw convert("Can not delete records", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DataIdentifier> getAllIdentifiers() throws DataStoreException {
        long totalSize = 0L;
        List<DataIdentifier> list = new ArrayList<DataIdentifier>();
        ResultSet rs = null;
        try {
            // SELECT ID FROM DATASTORE
            rs = conHelper.select(selectAllSQL);
            while (rs.next()) {
                String id = rs.getString(1);
                DataIdentifier identifier = new DataIdentifier(id);
                list.add(identifier);
                totalSize += rs.getLong(2);
            }
            dataStoreSize.set(totalSize);
            return list.iterator();
        } catch (Exception e) {
            throw convert("Can not read records", e);
        } finally {
            DbUtility.close(rs);
        }
    }

    private void loadAllDbRecords() throws DataStoreException {
        long start = System.nanoTime();
        long totalSize = 0L;
        ResultSet rs = null;
        try {
            // SELECT ID, LENGTH, LAST_MODIFIED FROM DATASTORE
            rs = conHelper.select(selectAllSQL);
            if (log.isTraceEnabled()) {
                log.trace("Executing " + selectAllSQL + " took " + (System.nanoTime() - start) / 1000000L + "ms");
            }
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
            DbUtility.close(rs);
        }
        if (log.isTraceEnabled()) {
            log.trace("loadAllDbRecords took " + (System.nanoTime() - start) / 1000000L + "ms for " +
                    allEntries.size() + " rows");
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
        DataRecord record = getRecordIfStored(identifier);
        if (record == null) {
            throw new MissingOrInvalidDataStoreRecordException("No such record: '" + identifier + "'.");
        }
        return record;
    }

    /**
     * {@inheritDoc}
     */
    public DataRecord getRecordIfStored(DataIdentifier identifier) throws DataStoreException {
        String id = identifier.toString();
        ArtifactoryDbDataRecord record = getFromAllEntries(id);
        if (record != null && record.setInUse()) {
            // The record is OK return it
            return record;
        }
        ResultSet rs = null;
        try {
            //Long result = allEntries.get(id);
            // SELECT LENGTH, LAST_MODIFIED FROM DATASTORE WHERE ID = ?

            rs = conHelper.select(selectMetaSQL, new Object[]{id});
            if (!rs.next()) {
                return null;
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
            DbUtility.close(rs);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void init(String homeDir) throws DataStoreException {
        try {
            initTmpDir(homeDir);
            initDatabaseType();
            conHelper = createConnectionHelper(getDataSource());
            if (isSchemaCheckEnabled()) {
                createCheckSchemaOperation().run();
            }
        } catch (Exception e) {
            throw convert("Can not init data store, driver=" + driver + " url=" + url + " user=" + user +
                    " schemaObjectPrefix=" + schemaObjectPrefix + " tableSQL=" + tableSQL + " createTableSQL=" +
                    createTableSQL, e);
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

    private DataSource getDataSource() throws Exception {
        if (getDataSourceName() == null || "".equals(getDataSourceName())) {
            return connectionFactory.getDataSource(getDriver(), getUrl(), getUser(), getPassword());
        } else {
            return connectionFactory.getDataSource(dataSourceName);
        }
    }

    /**
     * This method is called from the {@link #init(String)} method of this class and returns a {@link
     * org.apache.jackrabbit.core.util.db.ConnectionHelper} instance which is assigned to the {@code conHelper} field.
     * Subclasses may override it to return a specialized connection helper.
     *
     * @param dataSrc the {@link DataSource} of this persistence manager
     * @return a {@link org.apache.jackrabbit.core.util.db.ConnectionHelper}
     * @throws Exception on error
     */
    protected ArtifactoryConnectionHelper createConnectionHelper(DataSource dataSrc) throws Exception {
        ArtifactoryConnectionHelper helper = new ArtifactoryConnectionHelper(dataSrc);
        Connection con = null;
        try {
            con = helper.takeConnection();
            DatabaseMetaData meta = con.getMetaData();
            log.info("Using JDBC driver " + meta.getDriverName() + " " + meta.getDriverVersion());
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Cannot create a database connection.", e);
        } finally {
            helper.putConnection(con);
        }
        return helper;
    }

    /**
     * This method is called from {@link #init(String)} after the {@link #createConnectionHelper(javax.sql.DataSource)}
     * method, and returns a default {@link org.apache.jackrabbit.core.util.db.CheckSchemaOperation}.
     *
     * @return a new {@link org.apache.jackrabbit.core.util.db.CheckSchemaOperation} instance
     */
    protected final CheckSchemaOperation createCheckSchemaOperation() {
        String tableName = tablePrefix + schemaObjectPrefix + tableSQL;
        return new CheckSchemaOperation(conHelper, new ByteArrayInputStream(createTableSQL.getBytes()), tableName);
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

    public long scanDataStore(long startScanNanos) {
        log.debug("Starting scanning of all datastore entries");
        try {
            // Add all identifiers
            loadAllDbRecords();
            // Remove all entries in delete or error state
            Iterator<ArtifactoryDbDataRecord> recordIterator = getAllEntries().iterator();

            long datastoreScanStartMillis = System.currentTimeMillis();
            while (recordIterator.hasNext()) {
                slowDownIfLongScan(datastoreScanStartMillis);
                ArtifactoryDbDataRecord record = recordIterator.next();
                if (!validState(record) || !record.updateGcState()) {
                    recordIterator.remove();
                }
            }
        } catch (DataStoreException e) {
            throw new RuntimeException("Could not load all data store identifier: " + e.getMessage(), e);
        }
        long dataStoreQueryTime = (System.nanoTime() - startScanNanos) / 1000000;
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
        try {
            // UPDATE DATASTORE SET LAST_MODIFIED = ? WHERE ID = ? AND LAST_MODIFIED < ?
            conHelper.exec(updateLastModifiedSQL, now, id, now);
        } catch (Exception e) {
            throw convert("Can not update lastModified", e);
        }
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
     * @return whether the schema check is enabled
     */
    public final boolean isSchemaCheckEnabled() {
        return schemaCheckEnabled;
    }

    /**
     * @param enabled set whether the schema check is enabled
     */
    public final void setSchemaCheckEnabled(boolean enabled) {
        schemaCheckEnabled = enabled;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws DataStoreException {
        //If derby, prepare connection url for issuing shutdown command
        String derbyShutdownUrl = getDerbyShutdownUrl();
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

    public ArtifactoryConnectionHelper getConnectionHelper() {
        return conHelper;
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
     * Set the new table prefix. The default is empty. The table name is constructed like this:
     * ${tablePrefix}${schemaObjectPrefix}${tableName}
     *
     * @param tablePrefix the new value
     */
    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    /**
     * Get the schema prefix.
     *
     * @return the schema object prefix
     */
    public String getSchemaObjectPrefix() {
        return schemaObjectPrefix;
    }

    /**
     * Set the schema object prefix. The default is empty. The table name is constructed like this:
     * ${tablePrefix}${schemaObjectPrefix}${tableName}
     *
     * @param schemaObjectPrefix the new prefix
     */
    public void setSchemaObjectPrefix(String schemaObjectPrefix) {
        this.schemaObjectPrefix = schemaObjectPrefix;
    }

    private String getDerbyShutdownUrl() {
        String derbyShutdownUrl = null;
        if (DerbyPersistenceManager.DERBY_EMBEDDED_DRIVER.equals(driver)) {
            Connection con = null;
            try {
                con = conHelper.takeConnection();
                derbyShutdownUrl = con.getMetaData().getURL();
            } catch (Exception e) {
                log.warn("Cannot get Derby's shutdown URL: {}", e.getMessage());
                return null;
            } finally {
                conHelper.putConnection(con);
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

    /**
     * Called whenever a db record file is accessed.
     *
     * @param record             The record that was accessed (with the new access time)
     * @param previousAccessTime The previous access time (in nano seconds) of this record
     */
    protected void accessed(ArtifactoryDbDataRecord record, long previousAccessTime) {

    }

    /**
     * Determines if the the gc datastore scanning is taking too long, and sleeps accordingly
     *
     * @param start Scan start
     */
    private void slowDownIfLongScan(long start) {

        //Start slowing down if the threshold has been reached
        if (!slowdownScanning &&
                ((System.currentTimeMillis() - start) > ConstantValues.gcScanStartSleepingThresholdMillis.getLong())) {
            slowdownScanning = true;
            log.debug("Slowing down datastore scanning.");
        }

        if (shouldGcDatastoreScanSleep()) {
            try {
                lastSlowdownScanning = System.currentTimeMillis();
                Thread.sleep(slowdownScanningMillis);
            } catch (InterruptedException e) {
                log.debug("Interrupted while scanning datastore.");
            }
        }
    }

    /**
     * Indicates whether the gc datastore scanner thread should sleep
     *
     * @return True if the thresholds have been reached
     */
    private boolean shouldGcDatastoreScanSleep() {
        return slowdownScanning &&
                ((lastSlowdownScanning == 0) || ((System.currentTimeMillis() - lastSlowdownScanning) >
                        ConstantValues.gcFileScanSleepIterationMillis.getLong()));
    }
}
