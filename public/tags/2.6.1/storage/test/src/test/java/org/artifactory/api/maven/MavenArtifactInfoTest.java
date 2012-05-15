/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.api.maven;

import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.repo.RepoPath;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests the MavenArtifactInfo.
 *
 * @author Yossi Shaul
 */
@Test
public class MavenArtifactInfoTest extends ArtifactoryHomeBoundTest {

    public void fromSimplePath() {
        RepoPath path = InfoFactoryHolder.get().createRepoPath("repo",
                "/org/jfrog/artifactory-core/2.0/artifactory-core-2.0.pom");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "org.jfrog");
        assertEquals(artifactInfo.getArtifactId(), "artifactory-core");
        assertEquals(artifactInfo.getVersion(), "2.0");
        assertNull(artifactInfo.getClassifier());
        assertEquals(artifactInfo.getType(), "pom");
    }

    public void fromPathWithClassifier() {
        RepoPath path = InfoFactoryHolder.get().createRepoPath("repo",
                "/org/jfrog/artifactory-core/2.0/artifactory-core-2.0-sources.jar");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "org.jfrog");
        assertEquals(artifactInfo.getArtifactId(), "artifactory-core");
        assertEquals(artifactInfo.getVersion(), "2.0");
        assertEquals(artifactInfo.getClassifier(), "sources");
        assertEquals(artifactInfo.getType(), "jar");
    }

    public void fromPathNonUniqueSnapshotVersion() {
        // unique snapshot version is a version that includes the timestamp-buildnumber string in the version
        RepoPath path = InfoFactoryHolder.get().createRepoPath("repo",
                "com/core/5.4-SNAPSHOT/artifactory-core-5.4-SNAPSHOT-sources.jar");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "com");
        assertEquals(artifactInfo.getArtifactId(), "core");
        assertEquals(artifactInfo.getVersion(), "5.4-SNAPSHOT");
        assertEquals(artifactInfo.getClassifier(), "sources");
        assertEquals(artifactInfo.getType(), "jar");
    }

    public void fromPathUniqueSnapshotVersion() {
        // unique snapshot version is a version that includes the SNAPSHOT string in the version and not the
        // timestamp-buildnumber
        RepoPath path = InfoFactoryHolder.get().createRepoPath("repo",
                "com/core/5.4-SNAPSHOT/artifactory-core-5.4-20081214.090217-4-sources.jar");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "com");
        assertEquals(artifactInfo.getArtifactId(), "core");
        assertEquals(artifactInfo.getVersion(), "5.4-SNAPSHOT");
        assertEquals(artifactInfo.getClassifier(), "sources");
        assertEquals(artifactInfo.getType(), "jar");
    }

    public void fromPathUniqueMd5SnapshotVersion() {
        // non-unique snapshot version is a version that includes the SNAPSHOT string in the version and not the
        // timestamp-buildnumber
        RepoPath path = InfoFactoryHolder.get().createRepoPath("repo",
                "com/core/5.4-SNAPSHOT/core-5.4-20081214.090217-4-sources.jar.md5");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "com");
        assertEquals(artifactInfo.getArtifactId(), "core");
        assertEquals(artifactInfo.getVersion(), "5.4-SNAPSHOT");
        assertEquals(artifactInfo.getClassifier(), "sources");
        assertEquals(artifactInfo.getType(), "jar.md5");
    }

    public void fromInvalidPath() {
        RepoPath path = InfoFactoryHolder.get().createRepoPath("repo", "com/5.4-SNAPSHOT");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertFalse(artifactInfo.isValid());
    }

    public void fromPathWithNoGroupId() {
        RepoPath path = InfoFactoryHolder.get().createRepoPath("repo", "com/5.4-SNAPSHOT/bob.jar");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertFalse(artifactInfo.isValid());
    }
}
