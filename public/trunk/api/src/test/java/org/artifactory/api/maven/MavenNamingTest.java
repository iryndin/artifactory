package org.artifactory.api.maven;

import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * Tests the MavenNaming.
 *
 * @author Yossi Shaul
 */
@Test
public class MavenNamingTest {

    public void testIsVersionSnapshot() {
        assertTrue(MavenNaming.isVersionSnapshot("1.2-SNAPSHOT"));
        assertFalse(MavenNaming.isVersionSnapshot("1.2-SNAPSHOT123"));
        assertFalse(MavenNaming.isVersionSnapshot("1.2"));
    }

    public void testIsNonUniqueSnapshotFilePath() {
        assertFalse(MavenNaming.isNonUniqueSnapshot("a/path/1.0-SNAPSHOT/1.0-SNAPSHOT"));
        assertTrue(MavenNaming.isNonUniqueSnapshot("a/path/1.0-SNAPSHOT/1.0-SNAPSHOT.pom"));
        assertTrue(MavenNaming.isNonUniqueSnapshot("a/path/1.0-SNAPSHOT/1.0-SNAPSHOT-sources.jar"));
        assertFalse(MavenNaming.isNonUniqueSnapshot("a/path/5.4-SNAPSHOT/path-5.4-20081214.090217-4.pom"));
    }

    public void testIsSnapshot() {
        assertTrue(MavenNaming.isSnapshot("a/path/1.0-SNAPSHOT/1.0-SNAPSHOT"));
        assertTrue(MavenNaming.isSnapshot("a/path/1.0-SNAPSHOT/1.0-SNAPSHOT.pom"));
        assertTrue(MavenNaming.isSnapshot("a/path/1.0-SNAPSHOT/"));
        assertTrue(MavenNaming.isSnapshot("a/path/1.0-SNAPSHOT"));
        assertFalse(MavenNaming.isSnapshot("a/path/1.0-SNAPSHOT/sub/path"));
        assertTrue(MavenNaming.isSnapshot("a/path/5.4-SNAPSHOT/path-5.4-20081214.090217-4.pom"));
    }

    public void testIsVersionUniqueSnapshot() {
        assertTrue(MavenNaming.isUniqueSnapshot("g/a/1.0-SNAPSHOT/artifact-5.4-20081214.090217-4.pom"));
        assertFalse(MavenNaming.isUniqueSnapshot("g/a/1.0/artifact-5.4-20081214.090217-4.pom"));
        assertTrue(MavenNaming.isUniqueSnapshotFileName("artifact-5.4-20081214.090217-4.pom"));
        assertTrue(MavenNaming.isUniqueSnapshotFileName("artifact-5.4-20081214.090217-4-classifier.pom"));
        assertFalse(MavenNaming.isUniqueSnapshotFileName("-20081214.090217-4.pom"), "No artifact id");
        assertFalse(MavenNaming.isUniqueSnapshotFileName("5.4-20081214.090217-4.pom"), "No artifact id");
        assertFalse(MavenNaming.isUniqueSnapshotFileName("artifact-5.4-20081214.090217-4"), "no type");
        assertFalse(MavenNaming.isUniqueSnapshotFileName("artifact-5.4-20081214.090217-4."), "empty type");
    }

    public void testUniqueVersionTimestamp() {
        String versionFile = "artifact-5.4-20081214.090217-4.pom";
        assertEquals(MavenNaming.getUniqueSnapshotVersionTimestamp(versionFile), "20081214.090217");
    }

    public void testUniqueVersionTimestampAndBuildNumber() {
        String versionFile = "artifact-5.4-20081214.090217-4.pom";
        assertEquals(MavenNaming.getUniqueSnapshotVersionTimestampAndBuildNumber(versionFile), "20081214.090217-4");
    }

    public void testUniqueSnapshotVersionBuildNumber() {
        String versionFile = "artifact-5.4-20081214.090217-4.pom";
        assertEquals(MavenNaming.getUniqueSnapshotVersionBuildNumber(versionFile), 4);

        versionFile = "artifact-456-20081214.120217-777.pom";
        assertEquals(MavenNaming.getUniqueSnapshotVersionBuildNumber(versionFile), 777);
    }

    public void isMetadata() {
        assertTrue(MavenNaming.isMavenMetadata("path/1.0-SNAPSHOT/maven-metadata.xml"));
        assertFalse(MavenNaming.isMavenMetadata(
                "org/apache/maven/plugins/maven-plugin-plugin/maven-metadata-xyz-snapshots.xml"), "Not maven metadata");
    }

    public void isSnapshotMavenMetadata() {
        assertTrue(MavenNaming.isSnapshotMavenMetadata("path/1.0-SNAPSHOT/maven-metadata.xml"));
        assertTrue(MavenNaming.isSnapshotMavenMetadata("path/1.0-SNAPSHOT/resource#maven-metadata.xml"));
        assertFalse(MavenNaming.isSnapshotMavenMetadata("path/1.0/maven-metadata.xml"), "Not a snapshot");
        assertFalse(MavenNaming.isSnapshotMavenMetadata("path/1.0/resource#maven-metadata.xml"), "Not a snapshot");
        assertFalse(MavenNaming.isSnapshotMavenMetadata("path/1.0-SNAPSHOT/other.metadata.xml"), "Not maven metadata");
        assertFalse(MavenNaming.isSnapshotMavenMetadata("path/1.0-SNAPSHOT/resource#other.metadata.xml"),
                "Not maven metadata");
        assertFalse(MavenNaming.isSnapshotMavenMetadata("path/1.0-SNAPSHOT"), "Not metadata path");
        assertFalse(MavenNaming.isSnapshotMavenMetadata("path/1.0-SNAPSHOT/"), "Not metadata path");
        assertFalse(MavenNaming.isSnapshotMavenMetadata("path/1.0-SNAPSHOT/#matadata-name"), "Not maven metadata");
    }
}
