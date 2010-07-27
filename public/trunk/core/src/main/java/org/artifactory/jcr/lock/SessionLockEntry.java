/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.jcr.lock;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ConstantValues;
import org.artifactory.concurrent.LockingException;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.util.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.core.JdkVersion;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author freds
 * @author yoavl
 * @date Oct 19, 2008
 */
class SessionLockEntry implements FsItemLockEntry {
    private static final Logger log = LoggerFactory.getLogger(SessionLockEntry.class);

    private final LockEntryId id;
    private JcrFsItem lockedFsItem;
    private JcrFsItem immutableFsItem;
    //The (per-session) acquired lock state
    private ReentrantReadWriteLock.ReadLock acquiredReadLock;

    enum LockMode {
        READ, WRITE
    }

    SessionLockEntry(LockEntryId lockEntryId) {
        this.id = lockEntryId;
    }

    public MonitoringReadWriteLock getLock() {
        return id.getLock();
    }

    public JcrFsItem getLockedFsItem() {
        return lockedFsItem;
    }

    public JcrFsItem getImmutableFsItem() {
        return immutableFsItem;
    }

    public RepoPath getRepoPath() {
        return id.getRepoPath();
    }

    void acquireReadLock() {
        log.trace("Acquiring READ lock on {}", getRepoPath());
        if (acquiredReadLock != null) {
            // already done
            return;
        }
        if (isLockedByMe()) {
            throw new IllegalStateException(
                    "Cannot acquire read lock when write lock is acquired and mutable item was null " +
                            id);
        }
        acquire(LockMode.READ);
    }

    void acquireWriteLock() {
        log.trace("Acquiring WRITE lock on {}", id);
        if (isLockedByMe()) {
            // already done
            return;
        }
        //Attempt to 'upgrade' a read lock to a write lock by releasing and reacquiring
        if (acquiredReadLock != null) {
            log.debug("Attempting to upgrade read lock to write lock on " + id);
            releaseReadLock();
        }
        acquire(LockMode.WRITE);
    }

    public boolean isLockedByMe() {
        return getLock().isWriteLockedByCurrentThread();
    }

    void unlock() {
        releaseReadLock();
        releaseWriteLock();
    }

    /**
     * Update the repository caches for changes in this fsItem
     */
    void updateCache() {
        if (isLockedByMe()) {
            if (lockedFsItem != null && lockedFsItem.isDeleted()) {
                // Delete object will just clean the cache, so no worry about fsItem being mutable
                lockedFsItem.updateCache();
            } else if (immutableFsItem != null) {
                // Only fromJcr immutable fsItem can go in the cache
                immutableFsItem.updateCache();
            }
        }
    }

    void save() {
        if (!isLockedByMe()) {
            throw new LockingException("Cannot save item " + id + " which not locked by me!");
        }
        // Save the modified fsItem will return a JCR based immutable fsItem.
        // The return item from save is the immutable.
        // Activate save only if item is not deleted, and was modified
        if (isUnsaved()) {
            immutableFsItem = lockedFsItem.save(immutableFsItem);
            // Sanity check, if object are not identical after save the save process is failing
            if (!immutableFsItem.isIdentical(lockedFsItem)) {
                throw new IllegalStateException(
                        "Items are not identical after save " + immutableFsItem + " " + lockedFsItem);
            }
            log.trace("Saving lock entry " + getRepoPath());
        }
    }

    public void setWriteFsItem(JcrFsItem fsItem, JcrFsItem mutableFsItem) {
        // fsItem can be null on create
        if (fsItem != null && !getRepoPath().equals(fsItem.getRepoPath())) {
            throw new IllegalStateException(
                    "Updating a session lock " + id + " with a different item " + fsItem);
        }
        // mutable cannot be null
        if (mutableFsItem == null || !getRepoPath().equals(mutableFsItem.getRepoPath())) {
            throw new IllegalStateException(
                    "Updating a session lock " + id + " with a different item " + mutableFsItem);
        }
        if ((fsItem != null && fsItem.isMutable()) || !mutableFsItem.isMutable()) {
            throw new IllegalStateException(
                    "Updating a write session lock " + id + " with a immutable item " + fsItem +
                            " or mutable item " + mutableFsItem + " which are in invalid state");
        }
        if (!isLockedByMe()) {
            throw new IllegalStateException(
                    "Updating a write session lock " + id + " which does not have a write lock");
        }
        if (acquiredReadLock != null) {
            throw new IllegalStateException(
                    "Updating a write session lock " + id + " which has a read lock");
        }

        this.immutableFsItem = fsItem;
        this.lockedFsItem = mutableFsItem;
    }

    public void setReadFsItem(JcrFsItem fsItem) {
        if (fsItem == null || !getRepoPath().equals(fsItem.getRepoPath())) {
            throw new IllegalStateException(
                    "Updating a session lock " + id + " with a different item " + fsItem);
        }
        if (fsItem.isMutable()) {
            throw new IllegalStateException(
                    "Updating a read only session lock " + id + " with a mutable item " + fsItem);
        }
        if (isLockedByMe() || lockedFsItem != null) {
            throw new IllegalStateException(
                    "Updating a read only session lock " + id + " which has already a write lock");
        }
        if (acquiredReadLock == null) {
            throw new IllegalStateException(
                    "Updating a read only session lock " + id + " which does not have a read lock");
        }

        this.immutableFsItem = fsItem;
        this.lockedFsItem = null;
    }

    /**
     * Test the state of this entry for unsaved data.
     * @return true if the entry needs to be saved, false otherwise
     */
    boolean isUnsaved() {
        // The entry is unsaved if it has a mutable locked object
        // which is not deleted (Delete happens immediately has JCR delete method is called)
        // and if it comes from a cache immutable object (basically (immutableFsItem != null) means in update mode)
        //  We test that the item actually changed or is marked dirty
        return lockedFsItem != null && !lockedFsItem.isDeleted() &&
                (lockedFsItem.isDirty() || immutableFsItem == null || !immutableFsItem.isIdentical(lockedFsItem));
    }

    /**
     * Release the read lock if locked
     *
     * @return true if read lock was acquired, false otherwise
     */
    boolean releaseReadLock() {
        log.trace("Releasing READ lock on {}", id);
        try {
            if (acquiredReadLock != null) {
                acquiredReadLock.unlock();
                return true;
            }
            return false;
        } finally {
            acquiredReadLock = null;
        }
    }

    private void acquire(LockMode mode) {
        MonitoringReadWriteLock rwLock = getLock();
        Lock lock = mode == LockMode.READ ? rwLock.readLock() : rwLock.writeLock();
        try {
            boolean success =
                    lock.tryLock() || lock.tryLock(ConstantValues.locksTimeoutSecs.getLong(), TimeUnit.SECONDS);
            if (!success) {
                StringBuilder msg =
                        new StringBuilder().append(mode).append(" lock on ").append(id).append(" not acquired in ")
                                .append(ConstantValues.locksTimeoutSecs.getLong()).append(" seconds. Lock info: ")
                                .append(lock)
                                .append(".");
                if (ConstantValues.locksDebugTimeouts.getBoolean()) {
                    try {
                        if (JdkVersion.getMajorJavaVersion() >= JdkVersion.JAVA_16) {
                            Class<?> dumperClass = Class.forName("org.artifactory.thread.ThreadDumper");
                            Object dumper = dumperClass.newInstance();
                            Method method = dumperClass.getDeclaredMethod("dumpThreads");
                            CharSequence dump = (CharSequence) method.invoke(dumper);
                            log.info("Printing locking debug information...");
                            msg.append("\n").append(dump);
                        }
                    } catch (Throwable t) {
                        log.info("Could not dump threads", t);
                    }
                }
                //msg.append("Lock thread dump:\n").append(getThreadDump(rwLock));
                throw new LockingException(msg.toString());
            } else if (mode == LockMode.READ) {
                if (acquiredReadLock != null) {
                    throw new IllegalStateException(
                            "Acquiring a read only session lock " + id +
                                    " while already has a read only session lock.");
                }
                //Update the local read-lock tracker
                acquiredReadLock = (ReentrantReadWriteLock.ReadLock) lock;
            }
        } catch (InterruptedException e) {
            throw new LockingException(mode + " lock on " + id + " not acquired!", e);
        }
    }

    private void releaseWriteLock() {
        log.trace("Releasing WRITE lock on {}", id);
        if (isLockedByMe()) {
            if (immutableFsItem != null && lockedFsItem != null) {
                if (!immutableFsItem.isIdentical(lockedFsItem)) {
                    //Local modification will be discarded
                    log.error("Immutable item {} has local modifications that will be ignored.", lockedFsItem);
                }
            }
            lockedFsItem = null;
            getLock().writeLock().unlock();
        }
    }

    private String getThreadDump(MonitoringReadWriteLock lock) {
        StringBuilder b = new StringBuilder();
        try {
            //owner
            Thread owner = lock.getOwner();
            b.append("Current OWNER - ").append(owner).append(":\n");
            if (owner != null) {
                b.append(ExceptionUtils.getStackTrace(owner));
            }
            b.append("\n");
            //writers
            Collection<Thread> writers = lock.getQueuedWriterThreads();
            b.append("Queued WRITERS (").append(writers.size()).append("):\n");
            int i = 1;
            for (Thread writer : writers) {
                b.append("WRITER #").append(i).append(" - ").append(writer).append(":\n");
                b.append(ExceptionUtils.getStackTrace(writer));
                i++;
            }
            b.append("\n");
            //readers
            Collection<Thread> readers = lock.getQueuedReaderThreads();
            b.append("Queued READERS (").append(readers.size()).append("):\n");
            i = 1;
            for (Thread reader : readers) {
                b.append("READER #").append(i).append(" - ").append(reader).append(":\n");
                b.append(ExceptionUtils.getStackTrace(reader));
                i++;
            }
        } catch (Throwable t) {
            b.append("(Problem getting thread dump: ").append(t.getMessage()).append(')');
        }
        return b.toString();
    }
}
