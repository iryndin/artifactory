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

import org.artifactory.api.context.ArtifactoryContextThreadBinder;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

/**
 * A callback for doing the actual work of a scheduled {@link Task}, with some typed work context
 *
 * @author yoavl
 */
public abstract class TaskCallback<C> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(TaskCallback.class);

    private static final InheritableThreadLocal<String> currentTaskToken =
            new InheritableThreadLocal<String>();

    public static String currentTaskToken() {
        return currentTaskToken.get();
    }

    private TaskBase activeTask;

    protected abstract String triggeringTaskTokenFromWorkContext(C workContext);

    protected boolean beforeExecute(C callbackContext) {
        InternalArtifactoryContext context = ArtifactorySchedulerFactoryBean.getContext();
        if (context == null || !context.isReady()) {
            //TODO: [by yl] Get rid of this context readiness check
            log.warn("Running before initialization finished.");
            return false;
        }
        //Bind the context and the current task token
        ArtifactoryContextThreadBinder.bind(context);
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
                //We were not started (probably defered due to context not being ready)
                return;
            }
            //Notify listeners that we are done
            if (activeTask != null) {
                activeTask.completed();
            } else {
                log.warn("After execute: Could not locate active task with toke {}. Taks may have been canceled.",
                        token);
            }
            log.debug("Finished task {}.", token);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
            activeTask = null;
            currentTaskToken.remove();
            ArtifactoryContextThreadBinder.unbind();
        }
    }

    protected static TaskService getTaskService() {
        TaskService taskService = InternalContextHelper.get().getTaskService();
        return taskService;
    }
}