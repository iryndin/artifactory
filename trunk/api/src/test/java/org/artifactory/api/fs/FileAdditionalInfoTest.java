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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.artifactory.api.mime.ChecksumType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Tests FileAdditionalInfo class.
 *
 * @author Yossi Shaul
 */
@Test
public class FileAdditionalInfoTest {
    private FileAdditionalInfo info;
    private ChecksumInfo sha1;
    private ChecksumInfo md5;

    @BeforeMethod
    public void setup() {
        info = new FileAdditionalInfo();
        sha1 = new ChecksumInfo(ChecksumType.sha1);
        sha1.setOriginal("121232434534");
        sha1.setActual("34387534754");
        md5 = new ChecksumInfo(ChecksumType.md5);
        this.md5.setOriginal("efhiehfeih");
        md5.setActual("efhiehfeih");
        HashSet<ChecksumInfo> checksums = new HashSet<ChecksumInfo>(Arrays.asList(sha1, md5));
        info.setChecksums(checksums);
    }

    public void defaultConstructor() {
        FileAdditionalInfo info = new FileAdditionalInfo();
        Assert.assertNotNull(info.getChecksums(), "Checksums should not be null by default");
        Assert.assertNull(info.getSha1(), "Sha1 should be null by default");
        Assert.assertNull(info.getMd5(), "md5 should be null by default");
    }

    public void settingChecksums() {
        //Assert.assertEquals(info.getChecksums(), new HashSet<ChecksumInfo>(Arrays.asList(md5, sha1)));
        Assert.assertEquals(info.getSha1(), sha1.getActual());
        Assert.assertEquals(info.getMd5(), md5.getActual());

    }

    public void testIsIdentical() {
        FileAdditionalInfo copy = new FileAdditionalInfo(info);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(info, copy), "Orig and copy differ");
        Assert.assertTrue(info.isIdentical(copy), "Orig and copy differ");
    }

    public void copyConstructor() {
        FileAdditionalInfo orig = new FileAdditionalInfo();
        FileAdditionalInfo copy = new FileAdditionalInfo(orig);
        Assert.assertTrue(EqualsBuilder.reflectionEquals(orig, copy), "Orig and copy differ");
        Assert.assertTrue(orig.isIdentical(copy), "Orig and copy differ");
    }
}
