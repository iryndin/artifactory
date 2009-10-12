package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.mime.ChecksumType;

import java.util.Set;

/**
 * A checksum policy is responsible to handle any problem related to mismatches between
 * the original checksum and the one calculated by Artifactory.
 *
 * @author Yossi Shaul
 */
public interface ChecksumPolicy {
    /**
     * Processes the checksums info and possibly changes the checksum info.
     * @param checksumInfos The checksums to process and update.
     * @return True if the checksums are valid according to this policy.
     */
    boolean verify(Set<ChecksumInfo> checksumInfos);

    /**
     * Returns the checksum value by type. Actual implementation will decide if to return the original, calculated
     * or something else.
     * @param checksumType The checksum type
     * @return  Checksum value for the checksum type
     */
    String getChecksum(ChecksumType checksumType, Set<ChecksumInfo> checksumInfos);
}
