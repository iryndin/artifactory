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

package org.artifactory.maven;

import com.google.common.collect.HashMultimap;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.artifactory.api.common.BasicStatusHolder;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.jcr.fs.JcrFile;
import org.artifactory.jcr.fs.JcrFolder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.LocalRepo;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Plugins metadata is calculated for the whole repository.
 *
 * @author Yossi Shaul
 */
public class MavenPluginsMetadataCalculator extends AbstractMetadataCalculator {
    private static final Logger log = LoggerFactory.getLogger(MavenPluginsMetadataCalculator.class);

    /**
     * Calculate maven metadata using folder as the base.
     *
     * @param localRepo Base folder to start calculating from.
     */
    public void calculate(LocalRepo localRepo) {
        if (!localRepo.isLocal() || localRepo.isCache()) {
            log.warn("Maven metadata calculation is allowed only on local non-cache repositories");
            return;
        }

        log.debug("Calculating maven plugins metadata on repo '{}'", localRepo.getKey());
        List<JcrFile> pluginPoms = getJcrService().getPluginPomNodes(localRepo);
        log.debug("{} plugin poms found", pluginPoms.size());

        // aggregate one pom for each plugin under the plugins metadata container
        HashMultimap<JcrFolder, JcrFile> pluginsMetadataContainers = HashMultimap.create();
        for (JcrFile pom : pluginPoms) {
            // great-grandparent is the plugins metadata container
            // eg, if the plugin pom is org/jfrog/maven/plugins/maven-test-plugin/1.0/maven-test-plugin-1.0.pom
            // the node that contains the plugins metadata is org/jfrog/maven/plugins
            JcrFolder pluginsMetadataContainer = pom.getAncestor(3);
            if (pluginsMetadataContainer != null) {
                pluginsMetadataContainers.put(pluginsMetadataContainer, pom);
            } else {
                log.info("Found plugin pom without maven GAV path: '{}'. Ignoring...", pom.getRepoPath());
            }
        }

        // for each plugins folder container, create plugins metadata on the parent
        Set<JcrFolder> folders = pluginsMetadataContainers.keySet();
        for (JcrFolder pluginsMetadataContainer : folders) {
            //Metadata metadata = getOrCreatePluginMetadata(pluginsMetadataContainer);
            Metadata metadata = new Metadata();
            for (JcrFile pomFile : pluginsMetadataContainers.get(pluginsMetadataContainer)) {
                String artifactId = pomFile.getAncestor(2).getName();
                if (hasPlugin(metadata, artifactId)) {
                    continue;
                }

                // extract the plugin details and add to the metadata
                String pomStr = getRepositoryService().getStringContent(pomFile.getInfo());
                Model pomModel = MavenModelUtils.stringToMavenModel(pomStr);
                artifactId = pomModel.getArtifactId();
                Plugin plugin = new Plugin();
                plugin.setArtifactId(artifactId);
                plugin.setPrefix(PluginDescriptor.getGoalPrefixFromArtifactId(pomModel.getArtifactId()));
                String pluginName = pomModel.getName();
                if (StringUtils.isBlank(pluginName)) {
                    pluginName = "Unnamed - " + pomModel.getId();
                }
                plugin.setName(pluginName);
                metadata.addPlugin(plugin);
            }

            // save only if something changed
            if (modified(getPluginMetadata(pluginsMetadataContainer), metadata)) {
                saveMetadata(pluginsMetadataContainer.getRepoPath(), metadata, new BasicStatusHolder());
            }
        }
        log.debug("Finished maven plugins metadata calculation on '{}'", localRepo.getKey());
    }

    /**
     * Minimal checks for modified metadata.
     */
    private boolean modified(Metadata originalMetadata, Metadata newMetadata) {
        if (originalMetadata == null) {
            return true;
        }

        List originalPlugins = originalMetadata.getPlugins();
        if (originalPlugins.size() != newMetadata.getPlugins().size()) {
            return true;
        }

        for (Object pluginObj : newMetadata.getPlugins()) {
            if (!hasPlugin(originalMetadata, ((Plugin) pluginObj).getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the input plugin is listed in the metadata plugins
     */
    private boolean hasPlugin(Metadata metadata, String artifactId) {
        for (Object pluginObj : metadata.getPlugins()) {
            if (((Plugin) pluginObj).getArtifactId().equals(artifactId)) {
                return true;
            }
        }
        return false;
    }

    private Metadata getPluginMetadata(JcrFolder folder) {
        String xmlMavenMetadata =
                getRepositoryService().getXmlMetadata(folder.getRepoPath(), MavenNaming.MAVEN_METADATA_NAME);
        if (xmlMavenMetadata != null) {
            try {
                return MavenModelUtils.toMavenMetadata(xmlMavenMetadata);
            } catch (IOException e) {
                throw new RuntimeException("Failed to convert maven metadata into string", e);
            }
        } else {
            return null;
        }
    }
}
