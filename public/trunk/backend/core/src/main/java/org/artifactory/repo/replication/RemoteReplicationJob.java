/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.repo.replication;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.replication.RemoteReplicationSettingsBuilder;
import org.artifactory.addon.replication.ReplicationAddon;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.repo.service.ImportJob;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.StopCommand;
import org.artifactory.schedule.StopStrategy;
import org.artifactory.schedule.Task;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Noam Y. Tenne
 */
@JobCommand(schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.CURRENT,
        keyAttributes = {Task.REPO_KEY},
        commandsToStop = {
                @StopCommand(command = ImportJob.class, strategy = StopStrategy.IMPOSSIBLE),
                @StopCommand(command = ArtifactCleanupJob.class, strategy = StopStrategy.STOP)
        }
)
public class RemoteReplicationJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(RemoteReplicationJob.class);

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        InternalArtifactoryContext context = InternalContextHelper.get();
        if (!context.isReady()) {
            log.debug("Skipping execution of '{}', sever is not ready yet", RemoteReplicationJob.class.getName());
            return;
        }

        RemoteReplicationDescriptor replication = replicationDescriptorFromJobOrConfig(context, callbackContext);
        if (replication == null) {
            log.warn("Unable to execute replication for repo: cannot find replication descriptor.");
            return;
        }

        RepoPath repoPath = replication.getRepoPath();

        RemoteReplicationSettingsBuilder settingsBuilder = new RemoteReplicationSettingsBuilder(repoPath)
                .deleteExisting(replication.isSyncDeletes())
                .includeProperties(replication.isSyncProperties());

        RemoteRepoDescriptor remoteRepoDescriptor = context.getRepositoryService().remoteRepoDescriptorByKey(
                repoPath.getRepoKey());
        if (remoteRepoDescriptor instanceof HttpRepoDescriptor) {
            HttpRepoDescriptor httpRepoDescriptor = (HttpRepoDescriptor) remoteRepoDescriptor;
            settingsBuilder.url(httpRepoDescriptor.getUrl()).timeout(httpRepoDescriptor.getSocketTimeoutMillis());
        } else {
            log.warn("Unable to execute replication for repo: cannot find remote repo: '{}'.", repoPath.getRepoKey());
            return;
        }

        SecurityService securityService = context.beanForType(SecurityService.class);
        try {
            securityService.authenticateAsSystem();
            AddonsManager addonsManager = context.beanForType(AddonsManager.class);
            ReplicationAddon replicationAddon = addonsManager.addonByType(ReplicationAddon.class);
            replicationAddon.performRemoteReplication(settingsBuilder.build());
        } catch (Exception e) {
            log.error("An error occurred while performing replication for repository '{}': {}",
                    replication.getRepoKey(), e.getMessage());
            log.debug("An error occurred while performing replication for repository '{}'.", e);
        } finally {
            securityService.nullifyContext();
        }
    }

    private RemoteReplicationDescriptor replicationDescriptorFromJobOrConfig(InternalArtifactoryContext context,
            JobExecutionContext callbackContext) {
        JobDetail jobDetail = callbackContext.getJobDetail();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();

        Object manualInvocationDescriptor = jobDataMap.get(ReplicationAddon.TASK_MANUAL_DESCRIPTOR);
        if (manualInvocationDescriptor != null) {
            return ((RemoteReplicationDescriptor) manualInvocationDescriptor);
        } else {
            CentralConfigService centralConfig = context.getCentralConfig();
            CentralConfigDescriptor centralConfigDescriptor = centralConfig.getDescriptor();
            return centralConfigDescriptor.getRemoteReplication(jobDataMap.getString(Task.REPO_KEY));
        }
    }
}
