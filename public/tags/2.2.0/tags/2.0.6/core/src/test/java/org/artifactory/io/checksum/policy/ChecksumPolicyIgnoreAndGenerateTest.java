package org.artifactory.io.checksum.policy;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the ChecksumPolicyIgnoreAndGenerate.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumPolicyIgnoreAndGenerateTest extends ChecksumPolicyBaseTest {
    private ChecksumPolicyIgnoreAndGenerate policy;

    @BeforeClass
    public void createChecksumPolicy() {
        policy = new ChecksumPolicyIgnoreAndGenerate();
    }

    @Override
    ChecksumPolicy getPolicy() {
        return policy;
    }

    @Override
    public void checksumsMatch() {
        boolean ok = policy.verifyChecksum(matchedChecksums);
        Assert.assertTrue(ok, "Policy should ignore any checksum errors");
    }

    @Override
    public void checksumsDoesNotMatch() {
        boolean ok = policy.verifyChecksum(notMatchedChecksums);
        Assert.assertTrue(ok, "Policy should ignore any checksum errors");
    }

    @Override
    public void noOriginalChecksum() {
        boolean ok = policy.verifyChecksum(noOriginalChecksum);
        Assert.assertTrue(ok, "Policy should ignore any checksum errors");
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
