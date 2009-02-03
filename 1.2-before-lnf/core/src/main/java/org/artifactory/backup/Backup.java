package org.artifactory.backup;

import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.repo.CentralConfig;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.CronTriggerBean;

import java.io.File;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class Backup {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(Backup.class);

    public static final String DEFAULT_BACKUP_DIR =
            (ArtifactoryHome.path() + "/backup").replace('\\', '/');

    public Backup(CentralConfig cc, Scheduler scheduler) throws Exception {
        String backupDir = cc.getBackupDir();
        File file = new File(backupDir);
        boolean result = file.exists() || file.mkdirs();
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create backup directory: " + file.getAbsolutePath());
        }
        String cronExp = cc.getBackupCronExp();
        JobDetail jobDetail = new JobDetail("backupJobDetail", null, BackupJob.class);
        //Schedule the croned backup
        scheduler.unscheduleJob("backupTrigger", null);
        if (cronExp != null) {
            CronTriggerBean trigger = new CronTriggerBean();
            trigger.setName("backupTrigger");
            trigger.setCronExpression(cronExp);
            trigger.setJobDetail(jobDetail);
            trigger.afterPropertiesSet();
            scheduler.scheduleJob(jobDetail, trigger);
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.warn("No backup cron expression is configured. Backup will be disabled.");
            }
        }
    }
}
