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
package org.artifactory.version;

import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * @author freds
 * @date Nov 23, 2008
 */
public class ArtifactoryDbVersionTest {
    @Test
    public void testCoverage() {
        // Check that all Artifactory versions are covered by a DB version
        ArtifactoryDbVersion[] versions = ArtifactoryDbVersion.values();
        Assert.assertTrue(versions.length > 0);
        assertEquals(versions[0].getComparator().getFrom(), ArtifactoryVersion.v122rc0,
                "First version should start at first supported Artifactory version");
        assertEquals(versions[versions.length - 1].getComparator().getUntil(), ArtifactoryVersion.getCurrent(),
                "Last version should be the current one");
        for (int i = 0; i < versions.length; i++) {
            ArtifactoryDbVersion version = versions[i];
            if (i + 1 < versions.length) {
                assertEquals(version.getComparator().getUntil().ordinal(),
                        versions[i + 1].getComparator().getFrom().ordinal() - 1,
                        "Versions should ave full coverage and leave no holes in the list of Artifactory versions");
            }
        }
    }
}
