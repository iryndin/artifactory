package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.descriptor.repo.ChecksumPolicyType;

/**
 * This checksum policy ignores missing original checksums, but fails if original exist but
 * not equals to the actual.
 *
 * @author Yossi Shaul
 */
public class ChecksumPolicyGenerateIfAbsent extends ChecksumPolicyBase {

    @Override
    boolean verifyChecksum(ChecksumInfo checksumInfo) {
        if (checksumInfo.getOriginal() == null) {
            return true;
        } else if (!checksumInfo.checksumsMatches()) {
            return false;
        }
        return true;
    }

    @Override
    String getChecksum(ChecksumInfo checksumInfo) {
        return checksumInfo.getActual();
    }

    @Override
    ChecksumPolicyType getChecksumPolicyType() {
        return ChecksumPolicyType.GEN_IF_ABSENT;
    }
}