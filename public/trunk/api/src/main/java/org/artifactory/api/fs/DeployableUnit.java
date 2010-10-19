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

import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.repo.RepoPath;

import java.io.Serializable;

/**
 * Deployable unit represents a group of deployed files. For maven it is the version path.
 *
 * @author Yossi Shaul
 */
public class DeployableUnit implements Serializable {

    private final RepoPath repoPath;
    private final MavenArtifactInfo mavenInfo;

    public DeployableUnit(RepoPath repoPath, MavenArtifactInfo mavenInfo) {
        this.repoPath = repoPath;
        this.mavenInfo = mavenInfo;
    }

    public DeployableUnit(RepoPath repoPath) {
        this.repoPath = repoPath;
        String relPath = repoPath.getPath();

        // TODO: why aren't we using MavenArtifactInfo.fromRepoPath(repoPath)?

        // split the following format:
        // /group/id/artifactId/version
        String[] pathElements = relPath.split("\\/");

        if (pathElements.length < 3) {
            throw new IllegalArgumentException("Not a valid deployable unit path" + relPath);
        }
        String version = pathElements[pathElements.length - 1];
        String artifactId = pathElements[pathElements.length - 2];

        int groupStart = 0;
        int groupEnd = pathElements.length - 2;
        StringBuffer groupIdBuff = new StringBuffer();
        for (int i = groupStart; i < groupEnd; i++) {
            groupIdBuff.append(pathElements[i]);
            if (i < groupEnd - 1) {// not the last path element
                groupIdBuff.append('.');
            }
        }
        String groupId = groupIdBuff.toString();
        mavenInfo = new MavenArtifactInfo(groupId, artifactId, version);
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public MavenArtifactInfo getMavenInfo() {
        return mavenInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeployableUnit that = (DeployableUnit) o;

        if (!repoPath.equals(that.repoPath)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return repoPath.hashCode();
    }
}
