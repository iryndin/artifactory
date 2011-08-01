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

package org.artifactory.repo.replication;

import org.artifactory.addon.AddonsManager;
import org.artifactory.addon.ReplicationAddon;
import org.artifactory.addon.replication.RemoteReplicationSettings;
import org.artifactory.addon.replication.RemoteReplicationSettingsBuilder;
import org.artifactory.descriptor.replication.RemoteReplicationDescriptor;
import org.artifactory.repo.cleanup.ArtifactCleanupJob;
import org.artifactory.repo.service.ImportJob;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.security.SystemAuthenticationToken;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Noam Y. Tenne
 */
@JobCommand(schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.CURRENT,
        commandsToStop = {ImportJob.class, ArtifactCleanupJob.class})
public class RemoteReplicationJob extends QuartzCommand {

    private static final Logger log = LoggerFactory.getLogger(RemoteReplicationJob.class);

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        InternalArtifactoryContext context = InternalContextHelper.get();
        if (!context.isReady()) {
            log.debug("Skipping execution of '{}', sever is not ready yet", RemoteReplicationJob.class.getName());
            return;
        }

        String repoKey = callbackContext.getJobDetail().getJobDataMap().getString(ReplicationAddon.REPO_KEY);
        RemoteReplicationDescriptor replication = context.getCentralConfig().getDescriptor().getRemoteReplication(
                repoKey);
        if (replication == null) {
            log.debug("Skipping execution of '{}', unable to find target repository '{}'.",
                    RemoteReplicationJob.class.getName(), repoKey);
            return;
        }

        AddonsManager addonsManager = context.beanForType(AddonsManager.class);
        ReplicationAddon replicationAddon = addonsManager.addonByType(ReplicationAddon.class);

        RemoteReplicationSettings settings = new RemoteReplicationSettingsBuilder(replication.getRepoPath(),
                new LoggingWriter()).deleteExisting(replication.isSyncDeletes()).
                includeProperties(replication.isSyncProperties()).timeout(replication.getSocketTimeoutMillis())
                .build();
        try {
            SecurityContextHolder.getContext().setAuthentication(new SystemAuthenticationToken());
            replicationAddon.performRemoteReplication(settings);
        } catch (Exception e) {
            log.error("An error occurred while performing replication for repository '{}': {}", repoKey,
                    e.getMessage());
            log.debug("An error occurred while performing replication for repository '{}'.", e);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    /**
     * Appends the received text to the logger
     */
    private static class LoggingWriter extends Writer {

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            log.info(new String(cbuf, off, len));
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }
}
