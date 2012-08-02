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

package org.artifactory.jcr.md;

import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.MutableMetadataInfo;
import org.artifactory.md.MutablePropertiesInfo;
import org.artifactory.md.Properties;
import org.artifactory.md.PropertiesInfo;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.storage.StorageConstants;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.Calendar;
import java.util.Set;

/**
 * @author freds
 */
public class PropertiesPersistenceHandler
        extends AbstractMetadataPersistenceHandler<PropertiesInfo, MutablePropertiesInfo> {
    private static final Logger log = LoggerFactory.getLogger(PropertiesPersistenceHandler.class);

    public PropertiesPersistenceHandler(XmlMetadataProvider<PropertiesInfo, MutablePropertiesInfo> xmlProvider) {
        super(xmlProvider);
    }

    @Override
    protected String getXml(MetadataAware metadataAware) {
        return getXmlProvider().toXml(read(metadataAware));
    }

    private Node getPropertiesNode(MetadataAware metadataAware, boolean createIfEmpty) {
        Node metadataNode = getMetadataNode(metadataAware, createIfEmpty);
        if (!createIfEmpty && metadataNode == null) {
            return null;
        }
        Node propertiesNode = getJcr().getOrCreateUnstructuredNode(metadataNode,
                StorageConstants.NODE_ARTIFACTORY_PROPERTIES);
        return propertiesNode;
    }

    @Override
    public PropertiesInfo read(MetadataAware metadataAware) {
        Properties properties = (Properties) InfoFactoryHolder.get().createProperties();
        Node propertiesNode = getPropertiesNode(metadataAware, false);
        if (propertiesNode == null) {
            return properties;
        }
        try {
            PropertyIterator storedNodeProps = propertiesNode.getProperties();
            while (storedNodeProps.hasNext()) {
                Property prop = storedNodeProps.nextProperty();
                String key = prop.getName();
                if (ignoreProperty(key)) {
                    // Ignore jcr internal properties
                    continue;
                }
                Value[] values;
                if (prop.getDefinition().isMultiple()) {
                    values = prop.getValues();
                } else {
                    values = new Value[1];
                    values[0] = prop.getValue();
                }
                for (Value value : values) {
                    properties.put(key, value.getString());
                }
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Unexpected error while cleaning up previous properties on '" + metadataAware + "'.", e);
        }
        return properties;
    }

    private boolean ignoreProperty(String key) {
        return key.startsWith("jcr:") || key.startsWith(StorageConstants.ARTIFACTORY_PREFIX);
    }

    @Override
    public void update(MetadataAware metadataAware, MutablePropertiesInfo properties) {
        if (properties == null || properties.isEmpty()) {
            remove(metadataAware);

            return;
        }
        Node propertiesNode = getPropertiesNode(metadataAware, true);
        try {
            PropertyIterator storedNodeProps = propertiesNode.getProperties();
            while (storedNodeProps.hasNext()) {
                Property prop = storedNodeProps.nextProperty();
                if (!ignoreProperty(prop.getName()) && !prop.getDefinition().isProtected()) {
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
        markModified(getMetadataNode(metadataAware, false));
    }

    @Override
    public void remove(MetadataAware metadataAware) {
        try {
            Node metadataNode = getMetadataNode(metadataAware, false);
            if (metadataNode != null) {
                metadataNode.remove();
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to clear node's properties metadata for " + metadataAware, e);
        }
    }

    @Override
    public MutablePropertiesInfo copy(PropertiesInfo original) {
        return InfoFactoryHolder.get().copyProperties(original);
    }

    @Override
    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        Node metadataNode = getMetadataNode(metadataAware, false);
        if (metadataNode == null) {
            return null;
        }
        String metadataName = getMetadataName();
        MutableMetadataInfo mdi = InfoFactoryHolder.get().createMetadata(metadataAware.getRepoPath(), metadataName);
        try {
            Calendar created = metadataNode.getProperty(StorageConstants.PROP_ARTIFACTORY_CREATED).getDate();
            mdi.setCreated(created.getTimeInMillis());
            Calendar lastModified =
                    metadataNode.getProperty(StorageConstants.PROP_ARTIFACTORY_LAST_MODIFIED).getDate();
            mdi.setLastModified(lastModified.getTimeInMillis());
            String lastModifiedBy =
                    metadataNode.getProperty(StorageConstants.PROP_ARTIFACTORY_LAST_MODIFIED_BY).getString();
            mdi.setLastModifiedBy(lastModifiedBy);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot get metadata info " + metadataName + " for " + metadataAware + ".", e);
        }
        return mdi;
    }

}