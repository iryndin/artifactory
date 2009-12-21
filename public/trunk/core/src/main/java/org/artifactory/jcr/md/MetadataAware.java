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

import org.artifactory.api.repo.RepoPath;

import javax.annotation.Nullable;
import javax.jcr.Node;
import java.util.List;

/**
 * User: freds Date: Aug 10, 2008 Time: 3:39:02 PM
 */
public interface MetadataAware {

    /**
     * @return the JCR node that can have metadata
     */
    Node getMetadataContainer();

    /**
     * @return Get the absolute path of the this Metadata Aware item
     */
    String getAbsolutePath();

    RepoPath getRepoPath();

    void importInternalMetadata(MetadataDefinition definition, Object md);

    <MD> MD getXmlMetdataObject(Class<MD> clazz);

    <MD> MD getXmlMetdataObject(Class<MD> clazz, boolean createIfMissing);

    String getXmlMetdata(String metadataName);

    void setXmlMetadata(Object xstreamable);

    /**
     * Sets the specified metadata value. If the value is null, the metadata is removed.
     *
     * @param metadataName The metadata name.
     * @param value        Matedata value. If null the metadata will be deleted.
     */
    void setXmlMetadata(String metadataName, @Nullable String value);

    List<String> getXmlMetadataNames();

    boolean hasXmlMetadata(String metadataName);
}
