/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2013 JFrog Ltd.
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

package org.artifactory.security.jobs;

import org.artifactory.api.config.CentralConfigService;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.SecurityService;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A job that mark users as user.credentialsExpired=True if password has expired
 *
 * @author Michael Pasternak
 */
@JobCommand(description = "Credentials watch job",
        singleton = true, runOnlyOnPrimary = true, schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.SYSTEM)
public class CredentialsWatchJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(CredentialsWatchJob.class);
    private final ArtifactoryContext artifactoryContext = ContextHelper.get();

    @Override
    public void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        log.debug("Credentials watch is started");

        CentralConfigService configService = artifactoryContext.beanForType(CentralConfigService.class);
        if(!configService.getMutableDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy().isEnabled()) {
            log.debug("ExpirationPolicy is disabled");
            return;
        }

        SecurityService securityService = artifactoryContext.beanForType(SecurityService.class);

        securityService.markUsersCredentialsExpired(
                configService.getMutableDescriptor().getSecurity().getPasswordSettings().getExpirationPolicy().getPasswordMaxAge()
        );

        log.debug("Credentials watch is finished");
    }
}
