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
package org.artifactory.jcr;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.jackrabbit.api.XASession;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class PoolableSessionFactory extends BasePoolableObjectFactory {
    private static final Logger log = LoggerFactory.getLogger(PoolableSessionFactory.class);

    private Repository repository;
    private StackObjectPool pool;

    public PoolableSessionFactory(Repository repository) {
        this.repository = repository;
    }

    @Override
    public Object makeObject() {
        try {
            Session session = repository.login();
            JcrSession jcrSession = new JcrSession((XASession) session, pool);
            return jcrSession;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to create jcr session.", e);
        }
    }

    @Override
    public void destroyObject(Object obj) throws Exception {
        JcrSession session = (JcrSession) obj;
        session.getSessionResources().releaseResources(false);
        //Extremely important to call this so that all sesion-scoped node locks are cleaned!
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
        if (session.getSessionResources().hasResources()) {
            throw new IllegalStateException(
                    "Cannnot reuse a session that has pending locks or work messages.");
        }
        if (session.hasPendingChanges()) {
            throw new IllegalStateException("Cannnot reuse a session that has pending changes.");
        }
        if (log.isDebugEnabled()) {
            log.debug("Returning pooled session: " + session + ".");
        }
    }

    void setPool(StackObjectPool pool) {
        this.pool = pool;
    }
}