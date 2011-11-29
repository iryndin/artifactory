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

package org.artifactory.jcr.schedule;

import org.artifactory.common.ConstantValues;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * @author Yoav Landman
 */
@JobCommand(singleton = true,
        schedulerUser = TaskUser.SYSTEM,
        manualUser = TaskUser.SYSTEM
        //, commandsToStop = {@StopCommand(command = ImportJob.class, strategy = StopStrategy.IMPOSSIBLE)}
)
public class JcrGarbageCollectorJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(JcrGarbageCollectorJob.class);
    public static final String FIX_CONSISTENCY = "FIX_CONSISTENCY";

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Garbage collection starting...");
            String strFixConsistency = (String) context.getMergedJobDataMap().get(FIX_CONSISTENCY);
            boolean fixConsistency = ConstantValues.jcrFixConsistency.getBoolean();
            if (!fixConsistency && strFixConsistency != null) {
                fixConsistency = Boolean.parseBoolean(strFixConsistency);
            }
            StorageContextHelper.get().getJcrService().garbageCollect(fixConsistency);
        } finally {
            log.info("Garbage collection ended.");
        }
    }
}