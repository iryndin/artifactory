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

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.descriptor.repo.SnapshotVersionBehavior;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.jcr.fs.JcrFsItem;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.jcr.StoringRepo;
import org.artifactory.spring.InternalContextHelper;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Calculates maven metadata recursively for folder in a local non-cache repository. Plugins metadata is calculated for
 * the while repository.
 *
 * @author Yossi Shaul
 */
public class MavenMetadataImportCalculator {
    private static final Logger log = LoggerFactory.getLogger(MavenMetadataImportCalculator.class);

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

        createPluginsMetadata((LocalRepo) storingRepo, status);

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

        // if this folder contains snapshots create snapshots maven.metadata
        if (MavenNaming.isSnapshot(folder.getPath())) {
            log.trace("Detected snapshots container: {}", folder);
            createSnapshotsMetadata(folder);
        }

        // if this folder contains "version folders" create versions maven metadata
        List<JcrFolder> subFoldersContainingPoms = getSubFoldersContainingPoms(folder);
        if (!subFoldersContainingPoms.isEmpty()) {
            log.trace("Detected versions container: {}", folder);
            createVersionsMetadata(folder, subFoldersContainingPoms);
        }
    }

    private void createSnapshotsMetadata(JcrFolder folder) {
        List<JcrFile> poms = getPomItems(folder);
        if (poms.isEmpty()) {
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

    private void createPluginsMetadata(LocalRepo repo, StatusHolder status) {
        JcrService jcrService = InternalContextHelper.get().getJcrService();
        JcrFolder rootFolder = repo.getRootFolder();
        RepositoryService repoService = InternalContextHelper.get().getRepositoryService();
        try {
            List<JcrFile> pluginPoms = jcrService.getPluginPomNodes(rootFolder.getRepoPath(), repo);
            log.debug("{} plugin poms found", pluginPoms.size());

            // put all the poms belonging to the same plugin folder under the same hash entry
            MultiMap<JcrFolder, JcrFile> pluginFolders = new MultiHashMap<JcrFolder, JcrFile>();
            for (JcrFile pom : pluginPoms) {
                JcrFolder artifactIdNode = pom.getParentFolder().getParentFolder();
                pluginFolders.put(artifactIdNode, pom);
            }

            // for each plugins folder container, create plugins metadata on the parent
            Set<JcrFolder> folders = pluginFolders.keySet();
            for (JcrFolder pluginFolder : folders) {
                // read the first plugin pom to extract the plugin name
                JcrFile pomFile = pluginFolders.get(pluginFolder).iterator().next();
                String pomStr = repoService.getTextFileContent(pomFile.getInfo());
                Model pomModel = MavenModelUtils.stringToMavenModel(pomStr);
                JcrFolder pluginsMetadataContainer = pluginFolder.getParentFolder();
                Metadata metadata = getOrCreatePluginMetadata(pluginsMetadataContainer);
                String artifactId = pluginFolder.getName();
                if (!hasPluginMetadata(metadata, artifactId)) {
                    Plugin plugin = new Plugin();
                    plugin.setArtifactId(artifactId);
                    plugin.setPrefix(PluginDescriptor.getGoalPrefixFromArtifactId(pomModel.getArtifactId()));
                    String pluginName = pomModel.getName();
                    if (StringUtils.isBlank(pluginName)) {
                        pluginName = "Unnamed - " + pomModel.getId();
                    }
                    plugin.setName(pluginName);
                    metadata.addPlugin(plugin);
                    saveMetadata(pluginsMetadataContainer, metadata);
                }
            }

        } catch (RepositoryException e) {
            status.setError("Failed calculating plugins maven metadata", e, log);
        }
    }

    // return true if the input plugin is already listed in the metadata plugins
    private boolean hasPluginMetadata(Metadata metadata, String pluginArtifactId) {
        if (metadata.getPlugins() == null) {
            return false;
        }
        for (Object plugin : metadata.getPlugins()) {
            if (((Plugin) plugin).getArtifactId().equals(pluginArtifactId)) {
                return true;
            }
        }
        return false;
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

    private void saveMetadata(JcrFolder folder, Metadata metadata) {
        String metadataStr;
        try {
            metadataStr = MavenModelUtils.mavenMetadataToString(metadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert maven metadata into string", e);
        }
        folder.setXmlMetadata(MavenNaming.MAVEN_METADATA_NAME, metadataStr);
    }

    private Metadata getOrCreatePluginMetadata(JcrFolder folder) {
        if (folder.hasXmlMetadata(MavenNaming.MAVEN_METADATA_NAME)) {
            String metadataStr = folder.getXmlMetdata(MavenNaming.MAVEN_METADATA_NAME);
            try {
                return MavenModelUtils.toMavenMetadata(metadataStr);
            } catch (IOException e) {
                throw new RuntimeException("Failed to convert maven metadata into string", e);
            }
        } else {
            return new Metadata();
        }
    }
}
