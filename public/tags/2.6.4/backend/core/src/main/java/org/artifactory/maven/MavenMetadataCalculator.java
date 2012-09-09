/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.common.MultiStatusHolder;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.module.ModuleInfo;
import org.artifactory.api.module.ModuleInfoUtils;
import org.artifactory.common.ConstantValues;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrTreeNode;
import org.artifactory.jcr.fs.JcrTreeNodeFileFilter;
import org.artifactory.jcr.lock.LockingHelper;
import org.artifactory.jcr.lock.aop.LockingAdvice;
import org.artifactory.log.LoggerFactory;
import org.artifactory.maven.versioning.MavenMetadataVersionComparator;
import org.artifactory.maven.versioning.VersionNameMavenMetadataVersionComparator;
import org.artifactory.mime.MavenNaming;
import org.artifactory.repo.RepoPath;
import org.artifactory.util.RepoLayoutUtils;
import org.slf4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    public JcrTreeNode calculate(RepoPath folder, MultiStatusHolder status) {
        log.debug("Calculating maven metadata recursively on '{}'", folder);

        JcrTreeNode rootNode = getJcrService().getTreeNode(folder, status, new JcrTreeNodeFileFilter() {
            @Override
            public boolean acceptsFile(RepoPath repoPath) {
                String path = repoPath.getPath();
                return MavenNaming.isPom(path) || MavenNaming.isUniqueSnapshot(path);
            }
        });

        if (rootNode != null) {
            calculateAndSet(rootNode, status);
            log.debug("Finished maven metadata calculation on '{}'", folder);
        }
        return rootNode;
    }

    private void calculateAndSet(JcrTreeNode treeNode, MultiStatusHolder status) {
        if (!treeNode.isFolder()) {
            // Nothing to do here for non folder tree node
            return;
        }

        RepoPath repoPath = treeNode.getRepoPath();
        if (repoPath == null) {
            // since the JcrTreeNode is detached form the database and holds no locks, it might happen that a node
            // is in the trash in time of construction and hence the null repo path (see RTFACT-5053)
            log.debug("Null repo path for node with children: {}", treeNode.getChildren());
            return;
        }

        String nodePath = repoPath.getPath();
        boolean containsMetadataInfo;
        if (MavenNaming.isSnapshot(nodePath)) {
            // if this folder contains snapshots create snapshots maven.metadata
            log.trace("Detected snapshots container: {}", nodePath);
            containsMetadataInfo = createSnapshotsMetadata(repoPath, treeNode, status);
        } else {
            // if this folder contains "version folders" create versions maven metadata
            List<JcrTreeNode> subFoldersContainingPoms = getSubFoldersContainingPoms(treeNode);
            if (!subFoldersContainingPoms.isEmpty()) {
                log.trace("Detected versions container: {}", repoPath.getId());
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
        if (treeNode.isFolder()) {
            Set<JcrTreeNode> children = treeNode.getChildren();
            if (children != null) {
                for (JcrTreeNode child : children) {
                    calculateAndSet(child, status);
                }
            }
        }
    }

    private boolean createSnapshotsMetadata(RepoPath repoPath, JcrTreeNode treeNode, BasicStatusHolder status) {
        if (!folderContainsPoms(treeNode)) {
            return false;
        }
        Set<JcrTreeNode> folderItems = treeNode.getChildren();
        Iterable<JcrTreeNode> poms = Iterables.filter(folderItems, new Predicate<JcrTreeNode>() {
            @Override
            public boolean apply(@Nullable JcrTreeNode input) {
                return (input != null) && MavenNaming.isPom(input.getName());
            }
        });

        RepoPath firstPom = poms.iterator().next().getRepoPath();
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
        String latestUniquePom = getLatestUniqueSnapshotPomName(poms);
        if (snapshotBehavior.equals(SnapshotVersionBehavior.NONUNIQUE) ||
                (snapshotBehavior.equals(SnapshotVersionBehavior.DEPLOYER) && latestUniquePom == null)) {
            snapshot.setBuildNumber(1);
        } else if (snapshotBehavior.equals(SnapshotVersionBehavior.UNIQUE)) {
            // take the latest unique snapshot file file
            if (latestUniquePom != null) {
                snapshot.setBuildNumber(MavenNaming.getUniqueSnapshotVersionBuildNumber(latestUniquePom));
                snapshot.setTimestamp(MavenNaming.getUniqueSnapshotVersionTimestamp(latestUniquePom));
            }

            if (ConstantValues.mvnMetadataVersion3Enabled.getBoolean()) {
                List<SnapshotVersion> snapshotVersions = Lists.newArrayList(getFolderItemSnapshotVersions(folderItems));
                if (!snapshotVersions.isEmpty()) {
                    versioning.setSnapshotVersions(snapshotVersions);
                }
            }
        }
        saveMetadata(repoPath, metadata, status);
        return true;
    }

    private Collection<SnapshotVersion> getFolderItemSnapshotVersions(Set<JcrTreeNode> folderItems) {
        List<SnapshotVersion> snapshotVersionsToReturn = Lists.newArrayList();

        Map<SnapshotVersionType, ModuleInfo> latestSnapshotVersions = Maps.newHashMap();

        for (JcrTreeNode folderItem : folderItems) {
            String folderItemPath = folderItem.getRepoPath().getPath();
            if (MavenNaming.isUniqueSnapshot(folderItemPath)) {
                ModuleInfo folderItemModuleInfo;
                if (MavenNaming.isPom(folderItemPath)) {
                    folderItemModuleInfo = ModuleInfoUtils.moduleInfoFromDescriptorPath(folderItemPath,
                            RepoLayoutUtils.MAVEN_2_DEFAULT);
                } else {
                    folderItemModuleInfo = ModuleInfoUtils.moduleInfoFromArtifactPath(folderItemPath,
                            RepoLayoutUtils.MAVEN_2_DEFAULT);
                }
                if (!folderItemModuleInfo.isValid() || !folderItemModuleInfo.isIntegration()) {
                    continue;
                }
                SnapshotVersionType folderItemSnapshotVersionType = new SnapshotVersionType(
                        folderItemModuleInfo.getExt(), folderItemModuleInfo.getClassifier());
                if (latestSnapshotVersions.containsKey(folderItemSnapshotVersionType)) {
                    int folderItemBuildNumber = Integer.parseInt(StringUtils.substringAfter(
                            folderItemModuleInfo.getFileIntegrationRevision(), "-"));
                    ModuleInfo latestSnapshotVersion = latestSnapshotVersions.get(folderItemSnapshotVersionType);
                    int latestSnapshotVersionBuildNumber = Integer.parseInt(StringUtils.substringAfter(
                            latestSnapshotVersion.getFileIntegrationRevision(), "-"));
                    if (folderItemBuildNumber > latestSnapshotVersionBuildNumber) {
                        latestSnapshotVersions.put(folderItemSnapshotVersionType, folderItemModuleInfo);
                    }
                } else {
                    latestSnapshotVersions.put(folderItemSnapshotVersionType, folderItemModuleInfo);
                }
            }
        }

        for (ModuleInfo latestSnapshotVersion : latestSnapshotVersions.values()) {
            SnapshotVersion snapshotVersion = new SnapshotVersion();
            snapshotVersion.setClassifier(latestSnapshotVersion.getClassifier());
            snapshotVersion.setExtension(latestSnapshotVersion.getExt());

            String fileItegRev = latestSnapshotVersion.getFileIntegrationRevision();
            snapshotVersion.setVersion(latestSnapshotVersion.getBaseRevision() + "-" + fileItegRev);
            snapshotVersion.setUpdated(StringUtils.remove(StringUtils.substringBefore(fileItegRev, "-"), '.'));
            snapshotVersionsToReturn.add(snapshotVersion);
        }

        return snapshotVersionsToReturn;
    }

    private void createVersionsMetadata(RepoPath repoPath, List<JcrTreeNode> versionNodes,
            MultiStatusHolder status) {
        // get artifact info from the first pom

        RepoPath samplePomRepoPath = getFirstPom(versionNodes);
        if (samplePomRepoPath == null) {
            //Should never really be null, we've checked the list of version nodes for poms before passing it into here
            return;
        }
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(samplePomRepoPath);
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifactInfo.getGroupId());
        metadata.setArtifactId(artifactInfo.getArtifactId());
        metadata.setVersion(artifactInfo.getVersion());
        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        versioning.setLastUpdatedTimestamp(new Date());

        MavenMetadataVersionComparator comparator = createVersionComparator();
        TreeSet<JcrTreeNode> sortedVersions = Sets.newTreeSet(comparator);
        sortedVersions.addAll(versionNodes);

        // add the versions to the versioning section
        for (JcrTreeNode sortedVersion : sortedVersions) {
            versioning.addVersion(sortedVersion.getName());
        }

        // latest is simply the last (be it snapshot or release version)
        String latestVersion = sortedVersions.last().getName();
        versioning.setLatest(latestVersion);

        // release is the latest non snapshot version
        for (JcrTreeNode sortedVersion : sortedVersions) {
            String versionNodeName = sortedVersion.getName();
            if (!MavenNaming.isSnapshot(versionNodeName)) {
                versioning.setRelease(versionNodeName);
            }
        }

        saveMetadata(repoPath, metadata, status);
    }

    private RepoPath getFirstPom(List<JcrTreeNode> versionNodes) {
        for (JcrTreeNode versionNode : versionNodes) {
            for (JcrTreeNode jcrTreeNode : versionNode.getChildren()) {
                if (MavenNaming.isPom(jcrTreeNode.getName())) {
                    return jcrTreeNode.getRepoPath();
                }
            }
        }

        return null;
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

    private String getLatestUniqueSnapshotPomName(Iterable<JcrTreeNode> poms) {
        String latestUniquePom = null;
        int latest = 0;

        for (JcrTreeNode pom : poms) {
            String pomName = pom.getName();
            if (MavenNaming.isUniqueSnapshotFileName(pomName)) {
                int currentBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(pomName);
                if (currentBuildNumber >= latest) {
                    latest = currentBuildNumber;
                    latestUniquePom = pomName;
                }
            }
        }

        return latestUniquePom;
    }

    private List<JcrTreeNode> getSubFoldersContainingPoms(JcrTreeNode treeNode) {
        List<JcrTreeNode> result = Lists.newArrayList();

        if (treeNode.isFolder()) {
            Set<JcrTreeNode> children = treeNode.getChildren();

            for (JcrTreeNode child : children) {
                if (folderContainsPoms(child)) {
                    result.add(child);
                }
            }
        }

        return result;
    }

    private boolean folderContainsPoms(JcrTreeNode treeNode) {
        if (!treeNode.isFolder()) {
            return false;
        }

        Set<JcrTreeNode> children = treeNode.getChildren();
        for (JcrTreeNode child : children) {
            if (!child.isFolder() && MavenNaming.isPom(child.getName())) {
                return true;
            }
        }

        return false;
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

    private static class SnapshotVersionType {

        private String extension;
        private String classifier;

        private SnapshotVersionType(String extension, String classifier) {
            this.extension = extension;
            this.classifier = classifier;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SnapshotVersionType)) {
                return false;
            }

            SnapshotVersionType that = (SnapshotVersionType) o;

            if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null) {
                return false;
            }
            if (extension != null ? !extension.equals(that.extension) : that.extension != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = extension != null ? extension.hashCode() : 0;
            result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
            return result;
        }
    }
}
