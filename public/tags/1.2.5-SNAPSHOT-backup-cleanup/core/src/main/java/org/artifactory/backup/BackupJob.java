package org.artifactory.backup;

import org.apache.log4j.Logger;
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
        backup.backupRepos(fireTime);
        backup.cleanupOldBackups(fireTime);
    }
}
