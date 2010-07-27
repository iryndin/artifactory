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

package org.artifactory.update.md.v130beta3;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.fs.FolderInfoImpl;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.stat.StatsInfo;
import org.artifactory.api.xstream.XStreamFactory;
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
 * Tests the matadata reader from versions 1.3.3-beta-3 to 1.3.3-beta-6 (or later).
 *
 * @author Yossi Shaul
 */
@Test
public class MetadataReader130beta3Test extends MetadataReaderBaseTest {
    private XStream xstream;

    @BeforeClass
    public void setupXStream() {
        xstream = XStreamFactory.create(FileInfoImpl.class, StatsInfo.class, FolderInfoImpl.class);
    }

    public void readFolderMetadata() {
        MetadataReader130beta3 reader = new MetadataReader130beta3();
        File folderMetadataDirectory = getMetadataDirectory("/metadata/v130beta3/0.1.23.artifactory-metadata");
        StatusHolder status = new StatusHolder();
        List<MetadataEntry> entries = reader.getMetadataEntries(folderMetadataDirectory, null, status);
        assertFalse(status.isError());
        assertNotNull(entries);
        assertEquals(entries.size(), 1, "One matadata entry expected - folder");

        // the result should be compatible with the latest FolderInfo
        FolderInfo folderInfo = (FolderInfo) xstream.fromXML(entries.get(0).getXmlContent());
        assertEquals(folderInfo.getName(), "0.1.23", "Name mismatch");
        RepoPath repoPath = folderInfo.getRepoPath();
        assertEquals(repoPath.getRepoKey(), "repo1-cache", "Repository key mismatch");
        assertEquals(repoPath.getPath(), "com/jcraft/jsch/0.1.23", "Path mismatch");
    }

    public void readFileMetadata() {
        MetadataReader130beta3 reader = new MetadataReader130beta3();
        File fileMetadataDirectory = getMetadataDirectory(
                "/metadata/v130beta3/jsch-0.1.23.pom.artifactory-metadata");
        StatusHolder status = new StatusHolder();
        List<MetadataEntry> entries = reader.getMetadataEntries(fileMetadataDirectory, null, status);
        assertFalse(status.isError());
        assertNotNull(entries);
        assertEquals(entries.size(), 2, "Two matadata entries are expected - file and stats");

        MetadataEntry fileInfoEntry = getMetadataByName(entries, FileInfo.ROOT);
        FileInfo fileInfo = (FileInfo) xstream.fromXML(fileInfoEntry.getXmlContent());
        Set<ChecksumInfo> checksums = fileInfo.getChecksums();
        Assert.assertNotNull(checksums);
        Assert.assertEquals(checksums.size(), 2);
        Assert.assertEquals(fileInfo.getSha1(), "1d4266015cc4deba8bf4b56441ebc02cd170503d");
        Assert.assertEquals(fileInfo.getMd5(), "f1298dace833ceafd88f6cd6acc26c64");

        // just make sure the stats is readable
        MetadataEntry statsInfoEntry = getMetadataByName(entries, StatsInfo.ROOT);
        StatsInfo statsInfo = (StatsInfo) xstream.fromXML(statsInfoEntry.getXmlContent());
        assertEquals(statsInfo.getDownloadCount(), 1);
    }

    private File getMetadataDirectory(String path) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return new File(resource.getFile());
    }
}