package org.artifactory.update.test;

import junit.framework.TestCase;
import org.artifactory.update.ArtifactoryVersion;

import java.net.URL;

/**
 * User: freds
 * Date: May 30, 2008
 * Time: 10:22:23 AM
 */
public class TestArtifactoryVersion extends TestCase {
    private static final String REPO_XML = "repo.xml";
    private static final String ARTIFACTORY_NODETYPES_XML = "artifactory_nodetypes.xml";

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
}
