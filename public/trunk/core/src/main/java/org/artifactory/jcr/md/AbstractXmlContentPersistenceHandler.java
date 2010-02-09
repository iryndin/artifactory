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

package org.artifactory.jcr.md;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.mime.ContentType;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.jcr.JcrTypes;
import org.artifactory.repo.jcr.JcrHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * @author freds
 */
public abstract class AbstractXmlContentPersistenceHandler<T> extends AbstractMetadataPersistenceHandler<T> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final Logger log = LoggerFactory.getLogger(AbstractXmlContentPersistenceHandler.class);

    protected AbstractXmlContentPersistenceHandler(XmlMetadataProvider<T> xmlProvider) {
        super(xmlProvider);
    }

    protected abstract boolean saveXmlHierarchy();

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
        getJcr().setString(getOrCreateMetadataContainer(metadataAware),
                getMetadataName(),
                xmlData,
                ContentType.applicationXml.getMimeType(),
                getAuthorizationService().currentUsername(),
                saveXmlHierarchy());
    }

    public MetadataInfo getMetadataInfo(MetadataAware metadataAware) {
        Node metadataNode = getMetadataNode(metadataAware, false);
        if (metadataNode == null) {
            return null;
        }
        String metadataName = getMetadataName();
        MetadataInfo mdi = new MetadataInfo(metadataAware.getRepoPath(), metadataName);
        try {
            Calendar created = metadataNode.getProperty(JcrTypes.PROP_ARTIFACTORY_CREATED).getDate();
            mdi.setCreated(created.getTimeInMillis());
            Calendar lastModified =
                    metadataNode.getProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED).getDate();
            mdi.setLastModified(lastModified.getTimeInMillis());
            String lastModifiedBy =
                    metadataNode.getProperty(JcrTypes.PROP_ARTIFACTORY_LAST_MODIFIED_BY).getString();
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
        return getXmlProvider().fromXml(xmlData);
    }

    public void update(MetadataAware metadataAware, T metadataValue) {
        String s = getXmlProvider().toXml(metadataValue);
        setXml(metadataAware, s);
    }

    public void remove(MetadataAware metadataAware) {
        removeXml(metadataAware);
    }
}
