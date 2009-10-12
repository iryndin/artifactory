package org.artifactory.api.fs;

import org.artifactory.api.repo.RepoPath;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.jcr.RepositoryException;

/**
 * Tests the DeployableUnit.
 *
 * @author Yossi Shaul
 */
@Test
public class DeployableUnitTest {

    public void nodeConstructor() throws RepositoryException {
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
    public void invalidPath() throws RepositoryException {
        RepoPath repoPath = new RepoPath("libs-releases", "/core/5.6");
        new DeployableUnit(repoPath);
    }

}
