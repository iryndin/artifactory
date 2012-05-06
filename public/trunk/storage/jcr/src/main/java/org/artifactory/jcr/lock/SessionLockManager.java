/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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


import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.tx.SessionResource;

/**
 * A lock manager that delegates to the internal lock manager, updating it with session events: Save write locks state
 * to jcr when the session is saved, and Update the repository fsItems cache with the committed state of write locks
 * <p/>
 * The manager is created once per session and attached to it for its lifetime. Cleanup of state on passivation back to
 * pool is therefore crucial.
 *
 * @author freds
 * @author yoavl
 */
public class SessionLockManager implements SessionResource {

    @Override
    public boolean hasPendingResources() {
        InternalLockManager lockManager = LockingAdvice.getLockManager();
        return lockManager != null && lockManager.hasPendingResources();
    }

    @Override
    public void onSessionSave() {
        InternalLockManager lockManager = LockingAdvice.getLockManager();
        if (lockManager != null) {
            //Save the entries having write locks
            lockManager.save();
        }
    }

    /**
     * Called afterCompletion of the TX manager
     *
     * @param commit true if committed
     */
    @Override
    public void afterCompletion(boolean commit) {
        InternalLockManager lockManager = LockingAdvice.getLockManager();
        if (lockManager != null) {
            if (commit) {
                // JCR committed successfully - update caches
                lockManager.updateCaches();
            } else {
                // Release early on rollback
                lockManager.releaseResources();
            }
        }
    }
}
