/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.maven.versioning;

import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.testng.annotations.Test;

import java.util.Calendar;

import static org.testng.Assert.assertEquals;

/**
 * Tests {@link org.artifactory.maven.versioning.VersionNameMavenMetadataVersionComparator}. Additional tests for
 * various version strings are in {@link MavenVersionComparatorTest}.
 *
 * @author Yossi Shaul
 */
@Test
public class MavenMetadataVersionComparatorTest {

    public void compare() {
        VersionNameMavenMetadataVersionComparator comparator = new VersionNameMavenMetadataVersionComparator();

        Calendar cal1 = Calendar.getInstance();
        JcrTreeNode older = new JcrTreeNode(new RepoPathImpl("repo", "2.0"), false, cal1, null);

        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.HOUR, 2);
        JcrTreeNode newer = new JcrTreeNode(new RepoPathImpl("repo", "1.1"), false, cal2, null);

        assertEquals(comparator.compare(older, newer), 1, "The comparison should be version name based");

    }
}
