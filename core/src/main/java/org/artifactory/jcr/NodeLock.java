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
import javax.jcr.lock.LockException;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class NodeLock {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(NodeLock.class);

    public static void lock(Node node) {
        lock(node, 3, 1000);
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
                if (node.isLocked() || node.isNew()) {
                    return;
                }
                node.lock(false, true);
                return;
            } catch (InvalidItemStateException e) {
                LOGGER.warn("Cannot lock node '" + path + "'.");
                return;
            } catch (LockException e) {
                retries--;
                if (retries > 0) {
                    try {
                        Thread.sleep(msBetweenRetries);
                    } catch (InterruptedException e1) {
                        //Do nothing
                    }
                } else {
                    throw new RuntimeException(
                            "Failed to acquire node lock after " + maxRetries + " retries.");
                }
            } catch (RepositoryException e) {
                throw new RuntimeException("Failed to acquire node lock.", e);
            }
        }
    }
}
