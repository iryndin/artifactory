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

import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.test.ArtifactoryHomeBoundTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Unit tests for {@link UniqueSnapshotVersionAdapter}.<p/> Only the easy to unit test are here, the rest are in the
 * integration tests.
 *
 * @author Yossi Shaul
 */
@Test
public class UniqueSnapshotVersionAdapterTest extends ArtifactoryHomeBoundTest {
    private UniqueSnapshotVersionAdapter uniqueAdapter = new UniqueSnapshotVersionAdapter();

    private final String SNAPSHOT_PATH = "groupPart1/groupPart2/artifactId/2.5-SNAPSHOT/";

    public void alreadyUniqueArtifact() {
        String uniqueVersionFile = "artifactId-2.5-20071014.090200-4.jar";
        String result = uniqueAdapter.adaptSnapshotPath(new RepoPathImpl("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + uniqueVersionFile, "Unique snapshots shouldn't be touched");
    }

    public void alreadyUniqueArtifactWithClassifier() {
        String uniqueVersionFile = "artifactId-2.5-20071014.090200-4-classifier.jar";
        String result = uniqueAdapter.adaptSnapshotPath(new RepoPathImpl("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + uniqueVersionFile, "Unique snapshots shouldn't be touched");
    }

    public void alreadyUniqueChecksumArtifact() {
        String uniqueVersionFile = "artifactId-2.5-20071014.090200-4.jar.sha1";
        String result = uniqueAdapter.adaptSnapshotPath(new RepoPathImpl("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + uniqueVersionFile);
    }

    public void nonUniqueArtifactWithDifferentVersion() {
        String nonUniqueVersionFile = "artifactId-1.5-SNAPSHOT.jar";
        String result =
                uniqueAdapter.adaptSnapshotPath(new RepoPathImpl("local", SNAPSHOT_PATH + nonUniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + nonUniqueVersionFile, "When passing file with version " +
                "other than the snapshot version from the path, nothing should be affected");
    }

    public void artifactWithReleaseVersion() {
        String uniqueVersionFile = "artifactId-1.4.ivy";
        String result = uniqueAdapter.adaptSnapshotPath(new RepoPathImpl("local", SNAPSHOT_PATH + uniqueVersionFile));
        assertEquals(result, SNAPSHOT_PATH + uniqueVersionFile, "Files with release version should not be affected");
    }

    public void artifactWithNoMavenStructure() {
        String nonMavenFile = "blabla.xml";
        String result = uniqueAdapter.adaptSnapshotPath(new RepoPathImpl("local", SNAPSHOT_PATH + nonMavenFile));
        assertEquals(result, SNAPSHOT_PATH + nonMavenFile, "Non-maven structured files with release version " +
                "should not be affected");
    }

}
