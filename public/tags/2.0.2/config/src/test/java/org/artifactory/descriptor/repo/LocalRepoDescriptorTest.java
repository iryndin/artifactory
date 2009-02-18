package org.artifactory.descriptor.repo;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the LocalRepoDescriptor.
 *
 * @author Yossi Shaul
 */
@Test
public class LocalRepoDescriptorTest {

    public void defaultConstructor() {
        LocalRepoDescriptor localRepo = new LocalRepoDescriptor();

        Assert.assertNull(localRepo.getKey(), "Key should be null");
        Assert.assertNull(localRepo.getDescription(), "Description should be null");
        Assert.assertEquals(localRepo.getIncludesPattern(), "**/*",
                "Includes pattern should be **/*");
        Assert.assertNull(localRepo.getExcludesPattern(), "Excludes pattern should be null");
        Assert.assertEquals(localRepo.getMaxUniqueSnapshots(), 0,
                "Max unique snapshot should be 0 by default");
        Assert.assertEquals(localRepo.getSnapshotVersionBehavior(),
                SnapshotVersionBehavior.NONUNIQUE,
                "SnapshotVersionBehavior should be non-unique by default");
    }
}
