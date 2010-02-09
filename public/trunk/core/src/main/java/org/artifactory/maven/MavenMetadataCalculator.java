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
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
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
     */
    public void calculate(JcrFolder folder, StatusHolder status) {
        StoringRepo storingRepo = folder.getRepo();
        if (!storingRepo.isLocal() || storingRepo.isCache()) {
            throw new IllegalArgumentException(
                    "Maven metadata calculation is allowed only on local non-cache repositories");
        }

        log.debug("Calculating maven metadata recursively on '{}'", folder);
        processFolder(folder, status);
        log.debug("Finished maven metadata calculation on '{}'", folder);
    }

    /**
     * Scan folders recursively, postorder
     */
    private void processFolder(JcrFolder folder, StatusHolder status) {
        List<JcrFsItem> items = folder.getItems();
        for (JcrFsItem item : items) {
            if (item.isDirectory()) {
                processFolder((JcrFolder) item, status);
            }
        }

        boolean containsMetadataInfo = false;
        if (MavenNaming.isSnapshot(folder.getPath())) {
            // if this folder contains snapshots create snapshots maven.metadata
            log.trace("Detected snapshots container: {}", folder);
            createSnapshotsMetadata(folder);
            containsMetadataInfo = true;
        } else {
            // if this folder contains "version folders" create versions maven metadata
            List<JcrFolder> subFoldersContainingPoms = getSubFoldersContainingPoms(folder);
            if (!subFoldersContainingPoms.isEmpty()) {
                log.trace("Detected versions container: {}", folder);
                createVersionsMetadata(folder, subFoldersContainingPoms);
                containsMetadataInfo = true;
            }
        }

        if (!containsMetadataInfo) {
            removeMetadataIfExist(folder);
        }
    }

    private void createSnapshotsMetadata(JcrFolder folder) {
        List<JcrFile> poms = getPomItems(folder);
        if (poms.isEmpty()) {
            removeMetadataIfExist(folder);
            return;
        }
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(poms.get(0).getRepoPath());
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifactInfo.getGroupId());
        metadata.setArtifactId(artifactInfo.getArtifactId());
        metadata.setVersion(artifactInfo.getVersion());
        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        versioning.setLastUpdatedTimestamp(new Date());
        Snapshot snapshot = new Snapshot();
        versioning.setSnapshot(snapshot);

        SnapshotVersionBehavior snapshotBehavior = ((LocalRepo) folder.getRepo()).getSnapshotVersionBehavior();
        JcrFile latestUniquePom = getLatestUniqueSnapshotPom(poms);
        if (snapshotBehavior.equals(SnapshotVersionBehavior.NONUNIQUE) ||
                (snapshotBehavior.equals(SnapshotVersionBehavior.DEPLOYER) && latestUniquePom == null)) {
            snapshot.setBuildNumber(1);
        } else if (snapshotBehavior.equals(SnapshotVersionBehavior.UNIQUE)) {
            // take the latest unique snapshot file file
            if (latestUniquePom != null) {
                String pomName = latestUniquePom.getName();
                snapshot.setBuildNumber(MavenNaming.getUniqueSnapshotVersionBuildNumber(pomName));
                snapshot.setTimestamp(MavenNaming.getUniqueSnapshotVersionTimestamp(pomName));
            }
        }
        saveMetadata(folder, metadata);
    }

    private void createVersionsMetadata(JcrFolder folder, List<JcrFolder> subFoldersContainingPoms) {
        // get artifact info from the first pom
        JcrFile samplePom = getPomItems(subFoldersContainingPoms.get(0)).get(0);
        MavenArtifactInfo artifactInfo = MavenArtifactInfo.fromRepoPath(samplePom.getRepoPath());
        Metadata metadata = new Metadata();
        metadata.setGroupId(artifactInfo.getGroupId());
        metadata.setArtifactId(artifactInfo.getArtifactId());
        metadata.setVersion(artifactInfo.getVersion());
        Versioning versioning = new Versioning();
        metadata.setVersioning(versioning);
        versioning.setLastUpdatedTimestamp(new Date());

        // add the different versions and set the release to be the latest release version (if exists) 
        for (JcrFolder pomFilesContainer : subFoldersContainingPoms) {
            String version = pomFilesContainer.getName();
            versioning.addVersion(version);
            if (!MavenNaming.isSnapshot(version)) {
                versioning.setRelease(version);
            }
        }

        // the latest is the last folder
        versioning.setLatest(subFoldersContainingPoms.get(subFoldersContainingPoms.size() - 1).getName());

        saveMetadata(folder, metadata);
    }

    private JcrFile getLatestUniqueSnapshotPom(List<JcrFile> poms) {
        for (int i = poms.size() - 1; i >= 0; i--) {
            JcrFile pom = poms.get(i);
            String pomName = pom.getName();
            if (MavenNaming.isUniqueSnapshotFileName(pomName)) {
                return pom;
            }
        }
        return null;
    }

    private List<JcrFile> getPomItems(JcrFolder folder) {
        ArrayList<JcrFile> poms = new ArrayList<JcrFile>();
        for (JcrFsItem jcrFsItem : folder.getItems()) {
            if (jcrFsItem.isFile() && MavenNaming.isPom(jcrFsItem.getPath())) {
                poms.add((JcrFile) jcrFsItem);
            }
        }
        return poms;
    }

    private List<JcrFolder> getSubFoldersContainingPoms(JcrFolder parent) {
        ArrayList<JcrFolder> result = new ArrayList<JcrFolder>();
        for (JcrFsItem item : parent.getItems()) {
            if (item.isDirectory()) {
                JcrFolder folder = (JcrFolder) item;
                if (!getPomItems(folder).isEmpty()) {
                    result.add(folder);
                }
            }
        }
        return result;
    }

    protected void removeMetadataIfExist(JcrFolder folder) {
        if (folder.hasMetadata(MavenNaming.MAVEN_METADATA_NAME)) {
            folder.setMetadata(MavenNaming.MAVEN_METADATA_NAME, null);
        }
    }
}
