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
 * Tests the file metadata converter from version 1.2.5rc0 to 1.3.0beta3.
 *
 * @author Yossi Shaul
 */
@Test
public class MdFileConverterTest extends MetadataConverterTest {
    private final static Logger log = LoggerFactory.getLogger(MdFileConverterTest.class);

    public void convertValidFile() throws Exception {
        String fileMetadata = "/metadata/v125rc0/commons-cli-1.0.pom.artifactory-metadata";
        Document doc = convertMetadata(fileMetadata, new MdFileConverter());

        log.debug(ConverterUtils.outputString(doc));

        Element root = doc.getRootElement();
        assertEquals(root.getName(), "artifactory-file", "Root node should have been renamed");
        Element repoPath = root.getChild("repoPath");
        assertNotNull(repoPath, "Converter should create repoPath node");
        assertEquals(repoPath.getChildren().size(), 2, "Repo path should contains repoKey and path");
        Element extension = root.getChild("extension");
        assertNotNull(repoPath, "Converter should create extension node");
        assertEquals(extension.getChildText("modifiedBy"), "", "Modified by should be empty");
    }

}