package org.artifactory.backup;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.backup.config.Backup;
import org.artifactory.config.CentralConfig;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.spring.ArtifactoryContext;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.CronTriggerBean;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class BackupManager implements ApplicationContextAware, InitializingBean {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupManager.class);

    private static final String BACKUP_TRIGGER_NAME = "backupTrigger";

    private ArtifactoryContext context;
    private Scheduler scheduler;

    public BackupManager(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = (ArtifactoryContext) context;
    }

    public void init() {
        //Unschedule any previousely set backup job
        try {
            scheduler.unscheduleJob(BACKUP_TRIGGER_NAME, null);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to unschedule previous backup job.", e);
        }
        CentralConfig cc = context.getCentralConfig();
        Backup backup = cc.getBackup();
        if (backup == null) {
            LOGGER.info("No backup configured. Backup is disabled.");
            return;
        }
        backup.getBackupDir();
        String cronExp = backup.getCronExp();
        JobDetail jobDetail = new JobDetail("backupJobDetail", null, BackupJob.class);
        //Schedule the croned backup
        if (cronExp != null) {
            CronTriggerBean trigger = new CronTriggerBean();
            trigger.setName(BACKUP_TRIGGER_NAME);
            trigger.setJobDetail(jobDetail);
            try {
                trigger.setCronExpression(cronExp);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid cron exp '" + cronExp + "'.", e);
            }
            try {
                trigger.afterPropertiesSet();
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                throw new RuntimeException("Error in scheduling the backup job.", e);
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Backup activated with cron expression '" + cronExp + "'.");
            }
        } else {
            LOGGER.warn("No backup cron expression is configured. Backup will be disabled.");
        }
    }

    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void backupRepos(File backupDir) {
        backupRepos(backupDir, Collections.<RealRepo>emptyList(), new StatusHolder());
    }

    public void backupRepos(File backupDir, List<RealRepo> excludeRepositories,
            StatusHolder status) {
        List<LocalRepo> backedupRepos = getBackedupRepos(excludeRepositories);
        for (LocalRepo backedupRepo : backedupRepos) {
            backedupRepo.exportTo(backupDir, status);
        }
    }

    /**
     * @param time
     * @param context
     * @return true if backup was successful
     */
    public boolean backupSystem(Date time, ArtifactoryContext context) {
        Backup backup = getBackup();
        if (backup == null) {
            //This might happen after the first time a backup has been turned off if the scheduler
            //wakes up before old jobs were cleaned up
            LOGGER.info("Skipping empty backup config (probably a leftover from an old " +
                    "to-be-deleted backup job).");
            return false;
        }
        List<RealRepo> excludeRepositories = backup.getExcludedRepositories();
        List<LocalRepo> backedupRepos = getBackedupRepos(excludeRepositories);
        File backupDir = backup.getBackupDir();
        StatusHolder status = new StatusHolder();
        boolean createArchive = backup.isCreateArchive();
        int retentionPeriod = backup.getRetentionPeriodHours();
        Date backupTime = retentionPeriod > 0 ? time : null;
        context.exportTo(backupDir, backedupRepos, createArchive, backupTime, status);
        status.reset();
        return true;
    }

    /**
     * Iterate (non-recursively) on all folders/files in the backup dir and delete them if they are
     * older than "now"
     *
     * @param now
     */
    public void cleanupOldBackups(Date now) {
        Backup backup = getBackup();
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
        File file = backup.getBackupDir();
        File[] children = file.listFiles();
        //Delete anything not newer than the last valid time
        if (children.length > 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing old backups...");
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

    private List<LocalRepo> getBackedupRepos(List<RealRepo> excludeRepositories) {
        VirtualRepo virtualRepo = context.getCentralConfig().getGlobalVirtualRepo();
        List<LocalRepo> localRepos = virtualRepo.getLocalAndCachedRepositories();
        List<LocalRepo> backedupRepos = new ArrayList<LocalRepo>();
        for (LocalRepo repo : localRepos) {
            //Skip excluded repositories
            RealRepo checkForExclusionRepo;
            if (repo.isCache()) {
                LocalCacheRepo cache = ((LocalCacheRepo) repo);
                checkForExclusionRepo = cache.getRemoteRepo();
            } else {
                checkForExclusionRepo = repo;
            }
            if (!excludeRepositories.contains(checkForExclusionRepo)) {
                backedupRepos.add(repo);
            }

        }
        return backedupRepos;
    }

    private Backup getBackup() {
        return context.getCentralConfig().getBackup();
    }
}
