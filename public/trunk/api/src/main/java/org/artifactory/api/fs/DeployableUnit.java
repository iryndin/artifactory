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

import org.artifactory.api.maven.MavenUnitInfo;
import org.artifactory.api.repo.RepoPath;

import java.io.Serializable;

/**
 * Deployable unit represents a group of deployed files. For maven it is the version path.
 *
 * @author Yossi Shaul
 */
public class DeployableUnit implements Serializable {

    private final RepoPath repoPath;
    private final MavenUnitInfo mavenInfo;

    public DeployableUnit(RepoPath repoPath, MavenUnitInfo mavenInfo) {
        this.repoPath = repoPath;
        this.mavenInfo = mavenInfo;
    }

    public DeployableUnit(RepoPath repoPath) {
        this.repoPath = repoPath;
        String relPath = repoPath.getPath();

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
        mavenInfo = new MavenUnitInfo(groupId, artifactId, version);
    }

    public RepoPath getRepoPath() {
        return repoPath;
    }

    public MavenUnitInfo getMavenInfo() {
        return mavenInfo;
    }
}
