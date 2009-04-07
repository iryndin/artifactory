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
package org.artifactory.jcr.trash;

import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.tx.SessionResource;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Disposses of trashed items
 *
 * @author yoavl
 */
public class Trashman implements SessionResource {
    private static final Logger log = LoggerFactory.getLogger(Trashman.class);

    Set<String> folderNames = new HashSet<String>();

    public void afterCompletion(boolean commit) {
        //Fire a job to clean the trashed items
        if (!commit || !hasResources()) {
            return;
        }
        QuartzTask task = new QuartzTask(EmptyTrashJob.class, "EmptyTrash");
        String folderNamesString = PathUtils.collectionToDelimitedString(folderNames, ",");
        task.addAttribute(EmptyTrashJob.FOLDER_NAMES, folderNamesString);
        TaskService taskService = InternalContextHelper.get().getTaskService();
        taskService.startTask(task);
    }

    public boolean hasResources() {
        return folderNames.size() > 0;
    }

    public boolean hasPendingChanges() {
        return hasResources();
    }

    public void onSessionSave() {
        //Nothing to do as part of the transaction
    }

    public void addTrashedFolder(String folderName) {
        if (folderNames.contains(folderName)) {
            log.warn("The folder '" + folderName + "' is already registered.");
        }
        folderNames.add(folderName);
    }
}