package org.artifactory.backup;

import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrSessionWrapper;
import org.artifactory.scheduling.ArtifactoryJob;
import org.artifactory.spring.ArtifactoryContext;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class BackupJob extends ArtifactoryJob {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupJob.class);

    @SuppressWarnings({"unchecked"})
    protected void onExecute(JobExecutionContext context, JcrSessionWrapper jcrSession) throws JobExecutionException {
        jcrSession.setReadOnly(true);
        ArtifactoryContext artifactoryContext = getArtifactoryContext();
        BackupManager backup = artifactoryContext.beanForType(BackupManager.class);
        Date fireTime = context.getFireTime();
        boolean success = backup.backupSystem(fireTime, artifactoryContext);
        if (success) {
            //If backup was successful continue with old backups cleanup
            backup.cleanupOldBackups(fireTime);
        }
    }
}
