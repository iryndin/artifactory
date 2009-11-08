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

import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.aop.LockingAdvice;

/**
 * @author freds
 * @date Oct 27, 2008
 */
public class LockingHelper {

    public static InternalLockManager getLockManager() {
        InternalLockManager result = LockingAdvice.getLockManager();
        if (result == null) {
            throw new IllegalStateException("No lock manager exists, Please add the " +
                    Lock.class.getName() + " annotation to your method.");
        }
        return result;
    }

    public static FsItemLockEntry readLock(LockEntryId lockEntry) {
        return getLockManager().readLock(lockEntry);
    }

    public static FsItemLockEntry writeLock(LockEntryId lockEntryId) {
        return getLockManager().writeLock(lockEntryId);
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

}
