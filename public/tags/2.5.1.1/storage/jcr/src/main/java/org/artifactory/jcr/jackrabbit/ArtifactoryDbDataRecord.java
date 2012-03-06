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
 *
 * Based on: /org/apache/jackrabbit/jackrabbit-core/1.6.0/jackrabbit-core-1.6.0.jar!
 * /org/apache/jackrabbit/core/data/DataStore.class
 */
package org.artifactory.jcr.jackrabbit;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.AbstractDataRecord;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;
import org.artifactory.common.ConstantValues;
import org.artifactory.concurrent.ConcurrentStateManager;
import org.artifactory.concurrent.LockingException;
import org.artifactory.concurrent.State;
import org.artifactory.concurrent.StateAware;
import org.artifactory.log.LoggerFactory;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File data record that is stored in a database.
 *
 * @author freds
 * @date Mar 12, 2009
 */
public class ArtifactoryDbDataRecord extends AbstractDataRecord implements StateAware {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryDbDataRecord.class);

    /**
     * Marked records that their access time wasn't calculated yet or doesn't have a cache file.
     */
    public static final long NOT_ACCESSED = Long.MIN_VALUE;

    private final ArtifactoryDataStore store;

    // Length coming from DB (or file system ?)
    /* package */
    long length;

    // Last modified time coming from DB
    private final AtomicLong lastModified;

    // State Manager of this entry in the DB. It is set after the successful corresponding DB query was done.
    // The 3 IN_DB states are used by the garbage collector to track each Db store entry
    private final ConcurrentStateManager stateMgr;

    // Last exception if something wrong happens with the DB for this record
    private Exception error;

    // Flag set to true on first mark for deletion. If true and re mark for deletion then really marked for deletion.
    private boolean readyToMarkForDeletion = false;

    // Flag set to true on first mark to be removed from map. all deleted or in error entries should be removed on scan.
    private boolean readyToBeRemoved = false;

    // The nanosecond time of the last time the stream of this record was served
    private final AtomicLong lastAccessTime = new AtomicLong(NOT_ACCESSED);

    // Number of open stream readers
    private final AtomicInteger readersCount = new AtomicInteger(0);

    // For managing concurrent DB loading from file
    private final ReentrantLock fileActionSync = new ReentrantLock();


    public static ArtifactoryDbDataRecord createForDeletion(ArtifactoryDataStore store, String id, long length) {
        ArtifactoryDbDataRecord record =
                new ArtifactoryDbDataRecord(store, new DataIdentifier(id), length, System.currentTimeMillis());
        record.markForDeletion();
        return record;
    }


    /**
     * Creates a data record for the store based on the given identifier and length.
     *
     * @param store
     * @param identifier
     * @param length
     */
    public ArtifactoryDbDataRecord(ArtifactoryDataStore store,
            DataIdentifier identifier, long length, long lastModified) {
        super(identifier);
        this.store = store;
        this.length = length;
        this.lastModified = new AtomicLong(lastModified);
        this.stateMgr = new ConcurrentStateManager(this);
    }

    @Override
    public State getInitialState() {
        return DbRecordState.NEW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLength() throws DataStoreException {
        if (!setInUse()) {
            throw new MissingOrInvalidDataStoreRecordException("Record " + this + " is in invalid state");
        }
        return length;
    }

    @Override
    public long getLastModified() {
        return lastModified.get();
    }

    public void setLastModified(long lastModified) {
        this.lastModified.set(lastModified);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getStream() throws DataStoreException {
        if (!setInUse()) {
            throw new MissingOrInvalidDataStoreRecordException("Record " + this + " is in invalid state");
        }
        DataRecordFileStream fileStream = null;
        try {
            this.readersCount.incrementAndGet();
            File file = store.getOrCreateFile(getIdentifier(), length);
            try {
                fileStream = new DataRecordFileStream(file);
                return fileStream;
            } catch (IOException e) {
                throw new DataStoreException("Error opening input stream of " + file.getAbsolutePath(), e);
            }
        } finally {
            if (fileStream == null) {
                // The stream was not sent, decrement the readers
                readersCount.decrementAndGet();
            }
        }
    }

    public <V> V guardedActionOnFile(Callable<V> callable) throws DataStoreException {
        boolean success = false;
        try {
            try {
                success = fileActionSync.tryLock() || fileActionSync.tryLock(getLoadSyncTimeOut(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Set the interrupted flag back
                Thread.currentThread().interrupt();
                throw new LockingException("Interrupted while trying to lock " + this, e);
            }
            if (!success) {
                throw new LockingException("Could not acquire state lock in " + getLoadSyncTimeOut());
            }
            return callable.call();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof DataStoreException) {
                throw (DataStoreException) e;
            }
            throw new DataStoreException("Error during action on file " + getIdentifier(), e);
        } finally {
            if (success) {
                fileActionSync.unlock();
            }
        }
    }

    private long getLoadSyncTimeOut() {
        return ConstantValues.locksTimeoutSecs.getLong();
    }

    public class DataRecordFileStream extends FileInputStream {
        private boolean closed = false;

        public DataRecordFileStream(File file) throws FileNotFoundException {
            super(file);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (!closed) {
                    closed = true;
                    markAccessed();
                    readersCount.decrementAndGet();
                }
            }
        }
    }

    public void markAccessed() {
        long previousAccessTime = lastAccessTime.getAndSet(System.nanoTime());
        store.accessed(this, previousAccessTime);
    }

    public long getLastAccessTime() {
        return lastAccessTime.get();
    }

    public int getReadersCount() {
        return readersCount.intValue();
    }

    private DbRecordState getDbState() {
        return (DbRecordState) stateMgr.getState();
    }

    /**
     * Method called after the entry was successfully saved in DB, or loaded from DB.
     *
     * @return true if state is in db and used
     */
    public boolean setInDb() {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DbRecordState dbState = getDbState();
                if (dbState == DbRecordState.IN_ERROR) {
                    // Set it back to good
                    error = null;
                }
                if (dbState == DbRecordState.IN_DB_USED) {
                    return true;
                }
                stateMgr.guardedSetState(DbRecordState.IN_DB_USED);
                readyToBeRemoved = false;
                return true;
            }
        });
    }

    /**
     * This is called at the beginning of GC scan.<br/><ol> <li>If the entry is new (first time in GC), it will be
     * changed.</li> <li>If the entry is in db used it will be marked as "found" and so not eligible for deletion at the
     * end of GC.</li> <li>If the entry is mark for deletion it's an error (last GC should have clean it or set to
     * error). so set to error.</li> <li>If the entry is deleted or error it should be removed from the global
     * map.</li></ol>
     *
     * @return true if the init went well, false if element in error or delete state and should be removed from the map
     */
    public boolean updateGcState() {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DbRecordState dbState = getDbState();
                switch (dbState) {
                    case IN_ERROR:
                    case DELETED:
                        // Nothing to do entry should be removed
                        if (readyToBeRemoved) {
                            return false;
                        }
                        readyToBeRemoved = true;
                        return true;
                    case IN_DB_MARK_FOR_DELETION:
                        throw new IllegalStateException(
                                "Object " + ArtifactoryDbDataRecord.this.toString() +
                                        " should have been deleted during last GC!");
                    case IN_DB_USED:
                        stateMgr.guardedSetState(DbRecordState.IN_DB_FOUND);
                        readyToBeRemoved = false;
                        return true;
                    case IN_DB_FOUND:
                        // Second time there (should have the readyToBeRemoved flag set to true)
                        if (!readyToBeRemoved) {
                            log.error("Object " + ArtifactoryDbDataRecord.this.toString() +
                                    " should not be in found state during init scan without ready to be removed flag!");
                        }
                        return true;
                    case NEW:
                        // Nothing to do, leave it try to insert itself
                        readyToBeRemoved = readyToMarkForDeletion = false;
                        return true;
                }
                throw new IllegalStateException("Could not be reached");
            }
        });
    }

    /**
     * This is called everytime the db entry is used by JCR or by the GC scan read
     *
     * @return true if is in in used state
     */
    public boolean setInUse() {
        //noinspection SimplifiableIfStatement
        if (getDbState() == DbRecordState.IN_DB_USED) {
            // Shortcut since the method is called heavily
            return true;
        }
        return stateMgr.changeStateIn(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DbRecordState dbState = getDbState();
                switch (dbState) {
                    case IN_ERROR:
                        RepositoryRuntimeException ex = new RepositoryRuntimeException("Cannot use data store entry " +
                                " due to previous error: " + error.getMessage(), error);
                        throw ex;
                    case DELETED:
                    case IN_DB_MARK_FOR_DELETION:
                        // Entry cannot be used. Hopefully system manage the exception correctly
                        return false;
                    case IN_DB_USED:
                        // We are good
                        return true;
                    case IN_DB_FOUND:
                    case NEW:
                        stateMgr.guardedSetState(DbRecordState.IN_DB_USED);
                        readyToBeRemoved = readyToMarkForDeletion = false;
                        return true;
                }
                throw new IllegalStateException("Could not be reached");
            }
        });
    }

    /**
     * This method should be called brutally on all entries at the end of scan to ensure atomicity of the mark for
     * deletion. All USED entries will not be marked for deletion.
     *
     * @param now
     * @return true if marked for deletion, false otherwise
     */
    public boolean markForDeletion(final long now) {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DbRecordState dbState = getDbState();
                switch (dbState) {
                    case IN_ERROR:
                    case DELETED:
                        // Will be removed on next GC init
                    case IN_DB_MARK_FOR_DELETION:
                        // Already done
                        break;
                    case IN_DB_USED:
                        if (now > 0) {
                            //v2
                            if (lastAccessTime.get() < now) {
                                stateMgr.guardedSetState(DbRecordState.IN_DB_MARK_FOR_DELETION);
                            }
                            break;
                        }
                    case NEW:
                        if (now == -1) {
                            //v1
                            // Don't delete used and new objects :)
                            readyToBeRemoved = readyToMarkForDeletion = false;
                        }
                        break;
                    case IN_DB_FOUND:
                        if (now == -1) {
                            //v1
                            if (readyToMarkForDeletion) {
                                stateMgr.guardedSetState(DbRecordState.IN_DB_MARK_FOR_DELETION);
                            } else {
                                readyToMarkForDeletion = true;
                            }
                        } else if (now > 0) {
                            if (lastAccessTime.get() < now) {
                                stateMgr.guardedSetState(DbRecordState.IN_DB_MARK_FOR_DELETION);
                            }
                        }
                        break;
                }
                return getDbState() == DbRecordState.IN_DB_MARK_FOR_DELETION;
            }
        });
    }

    public void markForDeletion() {
        //TODO: [by yl] For pure v2, just allow moving directly from NEW to MARK_FOR_DELETION
        //TODO: [by fsi] Reused in v1 fix consistency, because DB bug
        stateMgr.changeStateIn(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                stateMgr.guardedSetState(DbRecordState.IN_DB_FOUND);
                stateMgr.guardedSetState(DbRecordState.IN_DB_MARK_FOR_DELETION);
                return true;
            }
        });
    }

    /**
     * Called after a successful delete in DB. No question asked here, state brutally changed.
     */
    public boolean setDeleted() {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                stateMgr.guardedSetState(DbRecordState.DELETED);
                boolean res = getDbState() == DbRecordState.DELETED;
                if (res) {
                    // Really delete the file now
                    res = deleteDbFile();
                }
                return res;
            }
        });
    }

    boolean deleteDbFile() throws DataStoreException {
        return guardedActionOnFile(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (getReadersCount() > 0) {
                    log.warn("Cannot delete file that is currently being read " + getIdentifier());
                    return false;
                }
                return store.deleteFile(getIdentifier());
            }
        });
    }

    /**
     * Called when a new entry is created and this entry match the checksum.
     *
     * @param now
     * @return true if the entry is new so the DB insert should be done, false if the entry is OK to used, throw
     *         exception if non of the above case are valid
     */
    public boolean needsReinsert(final long now, final File tempFile) {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                DbRecordState dbState = getDbState();
                switch (dbState) {
                    case IN_ERROR:
                        // TODO: Find what to do here. For the moment throw the old exception
                        RepositoryRuntimeException ex = new RepositoryRuntimeException(
                                "Cannot insert new record " + ArtifactoryDbDataRecord.this +
                                        " since the old one is in invalid state", error);
                        throw ex;
                    case DELETED:
                        // Reset it to NEW for reuse
                        stateMgr.guardedSetState(DbRecordState.NEW);
                        readyToBeRemoved = readyToMarkForDeletion = false;
                        break;
                    case IN_DB_MARK_FOR_DELETION:
                        // Wait 2 seconds for delete in DB to happen
                        stateMgr.guardedWaitForNextStep(2);
                        if (getDbState() == DbRecordState.DELETED) {
                            stateMgr.guardedSetState(DbRecordState.NEW);
                            readyToBeRemoved = readyToMarkForDeletion = false;
                        }
                        break;
                    case NEW:
                        // Wait normal timeout for actual insert in DB to happen
                        stateMgr.guardedWaitForNextStep();
                        // Go to in_db_found test (Scanner may have been activated!), so no break
                        // No break here
                    case IN_DB_FOUND:
                        // Move to in db used if scanner set the flag
                        while (getDbState() == DbRecordState.IN_DB_FOUND) {
                            stateMgr.guardedSetState(DbRecordState.IN_DB_USED);
                        }
                        break;
                    case IN_DB_USED:
                        // Ready to be used
                        break;
                }
                dbState = getDbState();

                if (dbState == DbRecordState.NEW) {
                    setLastModified(now);
                    readyToBeRemoved = readyToMarkForDeletion = false;
                    return true;
                }
                if (dbState == DbRecordState.IN_DB_USED) {
                    // Check the length are equal
                    if (length != tempFile.length()) {
                        String msg = "File collision for id=" + getIdentifier() + " length=" + tempFile.length() +
                                " oldLength=" + length;
                        log.error(msg);
                        throw new DataStoreException(msg);
                    }
                    File cachedFile = store.getFile(getIdentifier());
                    if (cachedFile == null || !cachedFile.exists()) {
                        // If no cache file copy the temp file to the cache folder and use it as the cache file
                        setFile(tempFile);
                    }
                    setLastModified(now);
                    readyToBeRemoved = readyToMarkForDeletion = false;
                    return false;
                }
                if (dbState == DbRecordState.IN_ERROR) {
                    RepositoryRuntimeException ex = new RepositoryRuntimeException(
                            "Cannot insert new record " + ArtifactoryDbDataRecord.this +
                                    " since the old one is in invalid state", error);
                    throw ex;
                }
                throw new RepositoryRuntimeException("Cannot reuse data entry " + ArtifactoryDbDataRecord.this);
            }
        });
    }

    public boolean deleteCacheFile(final long scanStartTime) {
        try {
            return guardedActionOnFile(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    if (getReadersCount() > 0) {
                        log.debug("Cannot delete file '{}'. Currently opened", getIdentifier());
                        return false;
                    }
                    long lastAccess = lastAccessTime.get();
                    if (lastAccess != NOT_ACCESSED && lastAccess > scanStartTime) {
                        log.debug("Cannot delete file '{}'. Last access time changed during scanning", getIdentifier());
                        return false;
                    }
                    // mark the record as not accessed (so it will be considered as record with no cache file)
                    if (!lastAccessTime.compareAndSet(lastAccess, NOT_ACCESSED)) {
                        log.debug("Cannot delete file '{}'. Last access time changed during delete", getIdentifier());
                        return false;
                    }
                    File cachedFile = store.getFile(getIdentifier());
                    if (cachedFile.exists()) {
                        log.debug("Deleting cache file '{}' to save {} bytes", cachedFile.getAbsolutePath(), length);
                        boolean deleted = cachedFile.delete();
                        if (!deleted) {
                            log.debug("Could not delete cache file '{}' to save {} bytes", cachedFile.getAbsolutePath(),
                                    length);
                        }
                        return deleted;
                    }
                    log.trace("Cache file '{}' doesn't exist! Already deleted?", cachedFile.getAbsolutePath());
                    return true;
                }
            });
        } catch (DataStoreException e) {
            log.error("Error deleting file " + getIdentifier() + " during GC");
            return false;
        }
    }

    /**
     * Associate this entry with a file on the file system. Will move the temp file to the cache folder if required.
     *
     * @param tempFile The file system file
     */
    void setFile(final File tempFile) throws DataStoreException {
        markAccessed();
        guardedActionOnFile(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                File cachedFile = store.getFile(getIdentifier());
                if (cachedFile.exists()) {
                    //The target storage file (cache or real storage) already exists - should never happen
                    if (cachedFile.length() != tempFile.length()) {
                        throw new DataStoreException("File collision for id=" + getIdentifier() +
                                " length=" + tempFile.length() +
                                " oldLength=" + cachedFile.length());
                    } else {
                        // The cache file is there from left over of a deletion that did not complete
                        // Since it's totally recovered, the message is debug level only
                        log.debug(
                                "Unexpected condition when switching temp file target datastore file {} already exists!");
                    }
                } else {
                    File parentFile = cachedFile.getParentFile();
                    // Check that folder exists in case that 2 threads are trying to create at the same time
                    if (!parentFile.mkdirs() && !parentFile.exists()) {
                        throw new DataStoreException(
                                "Could not create folder " + parentFile.getAbsolutePath() + " for file store");
                    }
                    // rename and move it to the cache folder
                    try {
                        FileUtils.moveFile(tempFile, cachedFile);
                    } catch (IOException e) {
                        throw new DataStoreException("File move for id " + getIdentifier() +
                                " from " + tempFile.getAbsolutePath() +
                                " to " + cachedFile.getAbsolutePath() + " failed!", e);
                    }
                }
                //TODO: [by YS] WHY here?
                length = cachedFile.length();
                return true;
            }
        });
    }

    public void setInError(final Exception e) {
        stateMgr.changeStateIn(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                error = e;
                stateMgr.guardedSetState(DbRecordState.IN_ERROR);
                // TODO: Remove the file
                return null;
            }
        });
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ArtifactoryDbDataRecord");
        sb.append("{id=").append(getIdentifier());
        sb.append(", length=").append(length);
        sb.append(", lastModified=").append(new Date(lastModified.get()));
        sb.append(", lastAccessTime=").append(lastAccessTime);
        sb.append(", state=").append(stateMgr.getState());
        sb.append(", readyToMarkForDeletion=").append(readyToMarkForDeletion);
        sb.append(", readyToBeRemoved=").append(readyToBeRemoved);
        sb.append(", nbReader=").append(readersCount);
        sb.append('}');
        return sb.toString();
    }
}
