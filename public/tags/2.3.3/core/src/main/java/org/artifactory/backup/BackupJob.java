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
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * @author Yoav Landman
 */
public class BackupJob extends QuartzCommand {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);

    @Override
    protected void onExecute(JobExecutionContext jobContext) throws JobExecutionException {
        InternalArtifactoryContext context = InternalContextHelper.get();
        if (!context.isReady()) {
            log.debug("Skipping execution of '{}', sever is not ready yet", BackupJob.class.getName());
            return;
        }
        final int backupIndex = jobContext.getJobDetail().getJobDataMap().getInt("index");
        InternalBackupService backup = context.beanForType(InternalBackupService.class);
        Date fireTime = jobContext.getFireTime();
        MultiStatusHolder jobStatus = new MultiStatusHolder();
        try {
            MultiStatusHolder backupStatus = backup.backupSystem(context, backupIndex);
            jobStatus.merge(backupStatus);
        } catch (Exception e) {
            jobStatus.setError("An error occurred while performing a backup", e, log);
        }
        //If backup failed, warn and do not clean up
        if (jobStatus.hasErrors()) {
            jobStatus.setWarning("Backup completed with some errors (see the log messages above for details). " +
                    "Old backups will not be auto-removed.", log);

            BackupDescriptor backupDescriptor = getBackup(backupIndex);
            if ((backupDescriptor != null) && backupDescriptor.isSendMailOnError()) {
                try {
                    backup.sendBackupErrorNotification(backupDescriptor.getKey(), jobStatus);
                } catch (Exception e) {
                    jobStatus.setError("An error occurred while sending backup error notification", e, log);
                }
            }
            return;
        }
        //If backup was successful continue with old backups cleanup
        backup.cleanupOldBackups(fireTime, backupIndex);
    }

    /**
     * Returns a backup descriptor via the given index
     *
     * @param backupIndex Index of descriptor to acquire
     * @return BackupDescriptor if the index is valid. Null if not
     */
    private BackupDescriptor getBackup(int backupIndex) {
        InternalArtifactoryContext context = InternalContextHelper.get();
        CentralConfigService centralConfig = context.getCentralConfig();
        final List<BackupDescriptor> list = centralConfig.getDescriptor().getBackups();
        if (list.size() <= backupIndex) {
            return null;
        }
        return list.get(backupIndex);
    }
}
