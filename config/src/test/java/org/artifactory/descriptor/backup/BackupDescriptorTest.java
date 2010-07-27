/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
        Assert.assertFalse(backup.isCreateArchive(), "Is create archive should be false by default");
        //Assert.assertFalse(backup.isIncrementalBackup(), "Is incremental backup should be false by default");
    }
}
