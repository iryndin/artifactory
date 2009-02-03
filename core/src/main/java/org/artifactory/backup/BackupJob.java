package org.artifactory.backup;

import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class BackupJob extends QuartzCommand {

    @Override
    protected void onExecute(JobExecutionContext jobContext)
            throws JobExecutionException {
        final int backupIndex = jobContext.getJobDetail().getJobDataMap().getInt("index");
        InternalArtifactoryContext context = InternalContextHelper.get();
        InternalBackupService backup = context.beanForType(InternalBackupService.class);
        Date fireTime = jobContext.getFireTime();
        boolean success = backup.backupSystem(fireTime, context, backupIndex);
        if (success) {
            //If backup was successful continue with old backups cleanup
            backup.cleanupOldBackups(fireTime, backupIndex);
        }
    }
}
