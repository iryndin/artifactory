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

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Immutable lock holder - holds a RWLock per a certain repo fsItem. RW-locks are managed per storing repo and are
 * passed to each session's InternalLockManager. On the InternalLockManager each session performs read-write
 * lock/release operations on the saved locks.
 *
 * @author freds
 * @date Oct 19, 2008
 */
public class ImmutableLockEntry implements FsItemLockEntry {
    /**
     * The unique lock from the lock maps for the repo path of the items
     */
    private final ReentrantReadWriteLock lock;
    /**
     * The fsitem from the cache, that cannot be modified. Can be null when creating a new entry.
     */
    private final JcrFsItem immutableFsItem;
    /**
     * The fsitem created for the session write lock. Should be null if no write lock acquired.
     */
    private final JcrFsItem lockedFsItem;

    public ImmutableLockEntry(ReentrantReadWriteLock lock, JcrFsItem immutableFsItem, JcrFsItem lockedFsItem) {
        if (lock == null) {
            throw new IllegalArgumentException("Cannot create lock entry with no lock object for " +
                    "inCache=" + immutableFsItem + " locked=" + lockedFsItem);
        }
        if (immutableFsItem == null && lockedFsItem == null) {
            throw new IllegalArgumentException("Cannot create lock entry with no file system object for " +
                    "inCache=" + immutableFsItem + " locked=" + lockedFsItem);
        }
        if (immutableFsItem != null && lockedFsItem != null &&
                (!immutableFsItem.equals(lockedFsItem) ||
                        !immutableFsItem.getRepoPath().equals(lockedFsItem.getRepoPath()))) {
            throw new IllegalArgumentException("Lock entry has incoherent file system object for " +
                    "inCache=" + immutableFsItem + " locked=" + lockedFsItem);
        }
        if (immutableFsItem != null && immutableFsItem.isMutable()) {
            throw new IllegalArgumentException(
                    "Lock entry created with an immutable file system object which is mutable for " +
                            "inCache=" + immutableFsItem + " locked=" + lockedFsItem);
        }
        if (lockedFsItem != null && !lockedFsItem.isMutable()) {
            throw new IllegalArgumentException(
                    "Lock entry created with an mutable file system object which is not mutable for " +
                            "inCache=" + immutableFsItem + " locked=" + lockedFsItem);
        }
        this.lock = lock;
        this.immutableFsItem = immutableFsItem;
        this.lockedFsItem = lockedFsItem;
    }

    public RepoPath getRepoPath() {
        return getFsItem().getRepoPath();
    }

    public JcrFsItem getFsItem() {
        if (lockedFsItem != null) {
            return lockedFsItem;
        }
        return immutableFsItem;
    }

    public JcrFsItem getLockedFsItem() {
        return lockedFsItem;
    }

    public JcrFsItem getImmutableFsItem() {
        return immutableFsItem;
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }
}