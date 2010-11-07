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

package org.artifactory.resource;

import org.artifactory.api.fs.InternalFileInfo;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.repo.RepoPath;
import org.sonatype.nexus.index.context.IndexingContext;

/**
 * @author yoavl
 */
public class ArtifactResource extends FileResource {
    public static final String NEXUS_MAVEN_REPOSITORY_INDEX_ZIP = IndexingContext.INDEX_FILE + ".zip";
    public static final String NEXUS_MAVEN_REPOSITORY_INDEX_PROPERTIES =
            IndexingContext.INDEX_FILE + ".properties";

    private final MavenArtifactInfo mavenInfo;

    public ArtifactResource(RepoPath repoPath) {
        super(repoPath);
        mavenInfo = calcMavenInfo();
    }

    public ArtifactResource(InternalFileInfo info) {
        super(info);
        mavenInfo = calcMavenInfo();
    }

    public MavenArtifactInfo getMavenInfo() {
        return mavenInfo;
    }

    private MavenArtifactInfo calcMavenInfo() {
        String name = getInfo().getName();
        MavenArtifactInfo mavenInfo;
        if (!NEXUS_MAVEN_REPOSITORY_INDEX_ZIP.equals(name) &&
                !NEXUS_MAVEN_REPOSITORY_INDEX_PROPERTIES.equals(name)) {
            mavenInfo = MavenArtifactInfo.fromRepoPath(getRepoPath());
        } else {
            mavenInfo = new MavenArtifactInfo();
        }
        return mavenInfo;
    }

    public static MavenArtifactInfo getMavenInfo(RepoPath repoPath) {
        return new ArtifactResource(repoPath).getMavenInfo();
    }
}
