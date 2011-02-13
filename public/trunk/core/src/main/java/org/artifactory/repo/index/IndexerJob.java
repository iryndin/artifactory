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

package org.artifactory.repo.index;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.security.SystemAuthenticationToken;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

/**
 * @author yoavl
 */
public class IndexerJob extends QuartzCommand {

    public static final String MANUAL_RUN = "manualRun";

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        SecurityContextHolder.getContext().setAuthentication(new SystemAuthenticationToken());
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        TaskService taskService = artifactoryContext.beanForType(TaskService.class);
        try {
            //Stop the clean-up and gc jobs while the indexer is running
            taskService.stopTasks(JcrGarbageCollectorJob.class, true);
            taskService.stopTasks(ArtifactCleanupJob.class, true);

            InternalIndexerService indexer = artifactoryContext.beanForType(InternalIndexerService.class);
            JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
            boolean manualRun = Boolean.TRUE.equals(jobDataMap.get(MANUAL_RUN));
            Date fireTime = context.getFireTime();
            indexer.index(fireTime, manualRun);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
            taskService.resumeTasks(ArtifactCleanupJob.class);
            taskService.resumeTasks(JcrGarbageCollectorJob.class);
        }
    }
}