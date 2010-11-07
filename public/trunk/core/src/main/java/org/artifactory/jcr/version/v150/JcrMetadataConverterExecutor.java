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

package org.artifactory.jcr.version.v150;

import com.google.common.collect.Sets;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.fs.FileAdditionalInfo;
import org.artifactory.api.fs.FileInfoImpl;
import org.artifactory.api.fs.FolderAdditionalInfo;
import org.artifactory.api.fs.FolderInfoImpl;
import org.artifactory.api.fs.InternalFileInfo;
import org.artifactory.api.fs.InternalFolderInfo;
import org.artifactory.api.fs.ItemAdditionalInfo;
import org.artifactory.api.jackson.JacksonFactory;
import org.artifactory.api.security.SecurityService;
import org.artifactory.api.xstream.XStreamFactory;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.jcr.JcrPath;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.jcr.md.GenericXmlProvider;
import org.artifactory.jcr.md.MetadataAwareAdapter;
import org.artifactory.jcr.md.MetadataDefinition;
import org.artifactory.jcr.md.MetadataDefinitionService;
import org.artifactory.jcr.md.XStreamMetadataProvider;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.jcr.JcrHelper;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.version.FatalConversionException;
import org.codehaus.jackson.JsonGenerator;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildFileBean;
import org.jfrog.build.api.Module;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import static org.apache.jackrabbit.JcrConstants.*;
import static org.artifactory.jcr.JcrTypes.*;
import static org.artifactory.repo.jcr.JcrHelper.*;

/**
 * @author freds
 */
public class JcrMetadataConverterExecutor {
    private static final Logger log = LoggerFactory.getLogger(JcrMetadataConverterExecutor.class);

    private static final NodeIterator EMPTY_NODE_ITERATOR = new NodeIterator() {
        public Node nextNode() {
            return null;
        }

        public void skip(long l) {
        }

        public long getSize() {
            return 0;
        }

        public long getPosition() {
            return 0;
        }

        public boolean hasNext() {
            return false;
        }

        public Object next() {
            return null;
        }

        public void remove() {
        }
    };

    private MetadataDefinitionService mdService;
    private MetadataDefinition<InternalFolderInfo> folderInfoMD;
    private MetadataDefinition<InternalFileInfo> fileInfoMD;
    private Session jcrSession;
    private int nbNodesConverted = 0;
    private final XStream buildXstream = XStreamFactory.create(Build.class);

    public JcrMetadataConverterExecutor(Session session) {
        jcrSession = session;
        mdService = InternalContextHelper.get().beanForType(MetadataDefinitionService.class);
        folderInfoMD = mdService.getMetadataDefinition(InternalFolderInfo.class);
        fileInfoMD = mdService.getMetadataDefinition(InternalFileInfo.class);
    }

    public void convert() {
        long start = System.currentTimeMillis();
        Node rootNode;
        try {
            rootNode = jcrSession.getRootNode();
        } catch (RepositoryException e) {
            throw new FatalConversionException("Could not get JCR root node", e);
        }

        // Change in the XML dump of artifactory configuration
        Node artConfNode = safeGetNode(rootNode, "configuration", "artifactory");
        if (artConfNode != null) {
            switchChecksumProperties(artConfNode);
        }

        // Add the standard checksum properties to exploded builds
        Node buildsNode = safeGetNode(rootNode, "builds");
        if (buildsNode != null) {
            convertBuilds(buildsNode);
        }

        log.info("Converting JCR storage for repositories data...");
        NodeIterator children;
        try {
            Node repositoriesNode = rootNode.getNode("repositories");
            children = repositoriesNode.getNodes();
        } catch (RepositoryException e) {
            throw new FatalConversionException("Could not save JCR session", e);
        }

        while (children.hasNext()) {
            Node repositoryNode = children.nextNode();
            convertFolderNode(repositoryNode);
        }

        try {
            // Making sure all is saved
            jcrSession.save();
        } catch (RepositoryException e) {
            log.error("Could not save JCR session", e);
        }
        log.info("Converted builds and {} JCR nodes in {} ms", nbNodesConverted, (System.currentTimeMillis() - start));
    }

    private void convertFolderNode(Node folderNode) {
        // Only artifactory folder can come in
        if (!JcrHelper.isFolder(folderNode)) {
            log.error("Recursive call to convert should be done only on folder node!");
            return;
        }
        convertNode(folderNode);

        try {
            NodeIterator children = folderNode.getNodes();
            while (children.hasNext()) {
                Node node = children.nextNode();
                String name = getNodeName(node);
                // Ignore metadata node, done in folder convert
                if (!name.equals(NODE_ARTIFACTORY_METADATA)) {
                    if (JcrHelper.isFile(node)) {
                        convertNode(node);
                    } else if (JcrHelper.isFolder(node)) {
                        convertFolderNode(node);
                    } else {
                        log.error("Node element from repo " + display(node) + " should be a file or folder");
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not migrate JCR children nodes " + display(folderNode), e);
        }
    }

    private String getNodeName(Node node) throws RepositoryException {
        return node.getName();
    }

    private void convertNode(Node node) {
        try {
            boolean mixinApplied = checkMixins(node, JcrTypes.MIX_ARTIFACTORY_BASE);
            if (mixinApplied) {
                // The node was an old version => need conversion
                if (JcrHelper.isFolder(node)) {
                    convertItemInfo(node, true);
                    convertMetadataNodes(node);
                    incrementAndSaveNode();
                } else if (JcrHelper.isFile(node)) {
                    convertItemInfo(node, false);
                    convertMetadataNodes(node);
                    incrementAndSaveNode();
                } else {
                    log.error("Node element from repo " + display(node) + " should be a file or folder");
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not migrate JCR node " + display(node), e);
        }
    }

    private void incrementAndSaveNode() throws RepositoryException {
        nbNodesConverted++;
        if (nbNodesConverted % 20 == 0) {
            jcrSession.save();
        }
        if (nbNodesConverted % 200 == 0) {
            log.info("" + nbNodesConverted + " JCR nodes converted...");
        }
    }

    private void convertMetadataNodes(Node node) throws RepositoryException {
        Node metadataContainerNode = safeGetNode(node, NODE_ARTIFACTORY_METADATA);
        if (metadataContainerNode != null) {
            // Convert metadata nodes
            NodeIterator iterator = metadataContainerNode.getNodes();
            while (iterator.hasNext()) {
                Node metadataNode = iterator.nextNode();
                log.debug("Converting metadata node " + metadataNode.getPath());
                convertMetadataNode(node, metadataNode);
            }
        }
    }

    private void convertMetadataNode(Node node, Node metadataNode) throws RepositoryException {
        String metadataName = getNodeName(metadataNode);
        if (metadataName.equals(FolderAdditionalInfo.ROOT) || metadataName.equals(FileAdditionalInfo.ROOT)) {
            // Should have been removed
            log.error("Ext metadata node " + metadataNode.getPath() + " should have been removed!");
            return;
        }
        String typeName = JcrHelper.getPrimaryTypeName(metadataNode);
        if (JcrTypes.NT_ARTIFACTORY_METADATA.equals(typeName)) {
            // Already done
            return;
        }
        if (metadataName.equals(org.artifactory.fs.FolderInfo.ROOT) ||
                metadataName.equals(org.artifactory.fs.FileInfo.ROOT)) {
            // Should have been removed
            log.error("There should not be a metadata node " + metadataNode.getPath() +
                    " for file item info!\n Deleting it!");
            metadataNode.remove();
            return;
        }

        MetadataDefinition definition = mdService.getMetadataDefinition(metadataName, true);
        // Retrieve the data, delete the node then re-create it
        String xmlData = getXmlData(safeGetNode(metadataNode, JCR_CONTENT));
        if (xmlData == null) {
            log.error("Metadata xml content for " + metadataNode.getPath() +
                    " does not contains data, ignored and removed!");
            metadataNode.remove();
        } else {
            Object metadata;
            if (definition.getXmlProvider() instanceof GenericXmlProvider) {
                metadata = xmlData;
            } else {
                metadata = getXstream(definition).fromXML(xmlData);
            }
            metadataNode.remove();
            definition.getPersistenceHandler().update(new MetadataAwareAdapter(node), metadata);
        }
    }

    private void convertItemInfo(Node node, boolean folder) throws RepositoryException {
        MetadataDefinition definition;
        if (folder) {
            definition = folderInfoMD;
        } else {
            definition = fileInfoMD;
        }
        Node addInfoNode =
                safeGetNode(node, NODE_ARTIFACTORY_METADATA,
                        folder ? FolderAdditionalInfo.ROOT : FileAdditionalInfo.ROOT);
        if (addInfoNode == null) {
            // We have a big problem, since we need to load AdditonalInfo
            log.error("JCR Node " + node.getPath() + " needs conversion and has no ext metadata!");
        } else {
            String xmlData = getXmlData(safeGetNode(addInfoNode, JCR_CONTENT));
            if (xmlData == null) {
                // We have a big problem, since we need to load AdditonalInfo
                log.error("JCR Node " + node.getPath() + " needs conversion and has no content for ext metadata");
            } else {
                // ATTENTION: This supposed that AdditionalInfo xstreamable from jcr:content
                // Should normally call the metadata converter here
                ItemAdditionalInfo itemAdditionalInfo = (ItemAdditionalInfo) getXstream(definition).fromXML(xmlData);
                org.artifactory.fs.ItemInfo itemInfo;
                if (folder) {
                    itemInfo = new FolderInfoImpl(JcrPath.get().getRepoPath(node.getPath()));
                    ((InternalFolderInfo) itemInfo).setAdditionalInfo((FolderAdditionalInfo) itemAdditionalInfo);
                } else {
                    itemInfo = new FileInfoImpl(JcrPath.get().getRepoPath(node.getPath()));
                    ((InternalFileInfo) itemInfo).setAdditionalInfo((FileAdditionalInfo) itemAdditionalInfo);
                }
                // The created and lastModified timestamps was coming from JCR property
                itemInfo.setCreated(getLongProperty(node, PROP_ARTIFACTORY_CREATED, getJcrCreated(node), false));
                itemInfo.setLastModified(
                        getLongProperty(node, PROP_ARTIFACTORY_LAST_MODIFIED, getJcrLastModified(node), false));
                // If JCR properties for createdBy or old modifiedBy is present use it over the AdditionalInfo XStream version
                itemInfo
                        .setCreatedBy(
                                getStringProperty(node, PROP_ARTIFACTORY_CREATED_BY, itemInfo.getCreatedBy(), true));
                itemInfo.setModifiedBy(
                        getStringProperty(node, "artifactory:modifiedBy", itemInfo.getModifiedBy(), true));
                itemInfo.setLastUpdated(
                        getLongProperty(node, PROP_ARTIFACTORY_LAST_UPDATED, itemInfo.getLastUpdated(), true));
                if (!folder) {
                    Node resNode = getResourceNode(node);
                    ((org.artifactory.fs.FileInfo) itemInfo).setSize(getLength(resNode));
                    ((org.artifactory.fs.FileInfo) itemInfo).setMimeType(getMimeType(resNode));
                }
                definition.getPersistenceHandler().update(new MetadataAwareAdapter(node), itemInfo);
                // Everything work, delete the additional info metadata entry
                addInfoNode.remove();
            }
        }
    }

    private String getXmlData(Node node) throws RepositoryException {
        if (node == null) {
            return null;
        }
        InputStream is = null;
        String xmlData = null;
        try {
            is = node.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
            xmlData = IOUtils.toString(is, "utf-8");
        } catch (IOException e) {
            log.warn("Could not read content of " + display(node), e);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return xmlData;
    }

    private XStream getXstream(MetadataDefinition<?> mdDef) {
        return ((XStreamMetadataProvider<?>) mdDef.getXmlProvider()).getXstream();
    }

    private void convertBuilds(Node buildsNode) {
        log.info("Converting build information graphs...");
        int nbBuild = 0;
        NodeIterator buildNames = safeGetChildren(buildsNode);
        while (buildNames.hasNext()) {
            Node buildNameNode = buildNames.nextNode();
            NodeIterator buildNumbers = safeGetChildren(buildNameNode);
            while (buildNumbers.hasNext()) {
                Node buildNumberNode = buildNumbers.nextNode();
                NodeIterator buildDates = safeGetChildren(buildNumberNode);
                while (buildDates.hasNext()) {
                    Node buildNode = buildDates.nextNode();
                    nbBuild++;
                    convertBuildNode(buildNode);
                }
            }
        }
        log.info("Converted " + nbBuild + " build information graphs!");
    }

    public void convertBuildNode(Node buildNode) {
        try {
            String xmlData = getXmlData(safeGetNode(buildNode, JCR_CONTENT));

            if (StringUtils.isBlank(xmlData)) {
                log.error("Build xml content for '{}' does not contains data, ignored and removed!",
                        buildNode.getPath());
                buildNode.remove();
            } else if (xmlData.contains("<build>")) {
                convertBuildNodeFromXmlData(buildNode, xmlData);
            } else {
                log.debug("Build node " + buildNode.getPath() + " already converted.");
            }
            jcrSession.save();
        } catch (Exception e) {
            log.error("Could not update build info " + buildNode, e);
        }
    }

    private void convertBuildNodeFromXmlData(Node buildNode, String xmlData) throws IOException, RepositoryException {
        InputStream xmlInputStream = null;
        ByteArrayOutputStream buildOutputStream = new ByteArrayOutputStream();
        ByteArrayInputStream buildInputStream = null;

        try {
            /**
             * TODO: [by nt] This will most-likely break when updating the build info next time.
             * TODO: [by nt] We will need a converter here for changing an old build XML to an up-to-date one
             */
            Build build = (Build) buildXstream.fromXML(xmlData);

            JsonGenerator jsonGenerator = JacksonFactory.createJsonGenerator(buildOutputStream);
            jsonGenerator.writeObject(build);
            buildInputStream = new ByteArrayInputStream(buildOutputStream.toByteArray());

            Calendar createdDate = Calendar.getInstance();
            if (buildNode.hasProperty(JcrTypes.PROP_ARTIFACTORY_CREATED)) {
                createdDate = buildNode.getProperty(JcrTypes.PROP_ARTIFACTORY_CREATED).getDate();
            }

            Calendar lastModified = Calendar.getInstance();
            if (buildNode.hasProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED)) {
                lastModified = buildNode.getProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED).getDate();
            }

            String userId = SecurityService.USER_SYSTEM;
            if (buildNode.hasProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED_BY)) {
                userId = buildNode.getProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED_BY).getString();
            }

            Set<String> artifactChecksums = Sets.newHashSet();
            Set<String> dependencyChecksums = Sets.newHashSet();

            List<Module> modules = build.getModules();
            if (modules != null) {
                for (Module module : modules) {
                    addBuildFileChecksums(module.getArtifacts(), artifactChecksums);
                    addBuildFileChecksums(module.getDependencies(), dependencyChecksums);
                }
            }

            Node buildParent = buildNode.getParent();
            String buildNodeName = getNodeName(buildNode);

            buildNode.remove();

            Node newBuildNode = buildParent.addNode(buildNodeName);

            newBuildNode.setProperty(JcrTypes.PROP_ARTIFACTORY_CREATED, createdDate);
            newBuildNode.setProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED, lastModified);
            newBuildNode.setProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED_BY, userId);
            saveBuildChecksumsProperty(newBuildNode, JcrTypes.PROP_BUILD_ARTIFACT_CHECKSUMS, artifactChecksums);
            saveBuildChecksumsProperty(newBuildNode, JcrTypes.PROP_BUILD_DEPENDENCY_CHECKSUMS, dependencyChecksums);

            Node resourceNode = newBuildNode.addNode(JCR_CONTENT, NT_RESOURCE);
            resourceNode.setProperty(JCR_MIMETYPE, "application/vnd.org.jfrog.build.BuildInfo+json");
            resourceNode.setProperty(JCR_ENCODING, "utf-8");
            resourceNode.setProperty(JCR_LASTMODIFIED, lastModified);

            //Write the raw data and calculate checksums on it
            ChecksumInputStream checksumInputStream = new ChecksumInputStream(buildInputStream,
                    new Checksum(ChecksumType.md5),
                    new Checksum(ChecksumType.sha1));
            //Save the data and calculate the checksums
            resourceNode.setProperty(JCR_DATA, checksumInputStream);
            checksumInputStream.close();

            for (Checksum checksum : checksumInputStream.getChecksums()) {
                //Save the checksum as a property
                String checksumStr = checksum.getChecksum();
                ChecksumType checksumType = checksum.getType();
                String propName = checksumType.getActualPropName();
                newBuildNode.setProperty(propName, checksumStr);
            }
        } finally {
            IOUtils.closeQuietly(xmlInputStream);
            IOUtils.closeQuietly(buildOutputStream);
            IOUtils.closeQuietly(buildInputStream);
        }
    }

    private void addBuildFileChecksums(List<? extends BuildFileBean> buildFiles, Set<String> buildFileChecksums) {
        if (buildFiles != null) {
            for (BuildFileBean buildFile : buildFiles) {
                String md5 = buildFile.getMd5();
                String sha1 = buildFile.getSha1();

                if (StringUtils.isNotBlank(md5)) {
                    buildFileChecksums.add(BuildService.BUILD_CHECKSUM_PREFIX_MD5 + md5);
                }
                if (StringUtils.isNotBlank(sha1)) {
                    buildFileChecksums.add(BuildService.BUILD_CHECKSUM_PREFIX_SHA1 + sha1);
                }
            }
        }
    }

    private void saveBuildChecksumsProperty(Node buildStartedNode, String checksumPropName, Set<String> checksums)
            throws RepositoryException {
        if (!checksums.isEmpty()) {
            buildStartedNode.setProperty(checksumPropName, checksums.toArray(new String[checksums.size()]));
        }
    }

    private static NodeIterator safeGetChildren(Node node) {
        try {
            return node.getNodes();
        } catch (RepositoryException e) {
            log.warn("Ignoring exception during safe get nodes!", e);
            return EMPTY_NODE_ITERATOR;
        }
    }

    private void switchChecksumProperties(Node artConfNode) {
        ChecksumType[] types = ChecksumType.values();
        for (ChecksumType type : types) {
            String origPropName = getChecksumOldPropertyName(type);
            String actualPropName = type.getActualPropName();
            switchProperty(artConfNode, origPropName, actualPropName);
        }
    }

    private static void switchProperty(Node node, String origPropName, String actualPropName) {
        try {
            if (node.hasProperty(origPropName)) {
                Property property = node.getProperty(origPropName);
                node.setProperty(actualPropName, property.getValue());
                property.remove();
            }
        } catch (RepositoryException e) {
            log.error("Could not switch JCR property " + origPropName + " to " + actualPropName +
                    " on node " + display(node), e);
        }
    }

    // the artifactory:modifiedBy does not exists anymore. Replaced with lastModifiedBy.

    /**
     * Used since all the artifactory types (file, folder, metadata) needs to have the artifactory:base mixin.
     *
     * @param node   the jcr node that should have the mixins
     * @param mixins the list of mixins needed
     * @return true if some mixin were added, false if the node was ok
     */
    public static boolean checkMixins(Node node, String... mixins) throws RepositoryException {
        boolean setSomeMixin = false;
        if (mixins != null && mixins.length > 0) {
            NodeType[] types = node.getMixinNodeTypes();
            for (String mixin : mixins) {
                boolean ok = false;
                for (NodeType type : types) {
                    if (type.isNodeType(mixin)) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    node.addMixin(mixin);
                    setSomeMixin = true;
                }
            }
        }
        return setSomeMixin;
    }

    // The checksum property on metadata node is renamed from this to checksumType.getActualPropName

    public static String getChecksumOldPropertyName(ChecksumType checksumType) {
        return JcrTypes.ARTIFACTORY_PREFIX + checksumType.alg();
    }
}