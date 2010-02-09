package org.artifactory.io.checksum.policy;

import org.artifactory.api.fs.ChecksumInfo;

import java.util.Set;

/**
 * Thrown when checksum policy doesn't allow saving a certain file.
 *
 * @author Yossi Shaul
 */
public class ChecksumPolicyException extends RuntimeException {
    public ChecksumPolicyException(ChecksumPolicy policy, Set<ChecksumInfo> checksums, String fileName) {
        super("Checksum policy '" + policy + "' rejected the artifact '" + fileName + "'. " +
                "Checksums info: " + checksums);
    }
}
