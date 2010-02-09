/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.schedule;

import org.artifactory.cache.InternalCacheService;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.WagonManagerTempArtifactsCleaner;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.LoggingUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yoavl
 */
@Service
@Reloadable(beanClass = TaskService.class, initAfter = {InternalCacheService.class, JcrService.class})
public class TaskServiceImpl implements TaskService, ContextReadinessListener {
    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private ConcurrentMap<String, TaskBase> activeTasksByToken = new ConcurrentHashMap<String, TaskBase>();
    private ConcurrentMap<String, TaskBase> inactiveTasksByToken = new ConcurrentHashMap<String, TaskBase>();

    private AtomicBoolean openForScheduling = new AtomicBoolean();

    public void init() {
        //Start the initial tasks:

        //Run the datastore gc every 12 hours after 2 hours from startup
        QuartzTask jcrGarbageCollectorTask = new QuartzTask(
                JcrGarbageCollectorJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.gcIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(1));
        jcrGarbageCollectorTask.setSingleton(true);
        startTask(jcrGarbageCollectorTask);

        //run the wagon leftovers cleanup every 15 minutes after 10 minutes from startup
        QuartzTask wagonManagerTempArtifactsCleanerTask = new QuartzTask(
                WagonManagerTempArtifactsCleaner.class, TimeUnit.SECONDS.toMillis(15 * 60),
                TimeUnit.SECONDS.toMillis(10 * 60));
        wagonManagerTempArtifactsCleanerTask.setSingleton(true);
        startTask(wagonManagerTempArtifactsCleanerTask);
    }

    public void destroy() {
        stopTasks(null, true);
        //Shut down the executor service to terminate any async operations not managed by the task service
        ExecutorService executorService = getExecutorService();
        executorService.shutdown();
    }

    public ExecutorService getExecutorService() {
        InternalArtifactoryContext context = InternalContextHelper.get();
        CachedThreadPoolTaskExecutor executor = context.beanForType(CachedThreadPoolTaskExecutor.class);
        ExecutorService executorService = executor.getConcurrentExecutor();
        return executorService;
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void onContextReady() {
        openForScheduling.set(true);
        for (String taskKey : inactiveTasksByToken.keySet()) {
            //Check the readiness and task status again since it can change
            if (openForScheduling.get()) {
                TaskBase task = inactiveTasksByToken.remove(taskKey);
                if (task != null) {
                    activeTasksByToken.put(taskKey, task);
                    task.schedule();
                }
            }
        }
    }

    public void onContextUnready() {
        openForScheduling.set(false);
    }

    public String startTask(TaskBase task) {
        String token = task.getToken();
        ConcurrentMap<String, TaskBase> taskMap = openForScheduling.get() ? activeTasksByToken : inactiveTasksByToken;
        if (task.isSingleton()) {
            //Reject duplicate singleton tasks - by type + check automatically during insert that we are not
            //rescheduling the same task instance
            if (hasTaskOfType(task.getType(), taskMap) || taskMap.putIfAbsent(token, task) != null) {
                throw new IllegalStateException("Cannot start a singleton task more than once (" + task + ").");
            }
        } else if (taskMap.put(token, task) != null) {
            log.warn("Overriding an active task with the same token {}.", task);
        }
        if (openForScheduling.get()) {
            task.schedule();
        }
        return task.getToken();
    }

    public void cancelTask(String token, boolean wait) {
        TaskBase task = activeTasksByToken.get(token);
        if (task != null) {
            task.stop(wait, false, true);
        } else {
            log.warn("Could not find task {} to cancel.", token);
        }
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
        if (task != null) {
            task.stop(wait, false, false);
        } else {
            log.warn("Could not find task {} to stop.", token);
        }
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
        if (task != null) {
            task.stop(wait, true, false);
        } else {
            log.warn("Could not find task {} to pause.", token);
        }
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
        if (task != null) {
            return task.resume();
        } else {
            log.warn("Could not find task {} to resume.", token);
            return false;
        }
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

    public boolean pauseOrBreak() {
        String token = TaskCallback.currentTaskToken();
        // If not in a task the token is null
        if (token == null) {
            //Since this is called by external clients, it may be called directly or in tests with no surrounding task
            log.debug("No current task is found on thread - nothing to block or pause.");
            return false;
        }
        TaskBase task = getInternalActiveTask(token);
        if (task == null) {
            log.warn("Could not find task {} to check block on.", token);
            return false;
        }
        return task.blockIfPausedAndShouldBreak();
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
        return hasTaskOfType(callbackType, activeTasksByToken);
    }

    public TaskBase getSingletonTaskTaskOfType(Class<? extends TaskCallback> callbackType) {
        if (callbackType == null) {
            throw new IllegalArgumentException("Task type cannot be null.");
        }
        for (TaskBase task : activeTasksByToken.values()) {
            if (ClassUtils.isAssignable(callbackType, task.getType())) {
                if (task.isSingleton()) {
                    return task;
                } else {
                    throw new IllegalArgumentException(
                            "Task of type '" + callbackType.getName() + "' is not a singleton.");
                }
            }
        }
        return null;
    }

    private boolean hasTaskOfType(Class<? extends TaskCallback> callbackType, ConcurrentMap<String, TaskBase> taskMap) {
        if (callbackType == null) {
            return false;
        }
        for (TaskBase task : taskMap.values()) {
            if (ClassUtils.isAssignable(callbackType, task.getType())) {
                return true;
            }
        }
        return false;
    }
}
