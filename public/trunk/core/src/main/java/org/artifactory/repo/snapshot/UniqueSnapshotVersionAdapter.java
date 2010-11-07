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
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.MavenModelUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.resource.ArtifactResource;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * This snapshot version adapter changes non-unique snapshot versions to unique.
 *
 * @author Yossi Shaul
 */
public class UniqueSnapshotVersionAdapter extends SnapshotVersionAdapterBase {
    private static final Logger log = LoggerFactory.getLogger(UniqueSnapshotVersionAdapter.class);

    public String adaptSnapshotPath(RepoPath repoPath) {
        String path = repoPath.getPath();
        if (!isApplicableOn(path)) {
            return path;
        }

        String fileName = PathUtils.getName(path);
        if (MavenNaming.isUniqueSnapshotFileName(fileName)) {
            log.debug("File '{}' has already a unique snapshot version. Returning the original path.", fileName);
            return path;
        }

        MavenArtifactInfo mavenInfo = ArtifactResource.getMavenInfo(repoPath);
        String pathBaseVersion = MavenNaming.getNonUniqueSnapshotBaseVersion(mavenInfo.getVersion());
        if (!fileName.contains(pathBaseVersion + "-" + MavenNaming.SNAPSHOT)) {
            log.debug("File '{}' doesn't contain the non-unique snapshot version {}. " +
                    "Returning the original path.", fileName, pathBaseVersion);
            return path;
        }

        // Replace 'SNAPSHOT' with version timestamp for unique snapshot repo
        return adjustNonUniqueSnapshotToUnique(repoPath, mavenInfo);
    }

    private String adjustNonUniqueSnapshotToUnique(RepoPath repoPath, MavenArtifactInfo mavenInfo) {
        //Get the latest build number from the metadata
        String filePath = repoPath.getPath();
        int metadataBuildNumber = getLastBuildNumber(repoPath);
        int nextBuildNumber = metadataBuildNumber + 1;

        RepoPath parentPath = repoPath.getParent();

        // determine if the next build number should be the one read from the metadata
        String classifier = mavenInfo.getClassifier();
        boolean isPomChecksum = MavenNaming.isChecksum(filePath) && MavenNaming.isPom(
                PathUtils.stripExtension(filePath));
        if (metadataBuildNumber > 0 && (StringUtils.isNotBlank(classifier) || isPomChecksum)) {
            // pom checksums and artifacts with classifier are always deployed after the pom (which triggers the
            // maven-metadata.xml calculation) so use the metadata build number
            nextBuildNumber = metadataBuildNumber;
        }
        if (metadataBuildNumber > 0 && MavenNaming.isPom(filePath)) {
            // the metadata might already represent an existing main artifact (in case the
            // maven-metadata.xml was deployed after the main artifact and before the pom/classifier)
            // so first check if there's already a file with the next build number
            if (getSnapshotFile(parentPath, metadataBuildNumber + 1) == null) {
                // no files deployed with the next build number so either this is a pom deployment (parent pom)
                // or this is a pom of a main artifact for which the maven-metadata.xml was deployed before this pom
                if (getSnapshotPomFile(parentPath, metadataBuildNumber) == null) {
                    // this is a pom attached to a main artifact deployed after maven-metadata.xml
                    nextBuildNumber = metadataBuildNumber;
                }
            }
        }

        String timestamp = getSnapshotTimestamp(parentPath, nextBuildNumber);
        if (timestamp == null) {
            // probably the first deployed file for this build, use now for the timestamp
            timestamp = MavenModelUtils.dateToUniqueSnapshotTimestamp(new Date());
        }

        // replace the SNAPSHOT string with timestamp-buildNumber
        String fileName = PathUtils.getName(filePath);
        String adaptedFileName = fileName.replace(MavenNaming.SNAPSHOT, timestamp + "-" + nextBuildNumber);
        return FilenameUtils.getPath(filePath) + adaptedFileName;
    }

    /**
     * @return The last build number for snapshot version. 0 if maven-metadata not found for the path.
     */
    private int getLastBuildNumber(RepoPath repoPath) {
        int buildNumber = 0;
        try {
            // get the parent path which should contains the maven-metadata
            RepoPath parentRepoPath = repoPath.getParent();
            RepositoryService repoService = ContextHelper.get().getRepositoryService();
            String mavenMetadataStr = null;
            if (repoService.exists(parentRepoPath) &&
                    repoService.hasMetadata(parentRepoPath, MavenNaming.MAVEN_METADATA_NAME)) {
                mavenMetadataStr = repoService.getXmlMetadata(parentRepoPath, MavenNaming.MAVEN_METADATA_NAME);
            }
            if (mavenMetadataStr != null) {
                Metadata metadata = MavenModelUtils.toMavenMetadata(mavenMetadataStr);
                Versioning versioning = metadata.getVersioning();
                if (versioning != null) {
                    Snapshot snapshot = versioning.getSnapshot();
                    if (snapshot != null) {
                        buildNumber = snapshot.getBuildNumber();
                    }
                }
            } else {
                // ok probably not found. just log
                log.debug("No maven metadata found for {}.", repoPath);
            }
        } catch (Exception e) {
            log.error("Cannot obtain build number from metadata.", e);
        }
        return buildNumber;
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The path of the first unique snapshot file with the input build number.
     */
    private String getSnapshotFile(RepoPath snapshotDirectoryPath, int buildNum) {
        return getSnapshotFile(snapshotDirectoryPath, buildNum, null);
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The path of the unique snapshot pom file with the input build number.
     */
    private String getSnapshotPomFile(RepoPath snapshotDirectoryPath, int buildNum) {
        return getSnapshotFile(snapshotDirectoryPath, buildNum, "pom");
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @param fileType              The file type to search for. Use null for any type
     * @return The path of the first unique snapshot file with the input build number.
     */
    private String getSnapshotFile(RepoPath snapshotDirectoryPath, int buildNum, String fileType) {
        log.debug("Searching for unique snapshot file in {} with build number {}",
                snapshotDirectoryPath, buildNum);
        RepositoryService repoService = ContextHelper.get().getRepositoryService();
        if (repoService.exists(snapshotDirectoryPath)) {
            List<String> children = repoService.getChildrenNames(snapshotDirectoryPath);
            for (String child : children) {
                if ((fileType == null || fileType.equals(PathUtils.getExtension(child))) &&
                        //In all cases - child must be a unique snapshot for the build number extraction
                        MavenNaming.isUniqueSnapshotFileName(child)) {
                    int childBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(child);
                    if (childBuildNumber == buildNum) {
                        log.debug("Found unique snapshot with build number {}: {}", buildNum, child);
                        return child;
                    }
                }
            }
        }
        log.debug("Unique snapshot file not found in {} for build number {}", snapshotDirectoryPath, buildNum);
        return null;
    }

    /**
     * @param snapshotDirectoryPath Path to a repository snapshot directory (eg, /a/path/1.0-SNAPSHOT)
     * @param buildNum              The file with build number to search for
     * @return The timestamp of the unique snapshot file with the input build number.
     */
    private String getSnapshotTimestamp(RepoPath snapshotDirectoryPath, int buildNum) {
        String snapshotFile = getSnapshotFile(snapshotDirectoryPath, buildNum);
        if (snapshotFile != null) {
            int childBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(snapshotFile);
            if (childBuildNumber == buildNum) {
                String timestamp = MavenNaming.getUniqueSnapshotVersionTimestamp(snapshotFile);
                log.debug("Extracted timestamp {} from {}", timestamp, snapshotFile);
                return timestamp;
            }
        }
        log.debug("Snapshot timestamp not found in {} for build number {}", snapshotDirectoryPath, buildNum);
        return null;
    }

}
