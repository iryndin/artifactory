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
package org.artifactory.jcr.jackrabbit;

import org.apache.jackrabbit.core.persistence.bundle.util.ConnectionRecoveryManager;
import org.artifactory.common.ConstantsValue;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Implementation of a simple ConnectionRecoveryManager pool.
 * The maximum number of pooled objects can be set, and if more objects
 * are requested the pool waits until one object is put back.
 */
public class ArtifactoryPool {
    protected final int maxSize;
    protected final List<ConnectionRecoveryManager> all = new CopyOnWriteArrayList<ConnectionRecoveryManager>();
    protected final ArtifactoryDbDataStore factory;
    protected final BlockingQueue<ConnectionRecoveryManager> pool =
            new LinkedBlockingQueue<ConnectionRecoveryManager>();
    private final long timeout = ConstantsValue.lockTimeoutSecs.getLong() / 10L;

    /**
     * Create a new pool using the given factory and maximum pool size.
     *
     * @param factory the db data store
     * @param maxSize the maximum number of objects in the pool.
     */
    protected ArtifactoryPool(ArtifactoryDbDataStore factory, int maxSize) {
        this.factory = factory;
        this.maxSize = Math.max(1, maxSize);
    }

    /**
     * Get a connection from the pool. This method may open a new connection if
     * required, or if the maximum number of connections are opened, it will
     * wait for one to be returned.
     *
     * @return the connection
     */
    protected ConnectionRecoveryManager get() throws InterruptedException, RepositoryException {
        ConnectionRecoveryManager o = pool.poll();
        if (o == null) {
            if (all.size() < maxSize) {
                o = factory.createNewConnection();
                all.add(o);
            }
            if (o == null) {
                o = pool.poll(timeout, TimeUnit.SECONDS);
            }
        }
        if (o == null) {
            throw new RepositoryException("No connections available in " + timeout + "ms" +
                    " and number of connections " + all.size());
        }
        return o;
    }

    /**
     * But a connection back into the pool.
     *
     * @param o the connection
     */
    protected void add(ConnectionRecoveryManager o) throws InterruptedException {
        pool.add(o);
    }

    /**
     * Get all connections (even if they are currently being used).
     *
     * @return all connections
     */
    protected List<ConnectionRecoveryManager> getAll() {
        return all;
    }
}