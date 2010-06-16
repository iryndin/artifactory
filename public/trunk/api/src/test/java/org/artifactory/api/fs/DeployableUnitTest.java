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

package org.artifactory.api.fs;

import org.artifactory.api.repo.RepoPath;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * Tests the DeployableUnit.
 *
 * @author Yossi Shaul
 */
@Test
public class DeployableUnitTest {

    public void nodeConstructor() {
        RepoPath repoPath = new RepoPath("libs-releases", "/org/artifactory/core/5.6");
        DeployableUnit du = new DeployableUnit(repoPath);

        Assert.assertEquals(du.getRepoPath(), repoPath, "Unexpected repo path");
        Assert.assertEquals(du.getMavenInfo().getGroupId(), "org.artifactory",
                "Unexpected group id");
        Assert.assertEquals(du.getMavenInfo().getArtifactId(), "core", "Unexpected artifact id");
        Assert.assertEquals(du.getMavenInfo().getVersion(), "5.6", "Unexpected version");
        Assert.assertEquals(du.getRepoPath().getRepoKey(), "libs-releases", "Unexpected repoKey");
        Assert.assertEquals(du.getRepoPath().getPath(), "org/artifactory/core/5.6",
                "Unexpected repo path");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void invalidPath() {
        RepoPath repoPath = new RepoPath("libs-releases", "/core/5.6");
        new DeployableUnit(repoPath);
    }

}
