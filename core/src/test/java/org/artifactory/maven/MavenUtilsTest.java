/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.maven.MavenNaming;
import org.testng.Assert;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Unit tests for MavenUtils
 *
 * @author Yossi Shaul
 */
@Test
public class MavenUtilsTest {

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

    public void testToMaven1PathMD5() {
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
        Metadata metadata = MavenUtils.toMavenMetadata(
                "<metadata>\n" +
                    "<groupId>boo</groupId>\n" +
                    "<artifactId>boo</artifactId>\n" +
                    "<version>0.5.1</version>\n" +
                "</metadata>");

        assertEquals(metadata.getGroupId(), "boo");
        assertEquals(metadata.getArtifactId(), "boo");
        assertEquals(metadata.getVersion(), "0.5.1");
        Assert.assertNull(metadata.getVersioning());
    }

    public void validStringToMetadata() throws IOException {
        Metadata metadata = MavenUtils.toMavenMetadata(
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
        MavenUtils.toMavenMetadata(
                "<metadatablabla\n" +
                    "<artifactId>boo</artifactId>\n" +
                    "<version>0.5.1</version>\n" +
                "</metadata>");
    }

    public void inputStreamToMavenMetadata() throws IOException {
        InputStream is = getClass().getResourceAsStream("/org/artifactory/maven/maven-metadata.xml");
        Metadata metadata = MavenUtils.toMavenMetadata(is);
        assertNotNull(metadata.getVersioning());
    }

    public void mavenMetadataToString() throws IOException {
        Metadata metadata = new Metadata();
        metadata.setArtifactId("theartid");
        metadata.setGroupId("thegroupid");

        String metadataStr = MavenUtils.mavenMetadataToString(metadata);
        assertNotNull(metadataStr);
        Metadata newMetadata = MavenUtils.toMavenMetadata(metadataStr);
        assertEquals(newMetadata.getArtifactId(), metadata.getArtifactId());
        assertEquals(newMetadata.getGroupId(), metadata.getGroupId());
    }
}
