package org.artifactory.api.maven;

import org.artifactory.api.repo.RepoPath;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * Tests the MavenArtifactInfo.
 *
 * @author Yossi Shaul
 */
@Test
public class MavenArtifactInfoTest {
    public void fromSimplePath() {
        RepoPath path = new RepoPath("repo", "/org/jfrog/artifactory-core/2.0/artifactory-core-2.0.pom");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "org.jfrog");
        assertEquals(artifactInfo.getArtifactId(), "artifactory-core");
        assertEquals(artifactInfo.getVersion(), "2.0");
        assertNull(artifactInfo.getClassifier());
        assertEquals(artifactInfo.getType(), "pom");
        assertFalse(artifactInfo.isSnapshot(), "This is a release version");
    }

    public void fromPathWithClassifier() {
        RepoPath path = new RepoPath("repo", "/org/jfrog/artifactory-core/2.0/artifactory-core-2.0-sources.jar");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "org.jfrog");
        assertEquals(artifactInfo.getArtifactId(), "artifactory-core");
        assertEquals(artifactInfo.getVersion(), "2.0");
        assertEquals(artifactInfo.getClassifier(), "sources");
        assertEquals(artifactInfo.getType(), "jar");
        assertFalse(artifactInfo.isSnapshot(), "This is a release version");
    }

    public void fromPathUniqueSnapshotVersion() {
        // unique snapshot version is a version that includes the timestamp-buildnumber string in the version
        RepoPath path = new RepoPath("repo", "com/core/5.4-SNAPSHOT/artifactory-core-5.4-SNAPSHOT-sources.jar");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "com");
        assertEquals(artifactInfo.getArtifactId(), "core");
        assertEquals(artifactInfo.getVersion(), "5.4-SNAPSHOT");
        assertEquals(artifactInfo.getClassifier(), "sources");
        assertEquals(artifactInfo.getType(), "jar");
        assertTrue(artifactInfo.isSnapshot(), "This is a snapshot version");
    }

    public void fromPathNonUniqueSnapshotVersion() {
        // non-unique snapshot version is a version that includes the SNAPSHOT string in the version and not the
        // timestamp-buildnumber
        RepoPath path = new RepoPath("repo",
                "com/core/5.4-SNAPSHOT/artifactory-core-5.4-20081214.090217-4-sources.jar");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertTrue(artifactInfo.isValid());
        assertEquals(artifactInfo.getGroupId(), "com");
        assertEquals(artifactInfo.getArtifactId(), "core");
        assertEquals(artifactInfo.getVersion(), "5.4-SNAPSHOT");
        assertEquals(artifactInfo.getClassifier(), "sources");
        assertEquals(artifactInfo.getType(), "jar");
        assertTrue(artifactInfo.isSnapshot(), "This is a snapshot version");
    }

    public void fromInvalidPath() {
        RepoPath path = new RepoPath("repo", "com/5.4-SNAPSHOT");
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(path);
        assertFalse(artifactInfo.isValid());
    }
}
