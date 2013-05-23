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

package org.artifactory.repo.index;

import org.artifactory.api.context.ContextHelper;
import org.artifactory.repo.service.ImportJob;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.StopCommand;
import org.artifactory.schedule.StopStrategy;
import org.artifactory.schedule.TaskUser;
import org.artifactory.storage.binstore.service.BinaryStoreGarbageCollectorJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author yoavl
 */
@JobCommand(
        singleton = true,
        schedulerUser = TaskUser.SYSTEM,
        manualUser = TaskUser.SYSTEM,
        commandsToStop = {
                @StopCommand(command = BinaryStoreGarbageCollectorJob.class, strategy = StopStrategy.IMPOSSIBLE),
                @StopCommand(command = ImportJob.class, strategy = StopStrategy.IMPOSSIBLE)
        }
)
public class MavenIndexerJob extends AbstractMavenIndexerJobs {

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        InternalMavenIndexerService indexer = ContextHelper.get().beanForType(InternalMavenIndexerService.class);
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        MavenIndexerRunSettings settings = (MavenIndexerRunSettings) jobDataMap.get(SETTINGS);
        settings.setFireTime(context.getFireTime());
        indexer.index(settings);
    }
}