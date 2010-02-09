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

import org.apache.commons.pool.impl.StackObjectPool;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.springmodules.jcr.jackrabbit.JackrabbitSessionFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * User: freds Date: Jul 21, 2008 Time: 7:10:02 PM
 */
public class JcrSessionFactory extends JackrabbitSessionFactory {

    private StackObjectPool sessionPool;

    @Override
    public void setRepository(final Repository repository) {
        super.setRepository(repository);

        //Create the session pool
        PoolableSessionFactory pooledSessionFactory =
                new PoolableSessionFactory(repository);
        sessionPool = new StackObjectPool(pooledSessionFactory, 30, 30) {
            @Override
            public void close() throws Exception {
                super.close();
                ((RepositoryImpl) repository).shutdown();
            }
        };
        pooledSessionFactory.setPool(sessionPool);
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        sessionPool.close();
    }

    /**
     * For internal framework use only!
     *
     * @return
     * @throws javax.jcr.RepositoryException
     */
    @Override
    public Session getSession() throws RepositoryException {
        JcrSession session = newSession();
        return addListeners(session);
    }

    /*
    * (non-Javadoc)
    * @see org.springmodules.jcr.JcrSessionFactory#registerNodeTypes()
    */
    @Override
    protected void registerNodeTypes() throws Exception {
        //Do nothing will do it later in init
    }

    @Override
    protected void registerNamespaces() throws Exception {
        //Do nothing will do it later in init
    }

    @Override
    protected void unregisterNodeTypes() throws Exception {
        //Do nothing
    }

    @Override
    protected void unregisterNamespaces() throws Exception {
        //Do nothing
    }

    private JcrSession newSession() {
        try {
            JcrSession session = (JcrSession) sessionPool.borrowObject();
            return session;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create jcr session.", e);
        }
    }
}
