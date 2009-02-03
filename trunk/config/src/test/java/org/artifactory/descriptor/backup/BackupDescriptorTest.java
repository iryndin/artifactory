package org.artifactory.descriptor.backup;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the BackupDescriptor.
 *
 * @author Yossi Shaul
 */
@Test
public class BackupDescriptorTest {

    public void defaultConstructor() {
        BackupDescriptor backup = new BackupDescriptor();
        Assert.assertNull(backup.getKey(), "Key should be null by default");
        Assert.assertTrue(backup.isEnabled(), "Backup should be enabled by default");
        Assert.assertNull(backup.getCronExp(), "Cron expression should be null by default");
        Assert.assertNull(backup.getDir(), "Dir should be null by default");
        Assert.assertTrue(backup.getExcludedRepositories().isEmpty(),
                "Excluded repositories should be empty by default");
        Assert.assertEquals(backup.getRetentionPeriodHours(),
                BackupDescriptor.DEFAULT_RETENTION_PERIOD_HOURS,
                "Retention period should be {} by default");
    }

}
