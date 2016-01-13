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

package org.artifactory.schedule;

import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.security.AnonymousAuthenticationToken;
import org.artifactory.security.SystemAuthenticationToken;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * User: freds
 * Date: 7/6/11
 * Time: 2:54 PM
 */
public abstract class TaskUtils {

    public static TaskBase createManualTask(@Nonnull Class<? extends QuartzCommand> commandClass, long initialDelay) {
        return createManualTask(commandClass, initialDelay, null);
    }

    public static TaskBase createManualTask(@Nonnull Class<? extends QuartzCommand> quartzCommand,
            long initialDelay, String description) {
        checkCommandClass(quartzCommand, true);

        description = getTaskDescription(quartzCommand, description) + " (manual trigger)";

        JobDetail jobDetail = JobBuilder.newJob(quartzCommand)
                .withIdentity(generateUniqueName(quartzCommand), QuartzTask.ARTIFACTORY_GROUP)
                .withDescription(description).build();

        Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
                .withIdentity(generateUniqueName(quartzCommand))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                .startAt(new Date(System.currentTimeMillis() + initialDelay)).build();

        return fillProperties(QuartzTask.createQuartzTask(quartzCommand, trigger, jobDetail), true);
    }

    public static TaskBase createRepeatingTask(@Nonnull Class<? extends QuartzCommand> commandClass,
            long interval, long initialDelay) {
        return createRepeatingTask(commandClass, interval, initialDelay, null);
    }

    public static TaskBase createRepeatingTask(@Nonnull Class<? extends QuartzCommand> quartzCommand,
            long interval, long initialDelay, String description) {
        checkCommandClass(quartzCommand, false);

        JobDetail jobDetail = JobBuilder.newJob(quartzCommand)
                .withIdentity(generateUniqueName(quartzCommand), QuartzTask.ARTIFACTORY_GROUP)
                .withDescription(getTaskDescription(quartzCommand, description)).build();

        SimpleScheduleBuilder schedulerBuilder = SimpleScheduleBuilder.simpleSchedule();
        if (interval > 0) {
            schedulerBuilder.repeatForever().withIntervalInMilliseconds(interval);
        }
        Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
                .withIdentity(generateUniqueName(quartzCommand))
                .withSchedule(schedulerBuilder).startAt(new Date(System.currentTimeMillis() + initialDelay)).build();

        return fillProperties(QuartzTask.createQuartzTask(quartzCommand, trigger, jobDetail), false);
    }

    public static TaskBase createCronTask(@Nonnull Class<? extends QuartzCommand> commandClass,
            @Nonnull String cronExpression) {
        return createCronTask(commandClass, cronExpression, null);
    }

    public static TaskBase createCronTask(@Nonnull Class<? extends QuartzCommand> quartzCommand,
            @Nonnull String cronExpression, String description) {
        checkCommandClass(quartzCommand, false);

        JobDetail jobDetail = JobBuilder.newJob(quartzCommand)
                .withIdentity(generateUniqueName(quartzCommand), QuartzTask.ARTIFACTORY_GROUP)
                .withDescription(getTaskDescription(quartzCommand, description)).build();

        Trigger trigger = TriggerBuilder.newTrigger().forJob(jobDetail)
                .withIdentity(generateUniqueName(quartzCommand))
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        return fillProperties(QuartzTask.createQuartzTask(quartzCommand, trigger, jobDetail), false);
    }

    private static String getTaskDescription(Class<? extends QuartzCommand> quartzCommand, String inDescription) {
        if (inDescription == null) {
            JobCommand jobCommand = quartzCommand.getAnnotation(JobCommand.class);
            return jobCommand.description();
        } else {
            return inDescription;
        }
    }

    private static TaskBase fillProperties(TaskBase task, boolean manual) {
        task.setManuallyActivated(manual);
        task.addAttribute(TaskBase.TASK_TOKEN, task.getToken());
        JobCommand jobCommand = task.getType().getAnnotation(JobCommand.class);
        task.addAttribute(TaskBase.TASK_AUTHENTICATION, getAuthentication(jobCommand, task, manual));
        task.addAttribute(TaskBase.TASK_RUN_ONLY_ON_PRIMARY, jobCommand.runOnlyOnPrimary());
        // Set good state for singleton
        if (jobCommand.singleton()) {
            // Manual activation of a singleton task is not singleton
            task.setSingleton(!manual);
        }
        return task;
    }

    private static Authentication getAuthentication(JobCommand jobCommand, TaskBase task, boolean manual) {
        TaskUser taskUser;
        if (manual) {
            taskUser = jobCommand.manualUser();
        } else {
            taskUser = jobCommand.schedulerUser();
        }
        Authentication authentication = null;
        switch (taskUser) {
            case INVALID:
                throw new IllegalArgumentException(
                        "Quartz command " + task.getType() + " did not set a task user for " +
                                (manual ? "manual" : "scheduled") + " activation!"
                );
            case SYSTEM:
                authentication = new SystemAuthenticationToken();
                break;
            case CURRENT:
                authentication = SecurityContextHolder.getContext().getAuthentication();
                // If authentication null continue to anonymous
                if (authentication != null) {
                    break;
                }
            case ANONYMOUS:
                authentication = new AnonymousAuthenticationToken();
                break;
        }
        if (authentication == null) {
            throw new IllegalStateException("Could not find authentication credential for task " + task.getType());
        }
        return authentication;
    }

    private static void checkCommandClass(Class<? extends TaskCallback> commandClass, boolean manual) {
        if (commandClass == null) {
            throw new IllegalArgumentException("Command class mandatory");
        }
        JobCommand jobCommand = commandClass.getAnnotation(JobCommand.class);
        if (jobCommand == null) {
            throw new IllegalArgumentException(
                    "Quartz command " + commandClass + " does not have the " + JobCommand.class + " annotation");
        }
        if (manual && jobCommand.manualUser() == TaskUser.INVALID) {
            throw new IllegalArgumentException(
                    "Quartz command " + commandClass + " does not support manual activation!");
        }
        if (!manual && jobCommand.schedulerUser() == TaskUser.INVALID) {
            throw new IllegalArgumentException(
                    "Quartz command " + commandClass + " does not support scheduled activation!");
        }
    }

    public static boolean pauseOrBreak() {
        InternalArtifactoryContext context = InternalContextHelper.get();
        return context != null && context.getTaskService().pauseOrBreak();
    }

    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 8];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
            if (pauseOrBreak()) {
                throw new IOException("Stopped copy on demand!");
            }
        }
        return count;
    }

    private static String generateUniqueName(Class<? extends QuartzCommand> commandClass) {
        return commandClass.getSimpleName() + "#" + UUID.randomUUID().toString();
    }

    public static Predicate<Task> createPredicateForType(final Class<? extends TaskCallback> callbackType) {
        return new TaskTypePredicate(callbackType);
    }
}
