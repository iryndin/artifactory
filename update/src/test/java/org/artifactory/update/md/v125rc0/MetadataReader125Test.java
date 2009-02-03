package org.artifactory.update.md.v125rc0;

import com.thoughtworks.xstream.XStream;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.api.md.MetadataEntry;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.stat.StatsInfo;
import org.testng.Assert;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Tests the matadata reader from versions 1.3.3-beta-3 to 1.3.3-beta-6 (or later).
 *
 * @author Yossi Shaul
 */
@Test
public class MetadataReader125Test {
    private XStream xstream;

    @BeforeClass
    public void setup() {
        xstream = new XStream();
        xstream.processAnnotations(new Class[]{FileInfo.class, StatsInfo.class, FolderInfo.class});
    }

    public void readFolderMetadata() {
        MetadataReader125 reader = new MetadataReader125();
        File folderMetadataDirectory = getMetadataDirectory("/metadata/v125rc0/commons-cli.artifactory-metadata");
        StatusHolder status = new StatusHolder();
        List<MetadataEntry> entries = reader.getMetadataEntries(folderMetadataDirectory, null, status);
        assertFalse(status.isError());
        assertNotNull(entries);
        assertEquals(entries.size(), 1, "One matadata entry expected - folder");

        // the result should be compatible with the latest FolderInfo
        FolderInfo folderInfo = (FolderInfo) xstream.fromXML(entries.get(0).getXmlContent());
        assertEquals(folderInfo.getName(), "commons-cli", "Name mismatch");
        RepoPath repoPath = folderInfo.getRepoPath();
        assertEquals(repoPath.getRepoKey(), "repo1-cache", "Repository key mismatch");
        assertEquals(repoPath.getPath(), "commons-cli", "Path mismatch");
    }

    public void readFileMetadata() {
        MetadataReader125 reader = new MetadataReader125();
        File fileMetadataDirectory = getMetadataDirectory(
                "/metadata/v125rc0/commons-cli-1.0.pom.artifactory-metadata");
        StatusHolder status = new StatusHolder();
        List<MetadataEntry> entries = reader.getMetadataEntries(fileMetadataDirectory, null, status);
        assertFalse(status.isError());
        assertNotNull(entries);
        assertEquals(entries.size(), 2, "Two matadata entries are expected - file and stats");

        FileInfo fileInfo = (FileInfo) xstream.fromXML(entries.get(0).getXmlContent());
        Set<ChecksumInfo> checksums = fileInfo.getChecksums();
        Assert.assertNotNull(checksums);
        Assert.assertEquals(checksums.size(), 2);
        Assert.assertEquals(fileInfo.getSha1(), "");
        Assert.assertEquals(fileInfo.getMd5(), "");

        // just make sure the stats is readable
        StatsInfo statsInfo = (StatsInfo) xstream.fromXML(entries.get(1).getXmlContent());
        assertEquals(statsInfo.getDownloadCount(), 99);
    }

    private File getMetadataDirectory(String path) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return new File(resource.getFile());
    }
}