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

package org.artifactory.repo.snapshot;

import org.artifactory.api.repo.RepoPath;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for {@link NonUniqueSnapshotVersionAdapter}.<p/>
 * Only the easy to unit test are here, the rest are in the integration tests.
 *
 * @author Yossi Shaul
 */
@Test
public class NonUniqueSnapshotVersionAdapterTest extends ArtifactoryHomeBoundTest {
    private NonUniqueSnapshotVersionAdapter nonUniqueAdapter = new NonUniqueSnapshotVersionAdapter();

    private final String SNAPSHOT_PATH = "groupId/artifactId/1.4-SNAPSHOT/";

    public void uniqueArtifact() {
        String uniqueVersionFile = "artifactId-1.4-20081214.090217-4.jar";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + "artifactId-1.4-SNAPSHOT.jar");
    }

    public void uniqueArtifactWithClassifier() {
        String uniqueVersionFile = "artifactId-1.4-20081214.090217-4-classifier.jar";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + "artifactId-1.4-SNAPSHOT-classifier.jar");
    }

    public void uniqueArtifactWithComplexClassifier() {
        String uniqueVersionFile = "artifactId-1.4-20081214.090217-4-a-2complex-classifier.jar";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + "artifactId-1.4-SNAPSHOT-a-2complex-classifier.jar");
    }

    public void alreadyNonUniqueArtifact() {
        String nonUniqueVersionFile = "artifactId-1.4-SNAPSHOT.jar";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + nonUniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + "artifactId-1.4-SNAPSHOT.jar");
    }

    public void uniqueChecksumArtifact() {
        String uniqueVersionFile = "artifactId-1.4-20081214.090217-4.jar.sha1";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + "artifactId-1.4-SNAPSHOT.jar.sha1");
    }

    public void alreadyNonUniqueChecksumArtifact() {
        String nonUniqueVersionFile = "artifactId-1.4-SNAPSHOT.jar.md5";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + nonUniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + nonUniqueVersionFile);
    }

    public void uniqueArtifactWithDifferentFileName() {
        String uniqueVersionFile = "different-1.4-20081214.090217-4.jar";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + "different-1.4-SNAPSHOT.jar", "When passing file with file name " +
                "other than the artifact id, only the version part should be affected, all the other should be the same");
    }

    public void nonUniqueArtifactWithDifferentFileName() {
        String nonUniqueVersionFile = "different-1.4-SNAPSHOT.jar";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + nonUniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + nonUniqueVersionFile, "When passing file with file name " +
                "other than the artifact id, only the version part should be affected, all the other should be the same");
    }

    public void nonUniqueArtifactWithDifferentVersion() {
        String nonUniqueVersionFile = "artifactId-1.5-SNAPSHOT.jar";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + nonUniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + nonUniqueVersionFile, "When passing file with version " +
                "other than the snapshot version from the path, nothing should be affected");
    }

    public void uniqueArtifactWithDifferentVersion() {
        String uniqueVersionFile = "artifactId-1.5-20101014.090217-2.pom";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + uniqueVersionFile, "When passing file with version " +
                "other than the snapshot version from the path, nothing should be affected");
    }

    public void artifactWithReleaseVersion() {
        String uniqueVersionFile = "artifactId-1.4.ivy";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + uniqueVersionFile, "Files with release version should not be affected");
    }

    public void artifactWithNoMavenStructure() {
        String nonMavenFile = "blabla.xml";
        String result = nonUniqueAdapter.adaptSnapshotPath(new RepoPath("local", SNAPSHOT_PATH + nonMavenFile));
        assertEquals(result, SNAPSHOT_PATH + nonMavenFile, "Non-maven structured files with release version " +
                "should not be affected");
    }

}
