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
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Per thread lock manager - bound to the locking advice
 *
 * @author freds
 * @date Nov 17, 2008
 */
public class InternalLockManager {
    private static final Logger log = LoggerFactory.getLogger(InternalLockManager.class);

    private final Map<RepoPath, SessionLockEntry> locks;

    public InternalLockManager() {
        locks = new HashMap<RepoPath, SessionLockEntry>();
    }

    public void unlockAllReadLocks(String repoKey) {
        for (Map.Entry<RepoPath, SessionLockEntry> entry : getLocks().entrySet()) {
            if (entry.getKey().getRepoKey().equals(repoKey)) {
                entry.getValue().releaseReadLock();
            }
        }
    }

    /**
     * This is called from outside the Tx scope
     */
    public void releaseResources() {
        releaseAllLocks();
    }

    public boolean hasPendingResources() {
        if (!hasLocks()) {
            return false;
        }
        for (SessionLockEntry lockEntry : getLocks().values()) {
            if (lockEntry.isLockedByMe() && lockEntry.isUnsaved()) {
                return true;
            }
        }
        return false;
    }

    boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    void debug(String message) {
        log.debug(message);
    }

    void debug(String message, Throwable t) {
        log.debug(message, t);
    }

    void readLock(FsItemLockEntry lockEntry) {
        // TODO: Check that the lock entry has only immutable and no locked fsItem
        SessionLockEntry sessionLockEntry = getOrCreateSessionLockEntry(lockEntry);
        sessionLockEntry.acquireReadLock();
    }

    void writeLock(FsItemLockEntry lockEntry, boolean upgradeLockIfNecessary) {
        // TODO: Check that the lock entry has a locked fsItem
        SessionLockEntry sessionLockEntry = getOrCreateSessionLockEntry(lockEntry);
        sessionLockEntry.acquireWriteLock(upgradeLockIfNecessary);
    }

    /**
     * Release the read lock for this repo path if locked
     *
     * @param repoPath the repo path key
     * @return true if read lock was actually released, false otherwise
     */
    boolean releaseReadLock(RepoPath repoPath) {
        SessionLockEntry sessionLockEntry = getSessionLockEntry(repoPath);
        return sessionLockEntry != null && sessionLockEntry.releaseReadLock();
    }

    //Update the fs item cache of the repo for the locked entries
    void updateCaches() {
        if (!hasLocks()) {
            return;
        }
        Collection<SessionLockEntry> lockEntries = getLocks().values();
        for (SessionLockEntry lockEntry : lockEntries) {
            lockEntry.updateCache();
        }
    }

    boolean removeEntry(RepoPath repoPath) {
        if (getLocks() == null) {
            log.warn("Removing lock entry " + repoPath + " but no locks present!");
            return false;
        }
        SessionLockEntry lockEntry = getLocks().remove(repoPath);
        if (lockEntry == null) {
            log.debug("Removing lock entry " + repoPath + " but not locked by me!");
            return false;
        }
        lockEntry.unlock();
        return true;
    }

    boolean hasLocks() {
        return getLocks() != null && !getLocks().isEmpty();
    }

    void save() {
        if (hasLocks()) {
            for (SessionLockEntry lockEntry : getLocks().values()) {
                if (lockEntry.isLockedByMe()) {
                    //Only save if we acquire the entry's write lock
                    lockEntry.save();
                }
            }
        }
    }

    JcrFsItem getIfLockedByMe(RepoPath repoPath) {
        SessionLockEntry lockEntry = getSessionLockEntry(repoPath);
        if (lockEntry != null && lockEntry.isLockedByMe()) {
            JcrFsItem fsItem = lockEntry.getLockedFsItem();
            if (fsItem == null) {
                throw new IllegalStateException(
                        "Session conatins a lock entry which is write locked but has no mutable fsitem! For " +
                                repoPath);
            }
            return fsItem;
        }
        return null;
    }

    private Map<RepoPath, SessionLockEntry> getLocks() {
        return locks;
    }

    private SessionLockEntry getOrCreateSessionLockEntry(FsItemLockEntry lockEntry) {
        RepoPath repoPath = lockEntry.getRepoPath();
        SessionLockEntry sessionLockEntry = getSessionLockEntry(repoPath);
        if (sessionLockEntry == null) {
            sessionLockEntry = new SessionLockEntry(lockEntry);
            log.trace("Creating new SLE for {}", lockEntry.getRepoPath());
            getLocks().put(repoPath, sessionLockEntry);
        } else {
            log.trace("Reusing existing SLE for {}", lockEntry.getRepoPath());
            sessionLockEntry.setFsItem(lockEntry);
        }
        return sessionLockEntry;
    }

    private SessionLockEntry getSessionLockEntry(RepoPath repoPath) {
        if (getLocks() == null) {
            return null;
        }
        return getLocks().get(repoPath);
    }

    private void releaseAllLocks() {
        try {
            if (!hasLocks()) {
                return;
            }
            Collection<SessionLockEntry> lockEntries = getLocks().values();
            for (SessionLockEntry lockEntry : lockEntries) {
                lockEntry.unlock();
            }
        } finally {
            getLocks().clear();
        }
    }
}
