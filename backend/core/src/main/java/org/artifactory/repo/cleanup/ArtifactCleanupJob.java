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

package org.artifactory.repo.cleanup;

import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.repo.replication.RemoteReplicationJob;
import org.artifactory.repo.service.ExportJob;
import org.artifactory.repo.service.ImportJob;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.StopCommand;
import org.artifactory.schedule.StopStrategy;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * A quartz command job that periodically cleans remote-repository caches from unused artifacts
 *
 * @author Noam Tenne
 */
@JobCommand(schedulerUser = TaskUser.SYSTEM,
        keyAttributes = {Task.REPO_KEY},
        commandsToStop = {
                @StopCommand(command = ImportJob.class, strategy = StopStrategy.IMPOSSIBLE),
                @StopCommand(command = ExportJob.class, strategy = StopStrategy.IMPOSSIBLE, useKey = true),
                @StopCommand(command = RemoteReplicationJob.class, strategy = StopStrategy.IMPOSSIBLE, useKey = true)}
)
public class ArtifactCleanupJob extends QuartzCommand {

    public static final String PERIOD_MILLIS = "periodMillis";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        InternalArtifactCleanupService cleaner = artifactoryContext.beanForType(InternalArtifactCleanupService.class);
        JobDataMap jobDataMap = callbackContext.getJobDetail().getJobDataMap();
        String repoKey = jobDataMap.get(Task.REPO_KEY).toString();
        long periodMillis = Long.parseLong(jobDataMap.get(PERIOD_MILLIS).toString());
        cleaner.clean(repoKey, periodMillis);
    }
}
