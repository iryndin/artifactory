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

import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.fs.JcrFsItem;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author freds
 * @date Oct 27, 2008
 */
public class LockingHelper {
    public static boolean hasLockManager() {
        return LockingAdvice.getLockManager() != null;
    }

    public static InternalLockManager getLockManager() {
        InternalLockManager result = LockingAdvice.getLockManager();
        if (result == null) {
            throw new IllegalStateException("No lock manager exists, Please add " +
                    Lock.class.getName() + " annotation to your method");
        }
        return result;
    }

    public static void readLock(LockEntry lockEntry) {
        getLockManager().readLock(lockEntry);
    }

    public static void writeLock(LockEntry lockEntry) {
        getLockManager().writeLock(lockEntry);
    }

    public static JcrFsItem getIfLockedByMe(RepoPath repoPath) {
        return getLockManager().getIfLockedByMe(repoPath);
    }

    /**
     * Release the read lock for this repo path if locked
     *
     * @param repoPath the repo path key
     * @return true if read lock was actually released, false otherwise
     */
    public static boolean releaseReadLock(RepoPath repoPath) {
        return getLockManager().releaseReadLock(repoPath);
    }

    /**
     * Reacquire the read lock, of a lock entry already part of this session. If this session does not have this repo
     * path entry, a LockingException will be thrown.
     *
     * @param repoPath the key to acquire read lock on
     */
    public static void reacquireReadLock(RepoPath repoPath) throws LockingException {
        getLockManager().reacquireReadLock(repoPath);
    }

    /**
     * Method to remove all read lock for this thread on this repo
     *
     * @param repoKey
     */
    public static void unlockAllReadLocks(String repoKey) {
        getLockManager().unlockAllReadLocks(repoKey);
    }

    public static void removeLockEntry(RepoPath repoPath) {
        getLockManager().removeEntry(repoPath);
    }

    public static boolean isInJcrTransaction() {
        return TransactionSynchronizationManager.isSynchronizationActive();
    }
}
