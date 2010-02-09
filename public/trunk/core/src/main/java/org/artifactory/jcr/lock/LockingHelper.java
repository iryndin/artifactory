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

import org.artifactory.api.repo.Lock;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.concurrent.LockingException;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.jcr.lock.aop.LockingAdvice;

/**
 * Public lock helper exposing static operations on the thread (tx) based lock manager.
 *
 * @author freds
 * @author Yoav Landman
 */
public class LockingHelper {

    private static LockingExceptionListener lockingExceptionListener;

    public static InternalLockManager getSessionLockManager() {
        InternalLockManager result = LockingAdvice.getLockManager();
        if (result == null) {
            throw new IllegalStateException("No lock manager exists, Please add the " +
                    Lock.class.getName() + " annotation to your method.");
        }
        return result;
    }

    public static FsItemLockEntry readLock(LockEntryId lockEntry) {
        try {
            return getSessionLockManager().readLock(lockEntry);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    public static FsItemLockEntry writeLock(LockEntryId lockEntryId) {
        try {
            return getSessionLockManager().writeLock(lockEntryId);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    public static JcrFsItem getIfLockedByMe(RepoPath repoPath) {
        try {
            return getSessionLockManager().getIfLockedByMe(repoPath);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    /**
     * Release the read lock for this repo path if locked
     *
     * @param repoPath the repo path key
     * @return true if read lock was actually released, false otherwise
     */
    public static boolean releaseReadLock(RepoPath repoPath) {
        try {
            return getSessionLockManager().releaseReadLock(repoPath);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    /**
     * Method to remove all read lock for this thread on this repo
     *
     * @param repoKey
     */
    public static void unlockAllReadLocks(String repoKey) {
        try {
            getSessionLockManager().unlockAllReadLocks(repoKey);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    public static void removeLockEntry(RepoPath repoPath) {
        try {
            getSessionLockManager().removeEntry(repoPath);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    public static void registerLockingExceptionListener(LockingExceptionListener lockingExceptionListener) {
        LockingHelper.lockingExceptionListener = lockingExceptionListener;
    }

    private static void handleException(RuntimeException e) {
        if (lockingExceptionListener != null) {
            if (e instanceof LockingException) {
                lockingExceptionListener.onLockingException((LockingException) e);
            }
        }
    }

    /**
     * Listener for locking exceptions (session and repository wide) - used for testing mainly
     */
    public interface LockingExceptionListener {
        void onLockingException(LockingException e);
    }
}
