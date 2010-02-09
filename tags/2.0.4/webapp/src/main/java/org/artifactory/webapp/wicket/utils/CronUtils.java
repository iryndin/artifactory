package org.artifactory.webapp.wicket.utils;

import org.quartz.CronExpression;

import java.text.ParseException;
import java.util.Date;

/**
 * A utility class for Cron Expressions.
 *
 * @author Noam Tenne
 */
public class CronUtils {

    /**
     * Returns a boolean value representing the validity of a given Cron Expression
     *
     * @param cronExpression A Cron Expression
     * @return boolean - Is given expression valid
     */
    public static boolean isValid(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }

    /**
     * Returns the next execution time based on the given Cron Expression
     *
     * @param cronExpression A Cron Expression
     * @return Date - The next time the given Cron Expression should fire
     */
    public static Date getNextExecution(String cronExpression) {
        try {
            CronExpression cron = new CronExpression(cronExpression);
            return cron.getNextValidTimeAfter(new Date(System.currentTimeMillis()));
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
