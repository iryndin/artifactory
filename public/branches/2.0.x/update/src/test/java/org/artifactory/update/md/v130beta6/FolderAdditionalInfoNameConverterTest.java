package org.artifactory.update.md.v130beta6;

import org.artifactory.api.fs.FolderAdditionaInfo;
import org.artifactory.api.fs.FolderInfo;
import org.artifactory.update.md.MetadataConverterTest;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the conversion of folder info metadata.
 *
 * @author Yossi Shaul
 */
@Test
public class FolderAdditionalInfoNameConverterTest extends MetadataConverterTest {
    private final static Logger log = LoggerFactory.getLogger(FolderAdditionalInfoNameConverterTest.class);

    public void convertValidFolderInfo() throws Exception {
        String fileMetadata = "/metadata/v130beta6/artifactory-folder.xml";
        Document doc = convertMetadata(fileMetadata, new FolderAdditionalInfoNameConverter());

        String result = ConverterUtils.outputString(doc);
        log.debug(result);

        // the result is intermediate so it might not be compatible with latest FolderInfo
        // but for now it is a good test to test the resulting FolderInfo
        FolderInfo folderInfo = (FolderInfo) xstream.fromXML(result);
        FolderAdditionaInfo additionalInfo = folderInfo.getInernalXmlInfo();
        Assert.assertNotNull(additionalInfo);

        FolderInfo expected = (FolderInfo) xstream.fromXML(
                loadResource("/metadata/v130beta6/artifactory-folder-expected.xml"));
        Assert.assertTrue(folderInfo.isIdentical(expected));
    }
}
