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

import org.artifactory.api.common.StatusHolder;
import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.repo.Lock;
import org.artifactory.spring.ReloadableBean;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:48:44 PM
 */
public interface MetadataService extends ReloadableBean {
    @Lock(transactional = true)
    List<String> getXmlMetadataNames(MetadataAware obj);

    <MD> MD getXmlMetadataObject(MetadataAware metadataAware, Class<MD> clazz);

    <MD> MD getXmlMetadataObject(MetadataAware metadataAware, Class<MD> clazz, boolean createIfMissing);

    @Lock(transactional = true)
    String getXmlMetadata(MetadataAware metadataAware, String metadataName);

    /**
     * Set raw metadata.
     */
    @Lock(transactional = true)
    String setXmlMetadata(MetadataAware metadataAware, Object xstreamable);

    /**
     * Set raw metadata.
     */
    @Lock(transactional = true)
    void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is, StatusHolder status);

    @Lock(transactional = true)
    void removeXmlMetadata(MetadataAware metadataAware, String metadataName);

    @Lock(transactional = true)
    void writeRawXmlStream(MetadataAware metadataAware, String metadataName, OutputStream out);

    /**
     * Gets MetadataInfo for <i>existing</i> metadata
     */
    @Lock(transactional = true)
    MetadataInfo getMetadataInfo(MetadataAware MetadataAware, String metadataName);

    @Lock(transactional = true)
    boolean hasXmlMetadata(MetadataAware metadataAware, String metadataName);

    @Lock(transactional = true)
    void setXmlMetadata(MetadataAware metadataAware, String metadataName, InputStream is);
}
