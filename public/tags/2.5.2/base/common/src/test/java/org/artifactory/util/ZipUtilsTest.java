/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.util;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Tests the {@link ZipUtils} class.
 *
 * @author Yossi Shaul
 */
@Test
public class ZipUtilsTest {
    private File zipFile;
    private ZipInputStream zis;

    // the zip test contains: file.txt, folder/another.txt

    @BeforeClass
    public void setup() {
        zipFile = ResourceUtils.getResourceAsFile("/ziptest.zip");
    }

    @BeforeMethod
    public void openStream() throws FileNotFoundException {
        zis = new ZipInputStream(new FileInputStream(zipFile));
    }

    @AfterMethod
    public void closeStream() throws FileNotFoundException {
        IOUtils.closeQuietly(zis);
    }

    public void locateExistingEntry() throws Exception {
        ZipEntry zipEntry = ZipUtils.locateEntry(zis, "file.txt", null);
        assertNotNull(zipEntry, "Couldn't find zip entry");
    }

    public void locateMissingEntry() throws Exception {
        ZipEntry zipEntry = ZipUtils.locateEntry(zis, "nosuchfile.txt", null);
        assertNull(zipEntry, "Shouldn't have found zip entry");
    }
}
