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

import org.artifactory.jcr.JcrService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.index.IndexerJob;
import org.artifactory.schedule.TaskService;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * @author Yoav Landman
 */
public class JcrGarbageCollectorJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(JcrGarbageCollectorJob.class);

    @Override
    protected void onExecute(JobExecutionContext context) throws JobExecutionException {
        InternalArtifactoryContext contextHelper = InternalContextHelper.get();
        JcrService jcr = contextHelper.getJcrService();
        TaskService taskService = contextHelper.beanForType(TaskService.class);

        taskService.stopTasks(IndexerJob.class, true);
        try {
            log.info("Garbage collection starting...");
            jcr.garbageCollect();
        } finally {
            log.info("Garbage collection ended.");
            taskService.resumeTasks(IndexerJob.class);
        }
    }
}