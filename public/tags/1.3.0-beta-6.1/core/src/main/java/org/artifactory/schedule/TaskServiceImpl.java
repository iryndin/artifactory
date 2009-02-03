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
package org.artifactory.schedule;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.data.FileDataStore;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.schedule.JcrGarbageCollector;
import org.artifactory.jcr.schedule.WorkingCopyCommitter;
import org.artifactory.maven.WagonManagerTempArtifactsCleaner;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author yoavl
 */
@Service
public class TaskServiceImpl implements TaskService {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private ConcurrentMap<String, TaskBase> activeTasksByToken = new ConcurrentHashMap<String, TaskBase>();

    public void init() {
        //Start the initial tasks
        // Check if we use File Data Store, then activate the Garbage collector
        InternalArtifactoryContext ctx = InternalContextHelper.get();
        JcrService jcr = ctx.getJcrService();
        if (((RepositoryImpl) jcr.getRepository()).getDataStore() instanceof FileDataStore) {
            //Run the datastore gc every 12 hours
            QuartzTask jcrGarbageCollectorTask = new QuartzTask(JcrGarbageCollector.class, 43200000);
            jcrGarbageCollectorTask.setSingleton(true);
            startTask(jcrGarbageCollectorTask);
        }
        //run the wagon leftovers cleanup every 15 minutes
        QuartzTask wagonManagerTempArtifactsCleanerTask =
                new QuartzTask(WagonManagerTempArtifactsCleaner.class, 900000);
        wagonManagerTempArtifactsCleanerTask.setSingleton(true);
        startTask(wagonManagerTempArtifactsCleanerTask);
        //run the wc committer every 20 minutes
        QuartzTask workingCopyCommitterTask = new QuartzTask(WorkingCopyCommitter.class, 1200000, 30000);
        workingCopyCommitterTask.setSingleton(true);
        startTask(workingCopyCommitterTask);
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
        stopTasks(null, true);
    }

    public String startTask(TaskBase task) {
        String token = task.getToken();
        if (task.isSingleton() && activeTasksByToken.putIfAbsent(token, task) != null) {
            //Reject duplicate singleton tasks
            throw new IllegalStateException("Cannot start a singleton task more than once (" + task + ").");
        } else if (activeTasksByToken.put(token, task) != null) {
            log.warn("Overriding an active task with the same token {}.", task);
        }
        task.schedule();
        return task.getToken();
    }

    public void cancelTask(String token, boolean wait) {
        TaskBase task = activeTasksByToken.get(token);
        task.stop(wait, false, true);
        activeTasksByToken.remove(token);
    }

    public void cancelTasks(Class<? extends TaskCallback> callbackType, boolean wait) {
        for (TaskBase task : activeTasksByToken.values()) {
            if (callbackType == null || ClassUtils.isAssignable(callbackType, task.getType())) {
                //TODO: Don't wait on each job in a serial fashion
                cancelTask(task.getToken(), wait);
            }
        }
    }

    public void cancelAllTasks(boolean wait) {
        cancelTasks(null, wait);
    }

    public void stopTask(String token, boolean wait) {
        TaskBase task = activeTasksByToken.get(token);
        task.stop(wait, false, false);
    }

    public void stopTasks(Class<? extends TaskCallback> callbackType, boolean wait) {
        for (TaskBase task : activeTasksByToken.values()) {
            if (callbackType == null || ClassUtils.isAssignable(callbackType, task.getType())) {
                //TODO: Don't wait on each job in a serial fashion
                stopTask(task.getToken(), wait);
            }
        }
    }

    public void pauseTask(String token, boolean wait) {
        TaskBase task = getInternalActiveTask(token);
        task.stop(wait, true, false);
    }

    public void pauseTasks(Class<? extends TaskCallback> callbackType, boolean wait) {
        for (TaskBase task : activeTasksByToken.values()) {
            if (callbackType == null || ClassUtils.isAssignable(callbackType, task.getType())) {
                //TODO: Don't wait on each job in a serial fashion
                pauseTask(task.getToken(), wait);
            }
        }
    }

    public boolean resumeTask(String token) {
        TaskBase task = getInternalActiveTask(token);
        return task.resume();
    }

    public void resumeTasks(Class<? extends TaskCallback> callbackType) {
        for (TaskBase task : activeTasksByToken.values()) {
            if (callbackType == null || ClassUtils.isAssignable(callbackType, task.getType())) {
                resumeTask(task.getToken());
            }
        }
    }

    public boolean waitForTaskCompletion(String token) {
        TaskBase task = getInternalActiveTask(token);
        if (task == null) {
            // may be already canceled
            return true;
        }
        return task.waitForCompletion();
    }

    public boolean blockIfPausedAndShouldBreak() {
        String token = TaskCallback.currentTaskToken();
        // If not in a task the token is null
        if (token == null) {
            throw new IllegalStateException("No current task is found on thread.");
        }
        TaskBase task = getInternalActiveTask(token);
        return task.blockIfPausedAndShouldBreak();
    }

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(TaskService.class);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalCacheService.class, JcrService.class};
    }

    public TaskBase getInternalActiveTask(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Could not find task with null token");
        }
        TaskBase task = activeTasksByToken.get(token);
        if (task == null) {
            log.warn("Could not locate active task with toke {}. Taks may have been canceled.", token);
        }
        return task;
    }
}