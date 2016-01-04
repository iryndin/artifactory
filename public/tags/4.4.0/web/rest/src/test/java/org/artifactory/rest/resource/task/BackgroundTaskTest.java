package org.artifactory.rest.resource.task;

import org.artifactory.rest.common.util.RestUtils;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for {@link BackgroundTask}.
 *
 * @author Yossi Shaul
 */
@Test
public class BackgroundTaskTest {

    public void backgroundTaskWithNoStartedDate() {
        BackgroundTask task = new BackgroundTask("1", "type", "running", "test", 0);
        assertEquals(task.getStartedMillis(), 0);
    }

    public void backgroundTaskWithStartedDate() {
        long started = System.currentTimeMillis() - 1000;
        BackgroundTask task = new BackgroundTask("1", "type", "running", "test", started);
        assertEquals(task.getStartedMillis(), started);
        assertEquals(RestUtils.fromIsoDateString(task.getStarted()), started);
    }
}