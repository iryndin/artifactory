package org.artifactory.api.fs;

import org.artifactory.api.mime.ChecksumType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the ChecksumInfo class.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumInfoTest {

    public void matchSameOriginalAndActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, "123", "123");
        Assert.assertTrue(info.checksumsMatches(), "Checksums should match");
    }

    public void matchDifferentOriginalAndActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, "123", "321");
        Assert.assertFalse(info.checksumsMatches(), "Checksums shouldn't match");
    }

    public void matchNullOriginal() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, null, "321");
        Assert.assertFalse(info.checksumsMatches(), "Checksums shouldn't if one is null");
    }

    public void matchNullActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, "123", null);
        Assert.assertFalse(info.checksumsMatches(), "Checksums shouldn't if one is null");
    }

    public void matchNullOriginalAndActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, null, null);
        Assert.assertFalse(info.checksumsMatches(), "Checksums shouldn't if one is null");
    }

    public void trustedOriginalShouldReturnActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, ChecksumInfo.TRUSTED_FILE_MARKER, "123");
        Assert.assertTrue(info.isMarkedAsTrusted(), "Shouls have been marked as trusted");
        Assert.assertEquals(info.getOriginal(), info.getActual(), "Original should return actual if marked " +
                "as trusted");
    }

    public void matchIfOriginalIsTruetedAndActualIsSet() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, ChecksumInfo.TRUSTED_FILE_MARKER, "123");
        Assert.assertTrue(info.checksumsMatches(), "Checksums should match if " +
                "marked as trusted and actual not null");
    }

}
