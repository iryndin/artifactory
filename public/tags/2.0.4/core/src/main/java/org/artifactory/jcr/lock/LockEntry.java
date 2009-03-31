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

import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.fs.JcrFsItem;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author freds
 * @date Oct 19, 2008
 */
public class LockEntry {
    private JcrFsItem lockedFsItem;
    private JcrFsItem immutableFsItem;
    private final ReentrantReadWriteLock lock;

    public LockEntry(LockEntry original, JcrFsItem fsCopyItem) {
        setFsItem(original.getFsItem());
        setFsItem(fsCopyItem);
        this.lock = original.lock;
    }

    public LockEntry(JcrFsItem fsItem) {
        setFsItem(fsItem);
        this.lock = new ReentrantReadWriteLock();
    }

    public RepoPath getRepoPath() {
        return getFsItem().getRepoPath();
    }

    public JcrFsItem getFsItem() {
        if (lockedFsItem != null) {
            return lockedFsItem;
        }
        return immutableFsItem;
    }

    public JcrFsItem getLockedFsItem() {
        return lockedFsItem;
    }

    public JcrFsItem getImmutableFsItem() {
        return immutableFsItem;
    }

    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    public void setFsItem(JcrFsItem fsItem) {
        if (fsItem.isMutable()) {
            lockedFsItem = fsItem;
        } else {
            immutableFsItem = fsItem;
        }
    }
}