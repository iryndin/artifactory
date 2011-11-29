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

package org.artifactory.jcr.version.v228;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.search.JcrQuerySpec;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.fs.MutableStatsInfo;
import org.artifactory.fs.StatsInfo;
import org.artifactory.fs.WatchersInfo;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrServiceImpl;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.factory.JcrPathFactory;
import org.artifactory.jcr.md.MetadataAwareAdapter;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.spring.ArtifactoryStorageContext;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.jcr.utils.JcrUtils;
import org.artifactory.jcr.version.MarkerFileConverterBase;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.storage.StorageConstants;
import org.artifactory.util.XmlUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.io.File;
import java.io.InputStream;

/**
 * @author Noam Y. Tenne
 */
public class MarkerFileConverter extends MarkerFileConverterBase {
    private static final Logger log = LoggerFactory.getLogger(MarkerFileConverter.class);
    public static final String CREATE_DEFAULT_STATS_MARKER_FILENAME = ".stats.create.default";
    public static final String REPAIR_WATCHERS_MARKER_FILENAME = ".watch.convert";

    @Override
    public void convert(JcrSession jcrSession) {
        createMarkerFile(CREATE_DEFAULT_STATS_MARKER_FILENAME, "creation of default stats");
        createMarkerFile(REPAIR_WATCHERS_MARKER_FILENAME, "conversion of watchers");
    }

    @Override
    public boolean needConversion() {
        File defaultStatsCreationMarker = new File(ArtifactoryHome.get().getDataDir(),
                MarkerFileConverter.CREATE_DEFAULT_STATS_MARKER_FILENAME);
        File repairWatchersMarker = new File(ArtifactoryHome.get().getDataDir(),
                MarkerFileConverter.REPAIR_WATCHERS_MARKER_FILENAME);
        if (defaultStatsCreationMarker.exists() || repairWatchersMarker.exists()) {
            return true;
        }
        return false;
    }

    @Override
    public void applyConversion() {
        /**
         * Create default stats metadata so that even files the were never downloaded will be considered in the last
         * downloaded query.
         */
        if (new File(ArtifactoryHome.get().getDataDir(), CREATE_DEFAULT_STATS_MARKER_FILENAME).exists()) {
            createDefaultStats();
        }
        /**
         * Repair old watchers metadata that was saved with the repo paths qualified name by removing the repo path all
         * together (field is deprecated)
         */
        if (new File(ArtifactoryHome.get().getDataDir(), REPAIR_WATCHERS_MARKER_FILENAME).exists()) {
            repairWatchersMetadata();
        }
    }

    private void createDefaultStats() {
        JcrQuerySpec querySpec = JcrQuerySpec.xpath(new StringBuilder().append("/jcr:root").
                append(PathFactoryHolder.get().getAllRepoRootPath()).append("//element(*, ").
                append(StorageConstants.NT_ARTIFACTORY_FILE).append(")").toString()).noLimit();

        log.info("Starting to create default stats metadata.");
        ArtifactoryStorageContext context = StorageContextHelper.get();
        JcrService jcrService = context.getJcrService();
        JcrSession unmanagedSession = jcrService.getUnmanagedSession();
        try {
            JcrServiceImpl.keepUnmanagedSession(unmanagedSession);
            QueryResult result = jcrService.executeQuery(querySpec, unmanagedSession);
            NodeIterator resultNodes = result.getNodes();

            while (resultNodes.hasNext()) {
                createDefaultStatsNode(resultNodes.nextNode());
                unmanagedSession.save();
            }
            FileUtils.deleteQuietly(new File(ArtifactoryHome.get().getDataDir(),
                    MarkerFileConverter.CREATE_DEFAULT_STATS_MARKER_FILENAME));
            log.info("Finished the creation of default stats metadata.");
        } catch (RepositoryException e) {
            log.error("Error occurred while creating default stats metadata: {}", e.getMessage());
            log.debug("Error occurred while creating default stats metadata.", e);
        } finally {
            JcrServiceImpl.removeUnmanagedSession();
            unmanagedSession.logout();
        }
    }

    public void createDefaultStatsNode(Node artifactNode) throws RepositoryException {
        boolean artifactHasStats = false;

        if (artifactNode.hasNode(StorageConstants.NT_ARTIFACTORY_METADATA)) {
            Node artifactoryMetadataNode = artifactNode.getNode(StorageConstants.NT_ARTIFACTORY_METADATA);
            artifactHasStats = artifactoryMetadataNode.hasNode(StatsInfo.ROOT);
        }

        if (artifactHasStats) {
            if (log.isDebugEnabled()) {
                log.debug("Stats metadata already exists for '{}', no need to create.",
                        JcrUtils.nodePathFromUuid(artifactNode.getIdentifier()));
            }
            return;
        }
        String artifactCreatedBy = artifactNode.getProperty(StorageConstants.PROP_ARTIFACTORY_CREATED_BY).getString();

        MutableStatsInfo statsInfo = InfoFactoryHolder.get().createStats();
        statsInfo.setLastDownloadedBy(artifactCreatedBy);

        MetadataDefinitionService metadataDefService = StorageContextHelper.get().getMetadataDefinitionService();
        MetadataDefinition<StatsInfo, MutableStatsInfo> statsDef =
                metadataDefService.getMetadataDefinition(StatsInfo.class);
        statsDef.getPersistenceHandler().update(new MetadataAwareAdapter(artifactNode), statsInfo);
        artifactNode.getSession().save();
        if (log.isDebugEnabled()) {
            log.debug("Creating default stats metadata for '{}' with last downloaded by '{}'.", new Object[]{
                    JcrUtils.nodePathFromUuid(artifactNode.getIdentifier()), artifactCreatedBy});
        }
    }

    private void repairWatchersMetadata() {
        log.info("Starting to convert watchers metadata.");

        ArtifactoryStorageContext context = StorageContextHelper.get();
        JcrService jcrService = context.getJcrService();
        JcrSession unmanagedSession = jcrService.getUnmanagedSession();
        try {
            JcrServiceImpl.keepUnmanagedSession(unmanagedSession);
            JcrQuerySpec jcrQuerySpec = JcrQuerySpec.xpath(
                    new StringBuilder("/jcr:root").append(new JcrPathFactory().getAllRepoRootPath()).append("//. [@")
                            .append(StorageConstants.PROP_ARTIFACTORY_METADATA_NAME).append(" = '")
                            .append(WatchersInfo.ROOT).append("']").toString()).noLimit();
            QueryResult queryResult = jcrService.executeQuery(jcrQuerySpec, unmanagedSession);
            NodeIterator resultNodes;
            try {
                resultNodes = queryResult.getNodes();
            } catch (Exception e) {
                log.error("Error occurred while searching for watchers metadata: {}", e.getMessage());
                log.debug("Error occurred while searching for watchers metadata.", e);
                return;
            }

            repairWatchersNodes(unmanagedSession, resultNodes);

            FileUtils.deleteQuietly(new File(ArtifactoryHome.get().getDataDir(),
                    MarkerFileConverter.REPAIR_WATCHERS_MARKER_FILENAME));
            log.info("Finished converting watchers metadata.");
        } finally {
            JcrServiceImpl.removeUnmanagedSession();
            unmanagedSession.logout();
        }
    }

    private void repairWatchersNodes(JcrSession unmanagedSession, NodeIterator resultNodes) {
        while (resultNodes.hasNext()) {
            try {
                Node watchersNode = resultNodes.nextNode();
                repairWatchersNode(watchersNode);
                unmanagedSession.save();
            } catch (Exception e) {
                log.error("Error occurred while converting watchers metadata: {}", e.getMessage());
                log.debug("Error occurred while converting watchers metadata.", e);
            }

        }
    }

    private void repairWatchersNode(Node watchersNode) throws RepositoryException {
        Node watchedItemNode = watchersNode.getParent().getParent();
        RepoPath watchedItemRepoPath = new JcrPathFactory().getRepoPath(watchedItemNode.getPath());
        log.debug("Converting watchers metadata of '{}'", watchedItemRepoPath);

        if (!watchersNode.hasNode(JcrConstants.JCR_CONTENT)) {
            log.debug("No JCR content in watchers node for '{}'; Nothing to convert.", watchersNode);
            return;
        }

        Node watchersContent = watchersNode.getNode(JcrConstants.JCR_CONTENT);
        if (!watchersContent.hasProperty(JcrConstants.JCR_DATA)) {
            log.debug("No JCR data property in watchers content node for '{}'; Nothing to convert.", watchersNode);
            return;
        }
        Property jcrDataProperty = watchersContent.getProperty(JcrConstants.JCR_DATA);
        Binary binary = jcrDataProperty.getBinary();
        InputStream xmlStream = null;
        try {
            xmlStream = binary.getStream();
            Document watchersDocument = XmlUtils.parse(xmlStream);
            Element rootElement = watchersDocument.getRootElement();
            Namespace namespace = rootElement.getNamespace();
            Element repoPath = rootElement.getChild("repoPath", namespace);
            if (repoPath == null) {
                log.debug("No repo path element in watchers XML metadata for '{}'; Nothing to convert.", watchersNode);
                return;
            }
            rootElement.removeChild("repoPath", namespace);
            ArtifactoryStorageContext context = StorageContextHelper.get();
            RepositoryService repositoryService = context.getRepositoryService();
            repositoryService.setXmlMetadata(watchedItemRepoPath, WatchersInfo.ROOT,
                    XmlUtils.outputString(watchersDocument));
        } finally {
            IOUtils.closeQuietly(xmlStream);
        }
    }
}
