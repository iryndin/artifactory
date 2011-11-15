/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.checksum;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Yossi Shaul
 */
@Test
public class ChecksumsInfoTest {

    public void copyConstructor() {
        ChecksumsInfo orig = new ChecksumsInfo();
        ChecksumInfo checksum = new ChecksumInfo(ChecksumType.sha1, "1", "2");
        orig.addChecksumInfo(checksum);

        ChecksumsInfo copy = new org.artifactory.checksum.ChecksumsInfo(orig);

        assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Orig and copy differ");
        assertTrue(orig.isIdentical(copy), "Orig and copy differ");
        assertSame(checksum, copy.getChecksumInfo(ChecksumType.sha1),
                "Should have made a copy, not use the same object");
    }

    public void addChecksum() {
        ChecksumsInfo checksumsInfo = new ChecksumsInfo();
        ChecksumInfo checksum = new ChecksumInfo(ChecksumType.sha1, "9", "5");
        ChecksumInfo checksum2 = new ChecksumInfo(ChecksumType.md5, "f", "e");
        ChecksumInfo checksum3 = new ChecksumInfo(ChecksumType.sha1, "1", "2");
        checksumsInfo.addChecksumInfo(checksum);
        checksumsInfo.addChecksumInfo(checksum2);
        checksumsInfo.addChecksumInfo(checksum3);
        assertEquals(checksumsInfo.getChecksums().size(), 2, "Size should be two");
        assertSame(checksumsInfo.getChecksumInfo(ChecksumType.md5), checksum2, "Should be the same");
        assertSame(checksumsInfo.getChecksumInfo(ChecksumType.sha1), checksum3, "Should be the same");
        assertEquals(checksumsInfo.getSha1(), "2", "Should be two");
        checksumsInfo.addChecksumInfo(new ChecksumInfo(ChecksumType.md5, "5", "3"));
        assertEquals(checksumsInfo.getChecksums().size(), 2, "Size should be two");
        assertEquals(checksumsInfo.getMd5(), "3", "Should be three");
    }

}
