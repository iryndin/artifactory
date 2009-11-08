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

package org.artifactory.jcr.jackrabbit;

import org.artifactory.common.ConstantValues;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Implementation of a simple ConnectionRecoveryManager pool. The maximum number of pooled objects can be set, and if
 * more objects are requested the pool waits until one object is put back.
 */
public class ArtifactoryPool {
    protected final int maxSize;
    protected final List<ArtifactoryConnectionRecoveryManager> all =
            new CopyOnWriteArrayList<ArtifactoryConnectionRecoveryManager>();
    protected final ArtifactoryBaseDataStore factory;
    protected final BlockingQueue<ArtifactoryConnectionRecoveryManager> pool =
            new LinkedBlockingQueue<ArtifactoryConnectionRecoveryManager>();
    private final long timeout = ConstantValues.lockTimeoutSecs.getLong() / 10L;

    /**
     * Create a new pool using the given factory and maximum pool size.
     *
     * @param factory the db data store
     * @param maxSize the maximum number of objects in the pool.
     */
    protected ArtifactoryPool(ArtifactoryBaseDataStore factory, int maxSize) {
        this.factory = factory;
        this.maxSize = Math.max(1, maxSize);
    }

    /**
     * Get a connection from the pool. This method may open a new connection if required, or if the maximum number of
     * connections are opened, it will wait for one to be returned.
     *
     * @return the connection
     */
    protected ArtifactoryConnectionRecoveryManager get() throws InterruptedException, RepositoryException {
        ArtifactoryConnectionRecoveryManager o = pool.poll();
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
            throw new RepositoryException(
                    "No connections available in " + timeout + " seconds and number of connections " + all.size());
        }
        return o;
    }

    /**
     * But a connection back into the pool.
     *
     * @param o the connection
     */
    protected void add(ArtifactoryConnectionRecoveryManager o) throws InterruptedException {
        pool.add(o);
    }

    /**
     * Get all connections (even if they are currently being used).
     *
     * @return all connections
     */
    protected List<ArtifactoryConnectionRecoveryManager> getAll() {
        return all;
    }
}