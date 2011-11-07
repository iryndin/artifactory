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
import org.artifactory.fs.WatchersInfo;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.factory.JcrPathFactory;
import org.artifactory.jcr.spring.ArtifactoryStorageContext;
import org.artifactory.jcr.spring.StorageContextHelper;
import org.artifactory.repo.RepoPath;
import org.artifactory.sapi.common.PathFactoryHolder;
import org.artifactory.storage.StorageConstants;
import org.artifactory.util.XmlUtils;
import org.artifactory.version.converter.ConfigurationConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Noam Y. Tenne
 */
public class MarkerFileConverter implements ConfigurationConverter<JcrSession> {
    private static final Logger log = LoggerFactory.getLogger(MarkerFileConverter.class);
    public static final String CREATE_DEFAULT_STATS_MARKER_FILENAME = ".stats.create.default";
    public static final String REPAIR_WATCHERS_MARKER_FILENAME = ".watch.convert";

    @Override
    public void convert(JcrSession jcrSession) {
        createMarkerFile(CREATE_DEFAULT_STATS_MARKER_FILENAME, "creation of default stats");
        createMarkerFile(REPAIR_WATCHERS_MARKER_FILENAME, "conversion of watchers");
    }

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

    private void createMarkerFile(String markerFileName, String actionDesc) {
        log.info("Marking for {} metadata.", actionDesc);
        File markerFile = new File(ArtifactoryHome.get().getDataDir(), markerFileName);
        try {
            boolean fileWasCreated = markerFile.createNewFile();
            if (!fileWasCreated) {
                String message = String.format("Failed to mark for %s metadata: marker file was not created: '%s'.",
                        actionDesc, markerFile.getAbsolutePath());
                message += markerFile.exists() ? "File already exist" : "File doesn't exist";
                log.warn(message, actionDesc);
            }
        } catch (IOException e) {
            log.error("Error while marking for {} metadata: {}", e.getMessage(), actionDesc);
            log.debug("Error while marking for " + actionDesc + " metadata.", e);
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
            QueryManager queryManager = unmanagedSession.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(querySpec.query(), querySpec.jcrType());
            QueryResult result = query.execute();
            NodeIterator resultNodes = result.getNodes();

            while (resultNodes.hasNext()) {
                jcrService.createDefaultStatsNode(resultNodes.nextNode());
                unmanagedSession.save();
            }
            FileUtils.deleteQuietly(new File(ArtifactoryHome.get().getDataDir(),
                    MarkerFileConverter.CREATE_DEFAULT_STATS_MARKER_FILENAME));
            log.info("Finished the creation of default stats metadata.");
        } catch (RepositoryException e) {
            log.error("Error occurred while creating default stats metadata: {}", e.getMessage());
            log.debug("Error occurred while creating default stats metadata.", e);
        } finally {
            unmanagedSession.logout();
        }
    }

    private void repairWatchersMetadata() {
        log.info("Starting to convert watchers metadata.");

        ArtifactoryStorageContext context = StorageContextHelper.get();
        JcrService jcrService = context.getJcrService();
        JcrSession unmanagedSession = jcrService.getUnmanagedSession();
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
