package org.artifactory.io.checksum.policy;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the ChecksumPolicyPassThru class.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumPolicyPassThruTest extends ChecksumPolicyBaseTest {
    private ChecksumPolicyPassThru policy;

    @BeforeClass
    public void createChecksumPolicy() {
        policy = new ChecksumPolicyPassThru();
    }

    @Override
    ChecksumPolicy getPolicy() {
        return policy;
    }

    @Override
    public void checksumsMatches() {
        boolean ok = policy.verifyChecksum(matchedChecksums);
        Assert.assertTrue(ok, "Policy should pass if checksums are same");
    }

    @Override
    public void checksumsDoesNotMatch() {
        boolean ok = policy.verifyChecksum(notMatchedChecksums);
        Assert.assertTrue(ok, "Policy should not fail even if checksums don't match");
    }

    @Override
    public void noOriginalChecksum() {
        boolean ok = policy.verifyChecksum(noOriginalChecksum);
        Assert.assertTrue(ok, "Policy should not fail if original checksum is missing");
    }

    @Override
    public void returnedChecksum() {
        String checksum = policy.getChecksum(matchedChecksums);
        Assert.assertEquals(checksum, matchedChecksums.getOriginal(), "Should always return the original value");
        checksum = policy.getChecksum(notMatchedChecksums);
        Assert.assertEquals(checksum, notMatchedChecksums.getOriginal(), "Should always return the original value");
        checksum = policy.getChecksum(noOriginalChecksum);
        Assert.assertNull(checksum, "Should always return the original value");
    }
}
