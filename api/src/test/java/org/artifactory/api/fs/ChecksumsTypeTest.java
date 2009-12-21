package org.artifactory.api.fs;

import org.artifactory.api.mime.ChecksumType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests the {@link ChecksumType} enum.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumsTypeTest {

    public void nullAndEmptyAreNotValidChecksums() {
        for (ChecksumType checksumType : ChecksumType.values()) {
            assertFalse(checksumType.isValid(null));
            assertFalse(checksumType.isValid(""));
        }
    }

    public void wrongLengthChecksums() {
        for (ChecksumType checksumType : ChecksumType.values()) {
            assertFalse(checksumType.isValid("aaa"));
        }
    }

    public void invalidMD5Checksum() {
        // good length but not hexadecimal
        assertFalse(ChecksumType.md5.isValid("xf222ca7499ed5bc49fe25a1182c59f7"));
    }

    public void invalidSha1Checksum() {
        // good length but not hexadecimal
        assertFalse(ChecksumType.sha1.isValid("96bcc93bec1f99e45b6c1bdfcef73948b8fa122g"));
    }

    public void validMD5Checksum() {
        assertTrue(ChecksumType.md5.isValid("2f222ca7499ed5bc49fe25a1182c59f7"));
        assertTrue(ChecksumType.md5.isValid("d06a3ab307d28384a235d0ab6b70d3ae"));
    }

    public void validSha1Checksum() {
        assertTrue(ChecksumType.sha1.isValid("911ca40cdb527969ee47dc6f782425d94a36b510"));
        assertTrue(ChecksumType.sha1.isValid("96bcc93bec1f99e45b6c1bdfcef73948b8fa122c"));
    }

}
