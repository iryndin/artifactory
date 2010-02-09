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
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class JcrSessionWrapperFactory extends BasePoolableObjectFactory {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(JcrSessionWrapperFactory.class);

    private JackrabbitRepository repository;

    public JcrSessionWrapperFactory(JackrabbitRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable"})
    public Object makeObject() {
        try {
            Session session = repository.login();
            JcrSessionWrapper sessionWrapper = new JcrSessionWrapper(session);
            return sessionWrapper;
        } catch (RepositoryException e) {
            throw new RuntimeException("Failed to create jcr session.", e);
        }
    }

    @Override
    public void destroyObject(Object obj) throws Exception {
        JcrSessionWrapper sessionWrapper = (JcrSessionWrapper) obj;
        //Extremely important to call this so that all sesion-scoped node locks are cleaned!
        sessionWrapper.logout();
    }

    @Override
    public boolean validateObject(Object obj) {
        JcrSessionWrapper sessionWrapper = (JcrSessionWrapper) obj;
        return sessionWrapper.isLive() && !sessionWrapper.isRollbackOnly();
    }

    @Override
    public void activateObject(Object obj) throws Exception {
        super.activateObject(obj);
    }

    @Override
    public void passivateObject(Object obj) throws Exception {
        JcrSessionWrapper sessionWrapper = (JcrSessionWrapper) obj;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Returning pooled session: " + sessionWrapper + ".");
        }
        sessionWrapper.release();
    }
}