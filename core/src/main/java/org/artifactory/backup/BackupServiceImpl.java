package org.artifactory.backup;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.repo.BackupService;
import org.artifactory.config.CentralConfigServiceImpl;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.PostInitializingBean;
import org.quartz.CronExpression;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
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
@Service
public class BackupServiceImpl implements PostInitializingBean, BackupService {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(BackupServiceImpl.class);

    private static final String BACKUP_TRIGGER_NAME = "backupTrigger";

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private CentralConfigServiceImpl centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addPostInit(getClass());
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends PostInitializingBean>[] initAfter() {
        return new Class[]{
                InternalRepositoryService.class
        };
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void init() {
        List<BackupDescriptor> descriptors = centralConfig.getDescriptor().getBackups();
        if (descriptors.isEmpty()) {
            LOGGER.info("No backups configured. Backups is disabled.");
            return;
        }
        for (int i = 0; i < descriptors.size(); i++) {
            BackupDescriptor descriptor = null;
            try {
                descriptor = descriptors.get(i);
                activateBackup(descriptor, i);
            } catch (Exception e) {
                LOGGER.warn("activation of backup number " + i + ":" + descriptor + " failed:" +
                        e.getMessage(), e);
            }
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        List<BackupDescriptor> list = oldDescriptor.getBackups();
        if (list != null && !list.isEmpty()) {
            //Unschedule any previousely set backup job
            for (int i = 0; i < list.size(); i++) {
                try {
                    scheduler.unscheduleJob(BACKUP_TRIGGER_NAME + i, null);
                } catch (SchedulerException e) {
                    throw new RuntimeException("Failed to unschedule previous backup job " + i, e);
                }
            }
        }
        init();
    }

    private void activateBackup(BackupDescriptor descriptor, int index) {
        descriptor.getBackupDir();
        String cronExp = descriptor.getCronExp();
        try {
            // Check cron validity
            new CronExpression(cronExp);
        } catch (ParseException e) {
            LOGGER.error(
                    "Bad backup cron expression '" + cronExp + "'" +
                            " backup number " + index + " will be ignored (" +
                            e.getMessage() + ").");
            return;
        }
        JobDetail jobDetail = new JobDetail("backupJobDetail" + index, null, BackupJob.class);
        jobDetail.getJobDataMap().put("index", index);
        //Schedule the croned backup
        if (cronExp != null) {
            CronTriggerBean trigger = new CronTriggerBean();
            trigger.setName(BACKUP_TRIGGER_NAME + index);
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

    public void backupRepos(File backupDir) {
        backupRepos(backupDir, Collections.<RealRepoDescriptor>emptyList(), new StatusHolder());
    }

    public void backupRepos(File backupDir, List<RealRepoDescriptor> excludeRepositories,
            StatusHolder status) {
        List<LocalRepoDescriptor> backedupRepos = getBackedupRepos(excludeRepositories);
        ExportSettings settings = new ExportSettings(backupDir);
        settings.setReposToExport(backedupRepos);
        repositoryService.exportTo(settings, status);
    }

    /**
     * @param time
     * @param context
     * @param backupIndex
     * @return true if backup was successful
     */
    public boolean backupSystem(Date time, InternalArtifactoryContext context, int backupIndex) {
        BackupDescriptor descriptor = getBackup(backupIndex);
        if (descriptor == null) {
            //This might happen after the first time a backup has been turned off if the scheduler
            //wakes up before old jobs were cleaned up
            LOGGER.info("Skipping empty backup config (probably a leftover from an old " +
                    "to-be-deleted backup job).");
            return false;
        }
        List<RealRepoDescriptor> excludeRepositories = descriptor.getExcludedRepositories();
        List<LocalRepoDescriptor> backedupRepos = getBackedupRepos(excludeRepositories);
        File backupDir = descriptor.getBackupDir();
        StatusHolder status = new StatusHolder();
        boolean createArchive = descriptor.isCreateArchive();
        int retentionPeriod = descriptor.getRetentionPeriodHours();
        Date backupTime = retentionPeriod > 0 ? time : null;
        //You cannot backup in place (retention period of 0) and archive backup at the same time
        if (backupTime == null && createArchive) {
            LOGGER.warn("An in place backup cannot be archived!\n" +
                    "Please change configuration of backup number " + (backupIndex + 1));
            createArchive = false;
        }
        ExportSettings settings = new ExportSettings(backupDir);
        settings.setReposToExport(backedupRepos);
        settings.setCreateArchive(createArchive);
        settings.setTime(backupTime);
        context.exportTo(settings, status);
        status.reset();
        return true;
    }

    /**
     * Iterate (non-recursively) on all folders/files in the backup dir and delete them if they are
     * older than "now"
     *
     * @param now
     * @param backupIndex
     */
    public void cleanupOldBackups(Date now, int backupIndex) {
        BackupDescriptor descriptor = getBackup(backupIndex);
        int retentionPeriodHours = descriptor.getRetentionPeriodHours();
        //No action if retention is 0 (or less)
        if (retentionPeriodHours <= 0) {
            return;
        }
        //Calculate last valid time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.HOUR, -retentionPeriodHours);
        Date validFrom = calendar.getTime();
        File file = descriptor.getBackupDir();
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

    private List<LocalRepoDescriptor> getBackedupRepos(
            List<RealRepoDescriptor> excludeRepositories) {
        List<LocalRepoDescriptor> localRepos = repositoryService.getLocalAndCachedRepoDescriptors();
        List<LocalRepoDescriptor> backedupRepos = new ArrayList<LocalRepoDescriptor>();
        for (LocalRepoDescriptor repo : localRepos) {
            //Skip excluded repositories
            RealRepoDescriptor checkForExclusionRepo;
            if (repo.isCache()) {
                checkForExclusionRepo = ((LocalCacheRepoDescriptor) repo).getRemoteRepo();
            } else {
                checkForExclusionRepo = repo;
            }
            //Skip excluded repositories
            boolean excluded = false;
            for (RealRepoDescriptor excludedRepo : excludeRepositories) {
                if (excludedRepo.getKey().equals(checkForExclusionRepo.getKey())) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                backedupRepos.add(repo);
            }
        }
        return backedupRepos;
    }

    private BackupDescriptor getBackup(int backupIndex) {
        final List<BackupDescriptor> list = centralConfig.getDescriptor().getBackups();
        if (list.size() <= backupIndex) {
            throw new RuntimeException(
                    "Backup number " + backupIndex + " does not exists in the configuration!");
        }
        return list.get(backupIndex);
    }
}
