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

package org.artifactory.repo.service;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * @author freds
 * @date Nov 6, 2008
 */
public class ExportJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(ExportJob.class);

    public static final String REPO_KEY = "repoKey";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        TaskService taskService = InternalContextHelper.get().beanForType(TaskService.class);
        MultiStatusHolder status = null;
        try {
            //Stop the clean-up job while the backup is running
            taskService.stopTasks(ArtifactCleanupJob.class, true);

            JobDataMap jobDataMap = callbackContext.getJobDetail().getJobDataMap();
            String repoKey = (String) jobDataMap.get(REPO_KEY);
            ExportSettings settings = (ExportSettings) jobDataMap.get(ExportSettings.class.getName());
            status = settings.getStatusHolder();
            InternalRepositoryService service =
                    InternalContextHelper.get().beanForType(InternalRepositoryService.class);
            if (repoKey != null) {
                service.exportRepo(repoKey, settings);
            } else {
                service.exportTo(settings);
            }
        } catch (Exception e) {
            if (status != null) {
                status.setError("Error occurred during export: " + e.getMessage(), e, log);
            } else {
                log.error("Error occurred during export", e);
            }
        } finally {
            taskService.resumeTasks(ArtifactCleanupJob.class);
        }
    }
}
