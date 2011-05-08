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

package org.artifactory.schedule;

import org.artifactory.log.LoggerFactory;
import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

/**
 * @author yoavl
 */
public class DummyQuartzCommand extends QuartzCommand {

    public static final String FAIL = "FAIL";
    public static final String MSECS_TO_RUN = "MSECS_TO_RUN";

    private static final Logger log = LoggerFactory.getLogger(DummyQuartzCommand.class);

    private static long SLEEP = 50;

    public DummyQuartzCommand() {
    }

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        log.debug("Command for task " + currentTaskToken() + " is executing.");
        if (callbackContext.getMergedJobDataMap().get(FAIL) != null &&
                callbackContext.getMergedJobDataMap().getBoolean(FAIL)) {
            log.info("Failing with exception!");
            throw new RuntimeException("Failed execution.");
        }
        long msecsToRun = 500;
        if (callbackContext.getMergedJobDataMap().get(MSECS_TO_RUN) != null) {
            msecsToRun = callbackContext.getMergedJobDataMap().getLongValue(MSECS_TO_RUN);
        }
        long count = (long) (msecsToRun / (float) SLEEP);
        for (int i = 0; i < count; i++) {
            boolean shouldBreak = getTaskService().pauseOrBreak();
            if (shouldBreak) {
                log.debug("Command for task " + currentTaskToken() + " is breaking.");
                break;
            } else {
                try {
                    Thread.sleep(SLEEP);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        log.debug("Command for task " + currentTaskToken() + " has ended.");
    }
}
