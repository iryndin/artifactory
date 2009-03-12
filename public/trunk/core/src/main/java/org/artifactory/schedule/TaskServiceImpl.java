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

import org.artifactory.cache.InternalCacheService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.schedule.JcrGarbageCollector;
import org.artifactory.jcr.schedule.WorkingCopyCommitter;
import org.artifactory.jcr.trash.EmptyTrashJob;
import org.artifactory.maven.WagonManagerTempArtifactsCleaner;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author yoavl
 */
@Service
public class TaskServiceImpl implements TaskService {
    private final static Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private ConcurrentMap<String, TaskBase> activeTasksByToken = new ConcurrentHashMap<String, TaskBase>();

    public void init() {
        // TODO: Minutes and Hours are only in Java 6 :(
        //Start the initial tasks
        //Activate the Garbage collector every 20 minutes after 5 minutes
        QuartzTask jcrGarbageCollectorTask =
                new QuartzTask(JcrGarbageCollector.class, TimeUnit.SECONDS.toMillis(3 * 60),
                        TimeUnit.SECONDS.toMillis(1 * 60));
        //new QuartzTask(JcrGarbageCollector.class, TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(1));
        jcrGarbageCollectorTask.setSingleton(true);
        startTask(jcrGarbageCollectorTask);
        //run the wagon leftovers cleanup every 15 minutes after 10 minutes
        QuartzTask wagonManagerTempArtifactsCleanerTask = new QuartzTask(
                WagonManagerTempArtifactsCleaner.class, TimeUnit.SECONDS.toMillis(15 * 60),
                TimeUnit.SECONDS.toMillis(10 * 60));
        //WagonManagerTempArtifactsCleaner.class, TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(10));
        wagonManagerTempArtifactsCleanerTask.setSingleton(true);
        startTask(wagonManagerTempArtifactsCleanerTask);
        //run the wc committer once
        QuartzTask workingCopyCommitterTask =
                new QuartzTask(WorkingCopyCommitter.class, 0, TimeUnit.SECONDS.toMillis(30));
        workingCopyCommitterTask.setSingleton(true);
        startTask(workingCopyCommitterTask);
        //Empty whatever is left in the trash
        QuartzTask emptyTrashTask =
                new QuartzTask(EmptyTrashJob.class, "EmptyTrashOnStartup", 0, TimeUnit.SECONDS.toMillis(30));
        startTask(emptyTrashTask);
    }

    public void destroy() {
        stopTasks(null, true);
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{InternalCacheService.class, JcrService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public String startTask(TaskBase task) {
        String token = task.getToken();
        if (task.isSingleton()) {
            //Reject duplicate singleton tasks - by type + check atomically during insert that we are not rescheduling
            //the same task instance
            if (hasTaskOfType(task.getType()) || activeTasksByToken.putIfAbsent(token, task) != null) {
                throw new IllegalStateException("Cannot start a singleton task more than once (" + task + ").");
            }
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
        //Check for null since task may have already been canceled
        return task == null || task.waitForCompletion();
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

    public TaskBase getInternalActiveTask(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Could not find task with null token");
        }
        TaskBase task = activeTasksByToken.get(token);
        if (task == null) {
            LoggingUtils.warnOrDebug(log,
                    "Could not locate active task with token " + token + ". Task may have been canceled.");
        }
        return task;
    }

    public boolean hasTaskOfType(Class<? extends TaskCallback> callbackType) {
        if (callbackType == null) {
            return false;
        }
        for (TaskBase task : activeTasksByToken.values()) {
            if (ClassUtils.isAssignable(callbackType, task.getType())) {
                return true;
            }
        }
        return false;
    }
}