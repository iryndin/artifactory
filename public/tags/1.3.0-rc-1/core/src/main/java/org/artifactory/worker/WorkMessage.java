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
package org.artifactory.worker;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;

/**
 * @author freds
 * @date Sep 18, 2008
 */
public abstract class WorkMessage {
    private final WorkAction action;
    private final RepoPath repoPath;

    public WorkMessage(WorkAction action, RepoPath repoPath) {
        this.action = action;
        this.repoPath = repoPath;
    }

    public WorkAction getAction() {
        return action;
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public final boolean publishAfterCommit() {
        return action.isPublishAfterCommit();
    }

    protected void call() {
        if (LockingHelper.hasLockManager() || LockingHelper.isInJcrTransaction()) {
            throw new IllegalStateException("Cannot run JetLang callback if thread associated with Lock Manager " +
                    LockingHelper.hasLockManager() + " or JCR transaction " + LockingHelper.isInJcrTransaction());
        }
        InternalRepositoryService service =
                InternalContextHelper.get().beanForType(InternalRepositoryService.class);
        service.executeMessage(this);
    }

    public abstract void execute();

    @Override
    public String toString() {
        return "WorkMessage{" +
                "action=" + action +
                ", repoPath=" + repoPath +
                '}';
    }

}
