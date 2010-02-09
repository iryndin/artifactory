package org.artifactory.update.md.v130beta6;

import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.FileInfo;
import org.artifactory.update.md.MetadataConverterTest;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;

/**
 * Tests the conversion of checksums info in the file metadata.
 *
 * @author Yossi Shaul
 */
@Test
public class ChecksumsConverterTest extends MetadataConverterTest {
    private final static Logger log = LoggerFactory.getLogger(ChecksumsConverterTest.class);

    public void convertValidFile() throws Exception {
        String fileMetadata = "/metadata/v130beta6/artifactory-file.xml";
        Document doc = convertMetadata(fileMetadata, new ChecksumsConverter());

        String result = ConverterUtils.outputString(doc);
        log.debug(result);

        // the result is intermediate so it might not be compatible with latest FileInfo
        // but for now it is a good test to test the resulting FileInfo 
        FileInfo fileInfo = (FileInfo) xstream.fromXML(result);
        Set<ChecksumInfo> checksums = fileInfo.getChecksums();
        Assert.assertNotNull(checksums);
        Assert.assertEquals(checksums.size(), 2);
        Assert.assertEquals(fileInfo.getSha1(), "99129f16442844f6a4a11ae22fbbee40b14d774f");
        Assert.assertEquals(fileInfo.getMd5(), "1f40fb782a4f2cf78f161d32670f7a3a");

        FileInfo expected = (FileInfo) xstream.fromXML(
                loadResource("/metadata/v130beta6/artifactory-file-expected.xml"));
        Assert.assertTrue(fileInfo.isIdentical(expected));
    }
}
