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
import static org.apache.jackrabbit.JcrConstants.*;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumCalculator;
import org.artifactory.io.checksum.ChecksumInputStream;
import org.artifactory.jcr.JcrRepoService;
import org.artifactory.jcr.JcrService;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.ReloadableBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Autowired
    private JcrRepoService jcrRepoService;

    @Autowired
    private AuthorizationService authorizationService;

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

    public void saveChecksums(MetadataAware metadataAware, String metadataName, Checksum[] checksums) {
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
    public <MD> MD getXmlMetadataObject(MetadataAware metadataAware, Class<MD> clazz, boolean createIfMissing) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        //If the node does not exist create it on demand, depending on specified flag, else try to
        //serve it from the metadata container
        return (MD) getMetadataValue(metadataAware, definition, createIfMissing);
    }

    public String getXmlMetadata(MetadataAware metadataAware, String metadataName) {
        Node metadataContainer = metadataAware.getMetadataContainer();
        try {
            if (!metadataContainer.hasNode(metadataName)) {
                return null;
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            writeRawXmlStream(metadataAware, metadataName, os);
            return os.toString("utf-8");
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed to read xml data '" + metadataName + "'.", e);
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
            setXmlMetadataInternal(metadataAware, metadataName, new ByteArrayInputStream(os.toByteArray()));
        } finally {
            IOUtils.closeQuietly(os);
        }
        return metadataName;
    }

    public void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is) {
        setXmlMetadata(metadataAware, metadataName, is, null);
    }

    public void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is, StatusHolder status) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(metadataName);
        if (definition.isInternal()) {
            try {
                metadataAware.importInternalMetadata(definition, definitionService.getXstream().fromXML(is));
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            if (definition.isPersistent()) {
                setXmlMetadataInternal(metadataAware, metadataName, is);
            } else if (status != null) {
                IOUtils.closeQuietly(is);
                status.setWarning(
                        "Read metadata file " + metadataName + " for item " + metadataAware.getAbsolutePath() +
                                " was declared non persistent.\n" + "Data will be discarded!", log);
            }
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

    public void removeXmlMetadata(MetadataAware obj, String metadataName) {
        setXmlMetadataInternal(obj, metadataName, null);
    }

    public List<String> getXmlMetadataNames(MetadataAware obj) {
        Node metadataContainer = obj.getMetadataContainer();
        try {
            NodeIterator children = metadataContainer.getNodes();
            List<String> names = new ArrayList<String>();
            while (children.hasNext()) {
                Node child = (Node) children.next();
                String name = child.getName();
                MetadataDefinition definition = definitionService.getMetadataDefinition(name);
                if (!definition.isInternal() && definition.isPersistent()) {
                    names.add(name);
                }
            }
            return names;
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get xml data names.", e);
        }
    }

    @SuppressWarnings({"MalformedFormatString"})
    public Set<ChecksumInfo> getOrCreateChecksums(MetadataAware metadataAware, String metadataName)
            throws RepositoryException {
        Set<ChecksumInfo> checksumInfos = new HashSet<ChecksumInfo>();
        Node metadataNode = getMetadataNode(metadataAware, metadataName);
        ChecksumType[] checksumTypes = ChecksumType.values();
        for (ChecksumType checksumType : checksumTypes) {
            String propName = getChecksumPropertyName(checksumType);
            if (metadataNode.hasProperty(propName)) {
                Property property = metadataNode.getProperty(propName);
                String checksumValue = property.getString();
                checksumInfos.add(new ChecksumInfo(checksumType, checksumValue, checksumValue));
                log.debug("Found metadata checksum '{}' for '{}#{}'",
                        new Object[]{checksumType, metadataAware, metadataName});
            }
        }
        if (checksumTypes.length == 0) {
            //No checksums found - need to create them
            Checksum[] checksums;
            InputStream is = getRawXmlStream(metadataNode);
            try {
                checksums = ChecksumCalculator.calculateAll(is);
            } catch (IOException e) {
                throw new RepositoryRuntimeException(String.format(
                        "Failed to compute metadata checksums for '%1$#%2$'", metadataAware, metadataName));
            } finally {
                IOUtils.closeQuietly(is);
            }
            //Store async (we are read-only)
            StoreChecksumsMessage message = new StoreChecksumsMessage(metadataAware, metadataName, checksums);
            repoService.publish(message);
            for (Checksum checksum : checksums) {
                log.debug("Creating non-exitent metadata checksum '{}' for '{}#{}'",
                        new Object[]{checksum.getType(), metadataAware, metadataName});
                checksumInfos.add(new ChecksumInfo(checksum.getType(), checksum.getChecksum(), checksum.getChecksum()));
            }
        }
        return checksumInfos;
    }

    public Node getResourceNode(MetadataAware metadataAware, String metadataName) throws RepositoryException {
        Node metadataNode = getMetadataNode(metadataAware, metadataName);
        boolean hasResourceNode = metadataNode.hasNode(JCR_CONTENT);
        if (!hasResourceNode) {
            throw new IllegalArgumentException("Cannot get last modified from resourceless metadata.");
        }
        Node resourceNode = metadataNode.getNode(JCR_CONTENT);
        return resourceNode;
    }

    public MetadataInfo getMetadataInfo(MetadataAware metadataAware, String metadataName) {
        MetadataInfo mdi = new MetadataInfo(metadataAware.getRepoPath(), metadataName);
        try {
            Node metadataNode = getMetadataNode(metadataAware, metadataName);
            Calendar created = metadataNode.getProperty(MetadataAware.PROP_ARTIFACTORY_CREATED).getDate();
            Node resourceNode = getResourceNode(metadataAware, metadataName);
            mdi.setCreated(created.getTimeInMillis());
            Calendar lastModified =
                    metadataNode.getProperty(MetadataAware.PROP_ARTIFACTORY_LAST_MODIFIED).getDate();
            mdi.setLastModified(lastModified.getTimeInMillis());
            String lastModifiedBy =
                    metadataNode.getProperty(MetadataAware.PROP_ARTIFACTORY_LAST_MODIFIED_BY).getString();
            mdi.setLastModifiedBy(lastModifiedBy);
            mdi.setLastModified(lastModified.getTimeInMillis());
            Set<ChecksumInfo> checksums = getOrCreateChecksums(metadataAware, metadataName);
            mdi.setChecksums(checksums);
            mdi.setSize(resourceNode.getProperty(JCR_DATA).getLength());
        } catch (RepositoryException e) {
            throw new IllegalArgumentException(
                    "Cannot get metadta info " + metadataName + " for " + metadataAware + ".", e);
        }
        return mdi;
    }

    public boolean hasXmlMetdata(MetadataAware metadataAware, String metadataName) {
        Node metadataContainer = metadataAware.getMetadataContainer();
        try {
            return metadataContainer.hasNode(metadataName);
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Cannot determine existence of '" + metadataName + "' metadata.", e);
        }
    }

    private Node getMetadataNode(MetadataAware metadataAware, String metadataName) {
        Node metadataContainer = metadataAware.getMetadataContainer();
        Node metadataNode = jcr.getOrCreateUnstructuredNode(metadataContainer, metadataName);
        return metadataNode;
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private Object getMetadataValue(
            MetadataAware metadataAware, MetadataDefinition definition, boolean createIfMissing) {
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
            throw new RepositoryRuntimeException("Failed to get metadata value for " + metadataAware, e);
        }
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private void setXmlMetadataInternal(MetadataAware metadataAware, String metadataName, InputStream is) {
        Node metadataContainer;
        InputStream xmlStream = null;
        ChecksumInputStream checksumInputStream = null;
        try {
            metadataContainer = metadataAware.getMetadataContainer();
            Node metadataNode;
            Node xmlNode;
            //TODO: No more JCR-1554 with readWriteLock... Need Testing
            // To avoid JCR-1554 we should create the node, unless it already exists
            boolean exists = metadataContainer.hasNode(metadataName);
            Calendar created;
            if (exists) {
                metadataNode = metadataContainer.getNode(metadataName);
                try {
                    //Maintain the creation date if already exists and overriden
                    created = metadataNode.getProperty(MetadataAware.PROP_ARTIFACTORY_CREATED).getDate();
                    metadataNode.remove();
                } catch (RepositoryException e) {
                    throw new RepositoryRuntimeException(
                            "Unable to remove existing xml data '" + metadataNode + "'", e);
                }
            } else {
                created = Calendar.getInstance();
            }
            if (is == null) {
                //Just the remove the md and return
                return;
            }
            //Add the metdata node and its xml child
            metadataNode = metadataContainer.addNode(metadataName);
            xmlNode = metadataNode.addNode(MetadataAware.NODE_ARTIFACTORY_XML);

            //Cache the xml in memory since when reading from an http stream directly we cannot mark
            String xml = IOUtils.toString(is, "utf-8");
            xmlStream = IOUtils.toInputStream(xml, "utf-8");

            //Import the xml: an xmlNode (with metadataName as its root element) will be created
            //from the input stream
            metadataNode.setProperty(MetadataAware.PROP_ARTIFACTORY_CREATED, created);
            //Update the last modified on the specific metadata
            Calendar lastModified = Calendar.getInstance();
            metadataNode.setProperty(MetadataAware.PROP_ARTIFACTORY_LAST_MODIFIED, lastModified);
            String userId = authorizationService.currentUsername();
            metadataNode.setProperty(MetadataAware.PROP_ARTIFACTORY_LAST_MODIFIED_BY, userId);
            jcrRepoService.importXml(xmlNode, xmlStream);
            xmlStream.reset();
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
            checksumInputStream = new ChecksumInputStream(xmlStream,
                    new Checksum(ChecksumType.md5),
                    new Checksum(ChecksumType.sha1));
            //Save the data and calc the checksums
            resourceNode.setProperty(JCR_DATA, checksumInputStream);
            setChecksums(metadataNode, checksumInputStream.getChecksums(), true);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to set xml data.", e);
        } finally {
            IOUtils.closeQuietly(checksumInputStream);
            IOUtils.closeQuietly(xmlStream);
            IOUtils.closeQuietly(is);
        }
    }

    private static void setChecksums(Node metadataNode, Checksum[] checksums, boolean override)
            throws RepositoryException {
        for (Checksum checksum : checksums) {
            //Save the checksum as a property
            String metadataName = metadataNode.getName();
            String checksumStr = checksum.getChecksum();
            ChecksumType checksumType = checksum.getType();
            String propName = getChecksumPropertyName(checksumType);
            if (override || !metadataNode.hasProperty(propName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Saving checksum for '" + metadataName + "' as '" + propName + "' (checksum=" +
                            checksumStr + ").");
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

    private static String getChecksumPropertyName(ChecksumType checksumType) {
        return MetadataAware.ARTIFACTORY_PREFIX + checksumType.alg();
    }
}
