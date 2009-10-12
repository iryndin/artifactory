package org.artifactory.descriptor.repo;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the RemoteRepoDescriptor class.
 *
 * @author Yossi Shaul
 */
@Test
public class RemoteRepoDescriptorTest {
    public void defaultConstructor() {
        RemoteRepoDescriptor remote = new RemoteRepoDescriptor(){};
        Assert.assertNull(remote.getKey());
        Assert.assertEquals(remote.getIncludesPattern(), "**/*");
        Assert.assertNull(remote.getExcludesPattern());
        Assert.assertNull(remote.getDescription());
        Assert.assertEquals(remote.getFailedRetrievalCachePeriodSecs(), 30);
        Assert.assertEquals(remote.getMaxUniqueSnapshots(), 0);
        Assert.assertEquals(remote.getMissedRetrievalCachePeriodSecs(), 43200);
        Assert.assertEquals(remote.getRetrievalCachePeriodSecs(), 43200);
        Assert.assertEquals(remote.getType(), RemoteRepoType.maven2);
        Assert.assertEquals(remote.getChecksumPolicyType(), ChecksumPolicyType.GEN_IF_ABSENT);
        Assert.assertNull(remote.getUrl());
    }
}
