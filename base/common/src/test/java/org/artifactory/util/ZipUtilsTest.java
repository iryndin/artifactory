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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.reporters.*;
import org.testng.reporters.Files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.testng.Assert.*;

/**
 * Tests the {@link ZipUtils} class.
 *
 * @author Yossi Shaul
 */
@Test
public class ZipUtilsTest extends ArtifactoryHomeBoundTest {
    private File zipFile;
    private File archiveWithSymLinks;
    private ZipInputStream zis;

    // the zip test contains: file.txt, folder/another.txt

    @BeforeClass
    public void setup() {
        zipFile = ResourceUtils.getResourceAsFile("/ziptest.zip");
        archiveWithSymLinks = ResourceUtils.getResourceAsFile("/zip-with-symlinks.tar.gz");
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

    public void testExtractZipFile() throws Exception {
        File tempExtractedDir = extract(zipFile);
        assertExtractedFiles(tempExtractedDir);
    }


    @Test // RTFACT-7596
    public void testExtractingSymLinks() throws Exception {
        File tempExtractedDir = extract(archiveWithSymLinks);
        assertExtractedSymlinks(new File(tempExtractedDir.getPath() + File.separator + "zip-with-symlinks"));
    }

    public void testExtractTarFile() throws Exception {
        File tarFile = ResourceUtils.getResourceAsFile("/tartest.tar");
        File tempExtractedDir = extract(tarFile);
        assertExtractedFiles(tempExtractedDir);
    }

    public void testExtractTarGzFile() throws Exception {
        File gzipFile = ResourceUtils.getResourceAsFile("/gziptest.tar.gz");
        File tempExtractedDir = extract(gzipFile);
        assertExtractedFiles(tempExtractedDir);
    }

    public void testExtractTgzFile() throws Exception {
        File tgzFile = ResourceUtils.getResourceAsFile("/tgztest.tgz");
        File tempExtractedDir = extract(tgzFile);
        assertExtractedFiles(tempExtractedDir);
    }

    @Test(expectedExceptions = RuntimeException.class,
            expectedExceptionsMessageRegExp = "(.*)Unsupported(.*)archive(.*)extension(.*)")
    public void testUnsupportedArchive() throws Exception {
        File unsupportedFile = ResourceUtils.getResourceAsFile("/unsupported.ar");
        extract(unsupportedFile);
    }

    private File extract(File fileToExtract) throws Exception {
        File tempExtractedDir = new File(fileToExtract.getParentFile(), "temp");
        FileUtils.deleteDirectory(tempExtractedDir);
        FileUtils.forceMkdir(tempExtractedDir);
        ZipUtils.extract(fileToExtract, tempExtractedDir);
        return tempExtractedDir;
    }

    private void assertExtractedFiles(File tempExtractedDir) {
        List<String> files = Lists.newArrayList(tempExtractedDir.list());
        assertEquals(files.size(), 2, "Unexpected files size");
        assertTrue(files.contains("file.txt"), "Unexpected file name");
        assertTrue(files.contains("folder"), "Unexpected folder name");
    }

    private void assertExtractedSymlinks(File extractedDir) throws IOException {
        List<String> files = Lists.newArrayList(extractedDir.list());
        assertEquals(files.size(), 5, "Unexpected files size");
        assertTrue(files.contains("chopper_ico.bmp"), "Missing file");
        assertTrue(files.contains("chopper_ico.bmp-hlink"), "Missing file");
        assertTrue(files.contains("chopper_ico.spr"), "Missing file");
        assertTrue(files.contains("chopper_ico.spr-slnk"), "Missing file");
        assertTrue(files.contains("chopper_top.bmp"), "Missing file");

        String file1 = Files.readFile(new File(extractedDir + File.separator + "chopper_ico.spr"));
        String linkToFile1 = Files.readFile(new File(extractedDir + File.separator + "chopper_ico.spr-slnk"));

        String file2 = Files.readFile(new File(extractedDir + File.separator + "chopper_ico.bmp"));
        String linkToFile2 = Files.readFile(new File(extractedDir + File.separator + "chopper_ico.bmp-hlink"));

        assertEquals(file1, linkToFile1, "Soft links reconstruction has failed");
        assertEquals(file2, linkToFile2, "Hard links reconstruction has failed");
    }
}
