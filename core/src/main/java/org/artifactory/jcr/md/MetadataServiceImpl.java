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

package org.artifactory.jcr.md;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.ChecksumInfo;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.maven.MavenNaming;
import org.artifactory.api.md.Properties;
import org.artifactory.api.mime.ChecksumType;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.cache.InternalCacheService;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.io.checksum.Checksum;
import org.artifactory.io.checksum.ChecksumCalculator;
import org.artifactory.jcr.JcrService;
import org.artifactory.jcr.JcrServiceImpl;
import org.artifactory.log.LoggerFactory;
import org.artifactory.repo.service.InternalRepositoryService;
import org.artifactory.spring.InternalContextHelper;
import org.artifactory.spring.Reloadable;
import org.artifactory.spring.ReloadableBean;
import org.artifactory.version.CompoundVersionDetails;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
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

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_DATA;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:27:38 PM
 */
@Service
@Reloadable(beanClass = MetadataService.class,
        initAfter = {MetadataDefinitionService.class, InternalCacheService.class})
public class MetadataServiceImpl implements InternalMetadataService {
    private static final Logger log = LoggerFactory.getLogger(MetadataServiceImpl.class);

    @Autowired
    private MetadataDefinitionService definitionService;

    @Autowired
    private InternalRepositoryService repoService;

    @Autowired
    private JcrService jcr;

    @Autowired
    private AuthorizationService authorizationService;

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

    public void convert(CompoundVersionDetails source, CompoundVersionDetails target) {
    }

    public void saveChecksums(MetadataAware metadataAware, String metadataName, Checksum[] checksums) {
        Node metadataNode = getMetadataNode(metadataAware, metadataName);
        try {
            JcrServiceImpl.setChecksums(metadataNode, checksums, false);
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
                    "Metadata " + definition + " is declared non persistent and cannot be saved!");
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
        //Store properties on an artifactory:properties node under the propertirs metadata node created by
        //setXmlMetadataInternal()
        if (Properties.ROOT.equals(metadataName)) {
            Properties properties = (Properties) xstreamable;
            setProperties(metadataAware, properties);
        }
        return metadataName;
    }

    public void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is) {
        setXmlMetadata(metadataAware, metadataName, is, null);
    }

    //Unprotected data - called from import (among others)

    public void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is, StatusHolder status) {
        //We need to verify the data requested to be saved, since we have no control over it
        MetadataDefinition definition = definitionService.getMetadataDefinition(metadataName);
        if (definition.isInternal()) {
            //Import fs item metadata directly into the fs item state
            try {
                metadataAware.importInternalMetadata(definition, definitionService.getXstream().fromXML(is));
            } finally {
                IOUtils.closeQuietly(is);
            }
        } else {
            if (definition.isPersistent()) {
                try {
                    if (Properties.ROOT.equals(metadataName)) {
                        if (is.markSupported()) {
                            is.mark(Integer.MAX_VALUE);
                        }
                        Properties properties = (Properties) definitionService.getXstream().fromXML(is);
                        setProperties(metadataAware, properties);
                        if (is.markSupported()) {
                            is.reset();
                        }
                    }
                    setXmlMetadataInternal(metadataAware, metadataName, is);
                } catch (IOException e) {
                    IOUtils.closeQuietly(is);
                    status.setWarning("Unable to reset metadata input stream.", e, log);
                }
            } else if (status != null) {
                IOUtils.closeQuietly(is);
                status.setWarning(
                        "Read metadata file " + metadataName + " for item " + metadataAware.getAbsolutePath() +
                                " was declared non persistent - data will be discarded!", log);
            }
        }
    }

    /*private Properties getProperties(MetadataAware metadataAware) {
        Properties properties = new Properties();
        Node metadataNode = getMetadataNode(metadataAware, Properties.ROOT);
        try {
            PropertyIterator storedNodeProps = metadataNode.getProperties();
            while (storedNodeProps.hasNext()) {
                Property prop = storedNodeProps.nextProperty();
                String key = prop.getName();
                String value = prop.getString();
                properties.put(key, value);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Unexpected error while getting properties for '" + metadataAware + "'.", e);
        }
        return properties;
    }*/

    public void writeRawXmlStream(MetadataAware metadataAware, String metadataName, OutputStream out) {
        InputStream is = null;
        try {
            //Read the raw xml
            Node metadataNode = getMetadataNode(metadataAware, metadataName);
            is = JcrServiceImpl.getRawXmlStream(metadataNode);
            BufferedInputStream bis = new BufferedInputStream(is);
            //Write it to the out stream
            IOUtils.copy(bis, out);
        } catch (Exception e) {
            throw new RepositoryRuntimeException("Failed to read xml metadata stream.", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void removeXmlMetadata(MetadataAware metadataAware, String metadataName) {
        //Remove xml
        setXmlMetadataInternal(metadataAware, metadataName, null);
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

    public Set<ChecksumInfo> getOrCreateChecksums(MetadataAware metadataAware, String metadataName)
            throws RepositoryException {
        Set<ChecksumInfo> checksumInfos = new HashSet<ChecksumInfo>();
        Node metadataNode = getMetadataNode(metadataAware, metadataName);
        ChecksumType[] checksumTypes = ChecksumType.values();
        for (ChecksumType checksumType : checksumTypes) {
            String propName = JcrServiceImpl.getChecksumPropertyName(checksumType);
            if (metadataNode.hasProperty(propName)) {
                Property property = metadataNode.getProperty(propName);
                String checksumValue = property.getString();
                checksumInfos.add(new ChecksumInfo(checksumType, checksumValue, checksumValue));
                log.debug("Found metadata checksum '{}' for '{}#{}'",
                        new Object[]{checksumType, metadataAware, metadataName});
            }
        }
        if (checksumInfos.isEmpty()) {
            //No checksums found - need to create them
            Checksum[] checksums;
            InputStream is = JcrServiceImpl.getRawXmlStream(metadataNode);
            try {
                checksums = ChecksumCalculator.calculateAll(is);
            } catch (IOException e) {
                throw new RepositoryRuntimeException(String.format(
                        "Failed to compute metadata checksums for '%s$#%s$'", metadataAware, metadataName));
            } finally {
                IOUtils.closeQuietly(is);
            }
            getTransactionalMe().saveChecksums(metadataAware, metadataName, checksums);
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
            Calendar created = metadataNode.getProperty(JcrService.PROP_ARTIFACTORY_CREATED).getDate();
            Node resourceNode = getResourceNode(metadataAware, metadataName);
            mdi.setCreated(created.getTimeInMillis());
            Calendar lastModified =
                    metadataNode.getProperty(JcrService.PROP_ARTIFACTORY_LAST_MODIFIED).getDate();
            mdi.setLastModified(lastModified.getTimeInMillis());
            String lastModifiedBy =
                    metadataNode.getProperty(JcrService.PROP_ARTIFACTORY_LAST_MODIFIED_BY).getString();
            mdi.setLastModifiedBy(lastModifiedBy);
            mdi.setLastModified(lastModified.getTimeInMillis());
            Set<ChecksumInfo> checksums = getOrCreateChecksums(metadataAware, metadataName);
            mdi.setChecksums(checksums);
            mdi.setSize(resourceNode.getProperty(JCR_DATA).getLength());
        } catch (RepositoryException e) {
            throw new IllegalArgumentException(
                    "Cannot get metadata info " + metadataName + " for " + metadataAware + ".", e);
        }
        return mdi;
    }

    private void setProperties(MetadataAware metadataAware, Properties properties) {
        Node propertiesNode = getPropertiesNode(metadataAware);
        try {
            PropertyIterator storedNodeProps = propertiesNode.getProperties();
            while (storedNodeProps.hasNext()) {
                Property prop = storedNodeProps.nextProperty();
                if (!prop.getDefinition().isProtected()) {
                    prop.remove();
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Unexpected error while cleaning up previous properties on '" + metadataAware + "'.", e);
        }
        Set<String> keys = properties.keySet();
        for (String key : keys) {
            Set<String> valueSet = properties.get(key);
            String[] valuesArray = valueSet.toArray(new String[valueSet.size()]);
            try {
                log.debug("Setting property '{}' to '{}' on '{}'.", new Object[]{key, valuesArray, metadataAware});
                propertiesNode.setProperty(key, valuesArray);
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException(
                        "Unexpected error while setting property '" + key + "' on '" + metadataAware + "'.", e);
            }
        }
    }

    public boolean hasXmlMetadata(MetadataAware metadataAware, String metadataName) {
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

    private Node getPropertiesNode(MetadataAware metadataAware) {
        Node metadataNode = getMetadataNode(metadataAware, Properties.ROOT);
        Node propertiesNode = jcr.getOrCreateUnstructuredNode(metadataNode, JcrService.NODE_ARTIFACTORY_PROPERTIES);
        return propertiesNode;
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

    private void setXmlMetadataInternal(MetadataAware metadataAware, String metadataName, InputStream is) {
        String xml = null;
        if (is != null) {
            try {
                xml = IOUtils.toString(is, "utf-8");
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Failed to read xml data from stream.", e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
        boolean importXmlDocument = shouldImportXmlDocument(metadataName);
        jcr.setXml(metadataAware.getMetadataContainer(), metadataName, xml, importXmlDocument,
                authorizationService.currentUsername());
    }

    /**
     * Do not import a jcr xml document for maven metadata or properties
     */
    private boolean shouldImportXmlDocument(String metadataName) {
        return !MavenNaming.MAVEN_METADATA_NAME.equals(metadataName) && !Properties.ROOT.equals(metadataName);
    }

    private InternalMetadataService getTransactionalMe() {
        return InternalContextHelper.get().beanForType(InternalMetadataService.class);
    }
}
