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

import org.apache.commons.collections15.map.ReferenceMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import static org.apache.jackrabbit.JcrConstants.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.config.xml.ArtifactoryXmlFactory;
import org.artifactory.config.xml.EntityResolvingContentHandler;
import org.artifactory.io.NonClosingInputStream;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrSession;
import org.artifactory.jcr.lock.SessionLockManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

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
import java.util.Collection;
import java.util.List;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:27:38 PM
 */
@Service
public class MetadataServiceImpl implements MetadataService {
    private static final Logger LOGGER =
            LogManager.getLogger(MetadataServiceImpl.class);

    @Autowired
    private MetadataDefinitionService definitionService;

    @Autowired
    private JcrService jcr;

    @Autowired
    private AuthorizationService authService;

    private final ReferenceMap<MetadataKey, MetadataValue> cache =
            new ReferenceMap<MetadataKey, MetadataValue>(
                    ReferenceMap.HARD, ReferenceMap.SOFT, 5000, 0.75f, false);

    public Node getMetadataNode(MetadataAware obj, String metadataName) {
        Node metadataContainer = obj.getMetadataContainer();
        Node metadataNode = jcr.getOrCreateUnstructuredNode(metadataContainer, metadataName);
        return metadataNode;
    }

    public MetadataValue lockCreateIfEmpty(Class clazz, String absolutePath) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        String metadataName = definition.getMetadataName();
        MetadataKey metadataKey = new MetadataKey(absolutePath, metadataName);
        MetadataValue value;
        synchronized (cache) {
            value = cache.get(metadataKey);
            if (value == null) {
                value = createEmptyValue(metadataKey);
                cache.put(metadataKey, value);
            }
        }
        value.lock();
        synchronized (cache) {
            return cache.get(metadataKey);
        }
    }

    @SuppressWarnings({"unchecked"})
    public <MD> MD getLockedXmlMetadataObject(MetadataAware obj, Class<MD> clazz) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        MetadataValue value = getMetadataValue(obj, definition, true);
        value.lock();
        return (MD) value.getValue();
    }

    public void delete(String absolutePath) {
        Collection<String> mdNames = definitionService.getAllDefinitionNames();
        synchronized (cache) {
            for (String mdName : mdNames) {
                MetadataKey key = new MetadataKey(absolutePath, mdName);
                cache.remove(key);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public <MD> MD getInfoFromCache(String absolutePath, Class<MD> clazz) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        MetadataKey key = new MetadataKey(absolutePath, definition.getMetadataName());
        MetadataValue value;
        synchronized (cache) {
            value = cache.get(key);
        }
        if (value != null) {
            return (MD) value.getValue();
        }
        return null;
    }

    public void unlockNoSave(Class clazz, String absolutePath) {
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        MetadataKey key = new MetadataKey(absolutePath, definition.getMetadataName());
        SessionLockManager sessionLockManager = getLockManager();
        MetadataValue value = sessionLockManager.getLockedMetadata(key);
        if (value != null) {
            value.unlock(sessionLockManager, false);
        } else {
            throw new LockingException("Trying to unlock " + key + " which is not locked by me!");
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
        //serve it from the metadata cache
        MetadataValue value = getMetadataValue(metadataAware, definition, createIfMissing);
        if (value != null) {
            return (MD) value.getValue();
        }
        return null;
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

    @Transactional
    public String setXmlMetadata(MetadataAware metadataAware, Object xstreamable) {
        Class clazz = xstreamable.getClass();
        MetadataDefinition definition = definitionService.getMetadataDefinition(clazz);
        String metadataName = definition.getMetadataName();
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            definitionService.getXstream().toXML(xstreamable, os);
            setXmlMetadata(metadataAware, metadataName, new ByteArrayInputStream(os.toByteArray()),
                    false);
        } finally {
            IOUtils.closeQuietly(os);
        }
        return metadataName;
    }

    public void importXmlMetadata(MetadataAware metadataAware, String metadataName,
            InputStream is) {
        setXmlMetadata(metadataAware, metadataName, is, true);
    }

    @Transactional
    public void removeXmlMetadata(MetadataAware obj, String metadataName) {
        setXmlMetadata(obj, metadataName, null, true);
    }

    public List<String> getXmlMetadataNames(MetadataAware obj) {
        Node metadataContainer = obj.getMetadataContainer();
        try {
            NodeIterator children = metadataContainer.getNodes();
            List<String> names = new ArrayList<String>();
            while (children.hasNext()) {
                Node child = (Node) children.next();
                String name = child.getName();
                names.add(name);
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
            Node xmlNode = metadataNode.getNode(JCR_CONTENT);
            Value attachedDataValue = xmlNode.getProperty(JCR_DATA).getValue();
            is = attachedDataValue.getStream();
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
        NodeIterator children = xmlNode.getNodes();
        while (children.hasNext()) {
            Node child = (Node) children.next();
            //Somehow later addition by xml import is not affected by JCR-1554
            child.remove();
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
            LOGGER.warn("Failed to parse XML stream to import into '" + absPath + "'.", e);
        }
    }

    @SuppressWarnings({"OverlyComplexMethod"})
    private MetadataValue getMetadataValue(MetadataAware metadataAware,
            MetadataDefinition definition, boolean createIfMissing) {
        String metadataName = definition.getMetadataName();
        MetadataKey metadataKey = new MetadataKey(metadataAware, metadataName);

        // First check the session lock objects
        MetadataValue value = getLockManager().getLockedMetadata(metadataKey);
        if (value != null) {
            value.assertLockOwner();
            return value;
        }

        // Then the global cache
        synchronized (cache) {
            value = cache.get(metadataKey);
        }
        long storedLastModified = -1L;
        try {
            if (value != null) {
                // Found in cache check validity
                if (value.isTransient()) {
                    value.waitIfTransient();
                    return value;
                } else {
                    Node metadataContainer = metadataAware.getMetadataContainer();
                    if (!metadataContainer.hasNode(metadataName)) {
                        if (!createIfMissing) {
                            return null;
                        }
                    } else {
                        Node metadataNode = metadataContainer.getNode(metadataName);
                        Property property = metadataNode.getProperty(
                                MetadataAware.PROP_ARTIFACTORY_LAST_MODIFIED_METADATA);
                        storedLastModified = property.getDate().getTimeInMillis();

                        if (value.getValue() != null) {
                            //Need to compare the cached md with the stored one's lastModified property, to
                            //ensure the cached value is valid (might have changed by another cluster node)
                            long cachedLastModified = value.getLastModified();
                            if (storedLastModified <= cachedLastModified) {
                                //Cache found and is good - use it
                                return value;
                            }
                        }
                    }
                }
            }

            synchronized (cache) {
                value = cache.get(metadataKey);
                Node metadataContainer = metadataAware.getMetadataContainer();
                if (!metadataContainer.hasNode(metadataName)) {
                    if (createIfMissing) {
                        if (value == null) {
                            //Create empty data object
                            value = createEmptyValue(metadataKey);
                            cache.put(metadataKey, value);
                        }
                        return value;
                    } else {
                        // Does not exists
                        return null;
                    }
                } else {
                    //Read and cache the metadata from storage
                    String xml = getXmlMetadata(metadataAware, metadataName);
                    Object data = definitionService.getXstream().fromXML(xml);
                    if (value == null) {
                        value = new MetadataValue(metadataKey, data, storedLastModified);
                        cache.put(metadataKey, value);
                    } else {
                        value.update(data, storedLastModified);
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Failed to get metadata value for " + metadataKey,
                    e);
        }
        return value;
    }

    private void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is,
            boolean updateCache) {
        Node metadataContainer;
        try {
            metadataContainer = metadataAware.getMetadataContainer();
            MetadataValue value;
            //If the object exists in the cache, it needs to have been locked by me
            MetadataKey metadataKey = new MetadataKey(metadataAware, metadataName);
            MetadataDefinition definition = definitionService.getMetadataDefinition(metadataName);
            Node metadataNode;
            Node xmlNode;
            synchronized (cache) {
                MetadataValue localValue =
                        getLockManager().getLockedMetadata(metadataKey);
                value = cache.get(metadataKey);
                if (localValue != null && value == null) {
                    // Add to the cache
                    value = localValue;
                    cache.put(metadataKey, value);
                } else if (value != null) {
                    if (!value.isLockedByMe()) {
                        throw new LockingException(
                                "Node " + value.toString() + " is not locked by current session");
                    }
                } else {
                    // Create a cache entry for locking purposes, if is not null
                    value = createEmptyValue(metadataKey);
                    cache.put(metadataKey, value);
                }
                //To avoid JCR-1554 we should create the node, unless it already exists
                boolean exists = metadataContainer.hasNode(metadataName);
                if (exists) {
                    try {
                        metadataNode = metadataContainer.getNode(metadataName);
                        xmlNode = metadataNode.getNode(MetadataAware.NODE_ARTIFACTORY_XML);
                    } catch (RepositoryException e) {
                        throw new RepositoryRuntimeException(
                                "Unable to get existing xml data '" + metadataKey + "'", e);
                    }
                } else {
                    //Add the metdata node and its xml child
                    metadataNode = metadataContainer.addNode(metadataName);
                    xmlNode = metadataNode.addNode(MetadataAware.NODE_ARTIFACTORY_XML);
                }
                if (is == null) {
                    cache.remove(metadataKey);
                    return;
                }
            }
            is.mark(Integer.MAX_VALUE);
            //Import the xml: an xmlNode (with metadataName as its root element) will be created
            //from the input stream with
            //Update the last modified on the specific metadata
            Calendar lastModified = Calendar.getInstance();
            metadataNode.setProperty(MetadataAware.PROP_ARTIFACTORY_LAST_MODIFIED_METADATA,
                    lastModified);
            value.setNewLastModified(lastModified);
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
            resourceNode.setProperty(JCR_MIMETYPE, "text/xml");
            resourceNode.setProperty(JCR_ENCODING, "utf-8");
            resourceNode.setProperty(JCR_LASTMODIFIED, lastModified);
            resourceNode.setProperty(JCR_DATA, is);
            if (updateCache && definition.getXstreamClass() != null) {
                is.reset();
                value.setNewMetadataValue(definitionService.getXstream().fromXML(is));
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Failed to set xml data.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private static MetadataValue createEmptyValue(MetadataKey key) {
        MetadataValue value;
        try {
            value = new MetadataValue(key);
            value.lock();
        } catch (Exception e) {
            throw new RepositoryRuntimeException(
                    "Request to create default persistent metadata for '" + key + "' failed.", e);
        }
        return value;
    }

    private JcrSession getSession() {
        return jcr.getManagedSession();
    }

    private SessionLockManager getLockManager() {
        return getSession().getLockManager();
    }
}
