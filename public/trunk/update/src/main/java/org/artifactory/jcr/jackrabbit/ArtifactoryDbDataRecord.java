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

import org.apache.jackrabbit.core.data.AbstractDataRecord;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Data record that is stored in a database
 *
 * @author freds
 * @date Mar 12, 2009
 */
public class ArtifactoryDbDataRecord extends AbstractDataRecord {
    enum DbRecordState {
        NEW, IN_DB, DELETED
    }

    enum ScannerState {
        FOUND, USED, MARK_FOR_DELETION
    }

    private final ArtifactoryDbDataStoreImpl store;

    private final long length;
    private final AtomicLong lastModified;

    // TODO: Will be nice to know which paths are actually using this data record
    //private Set<String> paths;

    // State of this entry in the DB. It is set after the succesful corresponding DB query was done.
    private final AtomicReference<DbRecordState> dbState;

    // Used by the garbage collector to track each Db store entry
    private final AtomicReference<ScannerState> scannerState;

    /**
     * Creates a data record for the store based on the given identifier and length.
     *
     * @param store
     * @param identifier
     * @param length
     */
    public ArtifactoryDbDataRecord(ArtifactoryDbDataStoreImpl store,
            DataIdentifier identifier,
            long length, long lastModified) {
        super(identifier);
        this.store = store;
        this.length = length;
        this.lastModified = new AtomicLong(lastModified);
        this.dbState = new AtomicReference<DbRecordState>(DbRecordState.NEW);
        this.scannerState = new AtomicReference<ScannerState>(ScannerState.USED);
    }

    /**
     * {@inheritDoc}
     */
    public long getLength() throws DataStoreException {
        setInUse();
        return length;
    }

    public long getLastModified() {
        return lastModified.get();
    }

    public void setLastModified(long lastModified) {
        this.lastModified.set(lastModified);
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getStream() throws DataStoreException {
        setInUse();
        return store.getInputStream(getIdentifier());
    }

    public boolean waitForDeletion() throws DataStoreException {
        while (dbState.get() != DbRecordState.DELETED &&
                scannerState.get() == ScannerState.MARK_FOR_DELETION) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new DataStoreException("Interrupted waiting for deletion of " + getIdentifier());
            }
        }
        return dbState.get() == DbRecordState.DELETED;
    }

    public boolean waitForInsertion() throws DataStoreException {
        while (dbState.get() != DbRecordState.IN_DB &&
                scannerState.get() != ScannerState.MARK_FOR_DELETION) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new DataStoreException("Interrupted waiting for insertion of " + getIdentifier());
            }
        }
        return dbState.get() == DbRecordState.IN_DB && setInUse();
    }

    public boolean setInDb() {
        return dbState.compareAndSet(DbRecordState.NEW, DbRecordState.IN_DB);
    }

    /**
     * This is called at the beginning of scan. It is brutal since there cannot be a deletion running at
     * the same time. One GC at a time.
     */
    public void initScanner() {
        scannerState.set(ScannerState.FOUND);
        dbState.set(DbRecordState.IN_DB);
    }

    public boolean setInUse() {
        return scannerState.get() == ScannerState.USED ||
                scannerState.compareAndSet(ScannerState.FOUND, ScannerState.USED);
    }

    /**
     * This method should be called brutally on all entries at the end of scan to ensure
     * atomicity of the mark for deletion. All USED entries will not be marked for deletion.
     *
     * @return true if marked for deletion, false otherwise
     */
    public boolean markForDeletion() {
        // Object that are younger than GC repeat time cannot be deleted
        // TODO: Works without it. So, should we activate it?
        //if ((System.currentTimeMillis() - lastModified.get()) < (ConstantsValue.gcIntervalMins.getLong() * 1000L)) {
        //    return false;
        //}
        return scannerState.compareAndSet(ScannerState.FOUND, ScannerState.MARK_FOR_DELETION);
    }

    public boolean isNew() {
        return dbState.get() == DbRecordState.NEW;
    }

    public boolean isInDb() {
        return dbState.get() == DbRecordState.IN_DB;
    }

    public boolean isDeleted() {
        return dbState.get() == DbRecordState.DELETED;
    }

    /**
     * Called after a sucessful delete in DB. No question asked here, state brutaly changed.
     */
    public void setDeleted() {
        dbState.set(DbRecordState.DELETED);
        scannerState.set(ScannerState.MARK_FOR_DELETION);
    }

    /**
     * Reused a deleted entry to be a new entry. This method is used if a new record found a mark
     * for deletion entry, then wait for real deletion in DB, and then reuse the object for its
     * own new record.
     *
     * @return true if entry was in delete/mark for deletion state before, false if state change issue
     */
    public boolean setToNew() {
        return dbState.compareAndSet(DbRecordState.DELETED, DbRecordState.NEW) && scannerState.compareAndSet(
                ScannerState.MARK_FOR_DELETION, ScannerState.USED);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ArtifactoryDbDataRecord");
        sb.append("{id=").append(getIdentifier());
        sb.append(", length=").append(length);
        sb.append(", dbState=").append(dbState.get());
        sb.append(", scannerState=").append(scannerState.get());
        sb.append('}');
        return sb.toString();
    }
}
