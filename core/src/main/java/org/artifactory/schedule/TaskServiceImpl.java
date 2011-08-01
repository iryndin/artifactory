/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.ContextReadinessListener;
import org.artifactory.spring.Reloadable;
import org.artifactory.util.LoggingUtils;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author yoavl
 */
@Service
@Reloadable(beanClass = TaskService.class, initAfter = {JcrService.class})
public class TaskServiceImpl implements TaskService, ContextReadinessListener {
    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private CachedThreadPoolTaskExecutor executor;

    private ConcurrentMap<String, TaskBase> activeTasksByToken = new ConcurrentHashMap<String, TaskBase>();
    private ConcurrentMap<String, TaskBase> inactiveTasksByToken = new ConcurrentHashMap<String, TaskBase>();

    private AtomicBoolean openForScheduling = new AtomicBoolean();

    public void init() {
        //Start the initial tasks:

        //Run the datastore gc once a day after 2 hours from startup
        TaskBase jcrGarbageCollectorTask = TaskUtils.createRepeatingTask(
                JcrGarbageCollectorJob.class,
                TimeUnit.SECONDS.toMillis(ConstantValues.gcIntervalSecs.getLong()),
                TimeUnit.SECONDS.toMillis(ConstantValues.gcDelaySecs.getLong()));
        startTask(jcrGarbageCollectorTask, false);
    }

    public void destroy() {
        cancelAllTasks(true);
        //Shut down the executor service to terminate any async operations not managed by the task service
        executor.destroy();
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
                    task.schedule(false);
                }
            }
        }
    }

    public void onContextUnready() {
        openForScheduling.set(false);
    }

    public String startTask(TaskBase task, boolean waitForRunning) {
        String token = task.getToken();
        ConcurrentMap<String, TaskBase> taskMap = openForScheduling.get() ? activeTasksByToken : inactiveTasksByToken;
        if (task.isManuallyActivated()) {
            if (!canRunManual(task.getType())) {
                throw new IllegalStateException("Cannot start task (" + task + ") manually!" +
                        " Manual user not defined or another task already running!");
            }
        }
        if (task.isSingleton()) {
            //Reject duplicate singleton tasks - by type + check automatically during insert that we are not
            //rescheduling the same task instance
            if (hasTaskOfType(task.getType(), taskMap, false) || taskMap.putIfAbsent(token, task) != null) {
                throw new IllegalStateException("Cannot start a singleton task more than once (" + task + ").");
            }
        } else if (taskMap.put(token, task) != null) {
            log.warn("Overriding an active task with the same token {}.", task);
        }
        if (openForScheduling.get()) {
            task.schedule(waitForRunning);
        }
        return task.getToken();
    }

    public void cancelTask(String token, boolean wait) {
        TaskBase task = activeTasksByToken.get(token);
        if (task != null) {
            task.cancel(wait);
        } else {
            log.warn("Could not find task {} to cancel.", token);
        }
        activeTasksByToken.remove(token);
    }

    public List<TaskBase> getActiveTasks(@Nonnull Predicate<Task> predicate) {
        List<TaskBase> results = Lists.newArrayList();
        for (TaskBase task : activeTasksByToken.values()) {
            if (predicate.apply(task)) {
                results.add(task);
            }
        }
        return results;
    }

    public void cancelTasks(@Nonnull Predicate<Task> predicate, boolean wait) {
        List<TaskBase> toCancel = getActiveTasks(predicate);
        for (Task task : toCancel) {
            //TODO: Don't wait on each job in a serial fashion
            cancelTask(task.getToken(), wait);
        }
    }

    public void cancelTasks(@Nonnull final Class<? extends TaskCallback> callbackType, boolean wait) {
        cancelTasks(createPredicateForType(callbackType), wait);
    }

    private Predicate<Task> createPredicateForType(final Class<? extends TaskCallback> callbackType) {
        return new Predicate<Task>() {
            public boolean apply(@Nullable Task input) {
                return ClassUtils.isAssignable(callbackType, input.getType());
            }
        };
    }

    public void cancelAllTasks(boolean wait) {
        cancelTasks(new Predicate<Task>() {
            public boolean apply(@Nullable Task input) {
                return true;
            }
        }, wait);
    }

    public void stopTask(String token, boolean wait) {
        TaskBase task = activeTasksByToken.get(token);
        if (task != null) {
            task.stop(wait);
        } else {
            log.warn("Could not find task {} to stop.", token);
        }
    }

    public boolean canRunManual(Class<? extends TaskCallback> callbackType) {
        JobCommand jobCommand = callbackType.getAnnotation(JobCommand.class);
        if (jobCommand == null) {
            throw new IllegalArgumentException(
                    "Callback type " + callbackType.getName() + " does not have the " +
                            JobCommand.class.getName() + " annotation!");
        }
        if (jobCommand.manualUser() == TaskUser.INVALID) {
            return false;
        }
        if (!jobCommand.singleton()) {
            return true;
        }
        // Check no other manual running, and the singleton one is not in running state
        for (TaskBase task : activeTasksByToken.values()) {
            if (ClassUtils.isAssignable(callbackType, task.getType())) {
                if (task.isManuallyActivated() && !task.wasCompleted()) {
                    return false;
                }
                if (task.processActive()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void checkCanStartManualTask(Class<? extends TaskCallback> typeToRun, MutableStatusHolder statusHolder) {
        JobCommand jobCommand = typeToRun.getAnnotation(JobCommand.class);
        if (jobCommand == null) {
            statusHolder.setError(
                    "Task type " + typeToRun.getName() + " does not have the " +
                            JobCommand.class.getName() + " annotation!", log);
            return;
        }
        if (jobCommand.manualUser() == TaskUser.INVALID) {
            statusHolder.setError("Task type " + typeToRun.getName() + " is not defined to run manually!", log);
            return;
        }
        String currentToken = TaskCallback.currentTaskToken();
        StopContainer stopContainer = getStopContainer(typeToRun);
        for (TaskBase taskBase : activeTasksByToken.values()) {
            // Don't count myself :)
            if (currentToken != null && currentToken.equals(taskBase.getToken())) {
                continue;
            }
            // If another manual task of the same kind already active
            if (taskBase.getType().equals(typeToRun) && taskBase.isManuallyActivated() && !taskBase.wasCompleted()) {
                statusHolder.setError(
                        "Another manual task " + typeToRun.getName() + " is still active!",
                        log);
                continue;
            }
            StopStrategy stopStrategy = stopContainer.classToStop.get(taskBase.getType());
            if (stopStrategy != null && taskBase.processActive()) {
                switch (stopStrategy) {
                    case IMPOSSIBLE:
                        statusHolder.setError(
                                "Task " + typeToRun.getName() + " cannot stop a mandatory related job " + taskBase.getType() + " while it's running!",
                                log);
                        break;
                    case STOP:
                        statusHolder.setWarning(
                                "Task " + typeToRun.getName() + " will be stop by running " + taskBase.getType() + " !",
                                log);
                        break;
                    case PAUSE:
                        statusHolder.setWarning(
                                "Task " + typeToRun.getName() + " will be paused by running " + taskBase.getType() + " !",
                                log);
                        break;
                }
            }
        }
        if (!statusHolder.isError()) {
            statusHolder.setStatus("Task " + typeToRun.getName() + " can run.", log);
        }
    }

    public static class StopContainer {
        boolean hasImpossible = false;
        /**
         * Map with classes to stop. Iteration is by the addition order (linked hash map)
         */
        LinkedHashMap<Class, StopStrategy> classToStop = Maps.newLinkedHashMap();
    }

    public void stopRelatedTasks(Class typeToRun, List<String> tokenStopped) {
        String currentToken = TaskCallback.currentTaskToken();
        StopContainer stopContainer = getStopContainer(typeToRun);
        if (stopContainer.hasImpossible) {
            for (TaskBase taskBase : activeTasksByToken.values()) {
                // Don't count myself :)
                if (currentToken != null && currentToken.equals(taskBase.getToken())) {
                    continue;
                }
                if (stopContainer.classToStop.get(taskBase.getType()) == StopStrategy.IMPOSSIBLE) {
                    if (taskBase.processActive()) {
                        throw new IllegalArgumentException(
                                "Job " + typeToRun.getName() + " cannot stop related job " + taskBase.getType() + " while it's running!");
                    }
                    break;
                }
            }
        }

        for (final Map.Entry<Class, StopStrategy> toStop : stopContainer.classToStop.entrySet()) {
            // get all active tasks
            List<TaskBase> activeTasksSameClass = getActiveTasks(new Predicate<Task>() {
                public boolean apply(Task input) {
                    return toStop.getKey().equals(input.getType());
                }
            });
            for (TaskBase taskToStop : activeTasksSameClass) {
                if (currentToken != null && currentToken.equals(taskToStop.getToken())) {
                    // Don't stop myself :)
                    continue;
                }
                StopStrategy stopStrategy = toStop.getValue();
                if (stopStrategy != null) {
                    switch (stopStrategy) {
                        case IMPOSSIBLE:
                            // Immediate stop fails if task in running mode
                            taskToStop.stop(false);
                            break;
                        case STOP:
                            taskToStop.stop(true);
                            break;
                        case PAUSE:
                            taskToStop.pause(true);
                            break;
                    }
                    // Single execution task that are stopped don't need to be resumed because they'll die.
                    // So, don't add the token to the list for stopped single exec tasks
                    if (!taskToStop.isSingleExecution() || stopStrategy == StopStrategy.PAUSE) {
                        tokenStopped.add(taskToStop.getToken());
                    }
                }
            }
        }
    }

    private StopContainer getStopContainer(Class typeToRun) {
        StopContainer stopContainer = new StopContainer();
        JobCommand toRunJobCommand = (JobCommand) typeToRun.getAnnotation(JobCommand.class);
        for (Class callbackType : toRunJobCommand.commandsToStop()) {
            if ((callbackType.getModifiers() & Modifier.ABSTRACT) != 0) {
                throw new IllegalArgumentException(
                        "Job command definition for " + typeToRun.getName() + " contain an abstract class to stop!");
            }
            JobCommand toStopJobCommand = (JobCommand) callbackType.getAnnotation(JobCommand.class);
            stopContainer.classToStop.put(callbackType, toStopJobCommand.stopStrategy());
            if (toStopJobCommand.stopStrategy() == StopStrategy.IMPOSSIBLE) {
                stopContainer.hasImpossible = true;
            }
        }
        if (toRunJobCommand.singleton()) {
            // If singleton and currently running => Impossible to stop
            stopContainer.classToStop.put(typeToRun, StopStrategy.IMPOSSIBLE);
            stopContainer.hasImpossible = true;
        }
        return stopContainer;
    }

    public List<String> stopTasks(Class<? extends TaskCallback> callbackType) {
        String currentToken = TaskCallback.currentTaskToken();
        List<String> results = Lists.newArrayList();
        for (TaskBase task : activeTasksByToken.values()) {
            // If callback type is null means all, cannot stop myself
            if ((callbackType == null || ClassUtils.isAssignable(callbackType, task.getType()))
                    && (currentToken == null || !task.getToken().equals(currentToken))) {
                //TODO: Don't wait on each job in a serial fashion
                stopTask(task.getToken(), true);
                results.add(task.getToken());
            }
        }
        return results;
    }

    public void pauseTask(String token, boolean wait) {
        TaskBase task = getInternalActiveTask(token);
        if (task != null) {
            task.pause(wait);
        } else {
            log.warn("Could not find task {} to pause.", token);
        }
    }

    public boolean resumeTask(String token) {
        TaskBase task = getInternalActiveTask(token);
        if (task != null) {
            return task.resume();
        } else {
            log.debug("Could not find task {} to resume.", token);
            return false;
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
        return hasTaskOfType(callbackType, activeTasksByToken, true);
    }

    private boolean hasTaskOfType(Class<? extends TaskCallback> callbackType, ConcurrentMap<String, TaskBase> taskMap,
            boolean withManual) {
        if (callbackType == null) {
            return false;
        }
        for (TaskBase task : taskMap.values()) {
            if (ClassUtils.isAssignable(callbackType, task.getType()) &&
                    !(withManual && task.isManuallyActivated())) {
                return true;
            }
        }
        return false;
    }
}
