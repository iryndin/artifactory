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
package org.artifactory.jcr.md;

import org.artifactory.common.ArtifactoryConstants;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.lock.SessionLockManager;
import org.artifactory.spring.InternalContextHelper;

import java.util.Calendar;

/**
 * @author freds
 * @date Sep 4, 2008
 */
public class MetadataValue {
    public enum State {
        PERSISTENT, TRANSIENT
    }

    private final Object lock = new Object();
    private final MetadataKey key;

    private State status;
    private Object value;
    private long lastModified;

    private Long lockOwner = null;
    private Object newMetadataValue = null;
    private long newLastModified = 0L;

    MetadataValue(MetadataKey key) {
        this.key = key;
        this.value = null;
        this.lastModified = 0L;
        this.status = State.TRANSIENT;
    }

    MetadataValue(MetadataKey key, Object value, long lastModified) {
        this.key = key;
        this.value = value;
        this.lastModified = lastModified;
        this.status = State.PERSISTENT;
    }

    public boolean isTransient() {
        return status == State.TRANSIENT;
    }

    public void setNormal() {
        status = State.PERSISTENT;
    }

    public Object getValue() {
        if (isLockedByMe()) {
            return newMetadataValue;
        }
        return value;
    }

    public void waitIfTransient() {
        synchronized (lock) {
            if (isTransient()) {
                // Currently created by another thread.
                // Will wait for it to finish.
                try {
                    waitForUnlock();
                } catch (InterruptedException e) {
                    throw new LockingException("Thread was interrupted waiting for lock", e);
                }
            }
        }
    }

    public boolean isLockedByMe() {
        SessionLockManager sessionLockManager = getSessionLockManager();
        MetadataValue localValue = sessionLockManager.getLockedMetadata(key);
        if (localValue == null) {
            return false;
        }
        // This sanity check synchronizes on lock so active only in debug
        if (sessionLockManager.isDebugEnabled()) {
            assertLockOwner();
        }
        return true;
    }

    public void assertLockOwner() {
        Long threadId = Thread.currentThread().getId();
        synchronized (lock) {
            if (!threadId.equals(lockOwner)) {
                throw new LockingException(
                        "Metadata '" + key + "' is not locked by current session.");
            }
        }
    }

    public Object getNewMetadataValue() {
        return newMetadataValue;
    }

    public void setNewMetadataValue(Object newMetadataValue) {
        this.newMetadataValue = newMetadataValue;
    }

    public void setNewLastModified(Calendar newLastModified) {
        this.newLastModified = newLastModified.getTimeInMillis();
    }

    public MetadataKey getKey() {
        return key;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void lock() {
        if (isLockedByMe()) {
            // Nothing to do, everything OK
            return;
        }
        long t0 = System.nanoTime();
        synchronized (lock) {
            // Due to Notify All I need to loop
            Thread thread = Thread.currentThread();
            Long thId = thread.getId();
            while (!thId.equals(lockOwner) && !hasExpiredTimeout(t0) &&
                    !thread.isInterrupted()) {
                try {
                    if (lockOwner == null) {
                        setLock(thId);
                        return;
                    }
                    // Already locked
                    waitForUnlock();
                } catch (InterruptedException e) {
                    throw new LockingException(
                            "Thread " + thread + " was interrupted waiting for lock", e);
                }
            }
        }
        throw new LockingException(
                "Node " + key.getAbsPath() + " is already locked by " + lockOwner +
                        " and could not acquire lock after " +
                        (System.nanoTime() - t0) / (1000000000L) + " seconds");
    }

    public void unlock(SessionLockManager sessionLockManager, boolean commit) {
        synchronized (lock) {
            lockOwner = null;
            if (commit) {
                if (sessionLockManager.isDebugEnabled()) {
                    sessionLockManager.debug("Unlocked with commit " + this);
                }
                value = newMetadataValue;
                lastModified = newLastModified;
                newMetadataValue = null;
                newLastModified = -1L;
                status = State.PERSISTENT;
            } else {
                if (sessionLockManager.isDebugEnabled()) {
                    sessionLockManager.debug("Unlocked with rollback " + this);
                }
                newMetadataValue = null;
                newLastModified = -1L;
                if (status == State.TRANSIENT) {
                    // Re initialize new metadata
                    initNewMetadata(getDefinition());
                }
            }
            sessionLockManager.removeEntry(key);
            lock.notifyAll();
        }
    }

    public void update(Object value, long lastModified) {
        synchronized (lock) {
            if (status == State.PERSISTENT) {
                // Just reloading from DB
                this.value = value;
                this.lastModified = lastModified;
                return;
            }
            throw new LockingException(
                    "Cannot update from DB a locked or transient Metadata object " + this);
        }
    }

    @Override
    public String toString() {
        return "MetadataValue{" +
                "key=" + key +
                ", status=" + status +
                ", metadataValue=" + value +
                ", lastModified=" + lastModified +
                ", lockOwner=" + lockOwner +
                ", newMetadataValue=" + newMetadataValue +
                ", newLastModified=" + newLastModified +
                '}';
    }

    private static long getWaitingTime() {
        return ArtifactoryConstants.lockTimeoutSecs * 1000L;
    }

    private static boolean hasExpiredTimeout(long time) {
        return (System.nanoTime() - time) > (getWaitingTime() * 1000000L);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    private void setLock(Long thId) {
        lockOwner = thId;
        initNewMetadata(getDefinition());
        SessionLockManager sessionLockManager = getSessionLockManager();
        sessionLockManager.addMetadataObject(this);
        if (sessionLockManager.isDebugEnabled()) {
            sessionLockManager.debug("Set lock for " + thId + " on " + this,
                    new LockingException("just stack info"));
        }
    }

    private void initNewMetadata(MetadataDefinition definition) {
        if (value != null) {
            newMetadataValue = definition.newInstance(value);
        } else {
            newMetadataValue = definition.newInstance();
        }
        newLastModified = -1L;
    }

    private void waitForUnlock() throws InterruptedException {
        SessionLockManager sessionLockManager = getSessionLockManager();
        if (sessionLockManager.isDebugEnabled()) {
            sessionLockManager.debug(
                    "Node " + key.getAbsPath() + " is already locked by " + lockOwner +
                            " waiting " + getWaitingTime() +
                            " milli seconds");
        }
        lock.wait(getWaitingTime());
    }

    private MetadataDefinition getDefinition() {
        return getMetadataDefinitionService().getMetadataDefinition(key.getMetadataName());
    }

    private static SessionLockManager getSessionLockManager() {
        JcrService jcr = InternalContextHelper.get().getJcrService();
        return jcr.getManagedSession().getLockManager();
    }

    private static MetadataDefinitionService getMetadataDefinitionService() {
        return InternalContextHelper.get().beanForType(MetadataDefinitionService.class);
    }
}
