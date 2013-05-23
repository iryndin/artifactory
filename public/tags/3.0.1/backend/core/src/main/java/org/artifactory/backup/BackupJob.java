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

package org.artifactory.backup;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.repo.cleanup.IntegrationCleanupJob;
import org.artifactory.repo.index.MavenIndexerJob;
import org.artifactory.repo.index.MavenIndexerServiceImpl;
import org.artifactory.repo.service.ImportJob;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.StopCommand;
import org.artifactory.schedule.StopStrategy;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.storage.binstore.service.BinaryStoreGarbageCollectorJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yoav Landman
 */
@JobCommand(schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.CURRENT,
        keyAttributes = {BackupJob.BACKUP_KEY},
        commandsToStop = {
                @StopCommand(command = BinaryStoreGarbageCollectorJob.class, strategy = StopStrategy.PAUSE),
                @StopCommand(command = MavenIndexerServiceImpl.FindOrCreateMavenIndexJob.class,
                        strategy = StopStrategy.PAUSE),
                @StopCommand(command = MavenIndexerServiceImpl.SaveMavenIndexFileJob.class,
                        strategy = StopStrategy.PAUSE),
                @StopCommand(command = MavenIndexerJob.class, strategy = StopStrategy.PAUSE),
                @StopCommand(command = ArtifactCleanupJob.class, strategy = StopStrategy.STOP),
                @StopCommand(command = IntegrationCleanupJob.class, strategy = StopStrategy.STOP),
                @StopCommand(command = ImportJob.class, strategy = StopStrategy.IMPOSSIBLE)})
public class BackupJob extends QuartzCommand {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);
    public static final String BACKUP_KEY = "backupKey";
    public static final String MANUAL_BACKUP = "manualBackup";

    @Override
    protected void onExecute(JobExecutionContext jobContext) throws JobExecutionException {
        InternalArtifactoryContext context = InternalContextHelper.get();
        if (!context.isReady()) {
            log.debug("Skipping execution of '{}', sever is not ready yet", BackupJob.class.getName());
            return;
        }

        Object manualBackupDescriptor = jobContext.getJobDetail().getJobDataMap().get(MANUAL_BACKUP);
        String backupKey = jobContext.getJobDetail().getJobDataMap().getString(BACKUP_KEY);
        MultiStatusHolder jobStatus = new MultiStatusHolder();
        if (manualBackupDescriptor instanceof BackupDescriptor) {
            runBackup(context, getBackupService(), jobStatus, ((BackupDescriptor) manualBackupDescriptor));
        } else {
            runAutomaticBackup(context, jobStatus, backupKey);
        }

        //If backup was successful continue with old backups cleanup
        if (!jobStatus.hasErrors() && StringUtils.isNotBlank(backupKey)) {
            getBackupService().cleanupOldBackups(jobContext.getFireTime(), backupKey);
        }
    }

    private void runAutomaticBackup(InternalArtifactoryContext context,
            MultiStatusHolder jobStatus, String backupKey) {
        InternalBackupService backupService = getBackupService();
        BackupDescriptor backup = backupService.getBackup(backupKey);
        if (backup == null) {
            jobStatus.setError("Backup: '" + backupKey + "' not found or disabled. Backup was not performed.", log);
        } else {
            runBackup(context, backupService, jobStatus, backup);
        }
        //If backup failed, warn and do not clean up
        boolean backupKeyNotBlank = StringUtils.isNotBlank(backupKey);
        if (jobStatus.hasErrors()) {
            jobStatus.setWarning("Backup completed with some errors (see the log messages above for details). " +
                    "Old backups will not be auto-removed.", log);

            if (backupKeyNotBlank) {
                BackupDescriptor backupDescriptor = backupService.getBackup(backupKey);
                if (backupDescriptor != null && backupDescriptor.isEnabled() && backupDescriptor.isSendMailOnError()) {
                    try {
                        backupService.sendBackupErrorNotification(backupDescriptor.getKey(), jobStatus);
                    } catch (Exception e) {
                        jobStatus.setError("An error occurred while sending backup error notification", e, log);
                    }
                }
            }
        }
    }

    private InternalBackupService getBackupService() {
        return InternalContextHelper.get().beanForType(InternalBackupService.class);
    }

    private void runBackup(InternalArtifactoryContext context, InternalBackupService backupService,
            MultiStatusHolder jobStatus, BackupDescriptor backup) {
        try {
            MultiStatusHolder backupStatus = backupService.backupSystem(context, backup);
            jobStatus.merge(backupStatus);
        } catch (Exception e) {
            jobStatus.setError("An error occurred while performing a backup", e, log);
        }
    }
}