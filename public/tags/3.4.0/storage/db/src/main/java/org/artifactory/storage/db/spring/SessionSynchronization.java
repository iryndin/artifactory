/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.storage.db.spring;

import org.artifactory.storage.fs.session.StorageSession;
import org.artifactory.storage.fs.session.StorageSessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

/**
 * Callback for synchronization between Artifactory {@link org.artifactory.storage.fs.session.StorageSession} and the
 * JDBC transaction.
 *
 * @author Yossi Shaul
 */
public class SessionSynchronization extends TransactionSynchronizationAdapter {
    private static final Logger log = LoggerFactory.getLogger(SessionSynchronization.class);

    private final StorageSession session;

    private boolean sessionActive = true;

    public SessionSynchronization(StorageSession session) {
        this.session = session;
        StorageSessionHolder.setSession(session);
    }

    @Override
    public void suspend() {
        if (sessionActive) {
            StorageSessionHolder.removeSession();
            //TransactionSynchronizationManager.unbindResource(this.session);
        }
        sessionActive = false;
    }

    @Override
    public void resume() {
        if (sessionActive) {
            log.warn("TX-resume when session is already active: {}", session);
            //TransactionSynchronizationManager.bindResource(this.session, this.session);
        }
        StorageSessionHolder.setSession(session);
        sessionActive = true;
    }

    @Override
    public void beforeCommit(boolean readOnly) {
        if (!sessionActive) {
            log.trace("Session {} is not active", session);
            return;
        }

        //Save any pending changes (no need to test for rollback at this phase)
        log.debug("Saving session: {} .", session);
        session.save();
    }

    @Override
    public void afterCompletion(int status) {
        if (sessionActive) {
            boolean success = status == TransactionSynchronization.STATUS_COMMITTED;
            // Commit the locks/discard changes on rollback
            try {
                session.afterCompletion(success);
            } finally {
                session.releaseResources();
                StorageSessionHolder.removeSession();
            }
        }
    }
}
