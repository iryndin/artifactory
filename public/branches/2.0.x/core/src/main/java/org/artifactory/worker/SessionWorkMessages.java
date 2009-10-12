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

import org.artifactory.spring.InternalContextHelper;
import org.artifactory.tx.SessionResource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author freds
 * @date Sep 22, 2008
 */
public class SessionWorkMessages implements SessionResource {
    private List<WorkMessage> workMessages = null;

    public void afterCompletion(boolean commit) {
        try {
            if (!hasResources()) {
                return;
            }
            if (commit) {
                WorkerService workerService = InternalContextHelper.get().beanForType(WorkerService.class);
                for (WorkMessage message : workMessages) {
                    workerService.publish(message);
                }
            }
        } finally {
            workMessages = null;
        }
    }

    public boolean hasResources() {
        return workMessages != null && !workMessages.isEmpty();
    }

    public void addWorkMessage(WorkMessage workMessage) {
        if (workMessages == null) {
            workMessages = new ArrayList<WorkMessage>(1);
        }
        workMessages.add(workMessage);
    }

    public boolean hasPendingChanges() {
        return false;
    }

    public void onSessionSave() {
    }
}
