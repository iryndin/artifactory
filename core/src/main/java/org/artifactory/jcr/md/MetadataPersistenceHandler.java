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

import org.artifactory.api.fs.MetadataInfo;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.search.SearcherBase;

/**
 * For each metadata class the implementation of this interface will provide a way to read and save the data to JCR.
 *
 * @author freds
 */
public interface MetadataPersistenceHandler<T> {
    boolean hasMetadata(MetadataAware metadataAware);

    T read(MetadataAware metadataAware);

    void update(MetadataAware metadataAware, T metadataValue);

    void remove(MetadataAware metadataAware);

    T copy(T original);

    MetadataInfo getMetadataInfo(MetadataAware metadataAware);

    SearcherBase<GenericMetadataSearchControls<T>, GenericMetadataSearchResult<T>> getSearcher();
}
