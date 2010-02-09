package org.artifactory.utils;

import org.apache.commons.io.IOUtils;
import org.artifactory.version.ArtifactoryVersioning;
import org.artifactory.version.VersionParser;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tests the behaviour of the Version Parser
 *
 * @author Noam Tenne
 */
public class VersionParserTest {

    /**
     * Supplies the version parser with a mock versioning xml file and tries to parse ot to an ArtifactoryVersioning
     * object
     *
     * @throws IOException - Exception thrown if there are any problems with reading from the stream
     */
    @Test
    public void testLoadResource() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/artifactory/utils/versioning.xml");
        String input = IOUtils.toString(is, "utf-8");
        ArtifactoryVersioning artifactoryVersioning = VersionParser.parse(input);
        artifactoryVersioning.getLatest();
    }
}