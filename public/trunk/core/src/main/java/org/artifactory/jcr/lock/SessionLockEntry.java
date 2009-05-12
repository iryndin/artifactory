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
package org.artifactory.jcr.lock;

import org.artifactory.common.ConstantsValue;
import org.artifactory.jcr.fs.JcrFsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author freds
 * @date Oct 19, 2008
 */
public class SessionLockEntry {
    private static final Logger log = LoggerFactory.getLogger(SessionLockEntry.class);

    private final ReentrantReadWriteLock lock;
    private JcrFsItem lockedFsItem;
    private JcrFsItem immutableFsItem;
    private Lock acquiredReadLock = null;

    public SessionLockEntry(LockEntry lockEntry) {
        this.lock = lockEntry.getLock();
        this.lockedFsItem = lockEntry.getLockedFsItem();
        this.immutableFsItem = lockEntry.getImmutableFsItem();
    }

    public void acquireReadLock() {
        log.trace("Acquiring READ lock for {}", getFsItem());
        if (acquiredReadLock != null) {
            // already done
            return;
        }
        ReentrantReadWriteLock.ReadLock lock = getAcquiredReadLock();
        acquire("Read", lock);
        acquiredReadLock = lock;
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

    public JcrFsItem getFsItem() {
        if (lockedFsItem != null) {
            return lockedFsItem;
        }
        return immutableFsItem;
    }

    public void setFsItem(JcrFsItem fsItem) {
        if (fsItem.isMutable()) {
            lockedFsItem = fsItem;
        } else {
            immutableFsItem = fsItem;
        }
    }

    public void acquireWriteLock() {
        log.trace("Acquiring WRITE lock for {}", getFsItem());
        if (isLockedByMe()) {
            // already done
            return;
        }
        JcrFsItem item = getFsItem();
        if (acquiredReadLock != null) {
            //log.trace("Upgrading from read lock to write lock on '{}'", item);
            throw new LockingException("Cannot acquire write lock if has read lock for " + item);
        }
        if (lockedFsItem == null) {
            throw new LockingException("Cannot write lock an immutable item " + item);
        }
        acquire("Write", getWriteLock());
    }

    private void acquire(String lockName, Lock lock) {
        try {
            boolean success = lock.tryLock(ConstantsValue.lockTimeoutSecs.getLong(), TimeUnit.SECONDS);
            if (!success) {
                throw new LockingException(lockName + " lock on " + getFsItem() + " not acquired in " +
                        ConstantsValue.lockTimeoutSecs.getLong() + " seconds");
            }
        } catch (InterruptedException e) {
            throw new LockingException(lockName + " lock on " + getFsItem() + " not acquired!", e);
        }
    }

    public boolean isLockedByMe() {
        return getLock().isWriteLockedByCurrentThread();
    }

    public void unlock() {
        releaseReadLock();
        releaseWriteLock();
    }

    public void updateCache() {
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

    private ReentrantReadWriteLock.ReadLock getAcquiredReadLock() {
        return getLock().readLock();
    }

    private ReentrantReadWriteLock.WriteLock getWriteLock() {
        return getLock().writeLock();
    }

    public void save() {
        if (!isLockedByMe()) {
            throw new LockingException("Cannot save item " + getFsItem() + " which not locked by me!");
        }
        if (lockedFsItem != null && !lockedFsItem.isDeleted()) {
            if (immutableFsItem != null) {
                if (!immutableFsItem.isIdentical(lockedFsItem)) {
                    immutableFsItem = lockedFsItem.save();
                }
            } else {
                immutableFsItem = lockedFsItem.save();
            }
        }
    }

    /**
     * Release the read lock if locked
     *
     * @return true if read lock was acquired, false otherwise
     */
    public boolean releaseReadLock() {
        log.trace("Releasing READ lock for {}", getFsItem());
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

    private void releaseWriteLock() {
        log.trace("Releasing WRITE lock for {}", getFsItem());
        if (isLockedByMe()) {
            getWriteLock().unlock();
        }
    }
}
