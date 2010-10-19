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

import org.apache.commons.io.FilenameUtils;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

/**
 * This snapshot version adapter changes unique snapshot versions to non-unique.
 *
 * @author Yossi Shaul
 */
public class NonUniqueSnapshotVersionAdapter extends SnapshotVersionAdapterBase {
    private static final Logger log = LoggerFactory.getLogger(NonUniqueSnapshotVersionAdapter.class);

    public String adaptSnapshotPath(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (!isApplicableOn(path)) {
            return path;
        }

        String fileName = PathUtils.getName(path);
        if (!MavenNaming.isUniqueSnapshotFileName(fileName)) {
            log.debug("File '{}' is not a unique snapshot version. Returning the original path.", fileName);
            return path;
        }

        MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(repoPath);
        String pathBaseVersion = MavenNaming.getNonUniqueSnapshotBaseVersion(mavenInfo.getVersion());
        String timestampAndBuildNumber = MavenNaming.getUniqueSnapshotVersionTimestampAndBuildNumber(fileName);
        if (!fileName.contains(pathBaseVersion + "-" + timestampAndBuildNumber)) {
            log.debug("File '{}' version is not equals to the path base version '{}'. " +
                    "Returning the original path.", fileName, pathBaseVersion);
            return path;
        }

        // replace the timestamp and build number part with 'SNAPSHOT' string
        String adaptedFileName = fileName.replace(timestampAndBuildNumber, MavenNaming.SNAPSHOT);
        String adaptedPath = FilenameUtils.getPath(path) + adaptedFileName;
        return adaptedPath;
    }

}
