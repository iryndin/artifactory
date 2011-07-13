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

package org.artifactory.jcr.lock;

import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
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

    FsItemLockEntry readLock(LockEntryId lockEntryId) {
        log.trace("Acquiring read lock on {} in lm={}", lockEntryId, this.hashCode());
        SessionLockEntry sessionLockEntry = getOrCreateSessionLockEntry(lockEntryId);
        sessionLockEntry.acquireReadLock();
        return sessionLockEntry;
    }

    FsItemLockEntry writeLock(LockEntryId lockEntryId) {
        log.trace("Acquiring write lock on {} in lm={}", lockEntryId, this.hashCode());
        SessionLockEntry sessionLockEntry = getOrCreateSessionLockEntry(lockEntryId);
        sessionLockEntry.acquireWriteLock();
        return sessionLockEntry;
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
        log.debug("lm={} updating cache.");
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
        log.trace("Removing lock entry {} for lm={}", repoPath, this.hashCode());
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
            return lockEntry.getLockedFsItem();
        }
        return null;
    }

    private Map<RepoPath, SessionLockEntry> getLocks() {
        return locks;
    }

    private SessionLockEntry getOrCreateSessionLockEntry(LockEntryId lockEntryId) {
        RepoPath repoPath = lockEntryId.getRepoPath();
        SessionLockEntry sessionLockEntry = getSessionLockEntry(repoPath);
        if (sessionLockEntry == null) {
            sessionLockEntry = new SessionLockEntry(lockEntryId);
            log.trace("Creating new SLE for {} in lm={}", repoPath, this.hashCode());
            getLocks().put(repoPath, sessionLockEntry);
        } else {
            log.trace("Reusing existing SLE for {} in lm={}", repoPath, this.hashCode());
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
        log.trace("Release all locks of lm={}", this.hashCode());
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
