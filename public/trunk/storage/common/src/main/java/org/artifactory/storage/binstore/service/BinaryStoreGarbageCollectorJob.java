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

package org.artifactory.storage.binstore.service;

import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.storage.spring.StorageContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Date: 11/26/12
 * Time: 9:53 PM
 *
 * @author freds
 */
@JobCommand(singleton = true,
        schedulerUser = TaskUser.SYSTEM,
        manualUser = TaskUser.SYSTEM
        //, commandsToStop = {@StopCommand(command = ImportJob.class, strategy = StopStrategy.IMPOSSIBLE)}
)
public class BinaryStoreGarbageCollectorJob extends QuartzCommand {

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        StorageContextHelper.get().beanForType(InternalBinaryStore.class).garbageCollect();
    }
}
