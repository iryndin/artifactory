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

package org.artifactory.repo.http;

import com.google.common.collect.Maps;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.artifactory.common.ConstantValues;
import org.artifactory.observers.CloseableObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Service providing idle connections monitoring
 * for {@link PoolingHttpClientConnectionManager}
 *
 * @author Michael Pasternak
 */
@Lazy(true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service
public class IdleConnectionMonitorServiceImpl implements IdleConnectionMonitorService, CloseableObserver {

    private static final Logger log = LoggerFactory.getLogger(IdleConnectionMonitorServiceImpl.class);
    private static int MONITOR_INTERVAL;

    private IdleConnectionMonitorThread idleConnectionMonitorThread;
    private final Map<Object, PoolingHttpClientConnectionManager> remoteRepoHttpConnMgrList = Maps.newConcurrentMap();

    public IdleConnectionMonitorServiceImpl() {
        MONITOR_INTERVAL = ConstantValues.idleConnectionMonitorInterval.getInt() * 1000;
        createTread();
    }

    /**
     * Creates thread
     */
    private void createTread() {
        if(!ConstantValues.disableIdleConnectionMonitoring.getBoolean()) {
            idleConnectionMonitorThread = new IdleConnectionMonitorThread(remoteRepoHttpConnMgrList);
            idleConnectionMonitorThread.setName("Idle Connection Monitor");
            idleConnectionMonitorThread.setDaemon(true);
            idleConnectionMonitorThread.start();
        }
    }

    /**
     * @return {@link Thread.State}
     */
    @Nullable
    @Override
    public Thread.State getStatus() {
        return idleConnectionMonitorThread != null ?
                idleConnectionMonitorThread.getState() : null;
    }

    /**
     * Causes this service to begin idle connections monitoring
     *
     * @exception  IllegalThreadStateException  if the thread was already started.
     */
    @Override
    public void start() {
        if(!ConstantValues.disableIdleConnectionMonitoring.getBoolean() &&
                (idleConnectionMonitorThread == null || !idleConnectionMonitorThread.isAlive()))
            createTread();
    }

    /**
     * Stops idleConnection monitoring
     */
    @Override
    public final void stop() {
        log.debug("Stopping IdleConnectionMonitorService");
        if (idleConnectionMonitorThread != null) {
            idleConnectionMonitorThread.shutdown();
        }
    }

    /**
     * Adds {@link org.apache.http.impl.conn.PoolingHttpClientConnectionManager} to monitor
     *
     * @param owner the owner of connectionManager
     * @param connectionManager {@link PoolingHttpClientConnectionManager}
     */
    @Override
    public final void add(Object owner, PoolingHttpClientConnectionManager connectionManager) {
        if (owner != null && connectionManager != null) {
            log.debug("Performing add request for params owner: {}, connectionManager: {}",
                    owner, connectionManager);
            remoteRepoHttpConnMgrList.put(owner, connectionManager);
        } else {
            log.debug("Ignoring add request for params owner: {}, connectionManager: {}",
                    owner, connectionManager);
        }
    }

    /**
     * Removes monitored {@link PoolingHttpClientConnectionManager}
     *
     * @param owner the object that owns this PoolingHttpClientConnectionManager
     */
    @Override
    public final void remove(Object owner) {
        if (owner != null) {
            log.debug("Performing remove request for owner: {}", owner);
            remoteRepoHttpConnMgrList.remove(owner);
        } else {
            log.debug("Ignoring remove request for undefined owner");
        }
    }

    /**
     * Invoked by observed objects on close() event {@see CloseableObserver}
     *
     * @param object the owner of observed event
     */
    @Override
    public void onObservedClose(Object object) {
        remove(object);
    }

    /**
     * thread to monitor expired and idle connection , if found clear it and return it back to pool
     */
    private static class IdleConnectionMonitorThread extends Thread {

        private final Map<Object, PoolingHttpClientConnectionManager> connMgrList;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(Map<Object, PoolingHttpClientConnectionManager> connMgrList) {
            super();
            this.connMgrList = connMgrList;
        }

        @Override
        public void run() {
            try {
                log.debug("Starting Idle Connection Monitor Thread ");
                synchronized (this) {
                    while (!shutdown) {
                        wait(MONITOR_INTERVAL);
                        if (!connMgrList.isEmpty()) {
                            for (PoolingHttpClientConnectionManager connPollMgr : connMgrList.values()) {
                                if (connPollMgr != null) {
                                    log.debug("Cleaning idle connections for ConnectionManager: {}", connPollMgr);
                                    connPollMgr.closeExpiredConnections();
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException ex) {
                log.debug("Terminating Idle Connection Monitor Thread ");
            }
        }

        public void shutdown() {
            log.debug("Shutdown Idle Connection Monitor Thread ");
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
