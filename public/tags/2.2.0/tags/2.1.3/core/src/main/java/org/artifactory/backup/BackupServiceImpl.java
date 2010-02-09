/*
 * This file is part of Artifactory.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.backup;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.common.StatusEntry;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.mail.MailService;
import org.artifactory.api.security.UserGroupService;
import org.artifactory.api.security.UserInfo;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.descriptor.backup.BackupDescriptor;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalCacheRepoDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RealRepoDescriptor;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzTask;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.util.EmailException;
import org.artifactory.version.CompoundVersionDetails;
import org.quartz.CronExpression;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
@Service
@Reloadable(beanClass = InternalBackupService.class, initAfter = {InternalRepositoryService.class, TaskService.class})
public class BackupServiceImpl implements InternalBackupService {
    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    private static final String BACKUP_TRIGGER_NAME = "backupTrigger";

    @Autowired
    private TaskService taskService;

    @Autowired
    private CentralConfigService centralConfig;

    @Autowired
    private InternalRepositoryService repositoryService;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserGroupService userGroupService;

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{
                InternalRepositoryService.class,
                TaskService.class
        };
    }

    public void init() {
        List<BackupDescriptor> backupDescriptors = centralConfig.getDescriptor().getBackups();
        if (backupDescriptors.isEmpty()) {
            log.info("No backups configured. Backup is disabled.");
            return;
        }
        for (int i = 0; i < backupDescriptors.size(); i++) {
            BackupDescriptor backupDescriptor = null;
            try {
                backupDescriptor = backupDescriptors.get(i);
                if (backupDescriptor.isEnabled()) {
                    activateBackup(backupDescriptor, i);
                }
            } catch (Exception e) {
                log.warn("activation of backup number " + i + ":" + backupDescriptor + " failed:" +
                        e.getMessage(), e);
            }
        }
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
        unschedule();
        init();
    }

    public void destroy() {
        unschedule();
    }

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    private void unschedule() {
        taskService.cancelTasks(BackupJob.class, true);
    }

    private void activateBackup(BackupDescriptor descriptor, int index) {
        String cronExp = descriptor.getCronExp();
        try {
            // Check cron validity
            new CronExpression(cronExp);
        } catch (ParseException e) {
            log.error(
                    "Bad backup cron expression '" + cronExp + "' backup number " + index + " will be ignored (" +
                            e.getMessage() + ").");
            return;
        }
        JobDetail jobDetail = new JobDetail("backupJob#" + index, null, BackupJob.class);
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
                QuartzTask task = new QuartzTask(trigger);
                taskService.startTask(task);
            } catch (Exception e) {
                throw new RuntimeException("Error in scheduling the backup job.", e);
            }
            if (log.isInfoEnabled()) {
                log.info("Backup activated with cron expression '" + cronExp + "'.");
            }
        } else {
            log.warn("No backup cron expression is configured. Backup will be disabled.");
        }
    }

    public void backupRepos(File backupDir, ExportSettings exportSettings) {
        backupRepos(backupDir, Collections.<RealRepoDescriptor>emptyList(), exportSettings);
    }

    public void backupRepos(File backupDir, List<RealRepoDescriptor> excludeRepositories,
            ExportSettings exportSettings) {
        List<LocalRepoDescriptor> backedupRepos = getBackedupRepos(excludeRepositories);
        ExportSettings settings = new ExportSettings(backupDir, exportSettings);
        settings.setRepositories(backedupRepos);
        repositoryService.exportTo(settings);
    }

    public MultiStatusHolder backupSystem(InternalArtifactoryContext context, int backupIndex) {
        MultiStatusHolder status = new MultiStatusHolder();
        BackupDescriptor backup = getBackup(backupIndex);
        if (backup == null) {
            status.setError("Backup index: '" + backupIndex + "' was not found. Backup was not performed.", log);
            return status;
        }
        List<RealRepoDescriptor> excludeRepositories = backup.getExcludedRepositories();
        List<LocalRepoDescriptor> backedupRepos = getBackedupRepos(excludeRepositories);
        File backupDir = getBackupDir(backup);
        boolean createArchive = backup.isCreateArchive();
        boolean incremental = backup.isIncremental();
        //Date backupTime = retentionPeriod > 0 ? new Date() : null;
        //You cannot backup in place (retention period of 0) and archive backup at the same time
        if (incremental && createArchive) {
            status.setWarning("An incremental backup cannot be archived!\n" +
                    "Please change the configuration of backup " + backup.getKey() + ".", log);
            createArchive = false;
        }
        ExportSettings settings = new ExportSettings(backupDir, status);
        settings.setRepositories(backedupRepos);
        settings.setCreateArchive(createArchive);
        settings.setIncremental(incremental);
        context.exportTo(settings);

        return status;
    }

    public void cleanupOldBackups(Date now, int backupIndex) {
        BackupDescriptor descriptor = getBackup(backupIndex);
        if (descriptor == null) {
            return;
        }
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
        File backupDir = getBackupDir(descriptor);
        File[] children = backupDir.listFiles();
        //Delete anything not newer than the last valid time
        if (children.length > 0) {
            log.debug("Removing old backups...");
        } else {
            log.debug("No old backup files to remove.");
        }
        for (File child : children) {
            if (!FileUtils.isFileNewer(child, validFrom)) {
                try {
                    log.debug("Removing old backup file '{}'.", child.getPath());
                    FileUtils.forceDelete(child);
                } catch (IOException e) {
                    log.warn("Failed to remove old backup file or folder '" + child.getPath() + "'.", e);
                }
            }
        }
    }

    public File getBackupDir(BackupDescriptor descriptor) {
        File dir = descriptor.getDir();
        File backupDir;
        if (dir == null) {
            ArtifactoryHome artifactoryHome = ContextHelper.get().getArtifactoryHome();
            backupDir = new File(artifactoryHome.getBackupDir(), descriptor.getKey());
        } else {
            backupDir = dir;
            try {
                FileUtils.forceMkdir(backupDir);
            } catch (IOException e) {
                throw new IllegalArgumentException("Backup directory provided in configuration: '" +
                        backupDir.getAbsolutePath() + "' cannot be created or is not a directory.");
            }
        }
        return backupDir;
    }

    public void sendBackupErrorNotification(String backupName, MultiStatusHolder statusHolder) throws Exception {
        List<UserInfo> userInfoList = userGroupService.getAllUsers(true);

        InputStream stream = null;
        try {
            //Get message body from properties and substitute variables
            stream = getClass().getResourceAsStream("/org/artifactory/email/messages/backupError.properties");
            ResourceBundle resourceBundle = new PropertyResourceBundle(stream);
            String body = resourceBundle.getString("body");
            String errorListBlock = getErrorListBlock(statusHolder);

            for (UserInfo userInfo : userInfoList) {
                if (userInfo.isAdmin()) {
                    String adminEmail = userInfo.getEmail();
                    if (StringUtils.isNotBlank(adminEmail)) {

                        String message = MessageFormat.format(body, backupName, errorListBlock);
                        mailService.sendMail(new String[]{adminEmail}, "Backup Error Notification", message);
                    }
                }
            }
        } catch (EmailException e) {
            log.error("Error while notification of: '" + backupName + "' errors.", e);
            throw e;
        } finally {
            IOUtils.closeQuietly(stream);
        }
        log.info("Error notification for backup '{}' was sent by mail.", backupName);
    }

    private List<LocalRepoDescriptor> getBackedupRepos(List<RealRepoDescriptor> excludeRepositories) {
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
            //This might happen after the first time a backup has been turned off if the scheduler
            //wakes up before old jobs were cleaned up
            log.warn("Skipping empty backup config (probably a leftover from an old " +
                    "to-be-deleted backup job).");
            return null;
        }
        return list.get(backupIndex);
    }

    /**
     * Returns an HTML list block of errors extracted from the status holder
     *
     * @param statusHolder Status holder containing errors that should be included in the notification
     * @return HTML list block
     */
    private String getErrorListBlock(MultiStatusHolder statusHolder) {
        StringBuilder builder = new StringBuilder();

        for (StatusEntry errorEntry : statusHolder.getErrors()) {

            //Make one error per row
            String errorMessage = errorEntry.getMessage();

            Throwable throwable = errorEntry.getException();
            if (throwable != null) {
                String throwableMessage = throwable.getMessage();
                if (StringUtils.isNotBlank(throwableMessage)) {
                    errorMessage += ": " + throwableMessage;
                }
            }
            builder.append(errorMessage).append("<br>");
        }

        builder.append("<p>");

        return builder.toString();
    }
}
