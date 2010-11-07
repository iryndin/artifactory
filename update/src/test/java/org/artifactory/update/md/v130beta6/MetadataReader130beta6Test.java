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

package org.artifactory.update.md.v130beta6;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.FolderInfoImpl;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.fs.FileInfo;
import org.artifactory.update.md.MetadataReaderBaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Tests the matadata reader from versions 1.3.3-beta-6 to rc1 (or later).
 *
 * @author Yossi Shaul
 */
@Test
public class MetadataReader130beta6Test extends MetadataReaderBaseTest {
    private XStream xstream;

    @BeforeClass
    public void setupXStream() {
        xstream = XStreamFactory.create(FileInfoImpl.class, StatsInfo.class, FolderInfoImpl.class);
    }

    public void readFolderMetadata() {
        MetadataReader130beta6 reader = new MetadataReader130beta6();
        File folderMetadataDirectory = getMetadataDirectory("/metadata/v130beta6/3.8.1.artifactory-metadata");
        BasicStatusHolder status = new BasicStatusHolder();
        List<MetadataEntry> entries = reader.getMetadataEntries(folderMetadataDirectory, null, status);
        assertFalse(status.isError());
        assertNotNull(entries);
        assertEquals(entries.size(), 1, "One matadata entry expected - folder");
        assertEquals(entries.get(0).getMetadataName(), "artifactory-folder");

        // the result should be compatible with the latest FolderInfo
        org.artifactory.fs.FolderInfo folderInfo =
                (org.artifactory.fs.FolderInfo) xstream.fromXML(entries.get(0).getXmlContent());
    }

    public void readFileMetadata() {
        MetadataReader130beta6 reader = new MetadataReader130beta6();
        File fileMetadataDirectory = getMetadataDirectory(
                "/metadata/v130beta6/junit-3.8.1.jar.artifactory-metadata");
        BasicStatusHolder status = new BasicStatusHolder();
        List<MetadataEntry> entries = reader.getMetadataEntries(fileMetadataDirectory, null, status);
        assertFalse(status.isError());
        assertNotNull(entries);
        assertEquals(entries.size(), 2, "Two matadata entries are expected - file and stats");

        MetadataEntry fileInfoEntry = getMetadataByName(entries, FileInfo.ROOT);
        org.artifactory.fs.FileInfo fileInfo = (FileInfo) xstream.fromXML(fileInfoEntry.getXmlContent());
        Set<ChecksumInfo> checksums = fileInfo.getChecksums();
        Assert.assertNotNull(checksums);
        Assert.assertEquals(checksums.size(), 2);
        Assert.assertEquals(fileInfo.getSha1(), "99129f16442844f6a4a11ae22fbbee40b14d774f");
        Assert.assertEquals(fileInfo.getMd5(), "1f40fb782a4f2cf78f161d32670f7a3a");

        // just make sure the stats is readable
        MetadataEntry statsInfoEntry = getMetadataByName(entries, StatsInfo.ROOT);
        StatsInfo statsInfo = (StatsInfo) xstream.fromXML(statsInfoEntry.getXmlContent());
        assertEquals(statsInfo.getDownloadCount(), 2);
    }

    private File getMetadataDirectory(String path) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return new File(resource.getFile());
    }
}
