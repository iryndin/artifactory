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

package org.artifactory.jcr;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.log.LoggerFactory;
import org.artifactory.tx.SessionResourceManager;
import org.slf4j.Logger;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * @author yoavl
 */
public class PoolableSessionFactory extends BasePoolableObjectFactory {
    private static final Logger log = LoggerFactory.getLogger(PoolableSessionFactory.class);

    private Repository repository;
    private ObjectPool pool;

    public PoolableSessionFactory(Repository repository) {
        this.repository = repository;
    }

    @Override
    public Object makeObject() {
        try {
            Session session = repository.login(new SimpleCredentials(SecurityConstants.ADMIN_ID, new char[]{}));
            JcrSession jcrSession = new JcrSession((XASession) session, pool);
            return jcrSession;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to create jcr session.", e);
        }
    }

    @Override
    public void destroyObject(Object obj) throws Exception {
        JcrSession session = (JcrSession) obj;
        validateSessionCleaness(session);
        //Extremely important to call this so that all session-scoped node locks are cleaned!
        session.getSession().logout();
    }

    @Override
    public boolean validateObject(Object obj) {
        JcrSession session = (JcrSession) obj;
        return session.isLive();
    }

    @Override
    public void activateObject(Object obj) throws Exception {
        super.activateObject(obj);
    }

    @Override
    public void passivateObject(Object obj) throws Exception {
        JcrSession session = (JcrSession) obj;
        log.debug("Returning pooled session: {}.", session);
        super.passivateObject(obj);
    }

    void setPool(ObjectPool pool) {
        this.pool = pool;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void validateSessionCleaness(JcrSession session) {
        final SessionResourceManager resourceManager = session.getSessionResourceManager();
        if (resourceManager.hasPendingResources()) {
            IllegalStateException e =
                    new IllegalStateException("Tried to return a session with unprocessed pending resources (" +
                            resourceManager.getClass().getName() + ".");
            log.error("Session passivation error.", e);
            //Throw in order to cause pooled object destruction
            throw e;
        }
    }
}