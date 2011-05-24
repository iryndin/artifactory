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

import org.artifactory.api.search.xml.metadata.GenericMetadataSearchControls;
import org.artifactory.api.search.xml.metadata.GenericMetadataSearchResult;
import org.artifactory.common.Info;
import org.artifactory.search.SearcherBase;

/**
 * @author freds
 * @date Sep 3, 2008
 */
public class MetadataDefinition<T> implements Info {
    /**
     * A Java class that can be marshall/unmarshall this metadata XML stream.
     */
    private final XmlMetadataProvider<T> xmlProvider;
    /**
     * A Java class that can read and save this metadata to the underlying JCR DB.
     */
    private final MetadataPersistenceHandler<T> persistenceHandler;
    /**
     * If true this metadata will not be display as part of metadata names for an fs item.
     */
    private final boolean internal;

    public MetadataDefinition(XmlMetadataProvider<T> xmlProvider,
            MetadataPersistenceHandler<T> persistenceHandler, boolean internal) {
        this.xmlProvider = xmlProvider;
        this.persistenceHandler = persistenceHandler;
        this.internal = internal;
    }

    public XmlMetadataProvider<T> getXmlProvider() {
        return xmlProvider;
    }

    public MetadataPersistenceHandler<T> getPersistenceHandler() {
        return persistenceHandler;
    }

    public String getMetadataName() {
        return xmlProvider.getMetadataName();
    }

    public SearcherBase<GenericMetadataSearchControls<T>, GenericMetadataSearchResult<T>> getSearcher() {
        return persistenceHandler.getSearcher();
    }

    public boolean isInternal() {
        return internal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MetadataDefinition that = (MetadataDefinition) o;
        return getMetadataName().equals(that.getMetadataName());
    }

    @Override
    public int hashCode() {
        return getMetadataName().hashCode();
    }

    @Override
    public String toString() {
        return "MetadataDefinition{" +
                "metadataName='" + getMetadataName() + '\'' +
                ", persistentClass=" + persistenceHandler.getClass().getName() +
                ", xmlProviderClass=" + xmlProvider.getClass().getName() +
                '}';
    }
}
