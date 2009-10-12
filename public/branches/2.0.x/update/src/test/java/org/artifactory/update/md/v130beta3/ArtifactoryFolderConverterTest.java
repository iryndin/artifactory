package org.artifactory.update.md.v130beta3;

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
 * Tests the file metadata converter from version 1.3.0beta3 to 1.3.0beta6.
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactoryFolderConverterTest extends MetadataConverterTest {
    private final static Logger log = LoggerFactory.getLogger(ArtifactoryFolderConverterTest.class);

    public void convertValidFile() throws Exception {
        String fileMetadata = "/metadata/v130beta3/artifactory.folder.xml";
        Document doc = convertMetadata(fileMetadata, new ArtifactoryFolderConverter());

        log.debug(ConverterUtils.outputString(doc));

        Element root = doc.getRootElement();
        assertEquals(root.getName(), "artifactory-folder", "Root node should have been renamed");
        Element repoPath = root.getChild("repoPath");
        assertNotNull(repoPath, "Converter should create repoPath node");
        assertEquals(repoPath.getChildren().size(), 2, "Repo path should contains repoKey and path");

        Element extension = root.getChild("extension");
        assertNotNull(repoPath, "Converter should create extension node");
        assertEquals(extension.getChildText("modifiedBy"), "anonymous",
                "Modified by should be under the extension");
    }

}