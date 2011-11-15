/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.repo.service;

import org.artifactory.api.config.ImportSettingsImpl;
import org.artifactory.backup.BackupJob;
import org.artifactory.common.MutableStatusHolder;
import org.artifactory.jcr.schedule.JcrGarbageCollectorJob;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.InternalRepoPathFactory;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.repo.index.IndexerServiceImpl;
import org.artifactory.repo.replication.LocalReplicationJob;
import org.artifactory.repo.replication.RemoteReplicationJob;
import org.artifactory.sapi.common.BaseSettings;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.StopCommand;
import org.artifactory.schedule.StopStrategy;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.search.InternalSearchService;
import org.artifactory.security.PermissionTargetInfo;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;

/**
 * @author freds
 * @date Nov 6, 2008
 */
@JobCommand(manualUser = TaskUser.CURRENT,
        keyAttributes = { Task.REPO_KEY },
        commandsToStop = {
                @StopCommand(command = JcrGarbageCollectorJob.class, strategy = StopStrategy.IMPOSSIBLE),
                @StopCommand(command = ExportJob.class, strategy = StopStrategy.IMPOSSIBLE),
                @StopCommand(command = BackupJob.class, strategy = StopStrategy.IMPOSSIBLE),
                @StopCommand(command = IndexerServiceImpl.FindOrCreateIndexJob.class, strategy = StopStrategy.STOP),
                @StopCommand(command = IndexerServiceImpl.SaveIndexFileJob.class, strategy = StopStrategy.STOP),
                @StopCommand(command = IndexerJob.class, strategy = StopStrategy.STOP),
                @StopCommand(command = ArtifactCleanupJob.class, strategy = StopStrategy.STOP),
                @StopCommand(command = LocalReplicationJob.class, strategy = StopStrategy.STOP),
                @StopCommand(command = RemoteReplicationJob.class, strategy = StopStrategy.STOP)
        })
public class ImportJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(ImportJob.class);

    public static final String DELETE_REPO = "deleteRepo";

    @Override
    protected void onExecute(JobExecutionContext callbackContext) {
        MutableStatusHolder status = null;
        try {
            JobDataMap jobDataMap = callbackContext.getJobDetail().getJobDataMap();
            String repoKey = (String) jobDataMap.get(Task.REPO_KEY);
            if (repoKey == null) {
                throw new IllegalStateException("Cannot Import unknown target for job "+this);
            }
            boolean deleteRepo = (Boolean) jobDataMap.get(DELETE_REPO);
            ImportSettingsImpl settings = (ImportSettingsImpl) jobDataMap.get(ImportSettingsImpl.class.getName());
            status = settings.getStatusHolder();
            InternalRepositoryService repositoryService =
                    InternalContextHelper.get().beanForType(InternalRepositoryService.class);
            if (BaseSettings.FULL_SYSTEM.equals(repoKey)) {
                repositoryService.importFrom(settings);
            } else {
                if (repoKey.equals(PermissionTargetInfo.ANY_REPO)) {
                    repositoryService.importAll(settings);
                } else {
                    if (deleteRepo && repositoryService.repositoryByKey(repoKey) != null) {
                        status.setStatus("Fully removing repository '" + repoKey + "'.", log);
                        RepoPath deleteRepoPath = InternalRepoPathFactory.repoRootPath(repoKey);
                        try {
                            repositoryService.undeploy(deleteRepoPath);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                        status.setStatus("Repository '" + repoKey + "' fully deleted.", log);
                        try {
                            // Wait 2 seconds for the DB to delete the files..
                            // Bug in Jackrabbit/Derby:
                            // A lock could not be obtained within the time requested, state/code: 40XL1/30000
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            status.setError(e.getMessage(), e, log);
                        }
                    }
                    repositoryService.importRepo(repoKey, settings);
                }
            }

            if (settings.isIndexMarkedArchives()) {
                InternalSearchService internalSearchService =
                        InternalContextHelper.get().beanForType(InternalSearchService.class);
                internalSearchService.asyncIndexMarkedArchives();
            }
        } catch (RuntimeException e) {
            if (status != null) {
                status.setError("Error occurred during import: " + e.getMessage(), e, log);
            } else {
                log.error("Error occurred during import", e);
            }
        }
    }

}