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
package org.artifactory.jcr.md;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import static org.apache.jackrabbit.JcrConstants.*;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.config.xml.EntityResolvingContentHandler;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.io.checksum.ChecksumType;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:27:38 PM
 */
@Service
public class MetadataServiceImpl implements MetadataService {
    private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

    @Autowired
    private MetadataDefinitionService definitionService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private JcrService jcr;

    @PostConstruct
    public void register() {
        InternalContextHelper.get().addReloadableBean(MetadataService.class);
    }

    public void init() {
    }

    @SuppressWarnings({"unchecked"})
    public Class<? extends ReloadableBean>[] initAfter() {
        return new Class[]{MetadataDefinitionService.class, InternalCacheService.class};
    }

    public void reload(CentralConfigDescriptor oldDescriptor) {
    }

    public void destroy() {
    }

    public void saveChecksums(
            MetadataAware metadataAware, String metadataName, Checksum[] checksums) {
        Node metadataNode = getMetadataNode(metadataAware, metadataName);
        try {
            setChecksums(metadataNode, checksums, false);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to save metadta checksums.", e);
        }
    }

    public <MD> MD getXmlMetadataObject(MetadataAware obj, Class<MD> clazz) {
        return getXmlMetadataObject(obj, clazz, true);
    }

    @SuppressWarnings({"unchecked"})
    public <MD> MD getXmlMetadataObject(MetadataAware metadataAware, Class<MD> clazz,
            boolean createIfMissing) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        //If the node does not exist create it on demand, depending on specified flag, else try to
        //serve it from the metadata container
        return (MD) getMetadataValue(metadataAware, definition, createIfMissing);
    }

    public String getXmlMetadata(MetadataAware obj, String metadataName) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            writeRawXmlStream(obj, metadataName, os);
            return os.toString("utf-8");
        } catch (Exception e) {
            throw new RepositoryRuntimeException(
                    "Failed to read xml data '" + metadataName + "'.", e);
        }
    }

    public String setXmlMetadata(MetadataAware metadataAware, Object xstreamable) {
        Class clazz = xstreamable.getClass();
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        if (!definition.isPersistent()) {
            throw new IllegalArgumentException(
                    "Metadata " + definition + " is declared non persisten so cannot be saved!");
        }
        String metadataName = definition.getMetadataName();
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            definitionService.getXstream().toXML(xstreamable, os);
            setXmlMetadata(metadataAware, metadataName, new ByteArrayInputStream(os.toByteArray())
            );
        } finally {
            IOUtils.closeQuietly(os);
        }
        return metadataName;
    }

    public void importXmlMetadata(MetadataAware metadataAware, String metadataName,
            InputStream is, StatusHolder status) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(metadataName);
        if (definition.isInternal()) {
            try {
                metadataAware.importInternalMetadata(definition,
                        definitionService.getXstream().fromXML(is));
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            if (definition.isPersistent()) {
                setXmlMetadata(metadataAware, metadataName, is);
            } else {
                status.setWarning(
                        "Read metadata file " + metadataName + " for item " + metadataAware.getAbsolutePath() +
                                " was declared non persistent.\n" + "Data will be discarded!", log);
            }
        }
    }

    public void removeXmlMetadata(MetadataAware obj, String metadataName) {
        setXmlMetadata(obj, metadataName, null);
    }

    public List<String> getExtraXmlMetadataNames(MetadataAware obj) {
        Node metadataContainer = obj.getMetadataContainer();
        try {
            NodeIterator children = metadataContainer.getNodes();
            List<String> names = new ArrayList<String>();
            while (children.hasNext()) {
                Node child = (Node) children.next();
                String name = child.getName();
                MetadataDefinition definition = this.definitionService.getMetadataDefinition(name);
                if (!definition.isInternal()) {
                    names.add(name);
                }
            }
            return names;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get xml data names.", e);
        }
    }

    public void writeRawXmlStream(MetadataAware obj, String metadataName, OutputStream out) {
        InputStream is = null;
        try {
            //Read the raw xml
            Node metadataNode = getMetadataNode(obj, metadataName);
            is = getRawXmlStream(metadataNode);
            BufferedInputStream bis = new BufferedInputStream(is);
            //Write it to the out stream
            IOUtils.copy(bis, out);
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed to read xml metadata stream.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * Import xml with characters entity resolving
     *
     * @param xmlNode
     * @param in
     * @throws javax.jcr.RepositoryException
     * @throws java.io.IOException
     */
    public void importXml(Node xmlNode, InputStream in)
            throws RepositoryException, IOException {
        if (!xmlNode.isNew()) {
            NodeIterator children = xmlNode.getNodes();
            while (children.hasNext()) {
                Node child = (Node) children.next();
                child.remove();
            }
        }
        final String absPath = xmlNode.getPath();
        JcrSession session = getSession();
        ContentHandler contentHandler = session.getImportContentHandler(
                absPath, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        EntityResolvingContentHandler resolvingContentHandler =
                new EntityResolvingContentHandler(contentHandler);
        NonClosingInputStream ncis = null;
        try {
            SAXParserFactory factory = ArtifactoryXmlFactory.getFactory();
            SAXParser parser = factory.newSAXParser();
            ncis = new NonClosingInputStream(in);
            parser.parse(ncis, resolvingContentHandler);
        } catch (ParserConfigurationException e) {
            //Here ncis is always null
            throw new RepositoryException("SAX parser configuration error", e);
        } catch (Exception e) {
            //Check for wrapped repository exception
            if (e instanceof SAXException) {
                Exception e1 = ((SAXException) e).getException();
                if (e1 != null && e1 instanceof RepositoryException) {
                    if (ncis != null) {
                        ncis.forceClose();
                    }
                    throw (RepositoryException) e1;
                }
            }
            log.warn("Failed to parse XML stream to import into '" + absPath + "'.", e);
        }
    }

    private Node getMetadataNode(MetadataAware metadataAware, String metadataName) {
        Node metadataContainer = metadataAware.getMetadataContainer();
        Node metadataNode = jcr.getOrCreateUnstructuredNode(metadataContainer, metadataName);
        return metadataNode;
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private Object getMetadataValue(MetadataAware metadataAware,
            MetadataDefinition definition, boolean createIfMissing) {
        if (!definition.isPersistent()) {
            throw new IllegalArgumentException(
                    "Metadata Definition " + definition + " cannot be used in xml container");
        }
        String metadataName = definition.getMetadataName();
        try {
            Node metadataContainer = metadataAware.getMetadataContainer();
            if (!metadataContainer.hasNode(metadataName)) {
                if (createIfMissing) {
                    //Create empty data object
                    return definition.newInstance();
                } else {
                    // Does not exists
                    return null;
                }
            } else {
                //Read and cache the metadata from storage
                String xml = getXmlMetadata(metadataAware, metadataName);
                return definitionService.getXstream().fromXML(xml);
            }

        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to get metadata value for " + metadataAware,
                    e);
        }
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is) {
        Node metadataContainer;
        try {
            metadataContainer = metadataAware.getMetadataContainer();
            Node metadataNode;
            Node xmlNode;
            //TODO: No more JCR-1554 with readWriteLock... Need Testing
            // To avoid JCR-1554 we should create the node, unless it already exists
            boolean exists = metadataContainer.hasNode(metadataName);
            if (exists) {
                metadataNode = metadataContainer.getNode(metadataName);
                try {
                    metadataNode.remove();
                } catch (RepositoryException e) {
                    throw new RepositoryRuntimeException(
                            "Unable to remove existing xml data '" + metadataNode + "'", e);
                }
            }
            if (is == null) {
                // just the remove need to be done
                return;
            }
            //Add the metdata node and its xml child
            metadataNode = metadataContainer.addNode(metadataName);
            xmlNode = metadataNode.addNode(MetadataAware.NODE_ARTIFACTORY_XML);

            is.mark(Integer.MAX_VALUE);
            //Import the xml: an xmlNode (with metadataName as its root element) will be created
            //from the input stream with
            //Update the last modified on the specific metadata
            Calendar lastModified = Calendar.getInstance();
            metadataNode.setProperty(MetadataAware.PROP_ARTIFACTORY_LAST_MODIFIED_METADATA,
                    lastModified);
            importXml(xmlNode, is);
            is.reset();
            //Set the original raw xml
            Node resourceNode;
            boolean hasResourceNode = metadataNode.hasNode(JCR_CONTENT);
            if (hasResourceNode) {
                resourceNode = metadataNode.getNode(JCR_CONTENT);
            } else {
                resourceNode = metadataNode.addNode(JCR_CONTENT, NT_RESOURCE);
            }
            resourceNode.setProperty(JCR_MIMETYPE, ContentType.applicationXml.getMimeType());
            resourceNode.setProperty(JCR_ENCODING, "utf-8");
            resourceNode.setProperty(JCR_LASTMODIFIED, lastModified);
            //Write the raw data and calculate checksums on it
            ChecksumInputStream checksumInputStream = new ChecksumInputStream(is,
                    new Checksum(metadataName, ChecksumType.md5),
                    new Checksum(metadataName, ChecksumType.sha1));
            //Save the data and calc the checksums
            resourceNode.setProperty(JCR_DATA, checksumInputStream);
            setChecksums(metadataNode, checksumInputStream.getChecksums(), true);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to set xml data.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @SuppressWarnings({"MalformedFormatString"})
    public String getOrCreateChecksum(
            MetadataAware metadataAware, String metadataName, ChecksumType checksumType)
            throws RepositoryException {
        Node metadataNode = getMetadataNode(metadataAware, metadataName);
        String propName = getChecksumPropertyName(checksumType);
        String[] args = {propName, metadataAware.getAbsolutePath(), metadataName};
        log.debug("Requested metadata checksum '{}' for '{}#{}'", args);
        if (!metadataNode.hasProperty(propName)) {
            log.debug("Creating non-exitent metadata checksum '{}' for '{}#{}'", args);
            //Need to create non-existing checksum
            ChecksumInputStream checksumInputStream = null;
            try {
                InputStream is = getRawXmlStream(metadataNode);
                checksumInputStream = new ChecksumInputStream(is,
                        new Checksum(metadataName, ChecksumType.md5),
                        new Checksum(metadataName, ChecksumType.sha1));
                //Stream the value through the checksum calculating stream
                IOUtils.copy(checksumInputStream, new NullOutputStream());
            } catch (IOException e) {
                throw new RepositoryRuntimeException(String.format(
                        "Failed to compute metadata checksum '%1$' for '%2$#%3$'",
                        ((Object[]) args)));
            } finally {
                IOUtils.closeQuietly(checksumInputStream);
            }
            Checksum[] checksums = checksumInputStream.getChecksums();
            StoreChecksumsMessage message =
                    new StoreChecksumsMessage(metadataAware, metadataName, checksums);
            repoService.publish(message);
            //Return only the value of the checksum we care about
            for (Checksum checksum : checksums) {
                if (checksum.getType() == checksumType) {
                    return checksum.getChecksum();
                }
            }
            throw new IllegalArgumentException(
                    "Unknown checksum type: " + checksumType.alg() + ".");
        }
        Property property = metadataNode.getProperty(propName);
        String value = property.getString();
        return value;
    }

    public static String getChecksumPropertyName(ChecksumType checksumType) {
        return MetadataAware.ARTIFACTORY_PREFIX + checksumType.alg();
    }

    private static void setChecksums(Node metadataNode, Checksum[] checksums, boolean overide)
            throws RepositoryException {
        for (Checksum checksum : checksums) {
            //Save the checksum as a property
            String metadataName = metadataNode.getName();
            String checksumStr = checksum.getChecksum();
            ChecksumType checksumType = checksum.getType();
            String propName = getChecksumPropertyName(checksumType);
            if (overide || !metadataNode.hasProperty(propName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Saving checksum for '" + metadataName + "' as '" + propName +
                            "' (checksum=" + checksumStr + ").");
                }
                metadataNode.setProperty(propName, checksumStr);
            }
        }
    }

    private static InputStream getRawXmlStream(Node metadataNode) throws RepositoryException {
        Node xmlNode = metadataNode.getNode(JCR_CONTENT);
        Value attachedDataValue = xmlNode.getProperty(JCR_DATA).getValue();
        InputStream is = attachedDataValue.getStream();
        return is;
    }

    private JcrSession getSession() {
        return jcr.getManagedSession();
    }
}
