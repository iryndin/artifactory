package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.descriptor.repo.ChecksumPolicyType;

/**
 * This checksum policy ignores checksums errors.
 *
 * @author Yossi Shaul
 */
public class ChecksumPolicyIgnoreAndGenerate extends ChecksumPolicyBase {

    @Override
    boolean verifyChecksum(ChecksumInfo checksumInfo) {
        // we don't care if it passes or not
        return true;
    }

    @Override
    String getChecksum(ChecksumInfo checksumInfo) {
        return checksumInfo.getActual();
    }

    @Override
    ChecksumPolicyType getChecksumPolicyType() {
        return ChecksumPolicyType.IGNORE_AND_GEN;
    }

}