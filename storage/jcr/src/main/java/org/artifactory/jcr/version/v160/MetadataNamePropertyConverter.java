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

package org.artifactory.jcr.version.v160;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.fs.StatsInfo;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.version.JcrVersion;
import org.artifactory.log.LoggerFactory;
import org.artifactory.mime.MavenNaming;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.version.CompoundVersionDetails;
import org.artifactory.version.converter.ConfigurationConverter;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import static org.artifactory.storage.StorageConstants.NODE_ARTIFACTORY_METADATA;
import static org.artifactory.storage.StorageConstants.PROP_ARTIFACTORY_METADATA_NAME;

/**
 * Searches for all metadata root nodes under artifactory:metadata and adds the metadata name property to them
 *
 * @author Noam Y. Tenne
 */
public class MetadataNamePropertyConverter implements ConfigurationConverter<JcrSession> {

    private static final Logger log = LoggerFactory.getLogger(MetadataNamePropertyConverter.class);

    @Override
    public void convert(JcrSession unManaged) {
        ArtifactoryHome artifactoryHome = StorageContextHelper.get().getArtifactoryHome();
        CompoundVersionDetails source = artifactoryHome.getOriginalVersionDetails();
        if (source != null) {
            JcrVersion originalVersion = source.getVersion().getSubConfigElementVersion(JcrVersion.class);

            if ((originalVersion != null) && originalVersion.compareTo(JcrVersion.v160) < 0) {
                log.info("Skipping metadata node name property conversion. Already converted by JCR metadata " +
                        "converter");
                return;
            }
        }

        log.info("Beginning metadata node name property conversion");

        String queryStr = new StringBuilder().append("/jcr:root").append(PathFactoryHolder.get().getAllRepoRootPath()).
                append("//").append(NODE_ARTIFACTORY_METADATA).append("/element(*, ").
                append(JcrConstants.NT_UNSTRUCTURED).append(")").toString();

        try {
            QueryManager queryManager = unManaged.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryStr, Query.XPATH);
            log.debug("Executing metadata name property converter query: {}", queryStr);
            QueryResult queryResult = query.execute();
            log.debug("Metadata name property converter query execution has completed.");

            NodeIterator resultIterator = queryResult.getNodes();
            performConversion(unManaged, resultIterator);
        } catch (RepositoryException e) {
            log.error("Error occurred while performing metadata name property conversion: {}", e.getMessage());
            log.debug("Error occurred while performing metadata name property conversion.", e);
        } finally {
            unManaged.save();
        }
    }

    /**
     * Perform the conversion on the results received from the query
     *
     * @param unManaged      Unmanaged session to act upon
     * @param resultIterator Search result iterator
     */
    private void performConversion(JcrSession unManaged, NodeIterator resultIterator) {
        long convertedCount = 0;
        while (resultIterator.hasNext()) {
            String nodePath = "N/A";
            Node node = resultIterator.nextNode();
            try {

                nodePath = node.getPath();
                if (!nodePath.endsWith(MavenNaming.MAVEN_METADATA_NAME) &&
                        !nodePath.endsWith(StatsInfo.ROOT)) {

                    if (node.hasProperty(PROP_ARTIFACTORY_METADATA_NAME)) {
                        String propValue = node.getProperty(PROP_ARTIFACTORY_METADATA_NAME).getString();
                        if (StringUtils.isNotBlank(propValue)) {
                            log.debug("Skipping conversion of metadata name property fore node '{}': Property exists.",
                                    nodePath);
                            continue;
                        }
                    }

                    node.setProperty(PROP_ARTIFACTORY_METADATA_NAME, node.getName());
                    node.save();
                    log.debug("Converted the metadata name property of node '{}'", nodePath);

                    if ((++convertedCount % 50) == 0) {
                        unManaged.save();
                        log.info("Converted the metadata name properties of {} nodes...", convertedCount);
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error occurred during metadata name property conversion of node '{}': {}", nodePath,
                        e.getMessage());
                log.debug("Error occurred during metadata name property conversion of node '" + nodePath + "'.", e);
            }
        }
        log.info("Metadata name property conversion is complete. {} nodes were converted during the process.",
                convertedCount);
    }
}