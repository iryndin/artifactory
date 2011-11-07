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

package org.artifactory.jcr.md;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.artifactory.factory.InfoFactoryHolder;
import org.artifactory.jcr.utils.JcrHelper;
import org.artifactory.log.LoggerFactory;
import org.artifactory.md.MetadataInfo;
import org.artifactory.md.MutableMetadataInfo;
import org.artifactory.mime.MimeType;
import org.artifactory.sapi.common.RepositoryRuntimeException;
import org.artifactory.storage.StorageConstants;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * @author freds
 */
public abstract class AbstractXmlContentPersistenceHandler<T, MT> extends AbstractMetadataPersistenceHandler<T, MT> {
    private static final Logger log = LoggerFactory.getLogger(AbstractXmlContentPersistenceHandler.class);

    protected AbstractXmlContentPersistenceHandler(XmlMetadataProvider<T, MT> xmlProvider) {
        super(xmlProvider);
    }

    protected abstract boolean shouldSaveXmlHierarchy();

    @Override
    protected String getXml(MetadataAware metadataAware) {
        Node metadataNode = getMetadataNode(metadataAware, false);
        if (metadataNode == null) {
            return null;
        }
        InputStream is = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            //Read the raw xml
            is = JcrHelper.getRawStringStream(metadataNode);
            BufferedInputStream bis = new BufferedInputStream(is);
            //Write it to the out stream
            IOUtils.copy(bis, os);
            return os.toString("utf-8");
        } catch (IOException e) {
            throw new RepositoryRuntimeException(
                    "Failed to read xml data '" + getMetadataName() + "' of " + metadataAware,
                    e);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException(
                    "Failed to read JCR DB for data '" + getMetadataName() + "' of " + metadataAware, e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    protected void setXml(MetadataAware metadataAware, String xmlData) {
        Node metadataNode = getOrCreateMetadataContainer(metadataAware);
        getJcr().setString(metadataNode,
                getMetadataName(),
                xmlData,
                MimeType.applicationXml,
                getAuthorizationService().currentUsername(),
                shouldSaveXmlHierarchy());
        markModified(metadataNode);
    }

    public boolean markModified(MetadataAware metadataAware, Calendar modified) {
        Node metadataNode = getMetadataNode(metadataAware, false);
        if (metadataNode == null) {
            log.warn("Metadata node doesn't exist: {}", metadataAware);
            return false;
        }
        super.markModified(metadataNode, modified);
        return true;
    }

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

            fillContentInfo(metadataAware, mdi);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot get metadata info " + metadataName + " for " + metadataAware + ".", e);
        }
        return mdi;
    }

    protected void removeXml(MetadataAware metadataAware) {
        setXml(metadataAware, null);
    }

    public T read(MetadataAware metadataAware) {
        String xmlData = getXml(metadataAware);
        if (xmlData == null) {
            return null;
        }
        return (T) getXmlProvider().fromXml(xmlData);
    }

    public void update(MetadataAware metadataAware, MT metadataValue) {
        String s = getXmlProvider().toXml((T) metadataValue);
        setXml(metadataAware, s);
    }

    public void remove(MetadataAware metadataAware) {
        removeXml(metadataAware);
    }
}
