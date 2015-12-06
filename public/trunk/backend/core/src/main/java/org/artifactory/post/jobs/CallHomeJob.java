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

package org.artifactory.post.jobs;

import com.google.common.base.Strings;
import org.artifactory.api.context.ArtifactoryContext;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.common.ConstantValues;
import org.artifactory.cron.CronUtils;
import org.artifactory.post.TimeRandomizer;
import org.artifactory.post.services.CallHomeService;
import org.artifactory.schedule.JobCommand;
import org.artifactory.schedule.TaskUser;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A job to trigger CallHome request.
 *
 * @author Michael Pasternak
 */
@JobCommand(singleton = true, runOnlyOnPrimary = false, schedulerUser = TaskUser.SYSTEM, manualUser = TaskUser.SYSTEM)
public class CallHomeJob extends QuartzCommand {
    private static final Logger log = LoggerFactory.getLogger(CallHomeJob.class);

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        log.debug("CallHome job started");
        ArtifactoryContext artifactoryContext = ContextHelper.get();
        CallHomeService callHomeService = artifactoryContext.beanForType(CallHomeService.class);
        callHomeService.callHome();
        log.debug("CallHome job finished");
    }

    /**
     * To lower load on bintray we produce random execution time
     * (every Sunday at random time)
     *
     * @return Quartz scheduling expression
     */
    public static String buildRandomQuartzExp() {
        String predefinedExp = ConstantValues.callHomeCron.getString();
        if(!Strings.isNullOrEmpty(predefinedExp)) {
            if(CronUtils.isValid(predefinedExp))
                return predefinedExp;
            log.warn(
                    "Specified CallHomeJob quartz expression '" + predefinedExp +
                        "' is not valid, using default ..."
            );
        }
        return String.format(
                "0 %d %d ? * SUN",
                TimeRandomizer.randomMinute(),
                TimeRandomizer.randomHour()
        );
    }
}

