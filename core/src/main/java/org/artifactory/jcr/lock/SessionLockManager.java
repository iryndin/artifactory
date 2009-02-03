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
package org.artifactory.jcr.lock;


import org.artifactory.tx.SessionResource;

/**
 * @author freds
 * @date Sep 5, 2008
 */
public class SessionLockManager implements SessionResource {

    /**
     * Called afterCompletion of the TX manager
     *
     * @param commit true if committed
     */
    public void releaseResources(boolean commit) {
        if (LockingHelper.hasLockManager()) {
            if (commit) {
                LockingAdvice.getLockManager().updateCache();
            } else {
                // TODO: Release early on rollback
            }
        }
    }

    public boolean hasResources() {
        return LockingHelper.hasLockManager() && LockingAdvice.getLockManager().hasResources();
    }

    public boolean hasPendingChanges() {
        return LockingHelper.hasLockManager() && LockingAdvice.getLockManager().hasPendingChanges();
    }

    public void save() {
        if (LockingHelper.hasLockManager()) {
            LockingAdvice.getLockManager().save();
        }
    }
}
