/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import com.google.common.collect.ImmutableMap;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.schedule.TaskBase;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.JobDetailAwareTrigger;

import java.util.Date;

/**
 * @author yoavl
 */
public class QuartzTask extends TaskBase {
    private static final Logger log = LoggerFactory.getLogger(QuartzTask.class);
    public static final String ARTIFACTORY_GROUP = "artifactory";

    private final Trigger trigger;
    private final JobDetail jobDetail;

    @SuppressWarnings({"unchecked"})
    public static TaskBase createQuartzTask(JobDetailAwareTrigger trigger) {
        return new QuartzTask(trigger.getJobDetail().getJobClass(), (Trigger) trigger, trigger.getJobDetail());
    }

    /**
     * Creates a new task.
     *
     * @param command      The command to schedule
     * @param triggerName  Trigger name
     * @param interval     Interval in milliseconds between executions. 0 means execute only once
     * @param initialDelay Delay in milliseconds before starting the task for the first time starting from now
     */
    @SuppressWarnings({"unchecked"})
    public static TaskBase createQuartzTask(Class<? extends QuartzCommand> command, long interval, long initialDelay) {
        return new QuartzTask(command, new SimpleTrigger(
                command.getName(), null,
                new Date(System.currentTimeMillis() + initialDelay), null,
                (interval <= 0) ? 0 : SimpleTrigger.REPEAT_INDEFINITELY, interval),
                new JobDetail(command.getName(), ARTIFACTORY_GROUP, command));
    }

    private QuartzTask(Class<? extends QuartzCommand> command, Trigger trigger, JobDetail jobDetail) {
        super(command);
        this.trigger = trigger;
        //Make sure the trigger is unique
        String uniqueName = getToken();
        this.trigger.setName(uniqueName);
        this.trigger.setJobName(uniqueName);
        this.trigger.setJobGroup(ARTIFACTORY_GROUP);
        this.jobDetail = jobDetail;
        this.jobDetail.setName(uniqueName);
        this.jobDetail.setGroup(ARTIFACTORY_GROUP);
    }

    @Override
    public void addAttribute(String key, Object value) {
        jobDetail.getJobDataMap().put(key, value);
    }

    public ImmutableMap getAttributeMap() {
        return ImmutableMap.copyOf(jobDetail.getJobDataMap());
    }

    @Override
    public Object getAttribute(String key) {
        return jobDetail.getJobDataMap().get(key);
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
            if (!scheduler.deleteJob(jobName, ARTIFACTORY_GROUP)) {
                log.info("Task " + jobName + " already deleted from scheduler");
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to unschedule previous job: " + trigger.getName(), e);
        }
    }

    private static Scheduler getScheduler() {
        return ContextHelper.get().beanForType(Scheduler.class);
    }
}