package org.artifactory.update.md.v125rc0;

import org.artifactory.update.md.MetadataConverterTest;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;

/**
 * Tests the stats metadata converter from version 1.3.0beta3 to 1.3.0beta6.
 *
 * @author Yossi Shaul
 */
@Test
public class MdStatsConverterTest extends MetadataConverterTest {
    private final static Logger log = LoggerFactory.getLogger(MdStatsConverterTest.class);

    public void convertValidFile() throws Exception {
        String fileMetadata = "/metadata/v125rc0/commons-cli-1.0.pom.artifactory-metadata";
        Document doc = convertMetadata(fileMetadata, new MdStatsConverter());

        log.debug(ConverterUtils.outputString(doc));

        Element root = doc.getRootElement();
        assertEquals(root.getName(), "artifactory.stats", "Root node should have been renamed");
        Element downloadCount = root.getChild("downloadCount");
        assertNotNull(downloadCount, "Converter should create downloadCount node");
        assertEquals(downloadCount.getText(), "99", "Download count should be 1");
    }

}