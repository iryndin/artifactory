package org.artifactory.backup;

import org.apache.log4j.Logger;
import org.artifactory.schedule.ArtifactoryJob;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class BackupJob extends ArtifactoryJob {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupJob.class);

    @Override
    protected void onExecute(JobExecutionContext jobContext)
            throws JobExecutionException {
        final int backupIndex = jobContext.getJobDetail().getJobDataMap().getInt("index");
        InternalArtifactoryContext context = InternalContextHelper.get();
        BackupServiceImpl backup = context.beanForType(BackupServiceImpl.class);
        Date fireTime = jobContext.getFireTime();
        boolean success = backup.backupSystem(fireTime, context, backupIndex);
        if (success) {
            //If backup was successful continue with old backups cleanup
            backup.cleanupOldBackups(fireTime, backupIndex);
        }
    }
}
