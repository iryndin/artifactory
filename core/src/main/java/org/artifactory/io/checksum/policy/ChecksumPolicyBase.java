package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.descriptor.repo.ChecksumPolicyType;

import java.util.Set;

/**
 * Base abstract implementation of the ChecksumPolicy.
 *
 * @author Yossi Shaul
 */
public abstract class ChecksumPolicyBase implements ChecksumPolicy {
    abstract boolean verifyChecksum(ChecksumInfo checksumInfo);

    abstract String getChecksum(ChecksumInfo checksumInfo);

    public boolean verify(Set<ChecksumInfo> checksumInfos) {
        for (ChecksumInfo checksumInfo : checksumInfos) {
            // call the actual implementation to do its job
            if (verifyChecksum(checksumInfo)) {
                // by default if one checksum passes the file is ok
                return true;
            }
        }
        return false;
    }

    public String getChecksum(ChecksumType checksumType, Set<ChecksumInfo> checksumInfos) {
        ChecksumInfo info = getChecksumInfo(checksumType, checksumInfos);
        return getChecksum(info);
    }

    private ChecksumInfo getChecksumInfo(ChecksumType type, Set<ChecksumInfo> infos) {
        for (ChecksumInfo info : infos) {
            if (type.equals(info.getType())) {
                return info;
            }
        }
        throw new IllegalArgumentException("Checksum not found for type " + type);
    }

    public static ChecksumPolicy getByType(ChecksumPolicyType type) {
        switch (type) {
            case GEN_IF_ABSENT:
                return new ChecksumPolicyGenerateIfAbsent();
            case FAIL:
                return new ChecksumPolicyFail();
            case IGNORE_AND_GEN:
                return new ChecksumPolicyIgnoreAndGenerate();
            case PASS_THRU:
                return new ChecksumPolicyPassThru();
            default:
                throw new IllegalArgumentException("No checksum policy found for type " + type);
        }
    }
}
