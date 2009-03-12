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

import org.artifactory.api.context.ContextHelper;
import org.artifactory.schedule.TaskBase;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.JobDetailAwareTrigger;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

import java.util.Date;

/**
 * @author yoavl
 */
public class QuartzTask extends TaskBase {
    public static final String TASK_TOKEN = "TASK_TOKEN";
    public static final String TASK_AUTHENTICATION = "TASK_AUTHENTICATION";

    private final Trigger trigger;
    private final JobDetail jobDetail;

    @SuppressWarnings({"unchecked"})
    public QuartzTask(JobDetailAwareTrigger trigger) {
        this(trigger.getJobDetail().getJobClass(), (Trigger) trigger, trigger.getJobDetail());
    }

    public QuartzTask(Class<? extends QuartzCommand> command, long interval) {
        this(command, interval, 0);
    }

    public QuartzTask(Class<? extends QuartzCommand> command, long interval, long initialDelay) {
        this(command, command.getName(), interval, initialDelay);
    }

    /**
     * Immediate QuartzTask execution
     *
     * @param command
     * @param triggerName
     */
    public QuartzTask(Class<? extends QuartzCommand> command, String triggerName) {
        this(command, triggerName, 0, 0);
    }

    /**
     * Creates a new task.
     *
     * @param command      The command to schedule
     * @param triggerName  Trigger name
     * @param interval     Interval in milliseconds between executions. 0 means execute only once
     * @param initialDelay Delay in milliseconds before starting the task for the first time starting from now
     */
    public QuartzTask(Class<? extends QuartzCommand> command, String triggerName, long interval, long initialDelay) {
        this(command, new SimpleTrigger(
                triggerName, "artifactory",
                new Date(System.currentTimeMillis() + initialDelay), null,
                (interval <= 0) ? 0 : SimpleTrigger.REPEAT_INDEFINITELY, interval),
                new JobDetail(triggerName + "JobDetail", null, command));
    }

    public QuartzTask(Class<? extends QuartzCommand> command, Trigger trigger, JobDetail jobDetail) {
        super(command);
        this.trigger = trigger;
        //Make sure the trigger is unique
        String uniqueName = this.trigger.getName() + "#" + getToken();
        this.trigger.setName(uniqueName);
        this.trigger.setJobName(uniqueName);
        this.jobDetail = jobDetail;
        this.jobDetail.setName(uniqueName);
        this.jobDetail.getJobDataMap().put(TASK_TOKEN, getToken());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            this.jobDetail.getJobDataMap().put(TASK_AUTHENTICATION, authentication);
        }
    }

    public void addAttribute(String key, Object value) {
        jobDetail.getJobDataMap().put(key, value);
    }

    @Override
    public boolean isSingleExecution() {
        if (trigger instanceof SimpleTrigger) {
            return ((SimpleTrigger) trigger).getRepeatCount() == 0;
        }
        return !trigger.mayFireAgain();
    }

    /**
     * Schedule the task
     */
    @Override
    protected void scheduleTask() {
        Scheduler scheduler = getScheduler();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException(
                    "Error in scheduling job: " + trigger.getName(), e);
        }
    }

    /**
     * Unschedule the task
     */
    @Override
    protected void cancelTask() {
        Scheduler scheduler = getScheduler();
        try {
            String jobName = jobDetail.getName();
            boolean deleted = scheduler.deleteJob(jobName, null);
            if (!deleted) {
                throw new IllegalStateException("Failed to cancel job: " + jobName + ".");
            }
        } catch (SchedulerException e) {
            throw new RuntimeException(
                    "Failed to unschedule previous job: " + trigger.getName(), e);
        }
    }

    private static Scheduler getScheduler() {
        return ContextHelper.get().beanForType(Scheduler.class);
    }
}