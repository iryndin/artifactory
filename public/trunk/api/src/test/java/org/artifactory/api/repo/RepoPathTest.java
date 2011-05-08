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

package org.artifactory.api.repo;

import org.artifactory.repo.RepoPath;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests the RepoPath class.
 *
 * @author Yossi Shaul
 */
@Test
public class RepoPathTest {

    public void getParentRepoPathWithParent() {
        RepoPath child = new RepoPathImpl("repo", "a/b/c");
        RepoPath parent = child.getParent();
        assertEquals(parent, new RepoPathImpl("repo", "/a/b"));
    }

    public void getParentRepoPathLastParent() {
        RepoPath child = new RepoPathImpl("repo", "a/");
        RepoPath parent = child.getParent();
        assertEquals(parent, new RepoPathImpl("repo", ""));
    }

    public void getParentRepoPathForRoot() {
        RepoPath child = new RepoPathImpl("repo", "/");
        RepoPath parent = child.getParent();
        assertNull(parent);
    }

    public void getParentRepoPathWithNoParent() {
        RepoPath child = new RepoPathImpl("repo", "");
        RepoPath parent = child.getParent();
        assertNull(parent);
    }

    public void repoRootPath() {
        RepoPath repoPath = RepoPathImpl.repoRootPath("repokey");
        assertEquals(repoPath.getRepoKey(), "repokey");
        assertEquals("", repoPath.getPath(), "Repository root path should be an empty string");
        assertTrue(repoPath.isRoot());
    }

    public void rootPath() {
        assertFalse(new RepoPathImpl("1", "2").isRoot());
        assertTrue(new RepoPathImpl("1", "").isRoot());
        assertTrue(new RepoPathImpl("1", "     ").isRoot());
        assertTrue(new RepoPathImpl("1", null).isRoot());
    }
}
