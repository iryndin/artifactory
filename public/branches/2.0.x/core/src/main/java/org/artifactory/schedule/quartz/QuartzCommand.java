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
package org.artifactory.schedule.quartz;

import org.artifactory.schedule.TaskCallback;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;

/**
 * @author yoavl
 */
public abstract class QuartzCommand extends TaskCallback<JobExecutionContext> implements StatefulJob {
    private static final Logger log = LoggerFactory.getLogger(QuartzCommand.class);

    public final void execute(final JobExecutionContext jobContext) throws JobExecutionException {
        try {
            boolean shouldExecute = beforeExecute(jobContext);
            String token = currentTaskToken();
            String taskName = getClass().getName();
            if (!shouldExecute) {
                log.debug("Skipping execution of task {} for token {}.", taskName, token);
            } else {
                log.debug("Executing task {} for token {}.", taskName, token);
                onExecute(jobContext);
                log.debug("Finished execution of task {} for token {}.", taskName, token);
            }
        } finally {
            afterExecute();
        }
    }

    @Override
    protected String triggeringTaskTokenFromWorkContext(JobExecutionContext jobContext) {
        String token = jobContext.getMergedJobDataMap().getString(QuartzTask.TASK_TOKEN);
        return token;
    }

    @Override
    protected Authentication getAuthenticationFromWorkContext(JobExecutionContext jobContext) {
        Authentication authentication =
                (Authentication) jobContext.getMergedJobDataMap().get(QuartzTask.TASK_AUTHENTICATION);
        return authentication;
    }
}