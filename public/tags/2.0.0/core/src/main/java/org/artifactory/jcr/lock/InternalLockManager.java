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

import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.fs.JcrFsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author freds
 * @date Nov 17, 2008
 */
public class InternalLockManager {
    private static final Logger log = LoggerFactory.getLogger(InternalLockManager.class);

    private final Map<RepoPath, SessionLockEntry> locks;

    public InternalLockManager() {
        locks = new HashMap<RepoPath, SessionLockEntry>();
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public void debug(String message) {
        log.debug(message);
    }

    public void debug(String message, Throwable t) {
        log.debug(message, t);
    }

    public void readLock(LockEntry lockEntry) {
        SessionLockEntry sessionLockEntry = getOrCreateSessionLockEntry(lockEntry);
        sessionLockEntry.acquireReadLock();
    }

    public void writeLock(LockEntry lockEntry) {
        SessionLockEntry sessionLockEntry = getOrCreateSessionLockEntry(lockEntry);
        sessionLockEntry.acquireWriteLock();
    }

    private Map<RepoPath, SessionLockEntry> getLocks() {
        return locks;
    }

    private SessionLockEntry getOrCreateSessionLockEntry(LockEntry lockEntry) {
        SessionLockEntry sessionLockEntry = getSessionLockEntry(lockEntry.getRepoPath());
        if (sessionLockEntry == null) {
            sessionLockEntry = new SessionLockEntry(lockEntry);
            getLocks().put(lockEntry.getRepoPath(), sessionLockEntry);
        } else {
            sessionLockEntry.setFsItem(lockEntry.getFsItem());
        }
        return sessionLockEntry;
    }

    private SessionLockEntry getSessionLockEntry(RepoPath repoPath) {
        if (getLocks() == null) {
            return null;
        }
        return getLocks().get(repoPath);
    }

    public JcrFsItem getIfLockedByMe(RepoPath repoPath) {
        SessionLockEntry lockEntry = getSessionLockEntry(repoPath);
        if (lockEntry != null && lockEntry.isLockedByMe()) {
            return lockEntry.getLockedFsItem();
        }
        return null;
    }

    /**
     * This is called from outside the Tx scope
     */
    public void releaseResources() {
        releaseAllLocks();
    }

    public void updateCache() {
        if (!hasResources()) {
            return;
        }
        Collection<SessionLockEntry> lockEntries = getLocks().values();
        for (SessionLockEntry lockEntry : lockEntries) {
            lockEntry.updateCache();
        }
    }

    private void releaseAllLocks() {
        try {
            if (!hasResources()) {
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

    public boolean removeEntry(RepoPath repoPath) {
        if (getLocks() == null) {
            log.warn("Removing lock entry " + repoPath + " but no locks present!");
            return false;
        }
        SessionLockEntry lockEntry = getLocks().remove(repoPath);
        if (lockEntry == null) {
            log.warn("Removing lock entry " + repoPath + " but not locked by me!");
            return false;
        }
        lockEntry.unlock();
        return true;
    }

    public boolean hasResources() {
        return getLocks() != null && !getLocks().isEmpty();
    }

    public boolean hasPendingChanges() {
        if (!hasResources()) {
            return false;
        }
        for (SessionLockEntry lockEntry : getLocks().values()) {
            if (lockEntry.isLockedByMe()) {
                return true;
            }
        }
        return false;
    }

    public void save() {
        if (hasResources()) {
            for (SessionLockEntry lockEntry : getLocks().values()) {
                if (lockEntry.isLockedByMe()) {
                    lockEntry.save();
                }
            }
        }
    }

    /**
     * Release the read lock for this repo path if locked
     *
     * @param repoPath the repo path key
     * @return true if read lock was actually released, false otherwise
     */
    public boolean releaseReadLock(RepoPath repoPath) {
        SessionLockEntry sessionLockEntry = getSessionLockEntry(repoPath);
        return sessionLockEntry != null && sessionLockEntry.releaseReadLock();
    }

    public void reacquireReadLock(RepoPath repoPath) {
        SessionLockEntry sessionLockEntry = getSessionLockEntry(repoPath);
        if (sessionLockEntry != null) {
            sessionLockEntry.acquireReadLock();
        } else {
            throw new LockingException(
                    "Repo path " + repoPath + " does not have a session lock entry!\n" +
                            "Cannot reacquire the read lock.");
        }
    }

    public void unlockAllReadLocks(String repoKey) {
        for (Map.Entry<RepoPath, SessionLockEntry> entry : getLocks().entrySet()) {
            if (entry.getKey().getRepoKey().equals(repoKey)) {
                entry.getValue().releaseReadLock();
            }
        }
    }
}
