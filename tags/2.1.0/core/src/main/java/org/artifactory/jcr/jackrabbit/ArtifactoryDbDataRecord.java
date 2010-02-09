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
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.concurrent.ConcurrentStateManager;
import org.artifactory.concurrent.State;
import org.artifactory.concurrent.StateAware;

import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data record that is stored in a database
 *
 * @author freds
 * @date Mar 12, 2009
 */
public class ArtifactoryDbDataRecord extends AbstractDataRecord implements StateAware {
    private enum DbRecordState implements State {
        NEW,
        // In DB with the 3 state of the GC scanner
        IN_DB_FOUND, IN_DB_USED, IN_DB_MARK_FOR_DELETION,
        DELETED, IN_ERROR;

        public boolean canTransitionTo(State newState) {
            if (newState == IN_ERROR) {
                // Always can go to error
                return true;
            }
            switch (this) {
                case NEW:
                    return (newState == IN_DB_FOUND) || (newState == IN_DB_USED) || (newState == DELETED);
                case IN_DB_FOUND:
                    return (newState == DELETED) || (newState == IN_DB_USED) || (newState == IN_DB_MARK_FOR_DELETION);
                case IN_DB_USED:
                    return (newState == IN_DB_FOUND);
                case IN_DB_MARK_FOR_DELETION:
                    return (newState == DELETED);
                case DELETED:
                    return (newState == NEW);
                case IN_ERROR:
                    return (newState == NEW) || (newState == IN_DB_USED);
            }
            throw new IllegalStateException("Could not be reached");
        }

    }

    private final ArtifactoryDbDataStoreImpl store;

    /* package */ final long length;
    private final AtomicLong lastModified;

    // State Manager of this entry in the DB. It is set after the succesful corresponding DB query was done.
    // The 3 IN_DB states are used by the garbage collector to track each Db store entry
    private final ConcurrentStateManager stateMgr;

    // Last exception if something wrong happens with the DB for this record
    private Exception error;

    // Flag set to true on first mark for deletion. If true and re mark for deletion then really marked for deletion.
    private boolean readyToMarkForDeletion = false;

    // Flag set to true on first mark to be removed from map. all deleted or in error entries should be removed on scan.
    private boolean readyToBeRemoved = false;

    /**
     * Creates a data record for the store based on the given identifier and length.
     *
     * @param store
     * @param identifier
     * @param length
     */
    public ArtifactoryDbDataRecord(ArtifactoryDbDataStoreImpl store,
            DataIdentifier identifier, long length, long lastModified) {
        super(identifier);
        this.store = store;
        this.length = length;
        this.lastModified = new AtomicLong(lastModified);
        this.stateMgr = new ConcurrentStateManager(this);
    }

    public State getInitialState() {
        return DbRecordState.NEW;
    }

    /**
     * {@inheritDoc}
     */
    public long getLength() throws DataStoreException {
        if (!setInUse()) {
            throw new DataStoreRecordNotFoundException("Record " + this + " is in invalid state");
        }
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
        if (!setInUse()) {
            throw new DataStoreRecordNotFoundException("Record " + this + " is in invalid state");
        }
        return store.getInputStream(getIdentifier());
    }

    private DbRecordState getDbState() {
        return (DbRecordState) stateMgr.getState();
    }

    /**
     * Method called after the entry was succesfully saved in DB, or loaded from DB.
     *
     * @return true if state is in db and used
     */
    public boolean setInDb() {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
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
                boolean res = getDbState() == DbRecordState.IN_DB_USED;
                if (res) {
                    readyToBeRemoved = false;
                }
                return res;
            }
        });
    }

    /**
     * This is called at the beginning of GC scan.<br/><ol> <li>If the entry is new (first time in GC), it will be
     * changed.</li> <li>If the entry is in db used it will be marked as "found" and so eligible for deletion at the end
     * of GC.</li> <li>If the entry is mark for deletion it's an error (last GC should have clean it or set to error).
     * so set to error.</li> <li>If the entry is deleted or error it should be removed from the global map.</li></ol>
     *
     * @return true if the init went well, false if element in error or delete state and should be removed from the map
     */
    public boolean initGCState() {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
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
                                "Object " + getIdentifier() + " should have been deleted during last GC!");
                    case IN_DB_USED:
                        stateMgr.guardedSetState(DbRecordState.IN_DB_FOUND);
                        readyToBeRemoved = false;
                        return true;
                    case IN_DB_FOUND:
                        // It's an error in the GC, should not reach here
                        throw new IllegalStateException(
                                "Object " + getIdentifier() + " should not be in found state during init scan!");
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
            public Boolean call() throws Exception {
                DbRecordState dbState = getDbState();
                switch (dbState) {
                    case IN_ERROR:
                        throw new RepositoryRuntimeException("Cannot use data store entry " +
                                " due to previous error: " + error.getMessage());
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
                        boolean res = getDbState() == DbRecordState.IN_DB_USED;
                        if (res) {
                            readyToBeRemoved = readyToMarkForDeletion = false;
                        }
                        return res;
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
        // Object that are younger than GC repeat time cannot be deleted
        // TODO: Should we activate it? Done with readyToBeMarkForDeletion flag
        //if ((now - lastModified.get()) < (ConstantsValue.gcIntervalMins.getLong() * 1000L)) {
        //    return false;
        //}
        return stateMgr.changeStateIn(new Callable<Boolean>() {
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
                    case NEW:
                        // Don't delete used and new objects :)
                        readyToBeRemoved = readyToMarkForDeletion = false;
                        return false;
                    case IN_DB_FOUND:
                        if (readyToMarkForDeletion) {
                            stateMgr.guardedSetState(DbRecordState.IN_DB_MARK_FOR_DELETION);
                        } else {
                            readyToMarkForDeletion = true;
                        }
                        break;
                }
                return getDbState() == DbRecordState.IN_DB_MARK_FOR_DELETION;
            }
        });
    }

    /**
     * Called after a sucessful delete in DB. No question asked here, state brutaly changed.
     */
    public void setDeleted() {
        stateMgr.changeStateIn(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                stateMgr.guardedSetState(DbRecordState.DELETED);
                return getDbState() == DbRecordState.DELETED;
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
    public boolean needReinsert(final long now) {
        return stateMgr.changeStateIn(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                DbRecordState dbState = getDbState();
                switch (dbState) {
                    case IN_ERROR:
                        // TODO: Find what to do here. For the moment throw the old exception
                        throw new RepositoryRuntimeException(
                                "Cannot insert new record " + ArtifactoryDbDataRecord.this +
                                        " since the old one is in invalid state", error);
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
                        }
                        break;
                    case NEW:
                        // Wait normal timeout for actual insert in DB to happen
                        stateMgr.guardedWaitForNextStep();
                        break;
                    case IN_DB_FOUND:
                        stateMgr.guardedSetState(DbRecordState.IN_DB_USED);
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
                    setLastModified(now);
                    readyToBeRemoved = readyToMarkForDeletion = false;
                    return false;
                }
                if (dbState == DbRecordState.IN_ERROR) {
                    throw new RepositoryRuntimeException(
                            "Cannot insert new record " + ArtifactoryDbDataRecord.this +
                                    " since the old one is in invalid state", error);
                }
                throw new RepositoryRuntimeException("Cannot reuse data entry " + ArtifactoryDbDataRecord.this);
            }
        });
    }

    public void setInError(final Exception e) {
        stateMgr.changeStateIn(new Callable<Object>() {
            public Object call() throws Exception {
                error = e;
                stateMgr.guardedSetState(DbRecordState.IN_ERROR);
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
        sb.append(", state=").append(stateMgr.getState());
        sb.append(", readyToMarkForDeletion=").append(readyToMarkForDeletion);
        sb.append(", readyToBeRemoved=").append(readyToBeRemoved);
        sb.append('}');
        return sb.toString();
    }
}
