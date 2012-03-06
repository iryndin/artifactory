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
import org.artifactory.addon.ReplicationAddon;
import org.artifactory.addon.replication.LocalReplicationSettings;
import org.artifactory.addon.replication.LocalReplicationSettingsBuilder;
import org.artifactory.api.security.SecurityService;
import org.artifactory.descriptor.replication.LocalReplicationDescriptor;
import org.artifactory.log.LoggerFactory;
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
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * @author Noam Y. Tenne
 */
@JobCommand(schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.CURRENT,
        keyAttributes = {Task.REPO_KEY},
        commandsToStop = {@StopCommand(command = ImportJob.class, strategy = StopStrategy.IMPOSSIBLE)}
)
public class LocalReplicationJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(LocalReplicationJob.class);

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        InternalArtifactoryContext context = InternalContextHelper.get();
        if (!context.isReady()) {
            log.debug("Skipping execution of '{}', sever is not ready yet", LocalReplicationJob.class.getName());
            return;
        }

        JobDataMap jobDataMap = callbackContext.getJobDetail().getJobDataMap();
        LocalReplicationDescriptor replication = ((LocalReplicationDescriptor) jobDataMap.get(
                ReplicationAddon.DESCRIPTOR));

        LocalReplicationSettings settings = new LocalReplicationSettingsBuilder(replication.getRepoPath(),
                replication.getUrl()).proxyDescriptor(replication.getProxy()).
                socketTimeoutMillis(replication.getSocketTimeoutMillis()).username(replication.getUsername()).
                password(replication.getPassword()).deleteExisting(replication.isSyncDeletes()).
                includeProperties(replication.isSyncProperties()).build();

        SecurityService securityService = context.beanForType(SecurityService.class);
        try {
            securityService.authenticateAsSystem();
            AddonsManager addonsManager = context.beanForType(AddonsManager.class);
            ReplicationAddon replicationAddon = addonsManager.addonByType(ReplicationAddon.class);
            replicationAddon.performLocalReplication(settings);
        } catch (Exception e) {
            log.error("An error occurred while performing replication for repository '{}': {}",
                    replication.getRepoKey(), e.getMessage());
            log.debug("An error occurred while performing replication for repository '{}'.", e);
        } finally {
            securityService.nullifyContext();
        }
    }
}
