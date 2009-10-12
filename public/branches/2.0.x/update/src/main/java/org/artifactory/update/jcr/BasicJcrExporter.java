/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.update.jcr;

import org.apache.commons.collections15.OrderedMap;
import org.apache.commons.io.IOUtils;
import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.config.ExportSettings;
import org.artifactory.api.config.ImportSettings;
import org.artifactory.api.fs.ItemInfo;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.descriptor.DescriptorAware;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.LocalRepoDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoDescriptor;
import org.artifactory.update.md.MetadataConverter;
import org.artifactory.update.utils.UpdateUtils;
import org.artifactory.version.ConverterUtils;
import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author freds
 * @date Nov 16, 2008
 */
public abstract class BasicJcrExporter implements JcrExporter {
    private static final Logger log = LoggerFactory.getLogger(BasicJcrExporter.class);
    private DescriptorAware<CentralConfigDescriptor> centralProvider;
    private JcrSessionProvider jcr;
    private List<String> reposToExport;
    private boolean includeCaches = false;
    public static final String ARTIFACTORY_PREFIX = "artifactory:";
    public static final String NODE_ARTIFACTORY_METADATA = ARTIFACTORY_PREFIX + "metadata";

    public JcrSessionProvider getJcr() {
        return jcr;
    }

    public void setJcr(JcrSessionProvider jcr) {
        this.jcr = jcr;
    }

    public DescriptorAware<CentralConfigDescriptor> getCentralProvider() {
        return centralProvider;
    }

    public void setCentralProvider(DescriptorAware<CentralConfigDescriptor> centralProvider) {
        this.centralProvider = centralProvider;
    }

    public void setRepositoriesToExport(List<String> reposToExport) {
        if (includeCaches && reposToExport != null) {
            throw new IllegalStateException("Include caches cannot be true if " +
                    "repos to export is not null");
        }
        this.reposToExport = reposToExport;
    }

    public void setIncludeCaches(boolean includeCaches) {
        if (includeCaches && reposToExport != null) {
            throw new IllegalStateException("Include caches cannot be true if " +
                    "repos to export is not null");
        }
        this.includeCaches = includeCaches;
    }

    public void exportTo(ExportSettings settings, StatusHolder status) {
        status.setStatus("Starting export of JCR repo data", log);
        try {
            String repoJcrRootPath = JcrPathUpdate.getRepoJcrRootPath();
            Session session = jcr.getSession();
            Node repoRoot = (Node) session.getItem(repoJcrRootPath);
            NodeIterator nodes = repoRoot.getNodes();
            while (nodes.hasNext()) {
                Node repoNode = nodes.nextNode();
                String jcrRepoName = repoNode.getName();
                if (shouldExportRepo(jcrRepoName, status)) {
                    exportRepository(settings.getBaseDir(), repoNode, jcrRepoName, status);
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        status.setStatus("End of export of JCR repo data", log);
    }

    private boolean shouldExportRepo(String jcrRepoName, StatusHolder status) {
        String newRepoKey = UpdateUtils.getNewRepoKey(jcrRepoName);
        String repoKeyForDescriptorMap = getRepoKeyForDescriptorMap(jcrRepoName, newRepoKey);
        boolean export = true;
        if (!isInRepositoriesList(repoKeyForDescriptorMap)) {
            String message = "Repository in JCR with name " + newRepoKey +
                    " does not exists in the config xml file. Will be ignored.";
            status.setStatus(message, log);
            export = false;
        }

        if (reposToExport != null) {
            // export only the listed repositories
            if (!reposToExport.contains(jcrRepoName)) {
                status.setStatus("Repository " + jcrRepoName + " not in list of repos to export. " +
                        "Skipping", log);
                export = false;
            }
        } else {
            // don't export caches if --caches not passed
            if (!includeCaches && isCacheRepository(newRepoKey)) {
                status.setStatus("Skipping cached repository " + newRepoKey, log);
                export = false;
            }
        }

        return export;
    }

    private static boolean isCacheRepository(String newRepoKey) {
        return newRepoKey.endsWith(UpdateUtils.CACHE_SUFFIX);
    }

    private boolean isInReposToExport(String jcrRepoName) {
        return reposToExport == null || reposToExport.contains(jcrRepoName);
    }

    private boolean isInRepositoriesList(String repoKeyForDescriptorMap) {
        // the migrated central config
        CentralConfigDescriptor descriptor = getCentralProvider().getDescriptor();

        OrderedMap<String, LocalRepoDescriptor> localRepositoriesMap =
                descriptor.getLocalRepositoriesMap();
        OrderedMap<String, RemoteRepoDescriptor> remoteRepositoriesMap =
                descriptor.getRemoteRepositoriesMap();
        return localRepositoriesMap.containsKey(repoKeyForDescriptorMap) ||
                remoteRepositoriesMap.containsKey(repoKeyForDescriptorMap);
    }

    /**
     * The remote repositories are saved in the the jcr with the suffix '-cache' but configured without it in the config
     * file. Same for the local repos but with the '-local' suffix. This method will return the name as expected in the
     * config file.
     */
    private static String getRepoKeyForDescriptorMap(String jcrRepoName, String newRepoKey) {
        if (newRepoKey.endsWith(UpdateUtils.CACHE_SUFFIX)) {
            return newRepoKey.substring(
                    0, newRepoKey.length() - UpdateUtils.CACHE_SUFFIX.length());
        } else if (!jcrRepoName.endsWith(UpdateUtils.LOCAL_SUFFIX) &&
                newRepoKey.endsWith(UpdateUtils.LOCAL_SUFFIX)) {
            // the -local string was added to the newRepoKey - remove it
            return newRepoKey.substring(
                    0, newRepoKey.length() - UpdateUtils.LOCAL_SUFFIX.length());
        } else {
            return newRepoKey;
        }
    }

    protected abstract void exportRepository(File exportDir, Node repoNode, String repoKey,
            StatusHolder status) throws Exception;

    public void importFrom(ImportSettings settings, StatusHolder status) {
    }

    public static void fillTimestamps(ItemInfo itemInfo, Node node) throws RepositoryException {
        if (node.hasProperty(JCR_CREATED)) {
            itemInfo.setCreated(node.getProperty(JCR_CREATED).getDate().getTimeInMillis());
        }
        if (node.hasProperty(JCR_LASTMODIFIED)) {
            itemInfo.setLastModified(node.getProperty(JCR_LASTMODIFIED).getDate().getTimeInMillis());
        }
    }

    public static String getXmlContent(Node mdNode, List<MetadataConverter> converters) throws RepositoryException {
        InputStream is = getRawXmlStream(mdNode);
        String xmlContent;
        try {
            xmlContent = IOUtils.toString(is, "utf-8");
        } catch (IOException e) {
            throw new RepositoryException("Cannot read metadata of " + mdNode, e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        if (converters != null) {
            Document doc = ConverterUtils.parse(xmlContent);
            for (MetadataConverter converter : converters) {
                converter.convert(doc);
            }
            xmlContent = ConverterUtils.outputString(doc);
        }
        return xmlContent;
    }

    public static Node getMetadataContainer(Node node) throws RepositoryException {
        return node.getNode(NODE_ARTIFACTORY_METADATA);
    }

    public static Node getMetadataNode(Node node, String metadataName) throws RepositoryException {
        return getMetadataContainer(node).getNode(metadataName);
    }

    public static boolean hasMetadataNode(Node node, String metadataName) throws RepositoryException {
        return getMetadataContainer(node).hasNode(metadataName);
    }

    public static InputStream getRawXmlStream(Node metadataNode) throws RepositoryException {
        Node xmlNode = metadataNode.getNode(org.apache.jackrabbit.JcrConstants.JCR_CONTENT);
        Value attachedDataValue = xmlNode.getProperty(org.apache.jackrabbit.JcrConstants.JCR_DATA).getValue();
        InputStream is = attachedDataValue.getStream();
        return is;
    }
}
