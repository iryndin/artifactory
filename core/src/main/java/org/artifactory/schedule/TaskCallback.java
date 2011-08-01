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

import com.google.common.collect.Lists;
import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * A callback for doing the actual work of a scheduled {@link Task}, with some typed work context
 *
 * @author yoavl
 */
public abstract class TaskCallback<C> {
    private static final Logger log = LoggerFactory.getLogger(TaskCallback.class);

    private static final InheritableThreadLocal<String> currentTaskToken = new InheritableThreadLocal<String>();

    public static String currentTaskToken() {
        return currentTaskToken.get();
    }

    private TaskBase activeTask;
    private List<String> tasksStopped = Lists.newArrayList();

    protected abstract String triggeringTaskTokenFromWorkContext(C workContext);

    protected boolean beforeExecute(C callbackContext) {
        InternalArtifactoryContext context = InternalContextHelper.get();
        if (context == null || !context.isReady()) {
            log.error("Running before initialization finished.");
            return false;
        }
        String taskToken = triggeringTaskTokenFromWorkContext(callbackContext);
        currentTaskToken.set(taskToken);
        Authentication authentication = getAuthenticationFromWorkContext(callbackContext);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        TaskService taskService = getTaskService();
        activeTask = taskService.getInternalActiveTask(taskToken);
        if (activeTask == null) {
            log.warn("Before execute: Could not locate active task with token {}. Task {} may have been canceled.",
                    taskToken, this);
            return false;
        }
        this.tasksStopped.clear();
        try {
            taskService.stopRelatedTasks(activeTask.getType(), this.tasksStopped);
        } catch (Exception e) {
            log.error("Couldn't start task " + taskToken + ": " + e.getMessage());
            log.debug("Couldn't start task " + taskToken + ": " + e.getMessage(), e);
            return false;
        }
        return activeTask.started();
    }

    protected abstract Authentication getAuthenticationFromWorkContext(C callbackContext);

    protected abstract void onExecute(C callbackContext) throws JobExecutionException;

    protected void afterExecute() {
        try {
            String token = currentTaskToken();
            if (token == null) {
                //We were not started (probably deferred due to context not being ready)
                return;
            }
            //Notify listeners that we are done
            if (activeTask != null) {
                activeTask.completed();
                if (activeTask.isSingleExecution()) {
                    TaskService taskService = getTaskService();
                    //Cancel the active task
                    taskService.cancelTask(token, true);
                }
            } else {
                log.warn("After execute: Could not locate active task with token {}. Task may have been canceled.",
                        token);
            }
            log.debug("Finished task {}.", token);
        } finally {
            if (!tasksStopped.isEmpty()) {
                try {
                    TaskService taskService = getTaskService();
                    for (String taskToken : tasksStopped) {
                        try {
                            taskService.resumeTask(taskToken);
                        } catch (Exception e) {
                            log.warn("After execute: Could not locate reactive task with token {}", taskToken);
                        }
                    }
                } finally {
                    tasksStopped.clear();
                }
            }
            SecurityContextHolder.getContext().setAuthentication(null);
            activeTask = null;
            currentTaskToken.remove();
        }
    }

    protected static TaskService getTaskService() {
        TaskService taskService = InternalContextHelper.get().getTaskService();
        return taskService;
    }
}