/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

import org.artifactory.log.LoggerFactory;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
        //Notify listeners that we started execution
        TaskService taskService = getTaskService();
        String token = currentTaskToken();
        activeTask = taskService.getInternalActiveTask(token);
        if (activeTask == null) {
            log.warn("Before execute: Could not locate active task with toke {}. Taks may have been canceled.", token);
            return false;
        }
        boolean shouldExecute = activeTask.started();
        return shouldExecute;
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
                log.warn("After execute: Could not locate active task with toke {}. Task may have been canceled.",
                        token);
            }
            log.debug("Finished task {}.", token);
        } finally {
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