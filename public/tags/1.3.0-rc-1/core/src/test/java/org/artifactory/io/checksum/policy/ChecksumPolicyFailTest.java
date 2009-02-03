package org.artifactory.io.checksum.policy;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the ChecksumPolicyFail.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumPolicyFailTest extends ChecksumPolicyBaseTest {
    private ChecksumPolicyFail policy;

    @BeforeClass
    public void createChecksumPolicy() {
        policy = new ChecksumPolicyFail();
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
        Assert.assertFalse(ok, "Policy should ignore any checksum errors");
    }

    @Override
    public void noOriginalChecksum() {
        boolean ok = policy.verifyChecksum(noOriginalChecksum);
        Assert.assertFalse(ok, "Policy should ignore any checksum errors");
    }

    @Override
    public void returnedChecksum() {
        String checksum = policy.getChecksum(matchedChecksums);
        Assert.assertEquals(checksum, matchedChecksums.getActual(), "Should always return the actual value");
        checksum = policy.getChecksum(notMatchedChecksums);
        Assert.assertEquals(checksum, notMatchedChecksums.getActual(), "Should always return the actual value");
        checksum = policy.getChecksum(noOriginalChecksum);
        Assert.assertEquals(checksum, noOriginalChecksum.getActual(), "Should always return the actual value");
    }

}