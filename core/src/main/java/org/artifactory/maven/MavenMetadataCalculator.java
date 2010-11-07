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

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepoPathImpl;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.FolderTreeNode;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.versioning.MavenMetadataVersionComparator;
import org.artifactory.maven.versioning.VersionNameMavenMetadataVersionComparator;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

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
    public boolean calculate(RepoPath folder, BasicStatusHolder status) {
        log.debug("Calculating maven metadata recursively on '{}'", folder);
        FolderTreeNode folders = getJcrService().getFolderTreeNode(folder, status);
        if (folders != null) {
            calculateAndSet(folder, folders, status);
            log.debug("Finished maven metadata calculation on '{}'", folder);
            return folders.hasMavenPlugins;
        }
        return false;
    }

    private void calculateAndSet(RepoPath repoPath, FolderTreeNode folder, BasicStatusHolder status) {
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
            // note: this will also remove plugins metadata. not sure it should
            removeMetadataIfExist(repoPath, status);
        }

        // Recursive call to calculate and set
        for (FolderTreeNode childFolder : folder.folders) {
            calculateAndSet(new RepoPathImpl(repoPath, childFolder.name), childFolder, status);
        }
    }

    private boolean createSnapshotsMetadata(RepoPath repoPath, FolderTreeNode folder, BasicStatusHolder status) {
        if (folder.poms.length == 0) {
            return false;
        }
        RepoPath firstPom = new RepoPathImpl(repoPath, folder.poms[0]);
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
            List<FolderTreeNode> versionNodes, BasicStatusHolder status) {
        // get artifact info from the first pom
        FolderTreeNode firstSubFolder = versionNodes.get(0);
        RepoPath firstSubRepoPath = new RepoPathImpl(repoPath, firstSubFolder.name);
        String samplePom = firstSubFolder.poms[0];
        RepoPath samplePomRepoPath = new RepoPathImpl(firstSubRepoPath, samplePom);
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(samplePomRepoPath);
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifactInfo.getGroupId());
        metadata.setArtifactId(artifactInfo.getArtifactId());
        metadata.setVersion(artifactInfo.getVersion());
        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        versioning.setLastUpdatedTimestamp(new Date());

        MavenMetadataVersionComparator comparator = createVersionComparator();
        TreeSet<FolderTreeNode> sortedVersions = Sets.newTreeSet(comparator);
        sortedVersions.addAll(versionNodes);

        // add the versions to the versioning section
        for (FolderTreeNode version : sortedVersions) {
            versioning.addVersion(version.name);
        }

        // latest is simply the last (be it snapshot or release version)
        String latestVersion = sortedVersions.last().name;
        versioning.setLatest(latestVersion);

        // release is the latest non snapshot version
        for (FolderTreeNode versionNode : sortedVersions) {
            if (!MavenNaming.isSnapshot(versionNode.name)) {
                versioning.setRelease(versionNode.name);
            }
        }

        saveMetadata(repoPath, metadata, status);
    }

    private MavenMetadataVersionComparator createVersionComparator() {
        String comparatorFqn = ConstantValues.mvnMetadataVersionsComparator.getString();
        if (StringUtils.isBlank(comparatorFqn)) {
            // return the default comparator
            return VersionNameMavenMetadataVersionComparator.get();
        }

        try {
            Class<?> comparatorClass = Class.forName(comparatorFqn);
            return (MavenMetadataVersionComparator) comparatorClass.newInstance();
        } catch (Exception e) {
            log.warn("Failed to create custom maven metadata version comparator '{}': {}", comparatorFqn,
                    e.getMessage());
            return VersionNameMavenMetadataVersionComparator.get();
        }
    }

    private String getLatestUniqueSnapshotPom(String[] poms) {
        String latestUniquePom = null;
        int latest = 0;

        for (String pom : poms) {
            if (MavenNaming.isUniqueSnapshotFileName(pom)) {
                int currentBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(pom);
                if (currentBuildNumber >= latest) {
                    latest = currentBuildNumber;
                    latestUniquePom = pom;
                }
            }
        }

        return latestUniquePom;
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

    private void removeMetadataIfExist(RepoPath repoPath, BasicStatusHolder status) {
        try {
            if (getRepositoryService().hasMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME)) {
                // Write lock auto upgrade supported LockingHelper.releaseReadLock(repoPath);
                getRepositoryService().removeMetadata(repoPath, MavenNaming.MAVEN_METADATA_NAME);
                if (!TransactionSynchronizationManager.isSynchronizationActive() &&
                        LockingAdvice.getLockManager() != null) {
                    LockingHelper.removeLockEntry(repoPath);
                }
            }
        } catch (Exception e) {
            status.setError("Error while removing metadata of folder " + repoPath + ".", e, log);
        }
    }

}
