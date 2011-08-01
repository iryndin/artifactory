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

package org.artifactory.backup;

import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.repo.index.IndexerServiceImpl;
import org.artifactory.repo.service.ImportJob;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

import java.util.Date;

/**
 * @author Yoav Landman
 */
@JobCommand(schedulerUser = TaskUser.SYSTEM,
        commandsToStop = {
                JcrGarbageCollectorJob.class,
                IndexerServiceImpl.FindOrCreateIndexJob.class,
                IndexerServiceImpl.SaveIndexFileJob.class,
                IndexerJob.class,
                ArtifactCleanupJob.class,
                ImportJob.class})
public class BackupJob extends QuartzCommand {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);
    public static final String BAKUP_KEY = "backupKey";

    @Override
    protected void onExecute(JobExecutionContext jobContext) throws JobExecutionException {
        InternalArtifactoryContext context = InternalContextHelper.get();
        if (!context.isReady()) {
            log.debug("Skipping execution of '{}', sever is not ready yet", BackupJob.class.getName());
            return;
        }
        final String backupKey = jobContext.getJobDetail().getJobDataMap().getString(BAKUP_KEY);
        InternalBackupService backup = context.beanForType(InternalBackupService.class);
        Date fireTime = jobContext.getFireTime();
        MultiStatusHolder jobStatus = new MultiStatusHolder();
        try {
            MultiStatusHolder backupStatus = backup.backupSystem(context, backupKey);
            jobStatus.merge(backupStatus);
        } catch (Exception e) {
            jobStatus.setError("An error occurred while performing a backup", e, log);
        }
        //If backup failed, warn and do not clean up
        if (jobStatus.hasErrors()) {
            jobStatus.setWarning("Backup completed with some errors (see the log messages above for details). " +
                    "Old backups will not be auto-removed.", log);

            BackupDescriptor backupDescriptor = backup.getBackup(backupKey);
            if (backupDescriptor != null && backupDescriptor.isEnabled() && backupDescriptor.isSendMailOnError()) {
                try {
                    backup.sendBackupErrorNotification(backupDescriptor.getKey(), jobStatus);
                } catch (Exception e) {
                    jobStatus.setError("An error occurred while sending backup error notification", e, log);
                }
            }
            return;
        }
        //If backup was successful continue with old backups cleanup
        backup.cleanupOldBackups(fireTime, backupKey);
    }
}
