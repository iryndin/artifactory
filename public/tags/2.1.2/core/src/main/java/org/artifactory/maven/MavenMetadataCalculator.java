/*
 * This file is part of Artifactory.
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
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RealRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.tx.SessionResource;
import org.artifactory.util.PathUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates the maven-metadata.xml for a folder
 *
 * @author yoavl
 * @author Yossi Shaul
 */
public class MavenMetadataCalculator implements SessionResource {
    private static final Logger log = LoggerFactory.getLogger(MavenMetadataCalculator.class);

    private Set<JcrFsItem> deletedItems = new LinkedHashSet<JcrFsItem>();

    public void afterCompletion(boolean commit) {
        //Reset internal state
        deletedItems.clear();
    }

    public boolean hasPendingResources() {
        return deletedItems.size() > 0;
    }

    //TODO: [by yl] Better call this in beforeCommit() to avoid unnecessary calculations, caused by manually calling
    //save() multiple times on the session (to free memory) during the same tx
    public void onSessionSave() {
        //Update the metadata
        for (JcrFsItem deletedItem : deletedItems) {
            recalculate(deletedItem);
        }
        deletedItems.clear();
    }

    /**
     * Add this deleted item to the list that might affect maven metadata updates.
     *
     * @param deletedItem The item deleted
     */
    public void addDeletedItem(JcrFsItem deletedItem) {
        if (deletedItem.isFolder()) {
            // no need to process fsitem of deleted folders so remove them from the deleted list
            // we cannot use the getItems() method of the folder since all its children were already deleted
            // so we must scan all the files and check if the parent is the current folder
            JcrFolder deletedFolder = (JcrFolder) deletedItem;
            Iterator<JcrFsItem> iterator = deletedItems.iterator();
            while (iterator.hasNext()) {
                JcrFsItem itemDeletedInThisSession = iterator.next();
                if (deletedFolder.equals(itemDeletedInThisSession.getParentFolder())) {
                    iterator.remove();
                }
            }
        }

        JcrFolder parentFolder = deletedItem.getParentFolder();
        boolean hasMavenMetadata = parentFolder.hasXmlMetadata(MavenNaming.MAVEN_METADATA_NAME);
        if (hasMavenMetadata) {
            deletedItems.add(deletedItem);
        } else {
            log.trace("Item {} doesn't have parent maven metadata");
        }
    }

    private void recalculate(JcrFsItem deletedItem) {

        if (deletedItem.isFile() && !MavenNaming.isSnapshot(deletedItem.getPath())) {
            // only snapshot files that were deleted updates a folder metadata
            log.trace("Skipping deleted release version file");
            return;
        }

        JcrFolder container = deletedItem.getParentFolder();
        String metadataStr = container.getXmlMetdata(MavenNaming.MAVEN_METADATA_NAME);
        //Sanity check
        if (metadataStr == null) {
            log.error("Cannot calculate maven-metadata for non-existing metadata on {}.", container);
            return;
        }

        try {
            Metadata metadata = MavenModelUtils.toMavenMetadata(metadataStr);
            if (isVersionsMetadata(deletedItem, container, metadata)) {
                handleVersionsMetadata(deletedItem, container, metadata);
            } else if (isSnapshotsMetadata(deletedItem, container, metadata)) {
                handleSnapshotsMetadata((JcrFile) deletedItem, container, metadata);
            } else if (isPluginsMetadata(metadata)) {
                handlePluginsMetadata(deletedItem, container, metadata);
            } else {
                log.warn("Unknown metadata type for {}", container);
            }
        } catch (Exception e) {
            log.error("Failed to update the maven metadata on " + container + ".", e);
        }
    }

    private void handleVersionsMetadata(JcrFsItem deletedItem, JcrFolder container, Metadata metadata)
            throws IOException {
        Versioning versioning = metadata.getVersioning();
        if (versioning == null) {
            log.debug("No versioning found for {}.", container);
            return;
        }

        @SuppressWarnings({"unchecked"})
        List<String> versions = versioning.getVersions();
        if (versions == null || versions.size() == 0) {
            log.debug("No versions found for {}.", container);
            return;
        }

        // the name of the folder is the version that was deleted
        String deletedVersion = deletedItem.getName();
        if (versions.contains(deletedVersion)) {
            versions.remove(deletedVersion);
            if (!versions.isEmpty()) {
                String latestVersion = versions.get(versions.size() - 1);
                versioning.setLatest(latestVersion);
                // also set the top level version to be the last version in the list
                metadata.setVersion(latestVersion);
            }
            versioning.updateTimestamp();
            String newMetadata = MavenModelUtils.mavenMetadataToString(metadata);
            container.setXmlMetadata(MavenNaming.MAVEN_METADATA_NAME, newMetadata);
        } else {
            log.debug("No maven metadata upgrade required on {}.", container);
        }
    }

    private void handleSnapshotsMetadata(JcrFile deletedItem, JcrFolder container, Metadata metadata)
            throws IOException {
        List<JcrFsItem> siblingItems = container.getItems();
        if (siblingItems.isEmpty()) {
            log.warn("No more files left under {}. Metadata is useless. Please remove the folder", container);
            return;
        }

        Versioning versioning = metadata.getVersioning();
        if (versioning == null) {
            log.debug("No versioning found for {}.", container);
            return;
        }

        Snapshot snapshot = versioning.getSnapshot();
        if (snapshot == null) {
            log.debug("No snapshot found for {}.", container);
            return;
        }

        boolean isUniqueSnapshotMetadata = PathUtils.hasText(snapshot.getTimestamp());
        if (!isUniqueSnapshotMetadata) {
            log.debug("Not updating metadata of non-unique snapshot metadata for {}", deletedItem);
            return;
        }

        // the name of the folder is the version that was deleted
        String fileName = deletedItem.getName();
        if (!MavenNaming.isUniqueSnapshotFileName(fileName)) {
            log.debug("Not updating metadata of non-unique snapshot deleted for {}", deletedItem);
            return;
        }

        // ok - the metadata is of unique version and the deleted file is has a unique snapshot version
        // we need to update the snapshot version if the deleted file is the one listed there (meaning it is the latest)
        String deletedFileTimestamp = MavenNaming.getUniqueSnapshotVersionTimestamp(fileName);
        boolean updated = false;
        if (deletedFileTimestamp.equals(snapshot.getTimestamp())) {
            // take the last item in the list
            for (int i = siblingItems.size() - 1; i >= 0; i--) {
                JcrFsItem sibling = siblingItems.get(i);
                String siblingVersion = sibling.getName();
                if (sibling.isFile() && MavenNaming.isUniqueSnapshotFileName(siblingVersion)) {
                    String siblingTimestamp = MavenNaming.getUniqueSnapshotVersionTimestamp(siblingVersion);
                    int siblingBuildNumber = MavenNaming.getUniqueSnapshotVersionBuildNumber(siblingVersion);
                    snapshot.setTimestamp(siblingTimestamp);
                    snapshot.setBuildNumber(siblingBuildNumber);
                    updated = true;
                    break;
                }
            }
        }

        if (updated) {
            versioning.updateTimestamp();
            String newMetadata = MavenModelUtils.mavenMetadataToString(metadata);
            container.setXmlMetadata(MavenNaming.MAVEN_METADATA_NAME, newMetadata);
        } else {
            log.debug("Metadata not updated for {}", container);
        }
    }

    private void handlePluginsMetadata(JcrFsItem deletedItem, JcrFolder container, Metadata metadata)
            throws IOException {
        StoringRepo repo = deletedItem.getRepo();
        if (repo instanceof RealRepo && repo.isCache()) {
            // we don't want to remove from the plugins list if this is a cache
            // maven uses the plugin prefix taken from the metadata to resolve plugins
            // configured in the pluginsGroup.
            log.debug("Skipping plugins metadata update for {}.", deletedItem);
            return;
        }
        String pluginArtifactId = deletedItem.getName();
        boolean updated = false;
        @SuppressWarnings({"unchecked"})
        Iterator<Plugin> iterator = metadata.getPlugins().iterator();
        while (iterator.hasNext()) {
            Plugin plugin = iterator.next();
            if (pluginArtifactId.equals(plugin.getArtifactId())) {
                iterator.remove();
                updated = true;
            }
        }

        if (updated) {
            String newMetadata = MavenModelUtils.mavenMetadataToString(metadata);
            container.setXmlMetadata(MavenNaming.MAVEN_METADATA_NAME, newMetadata);
        } else {
            log.debug("Metadata not updated for {}", container);
        }
    }

    private boolean isVersionsMetadata(JcrFsItem deletedItem, JcrFolder container, Metadata metadata) {
        return deletedItem.isFolder() && !MavenNaming.isSnapshot(container.getPath())
                && metadata.getVersioning() != null;
    }

    private boolean isSnapshotsMetadata(JcrFsItem deletedItem, JcrFolder container, Metadata metadata) {
        if (!deletedItem.isFile()) {
            // snapshot metadata update only relevant for deleted files
            return false;
        }

        Versioning versioning = metadata.getVersioning();
        return MavenNaming.isSnapshot(container.getPath()) && versioning != null && versioning.getSnapshot() != null;
    }

    private boolean isPluginsMetadata(Metadata metadata) {
        return metadata.getPlugins() != null && !metadata.getPlugins().isEmpty();
    }
}