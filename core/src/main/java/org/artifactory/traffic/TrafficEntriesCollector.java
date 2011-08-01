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

package org.artifactory.traffic;

import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.LoggingUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * @author yoavl
 */
@JobCommand(singleton = true, schedulerUser = TaskUser.SYSTEM)
public class TrafficEntriesCollector extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(TrafficEntriesCollector.class);

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        InternalArtifactoryContext artifactoryContext = InternalContextHelper.get();
        if (!artifactoryContext.isReady()) {
            log.debug("Skipping execution of '{}', sever is not ready yet", TrafficEntriesCollector.class.getName());
            return;
        }
        InternalTrafficService trafficService = artifactoryContext.beanForType(InternalTrafficService.class);
        //Query the entries from the last time collected
        try {
            trafficService.collect();
        } catch (Exception e) {
            LoggingUtils.warnOrDebug(log, "Unexpected error while collecting data.", e);
        }

        //Remove old collected entries
        try {
            trafficService.cleanup();
        } catch (Exception e) {
            LoggingUtils.warnOrDebug(log, "Unexpected error while removing old traffic entries.", e);
        }
    }
}