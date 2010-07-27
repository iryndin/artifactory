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

import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.mime.NamingUtils;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.log.LoggerFactory;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

/**
 * Base abstract class for {@link SnapshotVersionAdapter}s.
 *
 * @author Yossi Shaul
 */
public abstract class SnapshotVersionAdapterBase implements SnapshotVersionAdapter {
    private static final Logger log = LoggerFactory.getLogger(SnapshotVersionAdapterBase.class);

    public static SnapshotVersionAdapter getByType(SnapshotVersionBehavior type) {
        switch (type) {
            case DEPLOYER:
                return new DeployerSnapshotVersionAdapter();
            case UNIQUE:
                return new UniqueSnapshotVersionAdapter();
            case NONUNIQUE:
                return new NonUniqueSnapshotVersionAdapter();
            default:
                throw new IllegalArgumentException("No snapshot version adapter found for type " + type);
        }
    }

    /**
     * Shared method for inheriting adapters to determine if a certain path is eligible for path adapters.
     *
     * @param path The path to check
     */
    protected boolean isApplicableOn(String path) {
        // don't modify metadata paths
        boolean metadataArtifact = NamingUtils.isMetadata(path) || NamingUtils.isMetadataChecksum(path);
        if (metadataArtifact) {
            log.debug("Not applying snapshot policy on metadata path: {}", path);
            return false;
        }

        // don't modify files that are not snapshots according to the file name (RTFACT-3049)
        String fileName = PathUtils.getName(path);
        boolean snapshotVersionFile = MavenNaming.isSnapshot(fileName) ||
                MavenNaming.isUniqueSnapshotFileName(fileName);
        if (!snapshotVersionFile) {
            log.debug("Not applying snapshot policy on non snapshot file: {}", path);
            return false;
        }

        MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(new RepoPath("repo", path));
        if (!mavenInfo.isValid()) {
            log.debug("{} is not a valid maven GAV path. Not applying snapshot policy.", path);
            return false;
        }

        return true;
    }

}
