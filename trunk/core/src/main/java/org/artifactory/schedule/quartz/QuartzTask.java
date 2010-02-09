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

package org.artifactory.schedule.quartz;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.schedule.TaskBase;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.JobDetailAwareTrigger;
import org.springframework.scheduling.quartz.SimpleTriggerBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

    public static QuartzTask getOneTimeImmediateTask(Class<? extends QuartzCommand> command) {
        SimpleTriggerBean trigger = new SimpleTriggerBean();
        trigger.setStartTime(new Date());
        trigger.setRepeatCount(0);
        trigger.setMisfireInstruction(
                SimpleTriggerBean.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT);
        JobDetail jobDetail = new JobDetail(command.getSimpleName() + "JobDetail", null, command);
        trigger.setJobDetail(jobDetail);
        QuartzTask task = new QuartzTask(command, trigger, jobDetail);
        return task;
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
            throw new RuntimeException("Error in scheduling job: " + trigger.getName(), e);
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
            throw new RuntimeException("Failed to unschedule previous job: " + trigger.getName(), e);
        }
    }

    private static Scheduler getScheduler() {
        return ContextHelper.get().beanForType(Scheduler.class);
    }
}