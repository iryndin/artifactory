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
package org.artifactory.jcr.lock;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.common.ConstantValues;
import org.artifactory.concurrent.LockingException;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

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

    private final ReentrantReadWriteLock lock;
    private JcrFsItem lockedFsItem;
    private JcrFsItem immutableFsItem;
    //The (per-session) acquired lock state
    private ReentrantReadWriteLock.ReadLock acquiredReadLock;

    SessionLockEntry(FsItemLockEntry lockEntry) {
        this.lock = lockEntry.getLock();
        this.lockedFsItem = lockEntry.getLockedFsItem();
        this.immutableFsItem = lockEntry.getImmutableFsItem();
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    public JcrFsItem getLockedFsItem() {
        return lockedFsItem;
    }

    public JcrFsItem getImmutableFsItem() {
        return immutableFsItem;
    }

    public RepoPath getRepoPath() {
        return getFsItem().getRepoPath();
    }

    public JcrFsItem getFsItem() {
        if (lockedFsItem != null) {
            return lockedFsItem;
        }
        if (immutableFsItem == null) {
            throw new IllegalStateException("Session lock entry has no fsitem");
        }
        return immutableFsItem;
    }

    void acquireReadLock() {
        log.trace("Acquiring READ lock on {}", getFsItem());
        if (acquiredReadLock != null) {
            // already done
            return;
        }
        if (lockedFsItem != null) {
            throw new LockingException("Cannot acquire read lock on a mutable fsitem " + lockedFsItem);
        }
        if (isLockedByMe()) {
            throw new IllegalStateException(
                    "Cannot acquire read lock when write lock is acquired and mutable fsitem was null " +
                            immutableFsItem);
        }
        ReentrantReadWriteLock.ReadLock lock = getReadLock();
        acquire("Read", lock);
        acquiredReadLock = lock;
    }

    void acquireWriteLock(boolean upgradeLockIfNecessary) {
        log.trace("Acquiring WRITE lock on {}", getFsItem());
        if (isLockedByMe()) {
            // already done
            return;
        }
        JcrFsItem item = getFsItem();
        if (item == null) {
            throw new IllegalStateException("Session lock entry has no fsitem");
        }
        if (acquiredReadLock != null) {
            if (upgradeLockIfNecessary) {
                //Will only upgrade if readlocks are not yet shared
                log.trace("Trying read lock to write lock upgarde on '{}'", item);
                //int myReadLocksCount = getLock().getReadHoldCount();
                //Release all our read locks first
                //We may still fail if another thread acquires a new read lock before we try getting a write lock
                //Assuming no-one ever releases read locks owned by other threads!!! (e.g. agressive unlocking of all locks)
                //for (int i = 0; i < myReadLocksCount; i++) {
                //    getReadLock().unlock();
                //}
                acquiredReadLock = null;
                boolean upgraded = getLock().writeLock().tryLock();
                if (!upgraded) {
                    throw new LockingException("Cannot upgrade read to write lock on '" + item +
                            "' - new locks may have been acquired while trying.");
                }
            } else {
                throw new LockingException("Cannot acquire write lock if has read lock on " + item);
            }
        }
        if (lockedFsItem == null) {
            throw new LockingException("Cannot write lock an immutable item " + item);
        }
        acquire("Write", getWriteLock());
    }

    boolean isLockedByMe() {
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
        if (!isLockedByMe() || lockedFsItem == null) {
            throw new LockingException("Cannot save item " + getFsItem() + " which not locked by me!");
        }
        // Save the modified fsItem will return a JCR based immutable fsItem.
        // The return item from save is the immutable.
        // Activate save only if item is not deleted, and was modified
        if (isUnsaved()) {
            immutableFsItem = lockedFsItem.save();
            // Sanity check, if object are not identical after save the save process is failing
            if (!immutableFsItem.isIdentical(lockedFsItem)) {
                // Last modified and created timestamp are managed by JCR so no way to forced them from info -
                // copy them from the JCR immutable item
                lockedFsItem.getInfo().setLastModified(immutableFsItem.getInfo().getLastModified());
                lockedFsItem.getInfo().setCreated(immutableFsItem.getInfo().getCreated());
                if (!immutableFsItem.isIdentical(lockedFsItem)) {
                    throw new IllegalStateException(
                            "Items are not identical after save " + immutableFsItem + " " + lockedFsItem);
                }
            }
            log.trace("Saving lock entry " + getRepoPath());
        }
    }

    void setFsItem(FsItemLockEntry entry) {
        // Lock object should always be equal
        if (lock != entry.getLock()) {
            throw new IllegalStateException(
                    "Updating a session lock " + getFsItem() + " with a lock object different!");
        }
        if (entry.getImmutableFsItem() != null) {
            // Just update the imuutable fsitem (No risk ?)
            immutableFsItem = entry.getImmutableFsItem();
        }
        if (lockedFsItem != null) {
            // If there is one more locked fsitem, something went wrong in lock session management
            if (entry.getLockedFsItem() != lockedFsItem) {
                throw new IllegalStateException("Updating a session lock object with a different mutable fsitem!\n" +
                        "In session=" + immutableFsItem + " received=" + entry.getLockedFsItem());
            }
        } else {
            // If no mutable before just take it.
            lockedFsItem = entry.getLockedFsItem();
        }
    }

    boolean isUnsaved() {
        return !lockedFsItem.isDeleted() && (immutableFsItem == null || !immutableFsItem.isIdentical(lockedFsItem));
    }

    /**
     * Release the read lock if locked
     *
     * @return true if read lock was acquired, false otherwise
     */
    boolean releaseReadLock() {
        log.trace("Releasing READ lock on {}", getFsItem());
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

    private ReentrantReadWriteLock.ReadLock getReadLock() {
        return getLock().readLock();
    }

    private ReentrantReadWriteLock.WriteLock getWriteLock() {
        return getLock().writeLock();
    }

    private void acquire(String lockName, Lock lock) {
        try {
            boolean success = lock.tryLock(ConstantValues.lockTimeoutSecs.getLong(), TimeUnit.SECONDS);
            if (!success) {
                throw new LockingException(lockName + " lock on " + getFsItem() + " not acquired in " +
                        ConstantValues.lockTimeoutSecs.getLong() + " seconds");
            }
        } catch (InterruptedException e) {
            throw new LockingException(lockName + " lock on " + getFsItem() + " not acquired!", e);
        }
    }

    private void releaseWriteLock() {
        log.trace("Releasing WRITE lock on {}", getFsItem());
        if (isLockedByMe()) {
            if (immutableFsItem != null) {
                if (!immutableFsItem.isIdentical(lockedFsItem)) {
                    //Local modification will be discarded
                    log.error("Immutable item {} has local modifications that will be ignored.", lockedFsItem);
                }
            }
            lockedFsItem = null;
            getWriteLock().unlock();
        }
    }
}
