/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.artifactory.util.ResourceUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.*;

/**
 * Unit tests for MavenModelUtils
 *
 * @author Yossi Shaul
 */
@Test
public class MavenModelUtilsTest extends ArtifactoryHomeBoundTest {

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

        assertEquals(metadata.getGroupId(), "boo");
        assertEquals(metadata.getArtifactId(), "boo");
        assertEquals(metadata.getVersion(), "0.5.1");
        assertNull(metadata.getVersioning());
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

        assertEquals(metadata.getGroupId(), "boo");
        Versioning versioning = metadata.getVersioning();
        assertNotNull(versioning);
        assertEquals(versioning.getVersions().size(), 1);
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
        assertEquals(newMetadata.getArtifactId(), metadata.getArtifactId());
        assertEquals(newMetadata.getGroupId(), metadata.getGroupId());
    }

    public void getArtifactInfoPom() throws Exception {
        File artifact = ResourceUtils.getResourceAsFile("/org/artifactory/maven/yourpit-1.0.0-alpha2.pom");
        MavenArtifactInfo artifactInfo = MavenModelUtils.artifactInfoFromFile(artifact);
        assertEquals(artifactInfo.getGroupId(), "yourpit");
        assertEquals(artifactInfo.getArtifactId(), "yourpit");
        assertEquals(artifactInfo.getVersion(), "1.0.0-alpha2");
        assertNull(artifactInfo.getClassifier(), "Classifier should be null");
    }

    public void getArtifactInfoJar() throws Exception {
        File artifact = ResourceUtils.getResourceAsFile("/org/artifactory/maven/testng-5.11-jdk15.jar");
        MavenArtifactInfo artifactInfo = MavenModelUtils.artifactInfoFromFile(artifact);
        assertEquals(artifactInfo.getGroupId(), "testng");
        assertEquals(artifactInfo.getArtifactId(), "testng");
        assertEquals(artifactInfo.getVersion(), "5.11");
        assertEquals(artifactInfo.getClassifier(), "jdk15");
    }

    public void mavenModelToArtifactInfo() {
        Model model = new MavenPomBuilder().groupId("myGroupId").artifactId("myArtifactId").version("1.0.0").build();
        MavenArtifactInfo artifactInfo = MavenModelUtils.mavenModelToArtifactInfo(model);
        assertEquals(artifactInfo.getGroupId(), "myGroupId");
        assertEquals(artifactInfo.getArtifactId(), "myArtifactId");
        assertEquals(artifactInfo.getVersion(), "1.0.0");
    }

    public void mavenModelToArtifactInfoGroupIdInParent() {
        Model model = new MavenPomBuilder().artifactId("myArtifactId").version("1.0.0").build();
        Parent parent = new Parent();
        parent.setGroupId("parentGroupId");
        parent.setArtifactId("parentArifactId");
        parent.setVersion("1.2.0");
        model.setParent(parent);

        MavenArtifactInfo artifactInfo = MavenModelUtils.mavenModelToArtifactInfo(model);

        assertEquals(artifactInfo.getGroupId(), "parentGroupId");
        assertEquals(artifactInfo.getArtifactId(), "myArtifactId");
        assertEquals(artifactInfo.getVersion(), "1.0.0");
    }

    public void readNewMaven3MetadataFormat() throws IOException {
        String newMaven3MetadataFormat = ResourceUtils.getResourceAsString(
                "/org/artifactory/maven/maven3-metadata.xml");
        Metadata metadata = MavenModelUtils.toMavenMetadata(newMaven3MetadataFormat);
    }
}