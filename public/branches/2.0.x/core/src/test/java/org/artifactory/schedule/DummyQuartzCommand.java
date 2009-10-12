package org.artifactory.schedule;

import org.artifactory.schedule.quartz.QuartzCommand;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yoavl
 */
public class DummyQuartzCommand extends QuartzCommand {

    public static final String FAIL = "FAIL";

    private static final Logger log = LoggerFactory.getLogger(DummyQuartzCommand.class);

    @Override
    protected void onExecute(JobExecutionContext callbackContext) throws JobExecutionException {
        log.debug("Command for task " + currentTaskToken() + " is executing.");
        if (callbackContext.getMergedJobDataMap().get(FAIL) != null &&
                callbackContext.getMergedJobDataMap().getBoolean(FAIL)) {
            log.info("Failing with exception!");
            throw new RuntimeException("Failed execution.");
        }
        for (int i = 0; i < 10; i++) {
            boolean shouldBreak = getTaskService().blockIfPausedAndShouldBreak();
            if (shouldBreak) {
                log.debug("Command for task " + currentTaskToken() + " is breaking.");
                break;
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        log.debug("Command for task " + currentTaskToken() + " has ended.");
    }
}
