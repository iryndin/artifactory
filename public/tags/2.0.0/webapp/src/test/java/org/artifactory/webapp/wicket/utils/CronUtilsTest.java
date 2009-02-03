package org.artifactory.webapp.wicket.utils;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Tests the different methods in the CronUtils class
 *
 * @author Noam Tenne
 */

public class CronUtilsTest {
    /**
     * An example of a legal Cron Expression
     */
    private static final String LEGAL_EXPRESSION = "0 0 /1 * * ?";

    /**
     * Tests the validity of a legal cron expression
     */
    @Test
    public void validExpression() {
        Assert.assertTrue(CronUtils.isValid(LEGAL_EXPRESSION));
    }

    /**
     * Tests the validity of a null cron expression
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void nullExpression() {
        Assert.assertFalse(CronUtils.isValid(null));
    }

    /**
     * Tests the validity of an empty cron expression
     */
    @Test
    public void emptyExpression() {
        Assert.assertFalse(CronUtils.isValid(""));
    }

    /**
     * Tests the validity of an illegal cron expression
     */
    @Test
    public void illegalExpression() {
        Assert.assertFalse(CronUtils.isValid("* /* 8"));
    }

    /**
     * Tests the validity of the next run
     */
    @Test
    public void nextRun() {
        Date date = CronUtils.getNextExecution(LEGAL_EXPRESSION);
        Assert.assertFalse(date == null);
        if (date != null) {
            Assert.assertTrue(date.after(new Date(System.currentTimeMillis())));
        }
    }
}
