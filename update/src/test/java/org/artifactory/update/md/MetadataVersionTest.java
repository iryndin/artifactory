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
package org.artifactory.update.md;

import org.artifactory.update.test.TestUtils;
import org.artifactory.version.ArtifactoryVersion;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author freds
 * @date Nov 23, 2008
 */
@Test
public class MetadataVersionTest {
    public void testCoverage() {
        // Check that all Artifactory versions are covered by a DB version
        MetadataVersion[] versions = MetadataVersion.values();
        Assert.assertTrue(versions.length > 0);
        assertEquals(versions[0].getComparator().getFrom(), ArtifactoryVersion.v122rc0,
                "First version should start at first supported Artifactory version");
        assertEquals(versions[versions.length - 1].getComparator().getUntil(), ArtifactoryVersion.getCurrent(),
                "Last version should be the current one");
        for (int i = 0; i < versions.length; i++) {
            MetadataVersion version = versions[i];
            if (i + 1 < versions.length) {
                assertEquals(version.getComparator().getUntil().ordinal(),
                        versions[i + 1].getComparator().getFrom().ordinal() - 1,
                        "Versions should ave full coverage and leave no holes in the list of Artifactory versions");
            }
        }
    }

    public void detectVersion125rc0() {
        File metadataFile = TestUtils.getResourceAsFile(
                "/metadata/v125rc0/commons-cli-1.0.pom.artifactory-metadata");
        MetadataVersion version = MetadataVersion.findVersion(metadataFile);
        assertEquals(version, MetadataVersion.v125rc0);
    }

    public void detectVersion30beta3() {
        File metadataDir = TestUtils.getResourceAsFile(
                "/metadata/v130beta3/0.1.23.artifactory-metadata");
        MetadataVersion version = MetadataVersion.findVersion(metadataDir);
        assertEquals(version, MetadataVersion.v130beta3);
    }

    public void detectVersion30beta6() {
        File metadataDir = TestUtils.getResourceAsFile(
                "/metadata/v130beta6/junit-3.8.1.jar.artifactory-metadata");
        MetadataVersion version = MetadataVersion.findVersion(metadataDir);
        assertEquals(version, MetadataVersion.v130beta6);
    }

}