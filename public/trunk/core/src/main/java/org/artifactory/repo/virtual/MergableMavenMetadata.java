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

package org.artifactory.repo.virtual;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.fs.RepoResource;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.maven.versioning.MavenVersionComparator;
import org.artifactory.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
class MergableMavenMetadata {
    private Metadata metadata;
    private long lastModified;

    public Metadata getMetadata() {
        return metadata;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void merge(Metadata otherMetadata, RepoResource foundResource) {
        long otherLastModified = foundResource.getLastModified();
        if (metadata == null) {
            metadata = otherMetadata;
            lastModified = otherLastModified;
        } else {
            metadata.merge(otherMetadata);
            lastModified = Math.max(otherLastModified, lastModified);

            Versioning versioning = metadata.getVersioning();
            if (versioning != null) {
                List<String> versions = versioning.getVersions();
                if (!CollectionUtils.isNullOrEmpty(versions)) {
                    Collections.sort(versions, new MavenVersionComparator());
                    // latest is simply the last (be it snapshot or release version)
                    String latestVersion = versions.get(versions.size() - 1);
                    versioning.setLatest(latestVersion);

                    // release is the latest non snapshot version
                    for (String version : versions) {
                        if (!MavenNaming.isSnapshot(version)) {
                            versioning.setRelease(version);
                        }
                    }
                }

                // if there's a unique snapshot version prefer the one with the bigger build number
                Snapshot snapshot = versioning.getSnapshot();
                Snapshot otherSnapshot = otherMetadata.getVersioning() != null ?
                        otherMetadata.getVersioning().getSnapshot() : null;
                if (snapshot != null && otherSnapshot != null) {
                    if (snapshot.getBuildNumber() < otherSnapshot.getBuildNumber()) {
                        snapshot.setBuildNumber(otherSnapshot.getBuildNumber());
                        snapshot.setTimestamp(otherSnapshot.getTimestamp());
                    }
                }
            }
        }
    }
}
