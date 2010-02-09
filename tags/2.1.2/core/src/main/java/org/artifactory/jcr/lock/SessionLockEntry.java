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

    private final LockEntryId id;
    private JcrFsItem lockedFsItem;
    private JcrFsItem immutableFsItem;
    //The (per-session) acquired lock state
    private ReentrantReadWriteLock.ReadLock acquiredReadLock;

    SessionLockEntry(LockEntryId lockEntryId) {
        this.id = lockEntryId;
    }

    public ReentrantReadWriteLock getLock() {
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
        ReentrantReadWriteLock.ReadLock lock = getReadLock();
        acquire("Read", lock);
        acquiredReadLock = lock;
    }

    void acquireWriteLock() {
        log.trace("Acquiring WRITE lock on {}", id);
        if (isLockedByMe()) {
            // already done
            return;
        }
        if (acquiredReadLock != null) {
            throw new LockingException("Cannot acquire write lock if has read lock on " + id);
        }
        acquire("Write", getWriteLock());
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

    boolean isUnsaved() {
        return lockedFsItem != null && !lockedFsItem.isDeleted() && (immutableFsItem == null || !immutableFsItem.isIdentical(lockedFsItem));
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
                throw new LockingException(lockName + " lock on " + id + " not acquired in " +
                        ConstantValues.lockTimeoutSecs.getLong() + " seconds");
            }
        } catch (InterruptedException e) {
            throw new LockingException(lockName + " lock on " + id + " not acquired!", e);
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
            getWriteLock().unlock();
        }
    }
}
