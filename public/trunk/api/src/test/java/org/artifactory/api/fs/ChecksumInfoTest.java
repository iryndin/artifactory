/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

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
        Assert.assertTrue(info.checksumsMatch(), "Checksums should match");
    }

    public void matchDifferentOriginalAndActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, "123", "321");
        Assert.assertFalse(info.checksumsMatch(), "Checksums shouldn't match");
    }

    public void matchNullOriginal() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, null, "321");
        Assert.assertFalse(info.checksumsMatch(), "Checksums shouldn't if one is null");
    }

    public void matchNullActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, "123", null);
        Assert.assertFalse(info.checksumsMatch(), "Checksums shouldn't if one is null");
    }

    public void matchNullOriginalAndActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, null, null);
        Assert.assertFalse(info.checksumsMatch(), "Checksums shouldn't if one is null");
    }

    public void trustedOriginalShouldReturnActual() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, ChecksumInfo.TRUSTED_FILE_MARKER, "123");
        Assert.assertTrue(info.isMarkedAsTrusted(), "Shouls have been marked as trusted");
        Assert.assertEquals(info.getOriginal(), info.getActual(), "Original should return actual if marked " +
                "as trusted");
    }

    public void matchIfOriginalIsTruetedAndActualIsSet() {
        ChecksumInfo info = new ChecksumInfo(ChecksumType.sha1, ChecksumInfo.TRUSTED_FILE_MARKER, "123");
        Assert.assertTrue(info.checksumsMatch(), "Checksums should match if " +
                "marked as trusted and actual not null");
    }

}
