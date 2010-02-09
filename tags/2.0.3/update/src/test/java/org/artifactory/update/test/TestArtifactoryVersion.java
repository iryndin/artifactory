package org.artifactory.update.test;

import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.version.ArtifactoryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * User: freds Date: May 30, 2008 Time: 10:22:23 AM
 */
public class TestArtifactoryVersion {
    private static final Logger log =
            LoggerFactory.getLogger(TestArtifactoryVersion.class);

    private static final String REPO_XML = "repo.xml";
    private static final String ARTIFACTORY_NODETYPES_XML = "artifactory_nodetypes.xml";

    @Test
    public void testResourceFind() throws Exception {
        URL repoXml = ArtifactoryVersion.v122.findResource(REPO_XML);
        URL otherRepoXml = ArtifactoryVersion.v122rc1.findResource(REPO_XML);
        assertEquals(repoXml, otherRepoXml);
        otherRepoXml = ArtifactoryVersion.v125rc0.findResource(REPO_XML);
        assertNotSame(repoXml, otherRepoXml);
        URL nodeTypesXml = ArtifactoryVersion.v122.findResource(ARTIFACTORY_NODETYPES_XML);
        URL otherTypesXml = ArtifactoryVersion.v125rc0.findResource(ARTIFACTORY_NODETYPES_XML);
        assertEquals(nodeTypesXml, otherTypesXml);
        nodeTypesXml = ArtifactoryVersion.v125.findResource(ARTIFACTORY_NODETYPES_XML);
        assertNotSame(nodeTypesXml, otherTypesXml);
        otherTypesXml = ArtifactoryVersion.v125rc3.findResource(ARTIFACTORY_NODETYPES_XML);
        assertEquals(nodeTypesXml, otherTypesXml);
    }

    @Test
    public void testBackupPropertyFile() throws Exception {
        ArtifactoryVersion[] versions = ArtifactoryVersion.values();
        for (ArtifactoryVersion version : versions) {
            if (version.isCurrent()) {
                // No test here
                continue;
            }
            String backupVersionFolder = "/backups/" + version.name() + "/";
            InputStream artifactoryPropertyInputStream =
                    getClass().getResourceAsStream(backupVersionFolder +
                            ArtifactoryHome.ARTIFACTORY_PROPERTIES_FILE);
            if (artifactoryPropertyInputStream != null) {
                Properties properties = new Properties();
                properties.load(artifactoryPropertyInputStream);
                artifactoryPropertyInputStream.close();
                String artifactoryVersion =
                        properties.getProperty(ConstantsValue.artifactoryVersion.getPropertyName());
                assertEquals(artifactoryVersion, version.getValue(),
                        "Error in version value for " + version);
                int artifactoryRevision =
                        Integer.parseInt(properties.getProperty(
                                ConstantsValue.artifactoryRevision.getPropertyName()));
                assertEquals(artifactoryRevision, version.getRevision(),
                        "Error in revision value for " + version);
            } else {
                log.warn("Version " + version + " does not have a backup test folder in " +
                        backupVersionFolder);
            }
        }
    }
}
