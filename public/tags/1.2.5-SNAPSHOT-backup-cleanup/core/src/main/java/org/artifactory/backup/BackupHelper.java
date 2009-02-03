package org.artifactory.backup;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.transform.SnapshotTransformation;
import org.artifactory.ArtifactoryHome;
import org.artifactory.backup.config.Backup;
import org.artifactory.config.CentralConfig;
import org.artifactory.repo.LocalRepo;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.CronTriggerBean;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class BackupHelper {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupHelper.class);

    public static final String DEFAULT_BACKUP_DIR =
            (ArtifactoryHome.path() + "/backup").replace('\\', '/');

    private CentralConfig cc;

    public BackupHelper(CentralConfig cc, Scheduler scheduler) throws Exception {
        this.cc = cc;
        Backup backup = cc.getBackup();
        String dir = backup.getDir();
        File file = new File(dir);
        boolean result = file.exists() || file.mkdirs();
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create backup directory: " + file.getAbsolutePath());
        }
        String cronExp = backup.getCronExp();
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
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Backup activated with cron expression '" + cronExp + "'.");
            }
        } else {
            LOGGER.warn("No backup cron expression is configured. Backup will be disabled.");
        }
    }

    public void backupRepos() {
        backupRepos(new Date());
    }

    public void backupRepos(String dir) {
        backupRepos(dir, new Date());
    }

    public void backupRepos(Date date) {
        Backup backup = cc.getBackup();
        String dir = backup.getDir();
        backupRepos(dir, date);
    }

    public void backupRepos(String dir, Date date) {
        List<LocalRepo> localRepos = cc.getLocalAndCachedRepositories();
        String timestamp = SnapshotTransformation.getUtcDateFormatter().format(date);
        for (LocalRepo repo : localRepos) {
            String key = repo.getKey();
            File targetDir = new File(dir + "/" + timestamp + "/" + key);
            repo.exportToDir(targetDir);
        }
    }

    /**
     * Iterate (non-recursively) on all folders/files in the backup dir and delete them if they are
     * older than
     *
     * @param now
     */
    public void cleanupOldBackups(Date now) {
        Backup backup = cc.getBackup();
        int retentionPeriodHours = backup.getRetentionPeriodHours();
        //No action if retention is 0 (or less)
        if (retentionPeriodHours <= 0) {
            return;
        }
        //Calculate last valid time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.HOUR, -retentionPeriodHours);
        Date validFrom = calendar.getTime();
        String dir = backup.getDir();
        File file = new File(dir);
        File[] children = file.listFiles();
        //Delete anything not newer than the last valid time
        if (children.length > 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing old backup files...");
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No old backup files to remove.");
            }
        }
        for (File child : children) {
            if (!FileUtils.isFileNewer(child, validFrom)) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Removing old backup file '" + child.getPath() + "'.");
                    }
                    FileUtils.forceDelete(child);
                } catch (IOException e) {
                    LOGGER.warn("Failed to remove old backup file or folder '"
                            + child.getPath() + "'.", e);
                }
            }
        }
    }
}
