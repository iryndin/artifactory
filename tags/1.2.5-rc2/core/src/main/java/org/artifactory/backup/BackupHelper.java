package org.artifactory.backup;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.ArtifactoryHome;
import org.artifactory.backup.config.Backup;
import org.artifactory.config.CentralConfig;
import org.artifactory.maven.MavenUtils;
import org.artifactory.process.StatusHolder;
import org.artifactory.repo.LocalCacheRepo;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;
import org.artifactory.repo.virtual.VirtualRepo;
import org.artifactory.spring.ArtifactoryContext;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
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
public class BackupHelper {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupHelper.class);

    public static final String DEFAULT_BACKUP_DIR =
            (ArtifactoryHome.path() + "/backup").replace('\\', '/');

    private CentralConfig cc;
    private Scheduler scheduler;

    public BackupHelper(CentralConfig cc, Scheduler scheduler) {
        this.scheduler = scheduler;
        update(cc);
    }

    public void update(CentralConfig cc) {
        this.cc = cc;
        Backup backup = cc.getBackup();
        if (backup == null) {
            LOGGER.info("No backup configured. Backup is disabled.");
            return;
        }
        String dir = backup.getDir();
        File file = new File(dir);
        boolean result = file.exists() || file.mkdirs();
        //Sanity check
        if (!result) {
            throw new IllegalArgumentException(
                    "Failed to create backup directory: " + file.getPath());
        }
        String cronExp = backup.getCronExp();
        JobDetail jobDetail = new JobDetail("backupJobDetail", null, BackupJob.class);
        //Schedule the croned backup
        try {
            scheduler.unscheduleJob("backupTrigger", null);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to unschedule previous backup job.", e);
        }
        if (cronExp != null) {
            CronTriggerBean trigger = new CronTriggerBean();
            trigger.setName("backupTrigger");
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

    public void backupRepos(String dir) {
        backupRepos(dir, Collections.<Repo>emptyList(), new StatusHolder());
    }

    public void backupRepos(String basePath, List<Repo> excludeRepositories, StatusHolder status) {
        List<LocalRepo> backedupRepos = getBackedupRepos(excludeRepositories);
        for (LocalRepo backedupRepo : backedupRepos) {
            backedupRepo.exportTo(basePath, status);
        }
    }

    public void backupSystem(Date time, ArtifactoryContext context) {
        Backup backup = cc.getBackup();
        String dir = backup.getDir();
        List<Repo> excludeRepositories = backup.getExcludedRepositories();
        String timestamp = MavenUtils.dateToTimestamp(time);
        String basePath = new File(dir, timestamp).getPath();
        File tempDir = new File(basePath);
        try {
            FileUtils.forceMkdir(tempDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp backup dir.", e);
        }
        StatusHolder status = new StatusHolder();
        List<LocalRepo> backedupRepos = getBackedupRepos(excludeRepositories);
        cc.exportTo(basePath, backedupRepos, status);
        context.getSecurity().exportTo(basePath, status);
        context.getKeyVal().exportTo(basePath, status);
        //Creating the archive
        ZipArchiver archiver = new ZipArchiver();
        //Avoid ugly output to stdout
        archiver.enableLogging(new ConsoleLogger(
                org.codehaus.plexus.logging.Logger.LEVEL_DISABLED, ""));
        File archive = new File(tempDir.getParentFile(), tempDir.getName() + ".zip");
        archiver.setDestFile(archive);
        try {
            archiver.addDirectory(tempDir);
            archiver.createArchive();
            status.setCallback(archive);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create backup archive.", e);
        } finally {
            //Delete the dir
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete temp backup dir.", e);
            }
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

    private List<LocalRepo> getBackedupRepos(List<Repo> excludeRepositories) {
        VirtualRepo virtualRepo = cc.getGlobalVirtualRepo();
        List<LocalRepo> localRepos = virtualRepo.getLocalAndCachedRepositories();
        List<LocalRepo> backedupRepos = new ArrayList<LocalRepo>();
        for (LocalRepo repo : localRepos) {
            //Skip excluded repositories
            Repo checkForExlusionRepo;
            if (repo.isCache()) {
                LocalCacheRepo cache = ((LocalCacheRepo) repo);
                checkForExlusionRepo = cache.getRemoteRepo();
            } else {
                checkForExlusionRepo = repo;
            }
            if (!excludeRepositories.contains(checkForExlusionRepo)) {
                backedupRepos.add(repo);
            }

        }
        return backedupRepos;
    }
}
