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

import org.apache.log4j.Logger;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Manual node locking facility. This is required for the first place, because JCR uses optimistic
 * concurrency strategy and will fail a session transaction for concurrent state modifications,
 * unless node locking is used to avoid such modifications.
 * <p/>
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class NodeLock {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(NodeLock.class);

    public static String SYS_PROP_JCR_LOCKING_STICT = "artifactory.jcr.locking.strict";

    private static final boolean useStrictLocking = Boolean.parseBoolean(System.getProperty(
            SYS_PROP_JCR_LOCKING_STICT, Boolean.FALSE.toString()));

    public static void lock(Node node) {
        lock(node, 15, 2000);
    }

    public static void lock(Node node, int maxRetries, int msBetweenRetries) {
        JcrSessionWrapper session = JcrSessionThreadBinder.getSession();
        boolean rollbackOnly = session.isRollbackOnly();
        if (rollbackOnly) {
            return;
        }
        int retries = maxRetries;
        String path = null;
        while (true) {
            try {
                path = node.getPath();
                //Don't lock an already locked or newly created node
                if (!node.isNodeType("mix:lockable") || isLockedByCurrentSession(node) ||
                        node.isNew()) {
                    return;
                }
                node.lock(false, true);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Locked node '" + path + "' with session " +
                            node.getSession());
                }
                return;
            } catch (InvalidItemStateException e) {
                String message = "Cannot lock node '" + path + "'.";
                LOGGER.warn(message);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(message + ". Cause:", e);
                }
                return;
            } catch (LockException e) {
                String message = "Cannot lock node '" + path + "'.";
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(message + ". Cause:", e);
                }
                retries--;
                if (retries > 0) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Retrying lock on '" + path + "' in " + msBetweenRetries +
                                    "ms.");
                        }
                        Thread.sleep(msBetweenRetries);
                    } catch (InterruptedException e1) {
                        //Do nothing
                    }
                } else {
                    throw new RuntimeException(
                            "Failed to acquire node lock on '" + path + "' after " + maxRetries +
                                    " retries.");
                }
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to acquire node lock.", e);
            }
        }
    }

    static boolean isLockedByCurrentSession(Node node) throws RepositoryException {
        if (!node.isLocked()) {
            return false;
        }
        Lock lock = node.getLock();
        return lock != null && lock.getLockToken() != null;
    }

    public static boolean isUseStrictLocking() {
        return useStrictLocking;
    }
}
