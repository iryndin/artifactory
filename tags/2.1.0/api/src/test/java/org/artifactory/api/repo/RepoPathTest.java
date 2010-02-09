/*
 * This file is part of Artifactory.
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

package org.artifactory.api.repo;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the RepoPath class.
 *
 * @author Yossi Shaul
 */
@Test
public class RepoPathTest {

    public void getParentRepoPathWithParent() {
        RepoPath child = new RepoPath("repo", "a/b/c");
        RepoPath parent = child.getParent();
        Assert.assertEquals(parent, new RepoPath("repo", "/a/b"));
    }

    public void getParentRepoPathLastParent() {
        RepoPath child = new RepoPath("repo", "a/");
        RepoPath parent = child.getParent();
        Assert.assertEquals(parent, new RepoPath("repo", ""));
    }

    public void getParentRepoPathForRoot() {
        RepoPath child = new RepoPath("repo", "/");
        RepoPath parent = child.getParent();
        Assert.assertNull(parent);
    }

    public void getParentRepoPathWithNoParent() {
        RepoPath child = new RepoPath("repo", "");
        RepoPath parent = child.getParent();
        Assert.assertNull(parent);
    }

}
