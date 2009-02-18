package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.descriptor.repo.ChecksumPolicyType;

/**
 * This checksum policy always passes verification and return the original value when asked.
 *
 * @author Yossi Shaul
 */
public class ChecksumPolicyPassThru extends ChecksumPolicyBase {

    @Override
    boolean verifyChecksum(ChecksumInfo checksumInfo) {
        return true;
    }

    @Override
    String getChecksum(ChecksumInfo checksumInfo) {
        return checksumInfo.getOriginal();
    }

    @Override
    ChecksumPolicyType getChecksumPolicyType() {
        return ChecksumPolicyType.PASS_THRU;
    }
}