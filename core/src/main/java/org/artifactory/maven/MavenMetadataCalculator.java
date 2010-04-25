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

package org.artifactory.maven;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.FolderTreeNode;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Calculates maven metadata recursively for folder in a local non-cache repository. Plugins metadata is calculated for
 * the whole repository.
 *
 * @author Yossi Shaul
 */
public class MavenMetadataCalculator extends AbstractMetadataCalculator {
    private static final Logger log = LoggerFactory.getLogger(MavenMetadataCalculator.class);

    /**
     * Calculate maven metadata using folder as the base.
     *
     * @param folder Base folder to start calculating from.
     * @param status Status holder.
     * @return true if some pom are maven plugin type, false if no maven plugin pom exists under the folder
     */
    public boolean calculate(RepoPath folder, StatusHolder status) {
        log.debug("Calculating maven metadata recursively on '{}'", folder);
        FolderTreeNode folders = getJcrService().getFolderTreeNode(folder, status);
        if (folders != null) {
            calculateAndSet(folder, folders, status);
            log.debug("Finished maven metadata calculation on '{}'", folder);
            return folders.hasMavenPlugins;
        }
        return false;
    }

    private void calculateAndSet(RepoPath repoPath, FolderTreeNode folder, StatusHolder status) {
        String nodePath = repoPath.getPath();
        boolean containsMetadataInfo;
        if (MavenNaming.isSnapshot(nodePath)) {
            // if this folder contains snapshots create snapshots maven.metadata
            log.trace("Detected snapshots container: {}", nodePath);
            containsMetadataInfo = createSnapshotsMetadata(repoPath, folder, status);
        } else {
            // if this folder contains "version folders" create versions maven metadata
            List<FolderTreeNode> subFoldersContainingPoms = getSubFoldersContainingPoms(folder);
            if (!subFoldersContainingPoms.isEmpty()) {
                log.trace("Detected versions container: {}", folder);
                createVersionsMetadata(repoPath, subFoldersContainingPoms, status);
                containsMetadataInfo = true;
            } else {
                containsMetadataInfo = false;
            }
        }

        if (!containsMetadataInfo) {
            removeMetadataIfExist(repoPath, status);
        }

        // Recursive call to calculate and set
        for (FolderTreeNode childFolder : folder.folders) {
            calculateAndSet(new RepoPath(repoPath, childFolder.name), childFolder, status);
        }
    }

    private boolean createSnapshotsMetadata(RepoPath repoPath, FolderTreeNode folder, StatusHolder status) {
        if (folder.poms.length == 0) {
            return false;
        }
        RepoPath firstPom = new RepoPath(repoPath, folder.poms[0]);
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(firstPom);
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifactInfo.getGroupId());
        metadata.setArtifactId(artifactInfo.getArtifactId());
        metadata.setVersion(artifactInfo.getVersion());
        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        versioning.setLastUpdatedTimestamp(new Date());
        Snapshot snapshot = new Snapshot();
        versioning.setSnapshot(snapshot);

        LocalRepoDescriptor localRepoDescriptor =
                getRepositoryService().localOrCachedRepoDescriptorByKey(repoPath.getRepoKey());
        SnapshotVersionBehavior snapshotBehavior = localRepoDescriptor.getSnapshotVersionBehavior();
        String latestUniquePom = getLatestUniqueSnapshotPom(folder.poms);
        if (snapshotBehavior.equals(SnapshotVersionBehavior.NONUNIQUE) ||
                (snapshotBehavior.equals(SnapshotVersionBehavior.DEPLOYER) && latestUniquePom == null)) {
            snapshot.setBuildNumber(1);
        } else if (snapshotBehavior.equals(SnapshotVersionBehavior.UNIQUE)) {
            // take the latest unique snapshot file file
            if (latestUniquePom != null) {
                snapshot.setBuildNumber(MavenNaming.getUniqueSnapshotVersionBuildNumber(latestUniquePom));
                snapshot.setTimestamp(MavenNaming.getUniqueSnapshotVersionTimestamp(latestUniquePom));
            }
        }
        saveMetadata(repoPath, metadata, status);
        return true;
    }

    private void createVersionsMetadata(RepoPath repoPath,
            List<FolderTreeNode> subFoldersContainingPoms, StatusHolder status) {
        // get artifact info from the first pom
        FolderTreeNode firstSubFolder = subFoldersContainingPoms.get(0);
        RepoPath firstSubRepoPath = new RepoPath(repoPath, firstSubFolder.name);
        String samplePom = firstSubFolder.poms[0];
        RepoPath samplePomRepoPath = new RepoPath(firstSubRepoPath, samplePom);
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(samplePomRepoPath);
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifactInfo.getGroupId());
        metadata.setArtifactId(artifactInfo.getArtifactId());
        metadata.setVersion(artifactInfo.getVersion());
        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        versioning.setLastUpdatedTimestamp(new Date());

        // add the different versions and set the release to be the latest release version (if exists) 
        for (FolderTreeNode pomFilesContainer : subFoldersContainingPoms) {
            String version = pomFilesContainer.name;
            versioning.addVersion(version);
            if (!MavenNaming.isSnapshot(version)) {
                versioning.setRelease(version);
            }
        }

        // the latest is the last folder
        versioning.setLatest(subFoldersContainingPoms.get(subFoldersContainingPoms.size() - 1).name);

        saveMetadata(repoPath, metadata, status);
    }

    private String getLatestUniqueSnapshotPom(String[] poms) {
        for (int i = poms.length - 1; i >= 0; i--) {
            String pomName = poms[i];
            if (MavenNaming.isUniqueSnapshotFileName(pomName)) {
                return pomName;
            }
        }
        return null;
    }

    private List<FolderTreeNode> getSubFoldersContainingPoms(FolderTreeNode parent) {
        List<FolderTreeNode> result = new ArrayList<FolderTreeNode>();
        for (FolderTreeNode item : parent.folders) {
            if (item.poms.length > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private void removeMetadataIfExist(RepoPath repoPath, StatusHolder status) {
        try {
            // Write lock auto upgrade supported LockingHelper.releaseReadLock(repoPath);
            getRepositoryService().removeMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME);
        } catch (Exception e) {
            status.setError("Error while removing metadata of folder " + repoPath + ".", e, log);
        }
    }

}
