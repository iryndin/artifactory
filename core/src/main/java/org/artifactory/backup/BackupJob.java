package org.artifactory.backup;

import org.apache.log4j.Logger;
import org.artifactory.jcr.JcrHelper;
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
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ArtifactoryContext artifactoryContext = getArtifactoryContext();
        BackupHelper backup = artifactoryContext.getBackup();
        Date fireTime = context.getFireTime();
        try {
            backup.backupSystem(fireTime, artifactoryContext);
            backup.cleanupOldBackups(fireTime);
        } finally {
            JcrHelper jcr = artifactoryContext.getJcr();
            if (jcr != null) {
                jcr.unbindSession(false);
            }
        }
    }
}
