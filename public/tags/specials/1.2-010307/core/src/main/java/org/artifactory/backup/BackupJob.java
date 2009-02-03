package org.artifactory.backup;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.transform.SnapshotTransformation;
import org.artifactory.repo.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.artifactory.scheduling.ArtifactoryJob;
import org.artifactory.spring.ArtifactoryContext;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class BackupJob extends ArtifactoryJob {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupJob.class);

    @SuppressWarnings({"unchecked"})
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ArtifactoryContext artifactoryContext = getArtifactoryContext();
        CentralConfig cc = artifactoryContext.getCentralConfig();
        List<LocalRepo> localRepos = cc.getLocalRepositories();
        String backupDir = cc.getBackupDir();
        Date fireTime = context.getFireTime();
        String timestamp = SnapshotTransformation.getUtcDateFormatter().format(fireTime);
        for (LocalRepo repo : localRepos) {
            String key = repo.getKey();
            File targetDir = new File(backupDir + "/" + timestamp + "/" + key);
            if (!targetDir.mkdirs()) {
                throw new RuntimeException(
                        "Failed to create backup directory '" + targetDir + "'.");
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Exporting repository '" + key + "' to '" + targetDir + "'.");
            }
            repo.export(targetDir);
        }
    }
}
