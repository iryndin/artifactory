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

package org.artifactory.jcr.trash;

import org.artifactory.jcr.JcrPath;
import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.artifactory.spring.InternalArtifactoryContext;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.util.LoggingUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * Low-level jcr trash disposer
 *
 * @author yoavl
 */
//TODO: [by yl] Turn into a simple async call
public class EmptyTrashJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(EmptyTrashJob.class);

    public static final String SESSION_FOLDER = "sessionFolder";

    @Override
    protected void onExecute(JobExecutionContext jobContext) throws JobExecutionException {
        JobDataMap jobMap = jobContext.getJobDetail().getJobDataMap();
        String sessionFolderName = jobMap.getString(SESSION_FOLDER);
        if (sessionFolderName == null) {
            log.debug("Cannot empty the trash if no trash folder provided!");
            return;
        }

        String sessionFolderPath = JcrPath.get().getTrashJcrRootPath() + "/" + sessionFolderName;
        InternalArtifactoryContext context = InternalContextHelper.get();

        try {
            int deletedItems = context.getJcrService().delete(sessionFolderPath);
            if (deletedItems > 0) {
                log.debug("Emptied " + deletedItems + " nodes from trash folder " + sessionFolderName + ".");
            }
        } catch (Exception e) {
            //Fail gracefully
            LoggingUtils.warnOrDebug(log, "Could not empty trash folder " + sessionFolderName + ".", e);
        }
    }
}