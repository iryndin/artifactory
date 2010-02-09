package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This checksum policy doesn't allow mismatches between the original and the calculated checksums.
 *
 * @author Yossi Shaul
 */
public class ChecksumPolicyFail extends ChecksumPolicyBase {
    private final static Logger log = LoggerFactory.getLogger(ChecksumPolicyFail.class);

    @Override
    boolean verifyChecksum(ChecksumInfo checksumInfo) {
        String original = checksumInfo.getOriginal();
        if (original == null) {
            log.warn("Rejecting original {} null checksum", checksumInfo.getType());
            return false;
        }
        if (!checksumInfo.checksumsMatch()) {
            log.warn("Checksum mismatch: {}", checksumInfo);
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
        return ChecksumPolicyType.FAIL;
    }
}
