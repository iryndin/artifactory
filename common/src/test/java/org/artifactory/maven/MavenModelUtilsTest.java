/*
 * This file is part of Artifactory.
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

package org.artifactory.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.common.property.ArtifactorySystemProperties;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Unit tests for MavenUtils
 *
 * @author Yossi Shaul
 */
@Test
public class MavenModelUtilsTest {

    @BeforeMethod
    public void bindProperties() {
        ArtifactorySystemProperties.bind(new ArtifactorySystemProperties());
    }

    @AfterMethod
    public void unbindProperties() {
        ArtifactorySystemProperties.unbind();
    }

    public void testToMaven1Path() {
        String maven1Url = MavenNaming.toMaven1Path(
                "org/apache/commons/commons-email/1.1/commons-email-1.1.jar");
        assertEquals("org.apache.commons/jars/commons-email-1.1.jar", maven1Url);
    }

    public void testToMaven1PathPom() {
        String maven1Url = MavenNaming.toMaven1Path(
                "org/apache/commons/commons-email/1.1/commons-email-1.1.pom");
        assertEquals("org.apache.commons/poms/commons-email-1.1.pom", maven1Url);
    }

    public void testToMaven1PathMd5() {
        String maven1Url = MavenNaming.toMaven1Path(
                "com/sun/commons/logging-api/1.0.4/logging-api-1.0.4.jar.md5");
        assertEquals("com.sun.commons/jars/logging-api-1.0.4.jar.md5", maven1Url);
    }

    public void testToMaven1PathSha1() {
        String maven1Url = MavenNaming.toMaven1Path(
                "com/sun/commons/logging-api/1.0.4/logging-api-1.0.4.pom.sha1");
        assertEquals("com.sun.commons/poms/logging-api-1.0.4.pom.sha1", maven1Url);
    }

    public void validStringToMetadataNoVersioning() throws IOException {
        Metadata metadata = MavenModelUtils.toMavenMetadata(
                "<metadata>\n" +
                        "<groupId>boo</groupId>\n" +
                        "<artifactId>boo</artifactId>\n" +
                        "<version>0.5.1</version>\n" +
                        "</metadata>");

        Assert.assertEquals(metadata.getGroupId(), "boo");
        Assert.assertEquals(metadata.getArtifactId(), "boo");
        Assert.assertEquals(metadata.getVersion(), "0.5.1");
        Assert.assertNull(metadata.getVersioning());
    }

    public void validStringToMetadata() throws IOException {
        Metadata metadata = MavenModelUtils.toMavenMetadata(
                "<metadata>\n" +
                        "<groupId>boo</groupId>\n" +
                        "<artifactId>boo</artifactId>\n" +
                        "<version>0.7.0.1921</version>\n" +
                        "<versioning>\n" +
                        "<versions>\n" +
                        "<version>0.7.0.1921</version>\n" +
                        "</versions>\n" +
                        "</versioning>\n" +
                        "</metadata>");

        Assert.assertEquals(metadata.getGroupId(), "boo");
        Versioning versioning = metadata.getVersioning();
        assertNotNull(versioning);
        Assert.assertEquals(versioning.getVersions().size(), 1);
    }

    @Test(expectedExceptions = IOException.class)
    public void nonValidMetadataString() throws IOException {
        MavenModelUtils.toMavenMetadata(
                "<metadatablabla\n" +
                        "<artifactId>boo</artifactId>\n" +
                        "<version>0.5.1</version>\n" +
                        "</metadata>");
    }

    public void inputStreamToMavenMetadata() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/artifactory/maven/maven-metadata.xml");
        Metadata metadata = MavenModelUtils.toMavenMetadata(is);
        assertNotNull(metadata.getVersioning());
    }

    public void mavenMetadataToString() throws IOException {
        Metadata metadata = new Metadata();
        metadata.setArtifactId("theartid");
        metadata.setGroupId("thegroupid");

        String metadataStr = MavenModelUtils.mavenMetadataToString(metadata);
        assertNotNull(metadataStr);
        Metadata newMetadata = MavenModelUtils.toMavenMetadata(metadataStr);
        Assert.assertEquals(newMetadata.getArtifactId(), metadata.getArtifactId());
        Assert.assertEquals(newMetadata.getGroupId(), metadata.getGroupId());
    }
}
